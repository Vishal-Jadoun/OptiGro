# 👤 Member 4 (M4): Delivery & Platform — Security, Dispatch & Integration

> **Role:** Delivery & Platform (Security + Dispatch + Master System Integration)  
> **Branch:** `m4/delivery-platform`  
> **Files Owned:** 13 Java files + `data/users.csv`, `data/partners.csv`, `data/deliveries.csv`  
> **GitHub:** [Vishal-Jadoun/OptiGro](https://github.com/Vishal-Jadoun/OptiGro.git)

---

## 📌 Overview

You are building the **platform and integration layer** — the entry point of the application, security authentication, delivery dispatch, and the architectural glue that ties all slices together.

You own:
1. **Authentication & Security:** Login validation, role checks (`UserRole`), brute-force protection with account lockouts after 3 failed attempts tracked in a `Set<String> lockedUsernames`.
2. **Delivery Dispatch & SLA Validation:** Linear FIFO queues (`ArrayDeque`) managing available delivery partners and pending orders; strict 10-minute SLA checking via `java.time.Duration.between()`.
3. **Application Entry Point (`App.java`):** Manual dependency injection (constructor injection) instantiating all 7 repositories, 9 services, and wiring decoupled callbacks.
4. **Master Menu Routing (`MainMenuController.java`):** Root application loop, role-based screen routing, and orchestrating the evaluator's **Live Simulation**.
5. **CSV Persistence & File Reports Export:** Reading `data/users.csv` and `data/partners.csv`, appending completed deliveries to `data/deliveries.csv`, and exporting full system summaries via `BufferedWriter` to `reports/full_report.txt`.

---

## 📁 Your Complete File & Data Structure

```text
src/
└── com/
    └── optigro/
        ├── App.java                           ← Entry point, manual dependency injection (THE master file)
        ├── constants/
        │   ├── PartnerStatus.java             ← Enum: ONLINE, OFFLINE, ON_DELIVERY
        │   └── UserRole.java                  ← Enum: ADMIN, CUSTOMER, PICKER, DELIVERY_PARTNER
        ├── models/
        │   ├── User.java                      ← Credentials, roles, lockout counters (equals/hashCode by userId)
        │   ├── DeliveryPartner.java           ← Partner profile with ratings (equals/hashCode by partnerId)
        │   └── Delivery.java                  ← Delivery record with SLA tracking (equals/hashCode by deliveryId)
        ├── repository/
        │   ├── UserRepository.java            ← implements BaseRepository<User> + CSV loader (BufferedReader)
        │   └── DeliveryPartnerRepository.java ← implements BaseRepository<DeliveryPartner> + CSV loader (BufferedReader)
        ├── service/
        │   ├── AuthService.java               ← Login, role routing, Set<String> lockedUsernames
        │   └── DeliveryService.java           ← Queue<DeliveryPartner> (ArrayDeque), Duration SLA, Set flagged partners, CSV logger
        ├── controller/
        │   ├── MainMenuController.java        ← Root while(true) loop, role router, simulation & report exporter
        │   └── DeliveryMenu.java              ← Delivery partner terminal menu
        └── exception/
            └── InvalidAuthException.java      ← Custom checked exception for auth failures

data/
├── users.csv                                  ← User login credentials & assigned roles
├── partners.csv                               ← Delivery partner seed profiles & historical ratings
└── deliveries.csv                             ← Persistent append log of completed deliveries with SLA metrics

reports/
└── full_report.txt                            ← Formatted plain-text export of all 4 modules' reports
```

---

## 🔧 Detailed File-by-File Work Breakdown

### 1. `PartnerStatus.java` — Enum
**Path:** `src/com/optigro/constants/PartnerStatus.java`

```java
package com.optigro.constants;

public enum PartnerStatus {
    ONLINE,        // Available for order assignment in the dispatch queue
    OFFLINE,       // Shift ended, not taking deliveries
    ON_DELIVERY    // Currently en route fulfilling an assigned order
}
```

---

### 2. `UserRole.java` — Enum
**Path:** `src/com/optigro/constants/UserRole.java`

```java
package com.optigro.constants;

public enum UserRole {
    ADMIN,
    CUSTOMER,
    PICKER,
    DELIVERY_PARTNER
}
```

---

### 3. `User.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/User.java`

**Fields (all `private`):**
* `userId` (int) — Unique identifier.
* `username` (String) — Login username.
* `password` (String) — Login password.
* `role` (`UserRole`) — Access privilege level.
* `failedAttempts` (int) — Counter for failed logins.
* `locked` (boolean) — Flag set to true if `failedAttempts >= 3`.

```java
package com.optigro.models;

import com.optigro.constants.UserRole;

import java.util.Objects;

public class User {
    private final int userId;
    private final String username;
    private final String password;
    private final UserRole role;
    private int failedAttempts;
    private boolean locked;

    public User(int userId, String username, String password, UserRole role) {
        this.userId = userId;
        this.username = Objects.requireNonNull(username, "Username required").trim();
        this.password = Objects.requireNonNull(password, "Password required");
        this.role = Objects.requireNonNull(role, "Role required");
        this.failedAttempts = 0;
        this.locked = false;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserRole getRole() { return role; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        if (this.failedAttempts >= 3) {
            this.locked = true;
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void unlock() {
        this.locked = false;
        this.failedAttempts = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return String.format("User #%d: %-12s | Role: %-16s | Locked: %b",
            userId, username, role, locked);
    }
}
```

---

### 4. `DeliveryPartner.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/DeliveryPartner.java`

**Fields:** `partnerId`, `userId`, `name`, `status` (`PartnerStatus`), `totalRating` (`double`), `deliveriesDone` (`int`).

```java
package com.optigro.models;

import com.optigro.constants.PartnerStatus;

import java.util.Objects;

public class DeliveryPartner {
    private final int partnerId;
    private final int userId;
    private final String name;
    private PartnerStatus status;
    private double totalRating;
    private int deliveriesDone;

    public DeliveryPartner(int partnerId, int userId, String name, PartnerStatus status,
                           double totalRating, int deliveriesDone) {
        this.partnerId = partnerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "Partner name required");
        this.status = status != null ? status : PartnerStatus.ONLINE;
        this.totalRating = Math.max(0.0, totalRating);
        this.deliveriesDone = Math.max(0, deliveriesDone);
    }

    public int getPartnerId() { return partnerId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public double getTotalRating() { return totalRating; }
    public int getDeliveriesDone() { return deliveriesDone; }

    public void addRating(int stars) {
        if (stars >= 1 && stars <= 5) {
            this.totalRating += stars;
            this.deliveriesDone++;
        }
    }

    public double getAverageRating() {
        return (deliveriesDone > 0) ? (totalRating / deliveriesDone) : 5.0; // default 5.0 for newcomers
    }

    public boolean isLowRated() {
        return deliveriesDone >= 3 && getAverageRating() < 3.0; // flagged if below 3.0 stars
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryPartner)) return false;
        DeliveryPartner partner = (DeliveryPartner) o;
        return partnerId == partner.partnerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerId);
    }

    @Override
    public String toString() {
        return String.format("Partner #%d: %-15s | Status: %-11s | Avg Rating: %-4.1f (%d reviews)",
            partnerId, name, status, getAverageRating(), deliveriesDone);
    }
}
```

---

### 5. `Delivery.java` — Model (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Delivery.java`

**Fields:** `deliveryId`, `orderId`, `partnerId`, `dispatchedAt` (`LocalDateTime`), `deliveredAt` (`LocalDateTime`), `metSla` (`boolean`), `rating` (`int`).

```java
package com.optigro.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Delivery {
    private final int deliveryId;
    private final int orderId;
    private final int partnerId;
    private final LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private boolean metSla;
    private int rating; // 1 to 5 stars, 0 = unrated

    public Delivery(int deliveryId, int orderId, int partnerId) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.partnerId = partnerId;
        this.dispatchedAt = LocalDateTime.now();
        this.deliveredAt = null;
        this.metSla = false;
        this.rating = 0;
    }

    public int getDeliveryId() { return deliveryId; }
    public int getOrderId() { return orderId; }
    public int getPartnerId() { return partnerId; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public boolean isMetSla() { return metSla; }
    public void setMetSla(boolean metSla) { this.metSla = metSla; }
    public int getRating() { return rating; }

    public void setRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Delivery)) return false;
        Delivery delivery = (Delivery) o;
        return deliveryId == delivery.deliveryId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId);
    }

    @Override
    public String toString() {
        return String.format("Delivery #%d | Order #%d | Partner #%d | Met SLA: %b | Rating: %s",
            deliveryId, orderId, partnerId, metSla, (rating > 0 ? rating + "★" : "Pending"));
    }
}
```

---

### 6. `UserRepository.java` — Repository with CSV Persistence
**Path:** `src/com/optigro/repository/UserRepository.java`

```java
package com.optigro.repository;

import com.optigro.constants.UserRole;
import com.optigro.models.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UserRepository implements BaseRepository<User> {
    private final ConcurrentHashMap<Integer, User> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(100);

    @Override
    public void save(User user) {
        store.put(user.getUserId(), user);
    }

    @Override
    public Optional<User> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
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

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return store.values().stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
            .findFirst();
    }

    // ==========================================
    // File Persistence: Read data/users.csv
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
                if (parts.length < 4) continue;

                int id = Integer.parseInt(parts[0].trim());
                String username = parts[1].trim();
                String password = parts[2].trim();
                UserRole role = UserRole.valueOf(parts[3].trim());

                save(new User(id, username, password, role));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
```

---

### 7. `DeliveryPartnerRepository.java` — Repository with CSV Persistence
**Path:** `src/com/optigro/repository/DeliveryPartnerRepository.java`

```java
package com.optigro.repository;

import com.optigro.constants.PartnerStatus;
import com.optigro.models.DeliveryPartner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DeliveryPartnerRepository implements BaseRepository<DeliveryPartner> {
    private final ConcurrentHashMap<Integer, DeliveryPartner> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(6000);

    @Override
    public void save(DeliveryPartner partner) {
        store.put(partner.getPartnerId(), partner);
    }

    @Override
    public Optional<DeliveryPartner> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<DeliveryPartner> findAll() {
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

    public Optional<DeliveryPartner> findByUserId(int userId) {
        return store.values().stream()
            .filter(p -> p.getUserId() == userId)
            .findFirst();
    }

    public List<DeliveryPartner> findOnline() {
        return store.values().stream()
            .filter(p -> p.getStatus() == PartnerStatus.ONLINE)
            .collect(Collectors.toList());
    }

    // ==========================================
    // File Persistence: Read data/partners.csv
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
                PartnerStatus status = PartnerStatus.valueOf(parts[3].trim());
                double rating = Double.parseDouble(parts[4].trim());
                int deliveries = Integer.parseInt(parts[5].trim());

                save(new DeliveryPartner(id, userId, name, status, rating, deliveries));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
```

---

### 8. `AuthService.java` — Authentication with `Set<String>` Lockout
**Path:** `src/com/optigro/service/AuthService.java`

```java
package com.optigro.service;

import com.optigro.exception.InvalidAuthException;
import com.optigro.models.User;
import com.optigro.repository.UserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthService {
    private final UserRepository userRepo;
    private final Set<String> lockedUsernames = new HashSet<>();

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User login(String username, String password) throws InvalidAuthException {
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidAuthException("Username cannot be empty");
        }

        String cleanUsername = username.trim();

        // 1. Instant check in HashSet (O(1))
        if (lockedUsernames.contains(cleanUsername)) {
            throw new InvalidAuthException("Security Alert: Account '" + cleanUsername +
                "' is permanently locked after 3 failed attempts. Contact administrator.");
        }

        // 2. Lookup user in repository
        User user = userRepo.findByUsername(cleanUsername)
            .orElseThrow(() -> new InvalidAuthException("User profile not found: " + cleanUsername));

        // 3. Password Verification
        if (!user.getPassword().equals(password)) {
            user.incrementFailedAttempts();
            if (user.isLocked()) {
                lockedUsernames.add(cleanUsername); // Mark in Set
                throw new InvalidAuthException("Account locked! You exceeded 3 failed password attempts.");
            }
            int remaining = 3 - user.getFailedAttempts();
            throw new InvalidAuthException("Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        // 4. Success -> Reset failed attempts
        user.resetFailedAttempts();
        return user;
    }

    public Set<String> getLockedUsernames() {
        return Collections.unmodifiableSet(lockedUsernames);
    }
}
```

---

### 9. `DeliveryService.java` — Dispatch, SLA, `Set` & CSV Logging
**Path:** `src/com/optigro/service/DeliveryService.java`

**Core Concepts:**
* **`Queue<Integer>` (`ArrayDeque`)**: FIFO partner dispatch queue.
* **`java.time.Duration`**: SLA calculation `Duration.between(order.getPlacedAt(), deliveredAt).toMinutes() <= 10`.
* **`Set<Integer>`**: `flaggedLowRatedPartners` for tracking underperforming partners (< 3.0 stars).
* **CSV Logging**: Appending completed delivery records to `data/deliveries.csv`.

```java
package com.optigro.service;

import com.optigro.constants.OrderStatus;
import com.optigro.constants.PartnerStatus;
import com.optigro.models.Delivery;
import com.optigro.models.DeliveryPartner;
import com.optigro.models.Order;
import com.optigro.repository.DeliveryPartnerRepository;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryService {
    private final DeliveryPartnerRepository partnerRepo;
    private final OrderService orderService;
    private final Queue<Integer> availablePartners = new ArrayDeque<>();
    private final Queue<Order> pendingOrders = new ArrayDeque<>();
    private final ConcurrentHashMap<Integer, Delivery> deliveries = new ConcurrentHashMap<>();
    private final AtomicInteger deliveryCounter = new AtomicInteger(7000);
    private final Set<Integer> flaggedLowRatedPartners = new HashSet<>();

    public DeliveryService(DeliveryPartnerRepository partnerRepo, OrderService orderService) {
        this.partnerRepo = partnerRepo;
        this.orderService = orderService;
    }

    public synchronized void registerPartnerOnline(int partnerId) {
        partnerRepo.findById(partnerId).ifPresent(p -> {
            p.setStatus(PartnerStatus.ONLINE);
            if (!availablePartners.contains(partnerId)) {
                availablePartners.offer(partnerId);
            }
            System.out.printf("[DeliveryHub] Partner %s is now ONLINE (Queue Position: %d)%n",
                p.getName(), availablePartners.size());

            // Check if orders are waiting in pending queue
            if (!pendingOrders.isEmpty()) {
                Order waiting = pendingOrders.poll();
                System.out.printf("[DeliveryHub] Auto-dispatching pending Order #%d to newly online partner %s%n",
                    waiting.getOrderId(), p.getName());
                dispatch(waiting);
            }
        });
    }

    public synchronized void registerPartnerOffline(int partnerId) {
        partnerRepo.findById(partnerId).ifPresent(p -> {
            p.setStatus(PartnerStatus.OFFLINE);
            availablePartners.remove(partnerId);
            System.out.printf("[DeliveryHub] Partner %s is now OFFLINE.%n", p.getName());
        });
    }

    // ==========================================
    // Dispatch Engine: FIFO Assignment
    // ==========================================
    public synchronized Delivery dispatch(Order order) {
        if (availablePartners.isEmpty()) {
            pendingOrders.offer(order);
            System.out.printf("[DeliveryHub] ⚠ No delivery partners online! Order #%d queued in pending queue.%n",
                order.getOrderId());
            return null;
        }

        // Dequeue first available partner in FIFO order
        int partnerId = availablePartners.poll();
        DeliveryPartner partner = partnerRepo.findById(partnerId).orElseThrow();
        partner.setStatus(PartnerStatus.ON_DELIVERY);

        int deliveryId = deliveryCounter.incrementAndGet();
        Delivery delivery = new Delivery(deliveryId, order.getOrderId(), partnerId);
        deliveries.put(deliveryId, delivery);

        orderService.updateStatus(order.getOrderId(), OrderStatus.OUT_FOR_DELIVERY);
        System.out.printf("[DeliveryHub] 🛵 Order #%d dispatched to Partner %s (Delivery #%d)!%n",
            order.getOrderId(), partner.getName(), deliveryId);

        return delivery;
    }

    // ==========================================
    // 10-Minute SLA Verification
    // ==========================================
    public synchronized void markDelivered(int deliveryId) {
        Delivery delivery = deliveries.get(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery record not found: " + deliveryId);
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setDeliveredAt(now);

        Order order = orderService.getOrder(delivery.getOrderId()).orElseThrow();
        Duration totalDuration = Duration.between(order.getPlacedAt(), now);
        boolean met = totalDuration.toMinutes() <= 10;
        delivery.setMetSla(met);

        orderService.updateStatus(order.getOrderId(), OrderStatus.DELIVERED);

        DeliveryPartner partner = partnerRepo.findById(delivery.getPartnerId()).orElseThrow();
        partner.setStatus(PartnerStatus.ONLINE);
        availablePartners.offer(partner.getPartnerId()); // Re-enqueue in FIFO queue

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.printf("║ DELIVERED: Order #%-6d fulfilled by %-18s ║%n", order.getOrderId(), partner.getName());
        System.out.printf("║ Total Fulfillment Time: %-2d minutes                       ║%n", totalDuration.toMinutes());
        System.out.printf("║ 10-Minute SLA Verdict: [%-28s] ║%n", (met ? "MET ✓ (Within 10 Mins)" : "BREACHED ✗ (> 10 Mins)"));
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // File Persistence: Append delivery log to data/deliveries.csv
        try {
            appendDeliveryToCsv(delivery, "data/deliveries.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Delivery marked in memory, failed to log to disk: " + e.getMessage());
        }

        // If orders are waiting, dispatch immediately
        if (!pendingOrders.isEmpty()) {
            Order next = pendingOrders.poll();
            dispatch(next);
        }
    }

    public synchronized void appendDeliveryToCsv(Delivery d, String filePath) throws IOException {
        File file = new File(filePath);
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# deliveryId,orderId,partnerId,dispatchedAt,deliveredAt,metSla,rating\n");
            }
            writer.write(String.format("%d,%d,%d,%s,%s,%b,%d%n",
                d.getDeliveryId(), d.getOrderId(), d.getPartnerId(),
                d.getDispatchedAt(), d.getDeliveredAt(), d.isMetSla(), d.getRating()));
        }
    }

    public void rateDelivery(int deliveryId, int stars) {
        Delivery d = deliveries.get(deliveryId);
        if (d != null) {
            d.setRating(stars);
            DeliveryPartner p = partnerRepo.findById(d.getPartnerId()).orElseThrow();
            p.addRating(stars);

            // Set demonstration: Track low-rated partners
            if (p.isLowRated()) {
                flaggedLowRatedPartners.add(p.getPartnerId());
            }
        }
    }

    public Optional<Delivery> findActiveDeliveryByPartner(int partnerId) {
        return deliveries.values().stream()
            .filter(d -> d.getPartnerId() == partnerId && d.getDeliveredAt() == null)
            .findFirst();
    }

    public String generateDeliverySLAReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== OPTIGRO 10-MINUTE SLA & DELIVERY REPORT =====\n");
        sb.append(String.format("%-8s %-8s %-16s %-14s %-10s %-8s%n",
            "DelivID", "OrderID", "Partner", "Time (Mins)", "SLA", "Rating"));
        sb.append("-".repeat(70)).append("\n");

        int total = 0;
        int metCount = 0;

        for (Delivery d : deliveries.values()) {
            total++;
            if (d.isMetSla()) metCount++;

            Order order = orderService.getOrder(d.getOrderId()).orElse(null);
            String timeStr = "In Transit";
            if (d.getDeliveredAt() != null && order != null) {
                long mins = Duration.between(order.getPlacedAt(), d.getDeliveredAt()).toMinutes();
                timeStr = mins + " mins";
            }

            DeliveryPartner p = partnerRepo.findById(d.getPartnerId()).orElse(null);
            String partnerName = p != null ? p.getName() : "Unknown";

            sb.append(String.format("%-8d %-8d %-16s %-14s %-10s %-8s%n",
                d.getDeliveryId(), d.getOrderId(), partnerName, timeStr,
                d.isMetSla() ? "MET ✓" : (d.getDeliveredAt() == null ? "PENDING" : "BREACH ✗"),
                d.getRating() > 0 ? d.getRating() + "★" : "-"));
        }

        sb.append(String.format("%nTotal Completed Deliveries: %d%n", total));
        if (total > 0) {
            sb.append(String.format("10-Minute SLA Adherence: %d/%d (%.1f%%)%n",
                metCount, total, (metCount * 100.0 / total)));
        }

        if (!flaggedLowRatedPartners.isEmpty()) {
            sb.append("\n⚠ Flagged Low-Rated Partners (< 3.0 Stars):\n");
            for (int pid : flaggedLowRatedPartners) {
                partnerRepo.findById(pid).ifPresent(p ->
                    sb.append(String.format(" • Partner #%d: %s (Avg: %.1f★)%n", pid, p.getName(), p.getAverageRating())));
            }
        }
        return sb.toString();
    }
}
```

---

### 10. `MainMenuController.java` — Master Router, Simulation & Report Export
**Path:** `src/com/optigro/controller/MainMenuController.java`

```java
package com.optigro.controller;

import com.optigro.constants.PaymentMode;
import com.optigro.constants.UserRole;
import com.optigro.exception.InvalidAuthException;
import com.optigro.models.Customer;
import com.optigro.models.DeliveryPartner;
import com.optigro.models.Picker;
import com.optigro.models.User;
import com.optigro.repository.CustomerRepository;
import com.optigro.repository.DeliveryPartnerRepository;
import com.optigro.repository.PickerRepository;
import com.optigro.repository.ProductRepository;
import com.optigro.service.*;
import com.optigro.util.ConsoleScannerUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MainMenuController {
    private final AuthService authService;
    private final InventoryService inventoryService;
    private final BinLayoutService binLayoutService;
    private final ExpiryService expiryService;
    private final CartService cartService;
    private final OrderService orderService;
    private final PickTaskService pickTaskService;
    private final DeliveryService deliveryService;
    private final PricingService pricingService;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final PickerRepository pickerRepo;
    private final DeliveryPartnerRepository partnerRepo;

    public MainMenuController(AuthService authService, InventoryService inventoryService,
                              BinLayoutService binLayoutService, ExpiryService expiryService,
                              CartService cartService, OrderService orderService,
                              PickTaskService pickTaskService, DeliveryService deliveryService,
                              PricingService pricingService, ProductRepository productRepo,
                              CustomerRepository customerRepo, PickerRepository pickerRepo,
                              DeliveryPartnerRepository partnerRepo) {
        this.authService = authService;
        this.inventoryService = inventoryService;
        this.binLayoutService = binLayoutService;
        this.expiryService = expiryService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.pickTaskService = pickTaskService;
        this.deliveryService = deliveryService;
        this.pricingService = pricingService;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.pickerRepo = pickerRepo;
        this.partnerRepo = partnerRepo;
    }

    public void start() {
        printBanner();

        while (true) {
            System.out.println("\n===== MAIN SYSTEM MENU =====");
            System.out.println("1. Login to Portal");
            System.out.println("2. Exit System");

            int choice = ConsoleScannerUtil.readIntInRange("Select option [1-2]: ", 1, 2);
            if (choice == 2) {
                System.out.println("\nThank you for using OptiGro Logistics Engine. Goodbye!");
                break;
            }

            System.out.println("\n--- OptiGro Secure Authentication ---");
            String username = ConsoleScannerUtil.readString("Enter Username: ");
            String password = ConsoleScannerUtil.readString("Enter Password: ");

            try {
                User user = authService.login(username, password);
                System.out.printf("✓ Login Successful! Welcome %s [Role: %s]%n", user.getUsername(), user.getRole());
                routeByRole(user);
            } catch (InvalidAuthException e) {
                System.out.println("[!] Login Error: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                        OPTIGRO                           ║");
        System.out.println("║     10-Minute Grocery Delivery Logistics Engine          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Default Demo Credentials (seeded from CSV):             ║");
        System.out.println("║  • Administrator:     admin    / admin123                ║");
        System.out.println("║  • Customer Profile:  cust1    / pass1                   ║");
        System.out.println("║  • Warehouse Picker:  picker1  / pass1                   ║");
        System.out.println("║  • Delivery Partner:  partner1 / pass1                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private void routeByRole(User user) {
        switch (user.getRole()) {
            case ADMIN:
                showAdminMenu();
                break;
            case CUSTOMER:
                Customer customer = customerRepo.findByUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Profile missing for user: " + user.getUsername()));
                new CustomerMenu(customer, cartService, orderService, inventoryService, pricingService).show();
                break;
            case PICKER:
                Picker picker = pickerRepo.findByUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Profile missing for picker user"));
                new PickingMenu(picker, pickTaskService, binLayoutService, null).show();
                break;
            case DELIVERY_PARTNER:
                DeliveryPartner partner = partnerRepo.findByUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Profile missing for partner user"));
                new DeliveryMenu(partner, deliveryService).show();
                break;
        }
    }

    private void showAdminMenu() {
        while (true) {
            System.out.println("\n===== ADMINISTRATOR CONTROL PANEL =====");
            System.out.println("1. Manage Inventory & Catalog (M1)");
            System.out.println("2. Warehouse Store Grid Layout (M1)");
            System.out.println("3. Run Live Multi-Threaded Simulation (M3 Concurrency Demo)");
            System.out.println("4. System Reports & Plain-Text File Export (M4)");
            System.out.println("5. Logout to Main Screen");

            int ch = ConsoleScannerUtil.readIntInRange("Admin Option [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    new InventoryMenu(inventoryService, binLayoutService, expiryService, productRepo).show();
                    break;
                case 2:
                    binLayoutService.printGrid();
                    break;
                case 3:
                    runLiveSimulation();
                    break;
                case 4:
                    reportsAndExportFlow();
                    break;
                case 5:
                    return;
            }
        }
    }

    // ========================================================
    // Live Simulation: The Evaluator Demo Highlight
    // ========================================================
    private void runLiveSimulation() {
        System.out.println("\n========================================================");
        System.out.println("      LAUNCHING LIVE MULTI-THREADED DEMO SIMULATION     ");
        System.out.println("========================================================");

        // 1. Ensure Delivery Partner is Online
        deliveryService.registerPartnerOnline(6001);

        // 2. Start 3 Concurrent Background Picker Threads
        pickTaskService.startPickers(3);

        // 3. Auto-place 3 orders with varying products
        System.out.println("\n[Simulation] Auto-placing 3 customer orders...");
        try {
            Map<Integer, Integer> cart1 = new HashMap<>();
            cart1.put(1001, 2); // Milk
            cart1.put(1010, 1); // Chips
            orderService.placeOrder(2001, cart1, PaymentMode.WALLET, "WELCOME50");

            Map<Integer, Integer> cart2 = new HashMap<>();
            cart2.put(1005, 1); // Apples
            cart2.put(1018, 1); // Rice
            orderService.placeOrder(2002, cart2, PaymentMode.UPI, null);

            // Let worker threads pick orders visually
            System.out.println("\n[Simulation] Pickers are actively processing queue in background...");
            Thread.sleep(3500);

        } catch (Exception e) {
            System.out.println("[Simulation Error] " + e.getMessage());
        } finally {
            pickTaskService.stopPickers();
        }
        System.out.println("\n[Simulation] Live run completed successfully!");
    }

    private void reportsAndExportFlow() {
        System.out.println("\n===== CONSOLIDATED SYSTEM REPORTS =====");
        System.out.println("1. Print Stock Report (M1)");
        System.out.println("2. Print Sales Report (M2)");
        System.out.println("3. Print Picker Performance Report (M3)");
        System.out.println("4. Print Delivery SLA Report (M4)");
        System.out.println("5. Export All Reports to reports/full_report.txt (File I/O)");
        System.out.println("6. Back");

        int ch = ConsoleScannerUtil.readIntInRange("Choose report [1-6]: ", 1, 6);
        switch (ch) {
            case 1: System.out.println(inventoryService.generateStockReport()); break;
            case 2: System.out.println(orderService.generateSalesReport()); break;
            case 3: System.out.println(pickTaskService.generatePickerReport()); break;
            case 4: System.out.println(deliveryService.generateDeliverySLAReport()); break;
            case 5: exportAllReportsToFile(); break;
            case 6: return;
        }
    }

    // Demonstrates File I/O: BufferedWriter writing full summary to reports/full_report.txt
    private void exportAllReportsToFile() {
        new File("reports").mkdirs();
        String path = "reports/full_report.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("╔══════════════════════════════════════════════════════════╗\n");
            writer.write("║                 OPTIGRO - FULL SYSTEM AUDIT REPORT       ║\n");
            writer.write("║                 Generated: " + LocalDateTime.now() + "     ║\n");
            writer.write("╚══════════════════════════════════════════════════════════╝\n\n");

            writer.write(inventoryService.generateStockReport());
            writer.write("\n\n");
            writer.write(orderService.generateSalesReport());
            writer.write("\n\n");
            writer.write(pickTaskService.generatePickerReport());
            writer.write("\n\n");
            writer.write(deliveryService.generateDeliverySLAReport());

            System.out.println("✓ Complete consolidated audit report exported to " + path + "!");
        } catch (IOException e) {
            System.out.println("[!] Error writing export file: " + e.getMessage());
        }
    }
}
```

---

### 11. `DeliveryMenu.java` — Delivery Partner Terminal Controller
**Path:** `src/com/optigro/controller/DeliveryMenu.java`

```java
package com.optigro.controller;

import com.optigro.constants.PartnerStatus;
import com.optigro.models.Delivery;
import com.optigro.models.DeliveryPartner;
import com.optigro.service.DeliveryService;
import com.optigro.util.ConsoleScannerUtil;

import java.util.Optional;

public class DeliveryMenu {
    private final DeliveryPartner partner;
    private final DeliveryService deliveryService;

    public DeliveryMenu(DeliveryPartner partner, DeliveryService deliveryService) {
        this.partner = partner;
        this.deliveryService = deliveryService;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== DELIVERY PARTNER PORTAL | %s (Status: %s) =====%n",
                partner.getName(), partner.getStatus());
            System.out.println("1. Toggle Status (Go Online / Offline)");
            System.out.println("2. View Assigned Active Order");
            System.out.println("3. Mark Order as Delivered (Calculates 10-Min SLA)");
            System.out.println("4. My Performance & Customer Ratings");
            System.out.println("5. Logout to Main Menu");

            int ch = ConsoleScannerUtil.readIntInRange("Choose [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    if (partner.getStatus() == PartnerStatus.ONLINE) {
                        deliveryService.registerPartnerOffline(partner.getPartnerId());
                    } else if (partner.getStatus() == PartnerStatus.OFFLINE) {
                        deliveryService.registerPartnerOnline(partner.getPartnerId());
                    } else {
                        System.out.println("[!] Cannot go offline while actively delivering an order.");
                    }
                    break;

                case 2:
                    Optional<Delivery> active = deliveryService.findActiveDeliveryByPartner(partner.getPartnerId());
                    if (active.isEmpty()) {
                        System.out.println("No active order currently assigned. Status: " + partner.getStatus());
                    } else {
                        System.out.println("\n--- Assigned Active Delivery ---");
                        System.out.println(active.get());
                    }
                    break;

                case 3:
                    Optional<Delivery> deliveryToComplete = deliveryService.findActiveDeliveryByPartner(partner.getPartnerId());
                    if (deliveryToComplete.isEmpty()) {
                        System.out.println("[!] No active order assigned to mark delivered.");
                    } else {
                        deliveryService.markDelivered(deliveryToComplete.get().getDeliveryId());
                        System.out.println("✓ Order marked as DELIVERED!");
                    }
                    break;

                case 4:
                    System.out.println("\n--- My Performance Dashboard ---");
                    System.out.printf("Deliveries Completed: %d%n", partner.getDeliveriesDone());
                    System.out.printf("Average Customer Rating: %.2f / 5.0★%n", partner.getAverageRating());
                    if (partner.isLowRated()) {
                        System.out.println("⚠ Notice: Your average rating is below 3.0 stars. Quality review required.");
                    }
                    break;

                case 5:
                    return;
            }
        }
    }
}
```

---

### 12. `InvalidAuthException.java` — Custom Exception
**Path:** `src/com/optigro/exception/InvalidAuthException.java`

```java
package com.optigro.exception;

public class InvalidAuthException extends Exception {
    public InvalidAuthException(String message) {
        super(message);
    }
}
```

---

### 13. `App.java` — Master Manual Dependency Injection ⚡
**Path:** `src/com/optigro/App.java`

> ⚠️ **This is the single most important file in the entire project.**

```java
package com.optigro;

import com.optigro.config.DataInitializer;
import com.optigro.controller.MainMenuController;
import com.optigro.repository.*;
import com.optigro.service.*;

public class App {
    public static void main(String[] args) {
        System.out.println("Initializing OptiGro 10-Minute Grocery Logistics Engine...");

        // ==========================================
        // 1. REPOSITORIES (Data Access Layer)
        // ==========================================
        ProductRepository productRepo = new ProductRepository();
        CustomerRepository customerRepo = new CustomerRepository();
        OrderRepository orderRepo = new OrderRepository();
        PickerRepository pickerRepo = new PickerRepository();
        PickTaskRepository taskRepo = new PickTaskRepository();
        UserRepository userRepo = new UserRepository();
        DeliveryPartnerRepository partnerRepo = new DeliveryPartnerRepository();

        // ==========================================
        // 2. SERVICES (Constructor Injection)
        // ==========================================
        // Layer 1: Core Foundation Services
        InventoryService inventory = new InventoryService(productRepo);
        BinLayoutService layout = new BinLayoutService(inventory);
        ExpiryService expiry = new ExpiryService(productRepo);
        PricingService pricing = new PricingService();
        AuthService auth = new AuthService(userRepo);

        // Layer 2: Domain Logic Services
        CartService cart = new CartService(inventory);
        SubstitutionService substitutions = new SubstitutionService(inventory);
        PickRoutePlanner planner = new PickRoutePlanner(layout);
        OrderService orders = new OrderService(orderRepo, customerRepo, inventory, pricing);

        // Layer 3: Logistics & Fulfillment Services
        DeliveryService delivery = new DeliveryService(partnerRepo, orders);
        PickTaskService picking = new PickTaskService(
            taskRepo, pickerRepo, orders, inventory, planner, substitutions);

        // ==========================================
        // 3. DECOUPLED CALLBACK WIRING
        // ==========================================
        // When Harnoor (M2) places order -> triggers Kritika (M3) picking
        orders.setOnOrderPlaced(picking::createTask);

        // When Kritika (M3) finishes packing -> triggers Aditya (M4) delivery
        picking.setOnOrderPacked(delivery::dispatch);

        // ==========================================
        // 4. PERSISTENCE BOOTSTRAPPER (Load CSVs)
        // ==========================================
        DataInitializer.initialize(userRepo, productRepo, layout, customerRepo,
            pickerRepo, partnerRepo, orders);

        // ==========================================
        // 5. LAUNCH USER INTERFACE
        // ==========================================
        new MainMenuController(auth, inventory, layout, expiry, cart, orders,
            picking, delivery, pricing, productRepo, customerRepo, pickerRepo, partnerRepo).start();
    }
}
```

---

## 📋 Sprint-wise Task Checklist

### Sprint 0 (Day 1) — Setup
- [ ] Clone repo, checkout `m4/delivery-platform`.
- [ ] Publish `UserRole.java` and `PartnerStatus.java`.
- [ ] Verify `data/users.csv` and `data/partners.csv` exist.

### Sprint 1 — Models & Repositories
- [ ] `User.java` (`equals` & `hashCode` by `userId`, failed attempts counter).
- [ ] `DeliveryPartner.java` (`equals` & `hashCode` by `partnerId`, average rating).
- [ ] `Delivery.java` (`equals` & `hashCode` by `deliveryId`).
- [ ] `UserRepository.java` (`loadFromCsv("data/users.csv")`).
- [ ] `DeliveryPartnerRepository.java` (`loadFromCsv("data/partners.csv")`).

### Sprint 2 — Services
- [ ] `AuthService.java` (login with `Set<String> lockedUsernames`).
- [ ] `DeliveryService.java` (`Queue<Integer>` via `ArrayDeque`, `Duration` SLA math, `Set` flagged partners, CSV append).
- [ ] `InvalidAuthException.java`.

### Sprint 3 — Controllers & Integration (THE BIG SPRINT)
- [ ] `MainMenuController.java` (login loop, role routing, admin panel, simulation runner).
- [ ] `DeliveryMenu.java` (partner actions, SLA results).
- [ ] `App.java` (master wiring of all 52 files, callbacks, and CSV bootstrapper).
- [ ] Test end-to-end order flow from login to delivered.

### Sprint 4 — Hardening & Docs
- [ ] Test account lockout after 3 consecutive wrong passwords.
- [ ] Write Project Report / README & Presentation (your doc deliverable).
- [ ] Author "My Module" page (1 page).
- [ ] Fill OOP concepts map rows.
- [ ] Teach-back prep: Queues, `Duration`, Constructor Injection, `Set` for account security, CSV delivery logging.

---

## 🎓 Viva Preparation

### Your Java Topics to Master
1. **FIFO Queue (`ArrayDeque`):** Why use `ArrayDeque` for delivery partners? (O(1) offer/poll efficiency, FIFO fairness so the longest-waiting partner receives the next delivery, avoids linked list node allocation overhead).
2. **`Set` Interface in Auth (`lockedUsernames`):** Why use a `Set<String>` for account lockouts? (O(1) constant-time membership lookups before database or repository queries; naturally prevents duplicate lockout entries).
3. **`java.time.Duration` Math:** How is the 10-minute SLA validated? (`Duration.between(order.getPlacedAt(), deliveredAt).toMinutes() <= 10`).
4. **File Persistence vs. Memory:** How does `data/deliveries.csv` ensure historical delivery logs survive program restarts? (Uses `BufferedWriter` with `FileWriter(file, true)` in append mode).
5. **Constructor Injection in `App.java`:** What advantages does manual dependency injection offer? (Explicit dependencies, no hidden global singletons, simplified mock unit testing, compile-time validation of component wiring).
