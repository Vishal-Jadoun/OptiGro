# 👤 Member 2 (M2): Customer & Orders — The Business Rules

> **Role:** Customer & Orders — The Business Rules & Customer Interface  
> **Branch:** `m2/customer-orders`  
> **Files Owned:** 13 Java files + `data/customers.csv` & `data/orders.csv`  
> **GitHub:** [Vishal-Jadoun/OptiGro](https://github.com/Vishal-Jadoun/OptiGro.git)

---

## 📌 Overview

You are building the **business logic layer** — everything a customer interacts with: browsing products, managing a cart, applying coupons without duplicates (enforced via `Set<String>`), placing orders with automated stock rollback, generating printed GST invoices, and tracking order state transitions.

Your `OrderService` is the **central hub** of OptiGro:
* It calls M1's `InventoryService.reserveStock()` [synchronized].
* It logs each placed order to `data/orders.csv` for true file persistence.
* It fires the **`Consumer<Order>` callback** that triggers Kritika's (M3) picking engine without creating a circular dependency.

---

## 📁 Your Complete File & Data Structure

```text
src/
└── com/
    └── optigro/
        ├── constants/
        │   ├── OrderStatus.java              ← Enum with state machine (PLACED→PICKING→...→DELIVERED)
        │   └── PaymentMode.java              ← Enum: COD, WALLET, UPI
        ├── models/
        │   ├── Customer.java                 ← Customer profile, wallet, and Set<String> usedCoupons (equals/hashCode)
        │   ├── Order.java                    ← Full order with timestamps, pricing, status (equals/hashCode by orderId)
        │   └── OrderItem.java                ← Line item: productId, quantity, unitPrice (equals/hashCode by productId)
        ├── repository/
        │   ├── CustomerRepository.java       ← implements BaseRepository<Customer> + CSV loader (BufferedReader)
        │   └── OrderRepository.java          ← implements BaseRepository<Order> + CSV logger (BufferedWriter append)
        ├── service/
        │   ├── CartService.java              ← In-memory cart per customer, validates stock against InventoryService
        │   ├── PricingService.java           ← Streams API for GST by category, Set-based coupon checks, delivery fees
        │   └── OrderService.java             ← Place order, stock rollback, status transitions, callback trigger
        ├── controller/
        │   └── CustomerMenu.java             ← Customer-facing interactive menu (search, cart, invoice, track)
        ├── exception/
        │   └── InvalidCouponException.java   ← Custom checked exception for invalid/already-used coupons
        └── util/
            └── AsciiTableFormatter.java       ← Reusable table, invoice & divider printer (PUBLISH DAY 1)

data/
├── customers.csv                             ← Seed customer profiles & initial wallet balances
└── orders.csv                                ← Persistent append log of all placed customer orders
```

---

## 🔧 Detailed File-by-File Work Breakdown

### 1. `OrderStatus.java` — Enum with State Machine
**Path:** `src/com/optigro/constants/OrderStatus.java`

**What to do:**
1. Define values: `PLACED, PICKING, PACKED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED`.
2. Add `next()` method — enforces valid state transitions:
   ```
   PLACED → PICKING → PACKED → OUT_FOR_DELIVERY → DELIVERED
   ```
3. Add `canCancel()` → only `PLACED` or `PICKING` orders can be cancelled.
4. Throw `IllegalStateException` if `next()` is called on a terminal state (`DELIVERED` or `CANCELLED`).

```java
package com.optigro.constants;

public enum OrderStatus {
    PLACED,
    PICKING,
    PACKED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public OrderStatus next() {
        switch (this) {
            case PLACED:           return PICKING;
            case PICKING:          return PACKED;
            case PACKED:           return OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY: return DELIVERED;
            default:
                throw new IllegalStateException("No valid next status after terminal state: " + this);
        }
    }

    public boolean canCancel() {
        return this == PLACED || this == PICKING;
    }
}
```

**Viva point:** *"Why implement a state machine inside an enum?"* — It encapsulates business rules within the domain constant. Invalid jumps (e.g. moving directly from `PLACED` to `DELIVERED`, or modifying an already `DELIVERED` order) are rejected by design.

---

### 2. `PaymentMode.java` — Enum
**Path:** `src/com/optigro/constants/PaymentMode.java`

```java
package com.optigro.constants;

public enum PaymentMode {
    COD,
    WALLET,
    UPI;
}
```

---

### 3. `Customer.java` — Model (with `Set<String>` & `equals/hashCode`)
**Path:** `src/com/optigro/models/Customer.java`

**Fields (all `private`):**
* `customerId` (int) — Unique identifier.
* `userId` (int) — Links to M4's `User` login account.
* `name` (String) — Customer's name.
* `phone` (String) — 10-digit phone number.
* `address` (String) — Physical delivery address.
* `walletBalance` (double) — Current wallet balance.
* **`usedCoupons` (`Set<String>`)** — **`HashSet<String>` tracking coupons already redeemed on this account.**

**What to do:**
1. Constructor with validation (`phone` format, non-negative wallet).
2. All getters, setter for `walletBalance`.
3. `deductWallet(double amount)`: checks if balance >= amount, deducts, and returns `true`; else returns `false`.
4. `addUsedCoupon(String code)` and `hasUsedCoupon(String code)` — **demonstrates `Set` interface**.
5. **Implement `equals(Object o)` and `hashCode()` contract based on `customerId`:**

```java
package com.optigro.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Customer {
    private final int customerId;
    private final int userId;
    private final String name;
    private final String phone;
    private final String address;
    private double walletBalance;
    private final Set<String> usedCoupons;

    public Customer(int customerId, int userId, String name, String phone,
                    String address, double walletBalance) {
        if (walletBalance < 0) throw new IllegalArgumentException("Wallet balance cannot be negative");
        this.customerId = customerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "Name required");
        this.phone = Objects.requireNonNull(phone, "Phone required");
        this.address = Objects.requireNonNull(address, "Address required");
        this.walletBalance = walletBalance;
        this.usedCoupons = new HashSet<>();
    }

    public int getCustomerId() { return customerId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public double getWalletBalance() { return walletBalance; }

    public void setWalletBalance(double walletBalance) {
        if (walletBalance < 0) throw new IllegalArgumentException("Wallet balance cannot be negative");
        this.walletBalance = walletBalance;
    }

    public boolean deductWallet(double amount) {
        if (amount <= 0 || walletBalance < amount) {
            return false;
        }
        walletBalance -= amount;
        return true;
    }

    public void addWallet(double amount) {
        if (amount > 0) walletBalance += amount;
    }

    // Set / HashSet demonstration
    public boolean addUsedCoupon(String couponCode) {
        if (couponCode == null) return false;
        return usedCoupons.add(couponCode.trim().toUpperCase());
    }

    public boolean hasUsedCoupon(String couponCode) {
        if (couponCode == null) return false;
        return usedCoupons.contains(couponCode.trim().toUpperCase());
    }

    public Set<String> getUsedCoupons() {
        return Collections.unmodifiableSet(usedCoupons);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return customerId == customer.customerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer #%d: %s | Phone: %s | Wallet: Rs.%.2f",
            customerId, name, phone, walletBalance);
    }
}
```

---

### 4. `Order.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Order.java`

**Fields:** `orderId`, `customerId`, `items` (`List<OrderItem>`), `status` (`OrderStatus`), `paymentMode` (`PaymentMode`), `subtotal`, `gst`, `deliveryFee`, `discount`, `total`, `placedAt` (`LocalDateTime`).

**What to do:**
1. Constructor initializes status to `OrderStatus.PLACED` and `placedAt` to `LocalDateTime.now()`.
2. All getters.
3. `setStatus(OrderStatus newStatus)`: calls `status.next()` to validate legal state progression.
4. **Implement `equals(Object o)` and `hashCode()` based on `orderId`:**

```java
package com.optigro.models;

import com.optigro.constants.OrderStatus;
import com.optigro.constants.PaymentMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private final int orderId;
    private final int customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final PaymentMode paymentMode;
    private final double subtotal;
    private final double gst;
    private final double deliveryFee;
    private final double discount;
    private final double total;
    private final LocalDateTime placedAt;

    public Order(int orderId, int customerId, List<OrderItem> items, PaymentMode paymentMode,
                 double subtotal, double gst, double deliveryFee, double discount, double total) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.paymentMode = paymentMode;
        this.subtotal = subtotal;
        this.gst = gst;
        this.deliveryFee = deliveryFee;
        this.discount = discount;
        this.total = total;
        this.status = OrderStatus.PLACED;
        this.placedAt = LocalDateTime.now();
    }

    public int getOrderId() { return orderId; }
    public int getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            if (!status.canCancel()) {
                throw new IllegalStateException("Cannot cancel order in status: " + status);
            }
            this.status = OrderStatus.CANCELLED;
            return;
        }
        // State machine progression check
        if (this.status.next() != newStatus) {
            throw new IllegalStateException(String.format("Invalid transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public double getSubtotal() { return subtotal; }
    public double getGst() { return gst; }
    public double getDeliveryFee() { return deliveryFee; }
    public double getDiscount() { return discount; }
    public double getTotal() { return total; }
    public LocalDateTime getPlacedAt() { return placedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return orderId == order.orderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("Order #%d | Customer #%d | Total: Rs.%.2f | Status: %s | Time: %s",
            orderId, customerId, total, status, placedAt);
    }
}
```

---

### 5. `OrderItem.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/OrderItem.java`

```java
package com.optigro.models;

import java.util.Objects;

public class OrderItem {
    private final int productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(int productId, String productName, int quantity, double unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (unitPrice <= 0) throw new IllegalArgumentException("Unit price must be positive");
        this.productId = productId;
        this.productName = Objects.requireNonNull(productName);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getLineTotal() { return quantity * unitPrice; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem item = (OrderItem) o;
        return productId == item.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return String.format("%-20s x%-2d @ Rs.%-6.2f = Rs.%-7.2f",
            productName, quantity, unitPrice, getLineTotal());
    }
}
```

---

### 6. `CustomerRepository.java` — Repository with CSV Persistence
**Path:** `src/com/optigro/repository/CustomerRepository.java`

```java
package com.optigro.repository;

import com.optigro.models.Customer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomerRepository implements BaseRepository<Customer> {
    private final ConcurrentHashMap<Integer, Customer> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(2000);

    @Override
    public void save(Customer customer) {
        store.put(customer.getCustomerId(), customer);
    }

    @Override
    public Optional<Customer> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
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

    public Optional<Customer> findByUserId(int userId) {
        return store.values().stream()
            .filter(c -> c.getUserId() == userId)
            .findFirst();
    }

    // ==========================================
    // File Persistence: Load data/customers.csv
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
                if (parts.length < 6) continue;

                int id = Integer.parseInt(parts[0].trim());
                int userId = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                String phone = parts[3].trim();
                String address = parts[4].trim();
                double balance = Double.parseDouble(parts[5].trim());

                save(new Customer(id, userId, name, phone, address, balance));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
```

---

### 7. `OrderRepository.java` — Repository with CSV Persistence (Append)
**Path:** `src/com/optigro/repository/OrderRepository.java`

```java
package com.optigro.repository;

import com.optigro.models.Order;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OrderRepository implements BaseRepository<Order> {
    private final ConcurrentHashMap<Integer, Order> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(5000);

    @Override
    public void save(Order order) {
        store.put(order.getOrderId(), order);
    }

    @Override
    public Optional<Order> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
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

    public List<Order> findByCustomerId(int customerId) {
        return store.values().stream()
            .filter(o -> o.getCustomerId() == customerId)
            .collect(Collectors.toList());
    }

    // ==========================================
    // File Persistence: Append to data/orders.csv
    // ==========================================
    public synchronized void appendOrderToCsv(Order order, String filePath) throws IOException {
        File file = new File(filePath);
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# orderId,customerId,total,paymentMode,status,placedAt\n");
            }
            writer.write(String.format("%d,%d,%.2f,%s,%s,%s%n",
                order.getOrderId(), order.getCustomerId(), order.getTotal(),
                order.getPaymentMode().name(), order.getStatus().name(), order.getPlacedAt()));
        }
    }
}
```

---

### 8. `CartService.java` — In-Memory Shopping Cart
**Path:** `src/com/optigro/service/CartService.java`

```java
package com.optigro.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CartService {
    private final InventoryService inventoryService;
    // Map of customerId -> Map of (productId -> quantity)
    private final ConcurrentHashMap<Integer, Map<Integer, Integer>> carts = new ConcurrentHashMap<>();

    public CartService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public synchronized void addItem(int customerId, int productId, int qty) {
        if (qty <= 0) return;
        Map<Integer, Integer> cart = carts.computeIfAbsent(customerId, k -> new HashMap<>());
        int currentQty = cart.getOrDefault(productId, 0);
        int newQty = currentQty + qty;

        if (!inventoryService.isAvailable(productId, newQty)) {
            throw new IllegalArgumentException("Requested quantity exceeds available inventory stock");
        }
        cart.put(productId, newQty);
    }

    public synchronized void removeItem(int customerId, int productId) {
        Map<Integer, Integer> cart = carts.get(customerId);
        if (cart != null) {
            cart.remove(productId);
        }
    }

    public synchronized void updateQuantity(int customerId, int productId, int newQty) {
        if (newQty <= 0) {
            removeItem(customerId, productId);
            return;
        }
        if (!inventoryService.isAvailable(productId, newQty)) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }
        Map<Integer, Integer> cart = carts.computeIfAbsent(customerId, k -> new HashMap<>());
        cart.put(productId, newQty);
    }

    public Map<Integer, Integer> getCart(int customerId) {
        return carts.getOrDefault(customerId, Collections.emptyMap());
    }

    public void clearCart(int customerId) {
        carts.remove(customerId);
    }

    public boolean isEmpty(int customerId) {
        Map<Integer, Integer> cart = carts.get(customerId);
        return cart == null || cart.isEmpty();
    }
}
```

---

### 9. `PricingService.java` — GST, Coupons (`Set` Check), & Fees
**Path:** `src/com/optigro/service/PricingService.java`

```java
package com.optigro.service;

import com.optigro.constants.ProductCategory;
import com.optigro.exception.InvalidCouponException;
import com.optigro.models.Customer;
import com.optigro.models.OrderItem;
import com.optigro.models.Product;

import java.util.List;

public class PricingService {

    public double getGSTRate(ProductCategory category) {
        switch (category) {
            case STAPLES:
            case DAIRY:
                return 0.05; // 5% GST
            case FRUITS:
            case VEGETABLES:
                return 0.00; // 0% GST (Fresh essentials exempt)
            case SNACKS:
            case BEVERAGES:
                return 0.12; // 12% GST
            default:
                return 0.05;
        }
    }

    // Demonstrates Java Streams API: mapToDouble() and sum()
    public double calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .mapToDouble(OrderItem::getLineTotal)
            .sum();
    }

    // Demonstrates Java Streams API with external InventoryService lookup
    public double calculateGST(List<OrderItem> items, InventoryService inventory) {
        return items.stream()
            .mapToDouble(item -> {
                Product p = inventory.getProduct(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Product not found: " + item.getProductId()));
                double rate = getGSTRate(p.getCategory());
                return item.getLineTotal() * rate;
            })
            .sum();
    }

    // Delivery fee rule: Free on orders Rs. 500 and above; otherwise Rs. 30
    public double calculateDeliveryFee(double subtotal, String couponCode) {
        if ("FREEDEL".equalsIgnoreCase(couponCode)) {
            return 0.0;
        }
        return (subtotal >= 500.0) ? 0.0 : 30.0;
    }

    // Coupon verification using Customer's Set<String> usedCoupons
    public double applyCoupon(String couponCode, double subtotal, Customer customer)
            throws InvalidCouponException {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return 0.0;
        }
        String code = couponCode.trim().toUpperCase();

        // O(1) Set lookup: prevent duplicate coupon usage
        if (customer.hasUsedCoupon(code)) {
            throw new InvalidCouponException("Coupon '" + code + "' has already been redeemed on this account.");
        }

        switch (code) {
            case "WELCOME50":
                if (subtotal < 300.0) {
                    throw new InvalidCouponException("WELCOME50 requires a minimum cart subtotal of Rs.300.00");
                }
                return 50.0;

            case "SAVE10":
                // 10% discount capped at maximum Rs. 100
                return Math.min(100.0, subtotal * 0.10);

            case "FREEDEL":
                // Handled in delivery fee
                return 0.0;

            default:
                throw new InvalidCouponException("Invalid or unrecognized coupon code: " + code);
        }
    }
}
```

---

### 10. `OrderService.java` — Core Order Logic & Persistence
**Path:** `src/com/optigro/service/OrderService.java`

```java
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
    private Consumer<Order> onOrderPlaced; // Decoupled callback to M3

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

    // ========================================================
    // Core Transaction: Stock Reservation + Rollback + Persist
    // ========================================================
    public Order placeOrder(int customerId, Map<Integer, Integer> cartItems,
                            PaymentMode mode, String couponCode)
            throws OutOfStockException, InvalidCouponException {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart");
        }

        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        // 1. Build List<OrderItem>
        List<OrderItem> items = new ArrayList<>();
        for (var entry : cartItems.entrySet()) {
            int pid = entry.getKey();
            int qty = entry.getValue();
            Product p = inventoryService.getProduct(pid)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));
            items.add(new OrderItem(pid, p.getName(), qty, p.getPrice()));
        }

        // 2. Financial Calculations
        double subtotal = pricingService.calculateSubtotal(items);
        double gst = pricingService.calculateGST(items, inventoryService);
        double discount = pricingService.applyCoupon(couponCode, subtotal, customer);
        double deliveryFee = pricingService.calculateDeliveryFee(subtotal, couponCode);
        double total = Math.max(0.0, subtotal + gst + deliveryFee - discount);

        // 3. Payment Validation
        if (mode == PaymentMode.WALLET) {
            if (customer.getWalletBalance() < total) {
                throw new IllegalStateException(String.format("Insufficient wallet balance. Required: Rs.%.2f, Available: Rs.%.2f",
                    total, customer.getWalletBalance()));
            }
        }

        // 4. Atomic Stock Reservation with Automatic Rollback
        List<OrderItem> successfullyReserved = new ArrayList<>();
        try {
            for (OrderItem item : items) {
                inventoryService.reserveStock(item.getProductId(), item.getQuantity());
                successfullyReserved.add(item);
            }
        } catch (OutOfStockException e) {
            // ROLLBACK: Release any items reserved so far
            for (OrderItem reserved : successfullyReserved) {
                inventoryService.releaseStock(reserved.getProductId(), reserved.getQuantity());
            }
            throw e; // Re-throw to inform caller
        }

        // 5. Complete Financial Transaction
        if (mode == PaymentMode.WALLET) {
            customer.deductWallet(total);
        }
        if (couponCode != null && !couponCode.trim().isEmpty() && discount > 0) {
            customer.addUsedCoupon(couponCode); // Mark coupon used in Set
        }

        // 6. Instantiate & Save Order
        int newOrderId = orderRepo.nextId();
        Order order = new Order(newOrderId, customerId, items, mode, subtotal, gst, deliveryFee, discount, total);
        orderRepo.save(order);

        // 7. File Persistence: Append to data/orders.csv
        try {
            orderRepo.appendOrderToCsv(order, "data/orders.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Order saved to memory, but failed to append to disk: " + e.getMessage());
        }

        // 8. Fire callback to trigger M3 picking
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
```

---

### 11. `CustomerMenu.java` — Customer Menu Controller
**Path:** `src/com/optigro/controller/CustomerMenu.java`

```java
package com.optigro.controller;

import com.optigro.constants.PaymentMode;
import com.optigro.exception.InvalidCouponException;
import com.optigro.exception.OutOfStockException;
import com.optigro.models.Customer;
import com.optigro.models.Order;
import com.optigro.models.OrderItem;
import com.optigro.models.Product;
import com.optigro.service.CartService;
import com.optigro.service.InventoryService;
import com.optigro.service.OrderService;
import com.optigro.service.PricingService;
import com.optigro.util.AsciiTableFormatter;
import com.optigro.util.ConsoleScannerUtil;

import java.util.*;

public class CustomerMenu {
    private final Customer customer;
    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PricingService pricingService;

    public CustomerMenu(Customer customer, CartService cartService, OrderService orderService,
                        InventoryService inventoryService, PricingService pricingService) {
        this.customer = customer;
        this.cartService = cartService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== CUSTOMER PORTAL | Welcome, %s (Wallet: Rs.%.2f) =====%n",
                customer.getName(), customer.getWalletBalance());
            System.out.println("1. Browse / Search Products");
            System.out.println("2. Add Product to Cart");
            System.out.println("3. View Shopping Cart");
            System.out.println("4. Modify / Clear Cart");
            System.out.println("5. Checkout & Place Order (Generate GST Invoice)");
            System.out.println("6. Track Order Status");
            System.out.println("7. View My Order History");
            System.out.println("8. Logout to Main Menu");

            int choice = ConsoleScannerUtil.readIntInRange("Select option [1-8]: ", 1, 8);
            switch (choice) {
                case 1: browseProductsFlow(); break;
                case 2: addToCartFlow(); break;
                case 3: viewCartFlow(); break;
                case 4: modifyCartFlow(); break;
                case 5: checkoutFlow(); break;
                case 6: trackOrderFlow(); break;
                case 7: orderHistoryFlow(); break;
                case 8: return;
            }
        }
    }

    private void browseProductsFlow() {
        System.out.println("\n1. Search by keyword   2. View all active categories (Set)");
        int sub = ConsoleScannerUtil.readIntInRange("Choose [1-2]: ", 1, 2);
        if (sub == 1) {
            String kw = ConsoleScannerUtil.readString("Enter search keyword: ");
            var prods = inventoryService.searchByName(kw);
            displayProductsTable(prods);
        } else {
            var active = inventoryService.getActiveCategories();
            System.out.println("\nIn-Stock Categories: " + active);
            var prods = inventoryService.searchByName("");
            displayProductsTable(prods);
        }
    }

    private void displayProductsTable(List<Product> prods) {
        if (prods.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        String[] headers = {"ID", "Product Name", "Category", "Price (Rs.)", "Stock Status"};
        List<String[]> rows = new ArrayList<>();
        for (var p : prods) {
            rows.add(new String[]{
                String.valueOf(p.getId()),
                p.getName(),
                p.getCategory().name(),
                String.format("%.2f", p.getPrice()),
                p.isSellable() ? (p.getStock() > 0 ? p.getStock() + " in stock" : "OUT OF STOCK") : "EXPIRED"
            });
        }
        AsciiTableFormatter.printTable(headers, rows);
    }

    private void addToCartFlow() {
        int pid = ConsoleScannerUtil.readInt("Enter Product ID to add: ");
        var prod = inventoryService.getProduct(pid);
        if (prod.isEmpty() || !prod.get().isSellable() || prod.get().getStock() <= 0) {
            System.out.println("[!] Product is currently unavailable or out of stock.");
            return;
        }
        int qty = ConsoleScannerUtil.readIntInRange("Enter quantity [1-" + prod.get().getStock() + "]: ", 1, prod.get().getStock());
        try {
            cartService.addItem(customer.getCustomerId(), pid, qty);
            System.out.printf("✓ Added %dx %s to your cart!%n", qty, prod.get().getName());
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    private void viewCartFlow() {
        var cart = cartService.getCart(customer.getCustomerId());
        if (cart.isEmpty()) {
            System.out.println("Your shopping cart is empty.");
            return;
        }
        System.out.println("\n--- Your Shopping Cart ---");
        double subtotal = 0.0;
        String[] headers = {"ID", "Item Name", "Qty", "Unit Price", "Total (Rs.)"};
        List<String[]> rows = new ArrayList<>();
        for (var entry : cart.entrySet()) {
            var p = inventoryService.getProduct(entry.getKey()).orElseThrow();
            double lineTotal = p.getPrice() * entry.getValue();
            subtotal += lineTotal;
            rows.add(new String[]{
                String.valueOf(p.getId()), p.getName(), String.valueOf(entry.getValue()),
                String.format("%.2f", p.getPrice()), String.format("%.2f", lineTotal)
            });
        }
        AsciiTableFormatter.printTable(headers, rows);
        System.out.printf("Estimated Subtotal: Rs.%.2f%n", subtotal);
    }

    private void modifyCartFlow() {
        if (cartService.isEmpty(customer.getCustomerId())) {
            System.out.println("Cart is empty.");
            return;
        }
        viewCartFlow();
        System.out.println("1. Update Quantity  2. Remove Item  3. Clear Cart");
        int ch = ConsoleScannerUtil.readIntInRange("Choose [1-3]: ", 1, 3);
        if (ch == 1) {
            int pid = ConsoleScannerUtil.readInt("Enter Product ID: ");
            int qty = ConsoleScannerUtil.readInt("Enter new quantity (0 to remove): ");
            cartService.updateQuantity(customer.getCustomerId(), pid, qty);
            System.out.println("✓ Cart updated.");
        } else if (ch == 2) {
            int pid = ConsoleScannerUtil.readInt("Enter Product ID to remove: ");
            cartService.removeItem(customer.getCustomerId(), pid);
            System.out.println("✓ Item removed.");
        } else {
            cartService.clearCart(customer.getCustomerId());
            System.out.println("✓ Cart cleared.");
        }
    }

    private void checkoutFlow() {
        var cart = cartService.getCart(customer.getCustomerId());
        if (cart.isEmpty()) {
            System.out.println("[!] Your cart is empty. Add items before checking out.");
            return;
        }

        viewCartFlow();
        System.out.println("\nSelect Payment Mode: 1.COD  2.WALLET (Bal: Rs." + customer.getWalletBalance() + ")  3.UPI");
        int payChoice = ConsoleScannerUtil.readIntInRange("Payment Mode [1-3]: ", 1, 3);
        PaymentMode mode = PaymentMode.values()[payChoice - 1];

        String coupon = ConsoleScannerUtil.readString("Enter coupon code (e.g. WELCOME50, SAVE10, FREEDEL) or press Enter to skip: ");
        if (coupon.trim().isEmpty()) coupon = null;

        try {
            Order order = orderService.placeOrder(customer.getCustomerId(), cart, mode, coupon);
            cartService.clearCart(customer.getCustomerId());
            printInvoice(order);
            System.out.printf("✓ Order #%d placed successfully! Current status: %s%n", order.getOrderId(), order.getStatus());
        } catch (OutOfStockException | InvalidCouponException | IllegalStateException e) {
            System.out.println("[!] Checkout Failed: " + e.getMessage());
        }
    }

    private void printInvoice(Order order) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                       OPTIGRO                            ║");
        System.out.println("║                     GST INVOICE                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║ Order ID: %-15d Date: %-22s ║%n", order.getOrderId(), order.getPlacedAt().toLocalDate());
        System.out.printf("║ Customer: %-15s Phone: %-21s ║%n", customer.getName(), customer.getPhone());
        System.out.printf("║ Address:  %-44s ║%n", customer.getAddress().length() > 44 ? customer.getAddress().substring(0, 41) + "..." : customer.getAddress());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-25s %-4s %-10s %-11s ║%n", "Item", "Qty", "Price", "Total");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        for (OrderItem item : order.getItems()) {
            String name = item.getProductName().length() > 25 ? item.getProductName().substring(0, 22) + "..." : item.getProductName();
            System.out.printf("║ %-25s %-4d Rs.%-7.2f Rs.%-8.2f ║%n",
                name, item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║ Subtotal:                                    Rs.%-8.2f ║%n", order.getSubtotal());
        System.out.printf("║ GST (Taxes):                                 Rs.%-8.2f ║%n", order.getGst());
        System.out.printf("║ Delivery Fee:                                Rs.%-8.2f ║%n", order.getDeliveryFee());
        System.out.printf("║ Discount Applied:                           -Rs.%-8.2f ║%n", order.getDiscount());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║ FINAL PAYABLE:                               Rs.%-8.2f ║%n", order.getTotal());
        System.out.printf("║ Payment Method: %-40s ║%n", order.getPaymentMode());
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private void trackOrderFlow() {
        int oid = ConsoleScannerUtil.readInt("Enter Order ID to track: ");
        var order = orderService.getOrder(oid);
        if (order.isEmpty() || order.get().getCustomerId() != customer.getCustomerId()) {
            System.out.println("[!] Order not found on your profile.");
            return;
        }
        System.out.printf("Order #%d | Status: [%s] | Placed: %s%n",
            order.get().getOrderId(), order.get().getStatus(), order.get().getPlacedAt());
    }

    private void orderHistoryFlow() {
        var orders = orderService.getOrdersByCustomer(customer.getCustomerId());
        if (orders.isEmpty()) {
            System.out.println("No past orders found.");
            return;
        }
        System.out.printf("%-8s %-12s %-18s %-10s%n", "Order ID", "Items", "Status", "Total (Rs.)");
        System.out.println("-".repeat(55));
        for (var o : orders) {
            System.out.printf("%-8d %-12d %-18s Rs.%-8.2f%n",
                o.getOrderId(), o.getItems().size(), o.getStatus(), o.getTotal());
        }
    }
}
```

---

### 12. `InvalidCouponException.java` — Custom Exception
**Path:** `src/com/optigro/exception/InvalidCouponException.java`

```java
package com.optigro.exception;

public class InvalidCouponException extends Exception {
    public InvalidCouponException(String message) {
        super(message);
    }
}
```

---

### 13. `AsciiTableFormatter.java` — Reusable Formatter Utility
**Path:** `src/com/optigro/util/AsciiTableFormatter.java`

> ⚠️ **PUBLISH EARLY** — All menus use this for tabular console display.

```java
package com.optigro.util;

import java.util.List;

public class AsciiTableFormatter {

    private AsciiTableFormatter() { }

    public static void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                widths[i] = Math.max(widths[i], (row[i] != null ? row[i].length() : 0));
            }
        }

        printSeparator(widths);
        printRow(headers, widths);
        printSeparator(widths);

        for (String[] row : rows) {
            printRow(row, widths);
        }
        printSeparator(widths);
    }

    private static void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        System.out.println(sb);
    }

    private static void printRow(String[] items, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String val = (i < items.length && items[i] != null) ? items[i] : "";
            sb.append(String.format(" %-" + widths[i] + "s |", val));
        }
        System.out.println(sb);
    }

    public static void printDivider(int width) {
        System.out.println("═".repeat(Math.max(1, width)));
    }
}
```

---

## 📋 Sprint-wise Task Checklist

### Sprint 0 (Day 1) — Setup
- [ ] Clone repo, checkout `m2/customer-orders`.
- [ ] Publish `AsciiTableFormatter.java`, `OrderStatus.java`, `PaymentMode.java`.
- [ ] Verify `data/customers.csv` and `data/orders.csv` exist.

### Sprint 1 — Models & Repositories
- [ ] `Customer.java` (`Set<String> usedCoupons`, `equals` & `hashCode` by `customerId`).
- [ ] `Order.java` (state machine transition validation, `equals` & `hashCode` by `orderId`).
- [ ] `OrderItem.java` (`getLineTotal()`, `equals` & `hashCode` by `productId`).
- [ ] `CustomerRepository.java` (`loadFromCsv("data/customers.csv")`).
- [ ] `OrderRepository.java` (`appendOrderToCsv("data/orders.csv")`).

### Sprint 2 — Services
- [ ] `CartService.java` (in-memory cart with stock checks).
- [ ] `PricingService.java` (GST by category via Streams, coupon validation with `Set`).
- [ ] `OrderService.java` (placeOrder, stock rollback on failure, CSV logging, callback).
- [ ] `InvalidCouponException.java`.

### Sprint 3 — Menu & Integration
- [ ] `CustomerMenu.java` (all 8 options, GST bill printing).
- [ ] Verify integration: Order placed → stock deducted → appended to `data/orders.csv` → triggers Kritika's (M3) picking engine.

### Sprint 4 — Hardening & Docs
- [ ] Input validation sweep across menus.
- [ ] Create Class diagram / UML (your documentation deliverable).
- [ ] Author "My Module" page (1 page).
- [ ] Fill OOP concepts map rows.
- [ ] Teach-back prep: Enums with behaviour, Streams API, `Consumer<Order>` callbacks, `Set` for single-use coupons, File persistence (CSV append).

---

## 🎓 Viva Preparation

### Your Java Topics to Master
1. **The State Machine Enum Pattern:** How does `OrderStatus.next()` enforce business rules? (Illegal status leaps throw `IllegalStateException`; terminal states cannot be re-opened).
2. **Streams API in Pricing:** Explain `items.stream().mapToDouble(OrderItem::getLineTotal).sum()`. (Converts stream to primitive `DoubleStream` to avoid boxing/unboxing overhead, then aggregates sum).
3. **`Set` Interface in Coupons:** Why use `Set<String> usedCoupons` in `Customer`? (Provides O(1) constant-time membership lookups via hash codes; naturally rejects duplicate entries without manual loops).
4. **Decoupled Callbacks via `Consumer<Order>`:** Why use a functional interface instead of injecting `PickTaskService` directly into `OrderService`? (Eliminates tight coupling and circular package dependencies; `App.java` wires them via method reference `picking::createTask`).
5. **Atomic Stock Rollback:** What happens if 4 items are in cart, 3 are reserved, but the 4th throws `OutOfStockException`? (The catch block iterates over the reserved items and calls `inventoryService.releaseStock()`, maintaining transactional integrity).
