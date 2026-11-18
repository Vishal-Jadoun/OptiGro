# 👤 Member 3 (M3): Picking Engine — The Concurrency Showpiece

> **Role:** Picking Engine (Multithreading & Concurrency)  
> **Branch:** `m3/picking-engine`  
> **Files Owned:** 13 Java files + `data/pickers.csv` & `data/pick_history.csv`  
> **GitHub:** [Vishal-Jadoun/OptiGro](https://github.com/Vishal-Jadoun/OptiGro.git)

---

## 📌 Overview

You are building the **most technically impressive** slice of the project — the multi-threaded warehouse picking engine. When a customer places an order, your system:
1. Creates a `PickTask` and enqueues it into a thread-safe `PriorityBlockingQueue`.
2. Multiple picker worker threads (`PickerWorker` implementing `Runnable`), managed via `ExecutorService`, concurrently grab tasks from the queue.
3. Each worker thread navigates warehouse aisles using an optimized **snake-route algorithm** (`PickRoutePlanner` using a custom `Comparator`).
4. Uses a `Set<Integer>` (`pickedProductIds`) to track picked items, mathematically guaranteeing no double-picking across threads.
5. If an item is out of stock, your `SubstitutionService` automatically finds and substitutes a similar in-stock product.
6. Once packed, updates order status to `PACKED`, appends an audit record to `data/pick_history.csv`, and fires a callback to Aditya's (M4) delivery service.
7. You also own **`DataInitializer.java`**, the master bootstrapper that loads all CSV data files on application startup.

---

## 📁 Your Complete File & Data Structure

```text
src/
└── com/
    └── optigro/
        ├── constants/
        │   └── TaskStatus.java                ← Enum: PENDING, IN_PROGRESS, COMPLETED, FAILED
        ├── models/
        │   ├── Picker.java                    ← Picker profile (equals/hashCode by pickerId)
        │   ├── PickTask.java                  ← implements Comparable<PickTask>, holds Set<Integer> pickedProductIds (equals/hashCode)
        │   └── Substitution.java              ← Records original→substitute product swap (equals/hashCode)
        ├── repository/
        │   ├── PickerRepository.java          ← implements BaseRepository<Picker> + CSV loader (BufferedReader)
        │   └── PickTaskRepository.java        ← implements BaseRepository<PickTask> + CSV task history logger (BufferedWriter)
        ├── service/
        │   ├── PickTaskService.java           ← PriorityBlockingQueue, task lifecycle, thread management, callbacks
        │   ├── SubstitutionService.java       ← Finds nearest priced substitute in same category
        │   ├── PickRoutePlanner.java          ← Snake-route aisle sorting with custom Comparator
        │   └── PickerWorker.java              ← implements Runnable, independent worker loop per picker thread
        ├── controller/
        │   └── PickingMenu.java               ← Picker terminal menu (view tasks, manual pick, warehouse grid)
        ├── exception/
        │   └── NoPickerAvailableException.java ← Thrown when task cannot find active picker
        └── config/
            └── DataInitializer.java            ← YOU OWN THIS FILE (master bootstrapper coordinating all CSV loading)

data/
├── pickers.csv                                ← Warehouse picker seed profiles & status
└── pick_history.csv                           ← Persistent append log of completed pick tasks & substitutions
```

---

## 🧵 Thread Architecture & Concurrency Model

```text
Main / Admin Thread
  │
  ├── App.main() → DataInitializer.initialize() [loads all CSVs] → MainMenuController.start()
  │
  │   [Customer places Order OR Admin triggers "Run Live Simulation"]
  │   │
  │   ├── OrderService.placeOrder()
  │   │       │
  │   │       └── onOrderPlaced callback fires
  │   │               │
  │   │               ▼
  │   │       PickTaskService.createTask(order)
  │   │               │
  │   │               ▼
  │   │       PriorityBlockingQueue<PickTask>
  │   │       [ Express Task (p=1) | Normal Task (p=5, t=10:01) | Normal Task (p=5, t=10:02) ]
  │   │
  │   └── PickTaskService.startPickers(3)
  │           │
  │           ├── ExecutorService (FixedThreadPool of 3 threads)
  │           │   ├── Thread-1 (PickerWorker: Amit)   ──► take() [blocks] ──► processTask() ──► loop
  │           │   ├── Thread-2 (PickerWorker: Suresh) ──► take() [blocks] ──► processTask() ──► loop
  │           │   └── Thread-3 (PickerWorker: Neha)   ──► take() [blocks] ──► processTask() ──► loop
  │           │
  │           └── [Inside processTask: item marked in Set<Integer> → order marked PACKED]
  │                     │
  │                     └── onOrderPacked callback fires ──► M4 DeliveryService.dispatch(order)
```

---

## 🔧 Detailed File-by-File Work Breakdown

### 1. `TaskStatus.java` — Enum
**Path:** `src/com/optigro/constants/TaskStatus.java`

```java
package com.optigro.constants;

public enum TaskStatus {
    PENDING,        // Task created, waiting in PriorityBlockingQueue
    IN_PROGRESS,    // Worker thread dequeued task and is walking aisles
    COMPLETED,      // All items picked/substituted and order packed
    FAILED          // Task could not be completed
}
```

---

### 2. `Picker.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Picker.java`

**Fields (all `private`):**
* `pickerId` (int) — Unique identifier.
* `userId` (int) — Links to M4's `User` login credentials.
* `name` (String) — Picker's full name.
* `available` (boolean) — Currently available to take tasks.
* `tasksCompleted` (int) — Total tasks fulfilled in this shift.

```java
package com.optigro.models;

import java.util.Objects;

public class Picker {
    private final int pickerId;
    private final int userId;
    private final String name;
    private boolean available;
    private int tasksCompleted;

    public Picker(int pickerId, int userId, String name) {
        this.pickerId = pickerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.available = true;
        this.tasksCompleted = 0;
    }

    public int getPickerId() { return pickerId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getTasksCompleted() { return tasksCompleted; }

    public void incrementTasksCompleted() {
        this.tasksCompleted++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Picker)) return false;
        Picker picker = (Picker) o;
        return pickerId == picker.pickerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pickerId);
    }

    @Override
    public String toString() {
        return String.format("Picker #%d: %-15s | Status: %-10s | Completed: %d",
            pickerId, name, available ? "AVAILABLE" : "BUSY", tasksCompleted);
    }
}
```

---

### 3. `PickTask.java` — Model (implements `Comparable<PickTask>` + `Set`)
**Path:** `src/com/optigro/models/PickTask.java`

**Fields:** `taskId`, `orderId`, `pickerId`, `status` (`TaskStatus`), `createdAt` (`LocalDateTime`), `priority` (`int`), `substitutions` (`List<Substitution>`), `pickedProductIds` (`Set<Integer>`).

**Critical Implementation Details:**
1. **`implements Comparable<PickTask>`**: Priority queue naturally sorts by priority first (lower number = express order), then by creation timestamp FIFO.
2. **`Set<Integer> pickedProductIds`**: `HashSet<Integer>` tracking picked items. Guarantees no double-picking across threads.
3. **`equals()` & `hashCode()`**: Identity defined strictly by `taskId`.

```java
package com.optigro.models;

import com.optigro.constants.TaskStatus;

import java.time.LocalDateTime;
import java.util.*;

public class PickTask implements Comparable<PickTask> {
    private final int taskId;
    private final int orderId;
    private int pickerId;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private final int priority; // 1 = Express (high priority), 5 = Standard
    private final List<Substitution> substitutions;
    private final Set<Integer> pickedProductIds;

    public PickTask(int taskId, int orderId, int priority) {
        this.taskId = taskId;
        this.orderId = orderId;
        this.pickerId = -1; // Unassigned
        this.status = TaskStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.priority = priority;
        this.substitutions = new ArrayList<>();
        this.pickedProductIds = new HashSet<>();
    }

    public int getTaskId() { return taskId; }
    public int getOrderId() { return orderId; }
    public int getPickerId() { return pickerId; }
    public void setPickerId(int pickerId) { this.pickerId = pickerId; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getPriority() { return priority; }
    public List<Substitution> getSubstitutions() { return Collections.unmodifiableList(substitutions); }

    public void addSubstitution(Substitution sub) {
        substitutions.add(sub);
    }

    // Set / HashSet demonstration
    public synchronized boolean markItemPicked(int productId) {
        return pickedProductIds.add(productId); // returns false if already in Set
    }

    public synchronized boolean isItemPicked(int productId) {
        return pickedProductIds.contains(productId);
    }

    public Set<Integer> getPickedProductIds() {
        return Collections.unmodifiableSet(pickedProductIds);
    }

    // Natural ordering for PriorityBlockingQueue
    @Override
    public int compareTo(PickTask other) {
        int pCompare = Integer.compare(this.priority, other.priority);
        if (pCompare != 0) return pCompare; // Lower priority number comes first
        return this.createdAt.compareTo(other.createdAt); // FIFO for identical priority
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PickTask)) return false;
        PickTask pickTask = (PickTask) o;
        return taskId == pickTask.taskId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return String.format("Task #%d | Order #%d | Picker: %d | Status: %s | Priority: %d | Picked Items: %d",
            taskId, orderId, pickerId, status, priority, pickedProductIds.size());
    }
}
```

---

### 4. `Substitution.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Substitution.java`

```java
package com.optigro.models;

import java.util.Objects;

public class Substitution {
    private final int originalProductId;
    private final int substituteProductId;
    private final String originalName;
    private final String substituteName;
    private final boolean accepted;

    public Substitution(int originalProductId, int substituteProductId,
                        String originalName, String substituteName, boolean accepted) {
        this.originalProductId = originalProductId;
        this.substituteProductId = substituteProductId;
        this.originalName = originalName;
        this.substituteName = substituteName;
        this.accepted = accepted;
    }

    public int getOriginalProductId() { return originalProductId; }
    public int getSubstituteProductId() { return substituteProductId; }
    public String getOriginalName() { return originalName; }
    public String getSubstituteName() { return substituteName; }
    public boolean isAccepted() { return accepted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Substitution)) return false;
        Substitution that = (Substitution) o;
        return originalProductId == that.originalProductId &&
               substituteProductId == that.substituteProductId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalProductId, substituteProductId);
    }

    @Override
    public String toString() {
        return String.format("Substitution: %s (ID: %d) -> %s (ID: %d) [%s]",
            originalName, originalProductId, substituteName, substituteProductId,
            accepted ? "ACCEPTED" : "DECLINED");
    }
}
```

---

### 5. `PickerRepository.java` — Repository with CSV Persistence
**Path:** `src/com/optigro/repository/PickerRepository.java`

```java
package com.optigro.repository;

import com.optigro.models.Picker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PickerRepository implements BaseRepository<Picker> {
    private final ConcurrentHashMap<Integer, Picker> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(3000);

    @Override
    public void save(Picker picker) {
        store.put(picker.getPickerId(), picker);
    }

    @Override
    public Optional<Picker> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Picker> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean deleteById(int id) {
        return store.remove(id) != null;
    }

    @Override
    public int nextId() {
        return counter.incrementAndGet();
    }

    public List<Picker> findAvailable() {
        return store.values().stream()
            .filter(Picker::isAvailable)
            .collect(Collectors.toList());
    }

    public Optional<Picker> findByUserId(int userId) {
        return store.values().stream()
            .filter(p -> p.getUserId() == userId)
            .findFirst();
    }

    // ==========================================
    // File Persistence: Read data/pickers.csv
    // ==========================================
    public void loadFromCsv(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                int id = Integer.parseInt(parts[0].trim());
                int userId = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                boolean available = Boolean.parseBoolean(parts[3].trim());
                int tasks = Integer.parseInt(parts[4].trim());

                Picker picker = new Picker(id, userId, name);
                picker.setAvailable(available);
                for (int i = 0; i < tasks; i++) picker.incrementTasksCompleted();

                save(picker);
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
```

---

### 6. `PickTaskRepository.java` — Repository with CSV Persistence (Append)
**Path:** `src/com/optigro/repository/PickTaskRepository.java`

```java
package com.optigro.repository;

import com.optigro.constants.TaskStatus;
import com.optigro.models.PickTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PickTaskRepository implements BaseRepository<PickTask> {
    private final ConcurrentHashMap<Integer, PickTask> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(4000);

    @Override
    public void save(PickTask task) {
        store.put(task.getTaskId(), task);
    }

    @Override
    public Optional<PickTask> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PickTask> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean deleteById(int id) {
        return store.remove(id) != null;
    }

    @Override
    public int nextId() {
        return counter.incrementAndGet();
    }

    public List<PickTask> findByPickerId(int pickerId) {
        return store.values().stream()
            .filter(t -> t.getPickerId() == pickerId)
            .collect(Collectors.toList());
    }

    public List<PickTask> findByStatus(TaskStatus status) {
        return store.values().stream()
            .filter(t -> t.getStatus() == status)
            .collect(Collectors.toList());
    }

    // ==========================================
    // File Persistence: Append data/pick_history.csv
    // ==========================================
    public synchronized void appendTaskHistoryToCsv(PickTask task, String filePath) throws IOException {
        File file = new File(filePath);
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# taskId,orderId,pickerId,status,priority,substitutionsCount,createdAt\n");
            }
            writer.write(String.format("%d,%d,%d,%s,%d,%d,%s%n",
                task.getTaskId(), task.getOrderId(), task.getPickerId(),
                task.getStatus().name(), task.getPriority(),
                task.getSubstitutions().size(), task.getCreatedAt()));
        }
    }
}
```

---

### 7. `PickTaskService.java` — Core Concurrency & Task Engine ⚡
**Path:** `src/com/optigro/service/PickTaskService.java`

**This is your central engine.**

```java
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
    private Consumer<Order> onOrderPacked; // Decoupled callback to M4

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
        int priority = (order.getTotal() >= 500) ? 1 : 5; // Express priority on orders >= Rs.500
        PickTask task = new PickTask(taskId, order.getOrderId(), priority);
        taskRepo.save(task);

        taskQueue.offer(task);
        orderService.updateStatus(order.getOrderId(), OrderStatus.PICKING);
        System.out.printf("[PickEngine] Task #%d enqueued for Order #%d (Priority: %s)%n",
            taskId, order.getOrderId(), (priority == 1 ? "EXPRESS" : "STANDARD"));
        return task;
    }

    // Thread-blocking dequeue method
    public PickTask takeNextTask() throws InterruptedException {
        return taskQueue.take();
    }

    // ========================================================
    // Full Process Task Walkthrough (Snake Route + Set + Subs)
    // ========================================================
    public void processTask(Picker picker, PickTask task) {
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPickerId(picker.getPickerId());
        picker.setAvailable(false);

        Order order = orderService.getOrder(task.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Order missing for task: " + task.getTaskId()));

        System.out.printf("%n[Picker %s] Started Task #%d for Order #%d (%d items)%n",
            picker.getName(), task.getTaskId(), order.getOrderId(), order.getItems().size());

        // 1. Sort items via custom Comparator snake-route
        List<OrderItem> route = routePlanner.planRoute(order.getItems());

        for (OrderItem item : route) {
            Bin bin = routePlanner.getBinLayoutService().locate(item.getProductId());
            System.out.printf("  -> [Picker %s] Walking to Aisle %d, Shelf %d for '%s' (Qty: %d)...%n",
                picker.getName(), bin.getAisle(), bin.getShelf(), item.getProductName(), item.getQuantity());

            // Simulate walking time in terminal demo
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Check availability
            if (inventoryService.isAvailable(item.getProductId(), item.getQuantity())) {
                task.markItemPicked(item.getProductId()); // Added to Set<Integer>
                System.out.printf("  ✓ [Picker %s] Picked %dx %s%n",
                    picker.getName(), item.getQuantity(), item.getProductName());
            } else {
                // Out of stock -> Smart substitution
                System.out.printf("  ⚠ [Picker %s] OOS for %s! Finding substitute...%n",
                    picker.getName(), item.getProductName());

                Product original = inventoryService.getProduct(item.getProductId()).orElseThrow();
                Optional<Product> substitute = substitutionService.suggestSubstitute(original, item.getQuantity());

                if (substitute.isPresent()) {
                    Product subProd = substitute.get();
                    task.addSubstitution(new Substitution(
                        original.getId(), subProd.getId(), original.getName(), subProd.getName(), true));
                    task.markItemPicked(subProd.getId()); // Marked in Set

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

        // Complete Task
        task.setStatus(TaskStatus.COMPLETED);
        picker.setAvailable(true);
        picker.incrementTasksCompleted();
        orderService.updateStatus(order.getOrderId(), OrderStatus.PACKED);

        System.out.printf("[PickEngine] ✓ Order #%d is PACKED by %s! Total items picked: %d%n",
            order.getOrderId(), picker.getName(), task.getPickedProductIds().size());

        // File Persistence: Append task to data/pick_history.csv
        try {
            taskRepo.appendTaskHistoryToCsv(task, "data/pick_history.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Task complete but failed to log to disk: " + e.getMessage());
        }

        // Fire callback to M4 DeliveryService
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
            executor.shutdownNow(); // Interrupts waiting take()
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
```

---

### 8. `SubstitutionService.java` — Smart Substitutions
**Path:** `src/com/optigro/service/SubstitutionService.java`

```java
package com.optigro.service;

import com.optigro.models.Product;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SubstitutionService {
    private final InventoryService inventoryService;

    public SubstitutionService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Finds product in same category within ±25% price with closest price match
    public Optional<Product> suggestSubstitute(Product outOfStockProduct, int qty) {
        List<Product> similar = inventoryService.findSimilar(outOfStockProduct);

        return similar.stream()
            .filter(p -> p.getStock() >= qty)
            .min(Comparator.comparingDouble(p -> Math.abs(p.getPrice() - outOfStockProduct.getPrice())));
    }
}
```

---

### 9. `PickRoutePlanner.java` — Snake Route Sorting (`Comparator`)
**Path:** `src/com/optigro/service/PickRoutePlanner.java`

```java
package com.optigro.service;

import com.optigro.models.Bin;
import com.optigro.models.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PickRoutePlanner {
    private final BinLayoutService binLayoutService;

    public PickRoutePlanner(BinLayoutService binLayoutService) {
        this.binLayoutService = binLayoutService;
    }

    public BinLayoutService getBinLayoutService() {
        return binLayoutService;
    }

    // Demonstrates custom Comparator: Snake route traversal
    public List<OrderItem> planRoute(List<OrderItem> items) {
        return items.stream()
            .sorted((a, b) -> {
                Bin binA = binLayoutService.locate(a.getProductId());
                Bin binB = binLayoutService.locate(b.getProductId());

                // Primary sort: Aisle number ascending
                int aisleCompare = Integer.compare(binA.getAisle(), binB.getAisle());
                if (aisleCompare != 0) return aisleCompare;

                // Secondary sort: Parity direction
                // Even aisles (0, 2): low shelf -> high shelf
                // Odd aisles (1, 3): high shelf -> low shelf (minimizes backtracking)
                if (binA.getAisle() % 2 == 0) {
                    return Integer.compare(binA.getShelf(), binB.getShelf());
                } else {
                    return Integer.compare(binB.getShelf(), binA.getShelf());
                }
            })
            .collect(Collectors.toList());
    }
}
```

---

### 10. `PickerWorker.java` — Runnable Worker Loop
**Path:** `src/com/optigro/service/PickerWorker.java`

```java
package com.optigro.service;

import com.optigro.models.PickTask;
import com.optigro.models.Picker;

public class PickerWorker implements Runnable {
    private final Picker picker;
    private final PickTaskService taskService;

    public PickerWorker(Picker picker, PickTaskService taskService) {
        this.picker = picker;
        this.taskService = taskService;
    }

    @Override
    public void run() {
        System.out.printf("[WorkerThread] %s is active and waiting on queue...%n", picker.getName());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Blocks until a PickTask is offered to PriorityBlockingQueue
                PickTask task = taskService.takeNextTask();
                taskService.processTask(picker, task);
            } catch (InterruptedException e) {
                // Restore interrupted flag and terminate worker loop cleanly
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.printf("[WorkerThread] %s finished shift.%n", picker.getName());
    }
}
```

---

### 11. `PickingMenu.java` — Picker Terminal Controller
**Path:** `src/com/optigro/controller/PickingMenu.java`

```java
package com.optigro.controller;

import com.optigro.models.Picker;
import com.optigro.repository.PickTaskRepository;
import com.optigro.service.BinLayoutService;
import com.optigro.service.PickTaskService;
import com.optigro.util.ConsoleScannerUtil;

public class PickingMenu {
    private final Picker picker;
    private final PickTaskService taskService;
    private final BinLayoutService binLayoutService;
    private final PickTaskRepository taskRepo;

    public PickingMenu(Picker picker, PickTaskService taskService,
                       BinLayoutService binLayoutService, PickTaskRepository taskRepo) {
        this.picker = picker;
        this.taskService = taskService;
        this.binLayoutService = binLayoutService;
        this.taskRepo = taskRepo;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== WAREHOUSE PICKER PORTAL | %s =====%n", picker.getName());
            System.out.println("1. View Tasks Completed By Me");
            System.out.println("2. Manual Pick Next Queued Task");
            System.out.println("3. Inspect Warehouse Store Grid");
            System.out.println("4. Toggle My Shift (Online/Offline)");
            System.out.println("5. Logout to Main Menu");

            int ch = ConsoleScannerUtil.readIntInRange("Choose [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    var myTasks = taskRepo.findByPickerId(picker.getPickerId());
                    System.out.println("\n--- My Task History ---");
                    if (myTasks.isEmpty()) System.out.println("No completed tasks yet.");
                    for (var t : myTasks) System.out.println(t);
                    break;

                case 2:
                    System.out.println("Attempting to take next task from queue...");
                    try {
                        var task = taskService.takeNextTask();
                        taskService.processTask(picker, task);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted.");
                    }
                    break;

                case 3:
                    binLayoutService.printGrid();
                    break;

                case 4:
                    picker.setAvailable(!picker.isAvailable());
                    System.out.println("✓ Shift status toggled: " + (picker.isAvailable() ? "AVAILABLE" : "OFFLINE"));
                    break;

                case 5:
                    return;
            }
        }
    }
}
```

---

### 12. `NoPickerAvailableException.java` — Custom Exception
**Path:** `src/com/optigro/exception/NoPickerAvailableException.java`

```java
package com.optigro.exception;

public class NoPickerAvailableException extends Exception {
    public NoPickerAvailableException(String message) {
        super(message);
    }
}
```

---

### 13. `DataInitializer.java` — Master Persistence Bootstrapper (YOU OWN THIS)
**Path:** `src/com/optigro/config/DataInitializer.java`

Coordinates all file persistence on system startup:

```java
package com.optigro.config;

import com.optigro.repository.*;
import com.optigro.service.BinLayoutService;
import com.optigro.service.OrderService;

import java.io.File;
import java.io.IOException;

public class DataInitializer {

    public static void initialize(UserRepository userRepo,
                                  ProductRepository productRepo,
                                  BinLayoutService layout,
                                  CustomerRepository customerRepo,
                                  PickerRepository pickerRepo,
                                  DeliveryPartnerRepository partnerRepo,
                                  OrderService orderService) {

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         OPTIGRO SYSTEM BOOT & PERSISTENCE INITIALIZER    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        new File("data").mkdirs();
        new File("reports").mkdirs();

        try {
            // 1. M4: Load Users & Delivery Partners from CSV
            userRepo.loadFromCsv("data/users.csv");
            partnerRepo.loadFromCsv("data/partners.csv");

            // 2. M1: Load Inventory from CSV & Build Bin Grid
            productRepo.loadFromCsv("data/products.csv");
            layout.autoAssignBins();

            // 3. M2: Load Customers from CSV
            customerRepo.loadFromCsv("data/customers.csv");

            // 4. M3: Load Pickers from CSV
            pickerRepo.loadFromCsv("data/pickers.csv");

            System.out.println("✓ Persistence Boot Complete! System loaded from data/*.csv files.");
            System.out.printf("  Users: %-4d | Products: %-4d | Customers: %-4d | Pickers: %-4d | Partners: %-4d%n",
                userRepo.findAll().size(), productRepo.findAll().size(), customerRepo.findAll().size(),
                pickerRepo.findAll().size(), partnerRepo.findAll().size());

        } catch (IOException e) {
            System.out.println("[!] Warning during CSV load: " + e.getMessage() + ". Check file permissions.");
        }
    }
}
```

---

## 📋 Sprint-wise Task Checklist

### Sprint 0 (Day 1) — Setup
- [ ] Clone repo, checkout `m3/picking-engine`.
- [ ] Create `DataInitializer.java` skeleton.
- [ ] Verify `data/pickers.csv` exists.

### Sprint 1 — Models & Repositories
- [ ] `Picker.java` (`equals` & `hashCode` by `pickerId`).
- [ ] `PickTask.java` (`Comparable<PickTask>`, `Set<Integer> pickedProductIds`, `equals` & `hashCode`).
- [ ] `Substitution.java` (`equals` & `hashCode`).
- [ ] `PickerRepository.java` (`loadFromCsv("data/pickers.csv")`).
- [ ] `PickTaskRepository.java` (`appendTaskHistoryToCsv("data/pick_history.csv")`).

### Sprint 2 — Services
- [ ] `PickTaskService.java` (PriorityBlockingQueue, takeNextTask, processTask).
- [ ] `SubstitutionService.java` (find nearest price substitute in same category).
- [ ] `PickRoutePlanner.java` (snake-route aisle Comparator).
- [ ] `PickerWorker.java` (Runnable worker thread loop).
- [ ] `NoPickerAvailableException.java`.

### Sprint 3 — Menu & Integration
- [ ] `PickingMenu.java` (view tasks, process, warehouse view).
- [ ] Coordinate `DataInitializer.java` loading all 4 members' CSV data.
- [ ] Pair code review with Aditya (M4) (leave 5+ review comments on GitHub PR).

### Sprint 4 — Hardening & Docs
- [ ] Concurrency stress test (multiple simulated orders, verify zero duplicate picks).
- [ ] Create Flowcharts (menu flow, order lifecycle, picking route) + test case table (your doc deliverable).
- [ ] Author "My Module" page (1 page).
- [ ] Fill OOP concepts map rows.
- [ ] Teach-back prep: Multithreading, `PriorityBlockingQueue`, `Comparable` vs. `Comparator`, `Set` deduplication, CSV persistence bootstrapper.

---

## 🎓 Viva Preparation

### Your Java Topics to Master
1. **`PriorityBlockingQueue` vs. `ArrayList`:** Thread-safety without manual synchronized locks, natural ordering via `compareTo()`, and blocking `take()` that suspends threads in the operating system when idle (consuming 0% CPU).
2. **`Set` Interface in Concurrency (`pickedProductIds`):** Why use `Set<Integer>`? (`markItemPicked()` uses `Set.add()` which returns `false` if already present, preventing two threads from picking the identical product).
3. **`Comparable` vs. `Comparator`:** `PickTask` implements `Comparable` for natural priority/time queue ordering; `PickRoutePlanner` uses an external `Comparator` to sort items dynamically by aisle snake parity coordinates.
4. **Safely Stopping Threads:** Why use `executor.shutdownNow()` and handle `InterruptedException`? (`Thread.stop()` is deprecated because it releases locks arbitrarily, corrupting data. `shutdownNow()` sends an interrupt signal; `take()` catches it and restores the flag with `Thread.currentThread().interrupt()`, allowing clean loop exit).
5. **Master CSV Persistence Bootstrapping:** How does `DataInitializer.java` tie together file persistence across all 4 slices? (Reads each member's CSV, populates repositories, and assigns physical bins before menus launch).
