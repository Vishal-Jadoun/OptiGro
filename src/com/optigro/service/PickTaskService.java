package com.optigro.service;

import com.optigro.constants.OrderStatus;
import com.optigro.constants.TaskStatus;
import com.optigro.models.*;
import com.optigro.repository.PickTaskRepository;
import com.optigro.repository.PickerRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PickTaskService {
    private final PickTaskRepository taskRepo;
    private final PickerRepository pickerRepo;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PickRoutePlanner routePlanner;
    private final SubstitutionService substitutionService;
    private final PriorityBlockingQueue<PickTask> taskQueue = new PriorityBlockingQueue<>();

    private ExecutorService executor;
    private Consumer<Order> onOrderPacked; 

    public PickTaskService(PickTaskRepository taskRepo, PickerRepository pickerRepo,
                           OrderService orderService, InventoryService inventoryService,
                           PickRoutePlanner routePlanner, SubstitutionService substitutionService) {
        this.taskRepo = taskRepo;
        this.pickerRepo = pickerRepo;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.routePlanner = routePlanner;
        this.substitutionService = substitutionService;
    }

    public void setOnOrderPacked(Consumer<Order> listener) {
        this.onOrderPacked = listener;
    }

    public PickTask createTask(Order order) {
        int taskId = taskRepo.nextId();
        int priority = (order.getTotal() >= 500) ? 1 : 5; 
        PickTask task = new PickTask(taskId, order.getOrderId(), priority);
        taskRepo.save(task);

        taskQueue.offer(task);
        orderService.updateStatus(order.getOrderId(), OrderStatus.PICKING);
        System.out.printf("[PickEngine] Task #%d enqueued for Order #%d (Priority: %s)%n",
            taskId, order.getOrderId(), (priority == 1 ? "EXPRESS" : "STANDARD"));
        return task;
    }

   
    public PickTask takeNextTask() throws InterruptedException {
        return taskQueue.take();
    }

   
    public void processTask(Picker picker, PickTask task) {
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPickerId(picker.getPickerId());
        picker.setAvailable(false);

        Order order = orderService.getOrder(task.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Order missing for task: " + task.getTaskId()));

        System.out.printf("%n[Picker %s] Started Task #%d for Order #%d (%d items)%n",
            picker.getName(), task.getTaskId(), order.getOrderId(), order.getItems().size());

        
        List<OrderItem> route = routePlanner.planRoute(order.getItems());

        for (OrderItem item : route) {
            Bin bin = routePlanner.getBinLayoutService().locate(item.getProductId());
            System.out.printf("  -> [Picker %s] Walking to Aisle %d, Shelf %d for '%s' (Qty: %d)...%n",
                picker.getName(), bin.getAisle(), bin.getShelf(), item.getProductName(), item.getQuantity());

            
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

           
            if (inventoryService.isAvailable(item.getProductId(), item.getQuantity())) {
                task.markItemPicked(item.getProductId()); 
                System.out.printf("  ✓ [Picker %s] Picked %dx %s%n",
                    picker.getName(), item.getQuantity(), item.getProductName());
            } else {
                
                System.out.printf("  ⚠ [Picker %s] OOS for %s! Finding substitute...%n",
                    picker.getName(), item.getProductName());

                Product original = inventoryService.getProduct(item.getProductId()).orElseThrow();
                Optional<Product> substitute = substitutionService.suggestSubstitute(original, item.getQuantity());

                if (substitute.isPresent()) {
                    Product subProd = substitute.get();
                    task.addSubstitution(new Substitution(
                        original.getId(), subProd.getId(), original.getName(), subProd.getName(), true));
                    task.markItemPicked(subProd.getId()); 

                    try {
                        inventoryService.reserveStock(subProd.getId(), item.getQuantity());
                        System.out.printf("  ✓ [Picker %s] Substituted with %s (ID: %d)!%n",
                            picker.getName(), subProd.getName(), subProd.getId());
                    } catch (Exception ex) {
                        System.out.printf("  ✗ [Picker %s] Substitute reserve failed.%n", picker.getName());
                    }
                } else {
                    System.out.printf("  ✗ [Picker %s] No suitable substitute found for %s.%n",
                        picker.getName(), item.getProductName());
                }
            }
        }


        task.setStatus(TaskStatus.COMPLETED);
        picker.setAvailable(true);
        picker.incrementTasksCompleted();
        orderService.updateStatus(order.getOrderId(), OrderStatus.PACKED);

        System.out.printf("[PickEngine] ✓ Order #%d is PACKED by %s! Total items picked: %d%n",
            order.getOrderId(), picker.getName(), task.getPickedProductIds().size());

        
        try {
            taskRepo.appendTaskHistoryToCsv(task, "data/pick_history.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Task complete but failed to log to disk: " + e.getMessage());
        }

        
        if (onOrderPacked != null) {
            onOrderPacked.accept(order);
        }
    }

    public synchronized void startPickers(int count) {
        List<Picker> available = pickerRepo.findAvailable();
        int threads = Math.min(count, Math.max(1, available.size()));
        executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            Picker p = available.get(i % available.size());
            executor.submit(new PickerWorker(p, this));
        }
        System.out.printf("[PickEngine] Started %d background picker worker threads!%n", threads);
    }

    public synchronized void stopPickers() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            System.out.println("[PickEngine] All background picker threads safely stopped.");
        }
    }

    public String generatePickerReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== PICKER PERFORMANCE & LOGISTICS REPORT =====\n");
        sb.append(String.format("%-6s %-18s %-12s %-12s%n", "ID", "Name", "Status", "Tasks Done"));
        sb.append("-".repeat(55)).append("\n");
        for (Picker p : pickerRepo.findAll()) {
            sb.append(String.format("%-6d %-18s %-12s %-12d%n",
                p.getPickerId(), p.getName(), p.isAvailable() ? "ONLINE" : "BUSY", p.getTasksCompleted()));
        }
        sb.append("\nTasks in Priority Queue: ").append(taskQueue.size());
        sb.append("\nTotal Tasks Processed: ").append(taskRepo.findAll().size());
        return sb.toString();
    }
}