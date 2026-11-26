package com.optigro.service;

import com.optigro.constants.OrderStatus;
import com.optigro.constants.PaymentMode;
import com.optigro.exception.InvalidCouponException;
import com.optigro.exception.OutOfStockException;
import com.optigro.models.Customer;
import com.optigro.models.Order;
import com.optigro.models.OrderItem;
import com.optigro.models.Product;
import com.optigro.repository.CustomerRepository;
import com.optigro.repository.OrderRepository;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OrderService {
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private Consumer<Order> onOrderPlaced;

    public OrderService(OrderRepository orderRepo, CustomerRepository customerRepo,
                        InventoryService inventoryService, PricingService pricingService) {
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
    }

    public void setOnOrderPlaced(Consumer<Order> listener) {
        this.onOrderPlaced = listener;
    }

    public Optional<Order> getOrder(int orderId) {
        return orderRepo.findById(orderId);
    }

    public List<Order> getOrdersByCustomer(int customerId) {
        return orderRepo.findByCustomerId(customerId);
    }

    public void updateStatus(int orderId, OrderStatus newStatus) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(newStatus);
    }

    public Order placeOrder(int customerId, Map<Integer, Integer> cartItems,
                            PaymentMode mode, String couponCode)
            throws OutOfStockException, InvalidCouponException {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart");
        }

        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

       
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
            int pid = entry.getKey();
            int qty = entry.getValue();
            Product p = inventoryService.getProduct(pid)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));
            items.add(new OrderItem(pid, p.getName(), qty, p.getPrice()));
        }

        double subtotal = pricingService.calculateSubtotal(items);
        double gst = pricingService.calculateGST(items, inventoryService);
        double discount = pricingService.applyCoupon(couponCode, subtotal, customer);
        double deliveryFee = pricingService.calculateDeliveryFee(subtotal, couponCode);
        double total = Math.max(0.0, subtotal + gst + deliveryFee - discount);

        if (mode == PaymentMode.WALLET) {
            if (customer.getWalletBalance() < total) {
                throw new IllegalStateException(String.format("Insufficient wallet balance. Required: Rs.%.2f, Available: Rs.%.2f",
                    total, customer.getWalletBalance()));
            }
        }

        List<OrderItem> successfullyReserved = new ArrayList<>();
        try {
            for (OrderItem item : items) {
                inventoryService.reserveStock(item.getProductId(), item.getQuantity());
                successfullyReserved.add(item);
            }
        } catch (OutOfStockException e) {
            
            for (OrderItem reserved : successfullyReserved) {
                inventoryService.releaseStock(reserved.getProductId(), reserved.getQuantity());
            }
            throw e; 
        }

        if (mode == PaymentMode.WALLET) {
            customer.deductWallet(total);
        }
        if (couponCode != null && !couponCode.trim().isEmpty() && discount > 0) {
            customer.addUsedCoupon(couponCode); 
        }

        
        int newOrderId = orderRepo.nextId();
        Order order = new Order(newOrderId, customerId, items, mode, subtotal, gst, deliveryFee, discount, total);
        orderRepo.save(order);

        
        try {
            orderRepo.appendOrderToCsv(order, "data/orders.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Order saved to memory, but failed to append to disk: " + e.getMessage());
        }

        
        if (onOrderPlaced != null) {
            onOrderPlaced.accept(order);
        }

        return order;
    }

    public String generateSalesReport() {
        List<Order> allOrders = orderRepo.findAll();
        double totalRevenue = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .mapToDouble(Order::getTotal)
            .sum();

        Map<OrderStatus, Long> counts = allOrders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("===== OPTIGRO SALES & ORDER SUMMARY =====\n");
        sb.append("Total Orders Placed: ").append(allOrders.size()).append("\n");
        counts.forEach((status, count) ->
            sb.append(String.format(" • %-18s: %d%n", status, count)));
        sb.append("Total Completed Revenue: Rs.").append(String.format("%.2f", totalRevenue)).append("\n");
        return sb.toString();
    }
}
