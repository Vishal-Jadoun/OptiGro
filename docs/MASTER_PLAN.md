# 📋 MASTER PLAN — OptiGro: 10-Minute Grocery Delivery Logistics Engine

> **System Name:** OptiGro (10-Minute Dark Store Grocery Delivery Logistics Engine)  
> **Environment:** Pure Java Standard Library (Java 11+), Terminal Console Interface (No external frameworks)  
> **Repository:** [https://github.com/Vishal-Jadoun/OptiGro.git](https://github.com/Vishal-Jadoun/OptiGro.git)  
> **Team Lead:** Vishal (M1)  
> **Total Files:** 52 Java source files across 4 vertical slices + persistent CSV files in `data/`  
> **Target Course Evaluation:** Programming in Java (25CSE0202) — 25 Marks Total

---

## 📌 Project Overview & Design Philosophy

OptiGro simulates an ultra-fast quick-commerce micro-fulfillment center (dark store) modeled after industry platforms like Blinkit and Zepto:
* A dark store is a compact, localized warehouse closed to the general public, designed exclusively for rapid 10-minute automated fulfillment of online grocery orders.
* **Architecture:** 4-Tier Layered Architecture (`Controller` → `Service` → `Repository` → `Model`), ensuring total separation of concerns.
* **Equal Contribution Principle:** 4 team members, exactly 13 Java class/interface files each. Each member writes their own models, repository, service, and console controller, mastering core full-stack Java patterns.
* **True Persistence:** Seed data and live transactions (placed orders, stock levels, delivery logs) persist on disk across console restarts via CSV files in the `data/` folder.

---

## 🔄 Complete Order Lifecycle Flow (The Demo Story)

```text
Customer places order (via CustomerMenu)
        │
        ├── OrderService validates cart & calculates GST via Streams API (PricingService)
        ├── Synchronized stock reservation: InventoryService.reserveStock() [atomic with rollback]
        ├── Persists order to disk: data/orders.csv
        ▼
  onOrderPlaced Callback Fires ──► PickTaskService.createTask(order)
        │
        ├── PickTask instantiated (priority=1 for express >= Rs.500, else priority=5)
        └── Enqueued into thread-safe PriorityBlockingQueue<PickTask>
        │
        ▼
  Background PickerWorker thread takes task (blocks via PriorityBlockingQueue.take())
        │
        ├── PickRoutePlanner orders items by warehouse aisle snake-route (custom Comparator)
        ├── BinLayoutService locates 2D grid coordinates (aisle & shelf)
        │
        ├── For each item in route:
        │     ├── Item in stock ──► Marked in Set<Integer> pickedProductIds ──► Picked ✓
        │     └── Item OOS ──────► SubstitutionService finds closest-priced substitute (same category)
        │                            ├── Substitute accepted & reserved ──► Marked in Set ──► Picked ✓
        │                            └── No substitute available ──────► Skipped ✗
        ▼
  Order PACKED ──► Appended to data/pick_history.csv
        │
        ▼
  onOrderPacked Callback Fires ──► DeliveryService.dispatch(order)
        │
        ├── Dequeues next available partner from FIFO Queue<DeliveryPartner> (ArrayDeque)
        └── (If no partner online: order enters pendingOrders queue until partner logs in)
        ▼
  Partner marks OUT_FOR_DELIVERY ──► DELIVERED (DeliveryMenu)
        │
        ├── 10-Minute SLA Check: Duration.between(order.getPlacedAt(), deliveredAt).toMinutes() <= 10?
        ├── Appended to data/deliveries.csv
        ├── Customer star rating recorded (low-rated partners added to Set<Integer> flaggedPartners)
        └── Consolidated Reports exported to reports/full_report.txt via BufferedWriter
```

---

## 🗂️ Complete Directory & 52-File Architecture

Tags show the individual owner of each file: `[M1: Vishal]`, `[M2: Harnoor]`, `[M3: Kritika]`, `[M4: Aditya]`.

```text
src/
└── com/
    └── optigro/
        ├── App.java                          [M4: Aditya]  # Entry point & manual dependency injection wiring
        ├── config/
        │   └── DataInitializer.java          [M3: Kritika]  # Master bootstrapper loading all data/*.csv files
        ├── constants/
        │   ├── ProductCategory.java          [M1: Vishal]  # Enum: DAIRY, FRUITS, VEGETABLES, SNACKS, BEVERAGES, STAPLES
        │   ├── OrderStatus.java              [M2: Harnoor]  # Enum: State machine with next() and canCancel()
        │   ├── PaymentMode.java              [M2: Harnoor]  # Enum: COD, WALLET, UPI
        │   ├── TaskStatus.java               [M3: Kritika]  # Enum: PENDING, IN_PROGRESS, COMPLETED, FAILED
        │   ├── PartnerStatus.java            [M4: Aditya]  # Enum: ONLINE, OFFLINE, ON_DELIVERY
        │   └── UserRole.java                 [M4: Aditya]  # Enum: ADMIN, CUSTOMER, PICKER, DELIVERY_PARTNER
        ├── models/
        │   ├── Product.java                  [M1: Vishal]  # Abstract base class (equals & hashCode by id)
        │   ├── PerishableProduct.java        [M1: Vishal]  # Extends Product, adds LocalDate expiryDate
        │   ├── NonPerishableProduct.java     [M1: Vishal]  # Extends Product, adds shelfLifeMonths
        │   ├── Bin.java                      [M1: Vishal]  # Aisle/shelf/productId/capacity (equals/hashCode by aisle+shelf)
        │   ├── Customer.java                 [M2: Harnoor]  # Profile, wallet, and Set<String> usedCoupons (equals/hashCode)
        │   ├── Order.java                    [M2: Harnoor]  # Order header, status transition validation (equals/hashCode)
        │   ├── OrderItem.java                [M2: Harnoor]  # Line item: productId, quantity, unitPrice (equals/hashCode)
        │   ├── Picker.java                   [M3: Kritika]  # Warehouse picker profile (equals/hashCode by pickerId)
        │   ├── PickTask.java                 [M3: Kritika]  # implements Comparable<PickTask>, holds Set<Integer> picked items
        │   ├── Substitution.java             [M3: Kritika]  # Replacement audit record (equals/hashCode)
        │   ├── User.java                     [M4: Aditya]  # Credentials, role, failed attempts lockout (equals/hashCode)
        │   ├── DeliveryPartner.java          [M4: Aditya]  # Partner profile, ratings, isLowRated() (equals/hashCode)
        │   └── Delivery.java                 [M4: Aditya]  # Delivery record, SLA metrics (equals/hashCode by deliveryId)
        ├── repository/
        │   ├── BaseRepository.java           [M1: Vishal]  # Generic interface BaseRepository<T>
        │   ├── ProductRepository.java        [M1: Vishal]  # ConcurrentHashMap + loadFromCsv/saveToCsv (products.csv)
        │   ├── CustomerRepository.java       [M2: Harnoor]  # ConcurrentHashMap + loadFromCsv (customers.csv)
        │   ├── OrderRepository.java          [M2: Harnoor]  # ConcurrentHashMap + appendOrderToCsv (orders.csv)
        │   ├── PickerRepository.java         [M3: Kritika]  # ConcurrentHashMap + loadFromCsv (pickers.csv)
        │   ├── PickTaskRepository.java       [M3: Kritika]  # ConcurrentHashMap + appendTaskHistoryToCsv (pick_history.csv)
        │   ├── UserRepository.java           [M4: Aditya]  # ConcurrentHashMap + loadFromCsv (users.csv)
        │   └── DeliveryPartnerRepository.java[M4: Aditya]  # ConcurrentHashMap + loadFromCsv (partners.csv)
        ├── service/
        │   ├── InventoryService.java         [M1: Vishal]  # Synchronized reserve/release, Set active categories, search
        │   ├── ExpiryService.java            [M1: Vishal]  # TreeMap<LocalDate, ...> expiry tracking & headMap queries
        │   ├── BinLayoutService.java         [M1: Vishal]  # 2D Bin[][] grid, locate, Manhattan distance, printGrid
        │   ├── CartService.java              [M2: Harnoor]  # In-memory customer cart, availability validation
        │   ├── PricingService.java           [M2: Harnoor]  # Streams API GST calculation, coupon Set check, delivery fees
        │   ├── OrderService.java             [M2: Harnoor]  # placeOrder with atomic rollback, CSV persistence, callback
        │   ├── PickTaskService.java          [M3: Kritika]  # PriorityBlockingQueue, worker thread lifecycle, callback
        │   ├── SubstitutionService.java      [M3: Kritika]  # Nearest price substitute in same category
        │   ├── PickRoutePlanner.java         [M3: Kritika]  # Snake-route aisle sorting with custom Comparator
        │   ├── PickerWorker.java             [M3: Kritika]  # Runnable, concurrent worker thread execution loop
        │   ├── AuthService.java              [M4: Aditya]  # Login, lockout counter, Set<String> lockedUsernames
        │   └── DeliveryService.java          [M4: Aditya]  # ArrayDeque partner queue, 10-min SLA Duration, CSV logging
        ├── controller/
        │   ├── MainMenuController.java       [M4: Aditya]  # Login loop, role router, admin panel, live simulation
        │   ├── InventoryMenu.java            [M1: Vishal]  # Admin inventory portal (catalog, stock, grid, alerts)
        │   ├── CustomerMenu.java             [M2: Harnoor]  # Customer portal (search, cart, GST invoice, order tracking)
        │   ├── PickingMenu.java              [M3: Kritika]  # Picker terminal (view tasks, manual pick, warehouse view)
        │   └── DeliveryMenu.java             [M4: Aditya]  # Partner terminal (online/offline, deliver order, ratings)
        ├── exception/
        │   ├── OutOfStockException.java      [M1: Vishal]  # Checked exception: stock insufficient or expired
        │   ├── InvalidCouponException.java   [M2: Harnoor]  # Checked exception: unrecognized or already-used coupon
        │   ├── NoPickerAvailableException.java [M3: Kritika] # Checked exception: no pickers free
        │   └── InvalidAuthException.java     [M4: Aditya]  # Checked exception: invalid credentials or locked account
        └── util/
            ├── ConsoleScannerUtil.java       [M1: Vishal]  # Safe scanner utility, input validation (never crashes)
            └── AsciiTableFormatter.java      [M2: Harnoor]  # Reusable ASCII table, invoice & divider formatter

data/                                         # File Persistence Directory (CSV files)
├── products.csv                              [M1: Vishal]  # Catalog seed & live inventory stock levels
├── customers.csv                             [M2: Harnoor]  # Customer seed profiles & initial wallet balances
├── orders.csv                                [M2: Harnoor]  # Persistent append log of all placed customer orders
├── pickers.csv                               [M3: Kritika]  # Warehouse picker seed profiles & shift status
├── pick_history.csv                          [M3: Kritika]  # Persistent append log of completed pick tasks & subs
├── users.csv                                 [M4: Aditya]  # User login credentials & privilege roles
├── partners.csv                              [M4: Aditya]  # Delivery partner seed profiles & historical ratings
└── deliveries.csv                            [M4: Aditya]  # Persistent append log of completed deliveries with SLA

reports/                                      # Export Directory
└── full_report.txt                           [M4: Aditya]  # Consolidated text export generated via BufferedWriter
```

---

## ⚖️ Equal Contribution Table (13 Files + File Persistence Per Member)

| Layer | M1: Vishal (Inventory) | M2: Harnoor (Orders) | M3: Kritika (Picking) | M4: Aditya (Platform) |
|---|---|---|---|---|
| **Enums** | 1 (`ProductCategory`) | 2 (`OrderStatus`, `PaymentMode`) | 1 (`TaskStatus`) | 2 (`PartnerStatus`, `UserRole`) |
| **Models** | 4 (`Product`, `Perishable`, `NonPerishable`, `Bin`) | 3 (`Customer`, `Order`, `OrderItem`) | 3 (`Picker`, `PickTask`, `Substitution`) | 3 (`User`, `DeliveryPartner`, `Delivery`) |
| **Repositories** | 2 (`BaseRepo`, `ProductRepo`) | 2 (`CustomerRepo`, `OrderRepo`) | 2 (`PickerRepo`, `PickTaskRepo`) | 2 (`UserRepo`, `DeliveryPartnerRepo`) |
| **Services** | 3 (`Inventory`, `Expiry`, `BinLayout`) | 3 (`Cart`, `Pricing`, `Order`) | 4 (`PickTask`, `Substitution`, `RoutePlanner`, `PickerWorker`) | 2 (`Auth`, `Delivery`) |
| **Controllers** | 1 (`InventoryMenu`) | 1 (`CustomerMenu`) | 1 (`PickingMenu`) | 2 (`MainMenu`, `DeliveryMenu`) |
| **Exceptions** | 1 (`OutOfStockException`) | 1 (`InvalidCouponException`) | 1 (`NoPickerException`) | 1 (`InvalidAuthException`) |
| **Utilities / Core** | 1 (`ConsoleScannerUtil`) | 1 (`AsciiTableFormatter`) | 1 (`DataInitializer`) | 1 (`App.java`) |
| **Total Java Files** | **13** | **13** | **13** | **13** |
| **File Persistence** | `products.csv` (read/save) | `customers.csv` (read), `orders.csv` (append) | `pickers.csv` (read), `pick_history.csv` (append) | `users.csv`, `partners.csv` (read), `deliveries.csv` (append) |
| **Set Interface** | `getActiveCategories()` | `usedCoupons` | `pickedProductIds` | `lockedUsernames`, `flaggedPartners` |
| **equals/hashCode** | `Product`, `Bin` | `Customer`, `Order`, `OrderItem` | `Picker`, `PickTask`, `Substitution` | `User`, `Partner`, `Delivery` |

---

## 📊 Entity-Relationship Diagram (ERD Schema)

```text
 [USER] 1───1 [CUSTOMER] 1────────N [ORDER] 1────────N [ORDER_ITEM] N────1 [PRODUCT]
   │                                  │ 1                                    ▲   │ 1
   │                                  │                                      │   │
   ├──1───1 [PICKER]                  │ 1                          ┌─────────┴───┐ N
   │          │ 1                     ▼ 1                     [Perishable]  [NonPerishable]   [BIN]
   │          │                    [PICK_TASK] 1────N [SUBSTITUTION]                   (aisle, shelf)
   │          │ N                     ▲ N  (original product → substitute product)
   │          └───────────────────────┘
   │
   └──1───1 [DELIVERY_PARTNER] 1────N [DELIVERY] N────1 [ORDER]
```

### Data Dictionary

| Entity | Primary Key / Natural Key | Key Attributes | Relationships |
|---|---|---|---|
| **User** | `userId` | `username`, `password`, `role`, `failedAttempts`, `locked` | 1 ↔ 1 Customer / Picker / Partner profile |
| **Customer** | `customerId` | `userId`, `name`, `phone`, `address`, `walletBalance`, `usedCoupons` (Set) | 1 → N Orders |
| **Product** (abstract) | `id` | `name`, `category`, `price`, `stock`, `reorderLevel` | 1 → N OrderItems, N Bins |
| **PerishableProduct** | `id` (inherited) | `expiryDate` (`LocalDate`) | Extends `Product` |
| **NonPerishableProduct**| `id` (inherited) | `shelfLifeMonths` (int) | Extends `Product` |
| **Bin** | (`aisle`, `shelf`) | `productId`, `capacity` | Each bin holds 1 product type in grid |
| **Order** | `orderId` | `customerId`, `status`, `paymentMode`, `subtotal`, `gst`, `deliveryFee`, `discount`, `total`, `placedAt` | 1 → N OrderItems, 1 PickTask, 1 Delivery |
| **OrderItem** | `productId` in Order | `productName`, `quantity`, `unitPrice` | Belongs to 1 Order |
| **Picker** | `pickerId` | `userId`, `name`, `available`, `tasksCompleted` | 1 → N PickTasks |
| **PickTask** | `taskId` | `orderId`, `pickerId`, `status`, `createdAt`, `priority`, `pickedProductIds` (Set) | 1 → N Substitutions |
| **Substitution** | (`originalId`, `subId`) | `originalName`, `substituteName`, `accepted` | Belongs to 1 PickTask |
| **DeliveryPartner** | `partnerId` | `userId`, `name`, `status`, `totalRating`, `deliveriesDone` | 1 → N Deliveries |
| **Delivery** | `deliveryId` | `orderId`, `partnerId`, `dispatchedAt`, `deliveredAt`, `metSla`, `rating` | 1 ↔ 1 Order |

---

## 🎯 Syllabus & Rubric Compliance (25CSE0202 — 25 Marks)

| Evaluation Component | Marks | How OptiGro Achieves Maximum Marks |
|---|---|---|
| **1. Problem Statement** | **3** | Dark store micro-fulfillment center logistics engine solving 10-minute instant delivery bottleneck (inventory, smart pick routing, queue dispatch). Identifiable user roles: Administrator, Customer, Warehouse Picker, Delivery Partner. |
| **2. Objectives & Scope** | **2** | Realistic boundary: 20+ products, 6 categories, 4 picker worker threads, 4 delivery partners, 10-minute SLA validation, and order lifecycle state machine. |
| **3. Project Design & Prototype** | **9** | Layered 4-tier architecture, clean separation of concerns, 52 classes, defensive console I/O (never crashes on bad input), and interactive demo simulation. |
| **4. Integration of Course Concepts** | **6** | **100% Complete:**<br>• **OOP:** Inheritance, Abstract Classes, Polymorphism, Encapsulation, `equals()` & `hashCode()` contracts on all models.<br>• **Collections:** `List`, `Map`, `Set` (`HashSet`), `SortedMap` (`TreeMap`), `Queue` (`ArrayDeque`), `BlockingQueue` (`PriorityBlockingQueue`).<br>• **Concurrency:** `Runnable`, `Thread.sleep()`, `ExecutorService`, `synchronized` methods.<br>• **File Persistence:** CSV loading via `BufferedReader` & persistent appending via `BufferedWriter`.<br>• **Modern Java:** Streams API, Lambdas, `Consumer<T>`, `java.time` (`Duration`). |
| **5. Code Quality & Evidence** | **5** | Clean modular packages (`com.optigro.*`), Git history across 4 member branches, zero compiler warnings, and full markdown documentation. |

---

## 🔌 Frozen Integration Contracts

> ⚠️ Method signatures below are frozen so each member can code in parallel.

### M1 provides (Vishal - Inventory & Store Layout):
```java
// InventoryService
Optional<Product> getProduct(int productId);
List<Product> searchByName(String keyword);
List<Product> getByCategory(ProductCategory category);
Set<ProductCategory> getActiveCategories();                                      // Set interface
boolean isAvailable(int productId, int qty);
void reserveStock(int productId, int qty) throws OutOfStockException;          // synchronized
void releaseStock(int productId, int qty);                                      // synchronized
List<Product> getLowStock();
List<Product> findSimilar(Product original);

// BinLayoutService
Bin locate(int productId);
int distance(Bin a, Bin b);
void printGrid();

// ExpiryService
List<PerishableProduct> expiringWithinDays(int days);
List<PerishableProduct> getExpired();
```

### M2 provides (Harnoor - Customer & Orders):
```java
// OrderService
Order placeOrder(int customerId, Map<Integer,Integer> cartItems,
                 PaymentMode mode, String couponCode) throws OutOfStockException, InvalidCouponException;
Optional<Order> getOrder(int orderId);
void updateStatus(int orderId, OrderStatus newStatus);
List<Order> getOrdersByCustomer(int customerId);
void setOnOrderPlaced(Consumer<Order> listener);                                 // callback to M3

// PricingService
double calculateSubtotal(List<OrderItem> items);
double calculateGST(List<OrderItem> items, InventoryService inventory);
double applyCoupon(String couponCode, double subtotal, Customer customer) throws InvalidCouponException;
double calculateDeliveryFee(double subtotal, String couponCode);
```

### M3 provides (Kritika - Picking Engine):
```java
// PickTaskService
PickTask createTask(Order order);
PickTask takeNextTask() throws InterruptedException;
void processTask(Picker picker, PickTask task);
void setOnOrderPacked(Consumer<Order> listener);                                 // callback to M4
void startPickers(int count);
void stopPickers();

// SubstitutionService
Optional<Product> suggestSubstitute(Product outOfStockProduct, int qty);
```

### M4 provides (Aditya - Security & Delivery):
```java
// AuthService
User login(String username, String password) throws InvalidAuthException;

// DeliveryService
Delivery dispatch(Order order);
void markDelivered(int deliveryId);
void rateDelivery(int deliveryId, int stars);
void registerPartnerOnline(int partnerId);
void registerPartnerOffline(int partnerId);
```

---

## 📅 Sprint Plan & Milestones

| Sprint | Goal | Deliverable & Verification |
|---|---|---|
| **Sprint 0: Setup** | Initialize Git repo, create folder structure, create `data/*.csv` seed files, publish `ConsoleScannerUtil.java`, `BaseRepository.java`, and enums. | Repo cloned by all 4 members; empty stubs compile. |
| **Sprint 1: Models & Repos** | All 13 models written with `equals()` & `hashCode()`. Repositories implemented with CSV file loading (`BufferedReader`). | All models and repositories compile; `loadFromCsv()` reads records cleanly. |
| **Sprint 2: Services** | Business logic implemented: `synchronized` inventory, Streams GST calculation, `Set` coupon checks, worker threads (`PickerWorker`), `ArrayDeque` delivery dispatch. | Slices tested with temporary `main()` methods; unit checks pass. |
| **Sprint 3: Menus & Integration** | Connect CLI menus to services; wire decoupled callbacks in `App.java`; integrate `DataInitializer.java`; execute end-to-end order flow. | End-to-end run: Login → Cart → Order → Pick → Deliver → SLA verified. |
| **Sprint 4: Hardening & Docs** | Defensive input verification (zero unhandled exceptions); report export via `BufferedWriter`; complete documentation deliverables; mock viva teach-back. | Clean `javac` build; zero warnings; 5-minute demo script runs flawlessly. |

---

## 🎯 Java Concept Map (Complete Syllabus Coverage)

| Java Concept | Where It Is Used | Owner |
|---|---|---|
| **Abstraction & Polymorphism** | `Product`, `PerishableProduct`, `NonPerishableProduct` (`isSellable()`, `getStorageInfo()`) | M1 (Vishal) |
| **`equals()` & `hashCode()` Contract** | All 11 model classes (based on ID / natural coordinates) | **All 4 Members** |
| **Generics & Generic Interfaces** | `BaseRepository<T>` and 7 repository implementations | M1 (Vishal) |
| **2D Arrays & Coordinate Math** | `Bin[4][5]` warehouse shelf layout & Manhattan distance calculation | M1 (Vishal) |
| **Sorted Collections (`TreeMap`)** | Expiry date range tracking via `headMap()` | M1 (Vishal) |
| **`Set` & `HashSet` (Unique Elements)** | `getActiveCategories()`, `usedCoupons`, `pickedProductIds`, `lockedUsernames`, `flaggedPartners` | **All 4 Members** |
| **Enums with Methods (State Machine)** | `OrderStatus.next()`, `OrderStatus.canCancel()` | M2 (Harnoor) |
| **Streams API & Collectors** | GST calculation (`mapToDouble`), sales grouping (`groupingBy`), category filtering | M2 (Harnoor) |
| **Functional Interfaces & Callbacks** | `Consumer<Order>` callbacks breaking circular dependencies | Harnoor, Kritika (Kritika) |
| **Multithreading & Concurrency** | `Runnable` (`PickerWorker`), `ExecutorService`, `Thread.sleep()` | M3 (Kritika) |
| **Priority Queue (`BlockingQueue`)** | `PriorityBlockingQueue<PickTask>` (express priority first, FIFO fallback) | M3 (Kritika) |
| **Comparable vs. Comparator** | `PickTask.compareTo()` (natural order) vs. `PickRoutePlanner` (aisle snake-route parity) | M3 (Kritika) |
| **Thread Synchronization** | `synchronized reserveStock()` and `releaseStock()` preventing overselling race conditions | M1 (Vishal) |
| **Linear FIFO Queue** | `Queue<DeliveryPartner>` and pending orders using `ArrayDeque` | M4 (Aditya) |
| **Time & Duration Math** | `java.time.Duration.between(placedAt, deliveredAt)` for 10-minute SLA verification | M4 (Aditya) |
| **File Persistence (CSV & Text I/O)** | `BufferedReader` reading `data/*.csv`, `BufferedWriter` appending logs, and exporting reports | **All 4 Members** |
| **Custom Checked Exceptions** | `OutOfStockException`, `InvalidCouponException`, `NoPickerAvailableException`, `InvalidAuthException` | **All 4 Members** |
| **Manual Dependency Injection** | Constructor injection in `App.java` (wiring all 52 classes cleanly without frameworks) | M4 (Aditya) |

---

## ⚡ How to Build and Run

### Windows (PowerShell or Command Prompt)
```cmd
# Navigate to repository root
cd X:\SEM3\Clone\OptiGro

# Collect all source files
dir /s /b src\*.java > sources.txt

# Compile cleanly into out/
javac -d out @sources.txt

# Launch OptiGro
java -cp out com.optigro.App
```

### macOS / Linux
```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.optigro.App
```

---

## 🎬 5-Minute Evaluator Demo Script

```text
[0:00 - 0:30] SYSTEM STARTUP & ARCHITECTURE EXPLANATION
• Run: java -cp out com.optigro.App
• Point out: DataInitializer loads 20+ products, customers, pickers, and partners from data/*.csv.
• Show demo credentials printed on the banner.

[0:30 - 1:15] M1 (Vishal): INVENTORY & 2D WAREHOUSE GRID
• Login as 'admin' / 'admin123'.
• Select "Warehouse Store Grid Layout" -> Evaluator sees 4x5 2D Bin grid with aisle & shelf mapping.
• Select "Expiry Alerts" -> Show TreeMap range query alerting perishables expiring soon.
• Select "View Active Categories" -> Show Set<ProductCategory> ensuring no empty categories appear.

[1:15 - 2:30] M2 (Harnoor): CUSTOMER ORDER & GST INVOICE GENERATION
• Logout -> Login as customer 'cust1' / 'pass1'.
• Browse products -> Add Milk (ID 1001) and Chips (ID 1010) to cart.
• View cart -> Select Checkout.
• Apply coupon 'WELCOME50' -> Show discount applied.
• Select Payment: WALLET -> Order placed!
• Evaluator sees printed ASCII GST Invoice with category-wise taxes (5% staples, 12% snacks).
• Show: Order appended to data/orders.csv!

[2:30 - 3:45] M3 (Kritika): CONCURRENCY & SMART PICKING ENGINE
• Logout -> Login as 'admin' / 'admin123' -> Select "Run Live Multi-Threaded Simulation".
• Evaluator watches console in real time:
  - 3 PickerWorker threads start via ExecutorService.
  - Workers take tasks from PriorityBlockingQueue without race conditions.
  - Workers follow Snake Route through aisles using custom Comparator.
  - Items marked in Set<Integer> pickedProductIds (guaranteeing no duplicate picks).
  - An out-of-stock item triggers SubstitutionService to suggest nearest-priced alternative!
  - Order status moves to PACKED and appends to data/pick_history.csv.

[3:45 - 4:30] M4 (Aditya): DISPATCH & 10-MINUTE SLA VERIFICATION
• Logout -> Login as delivery partner 'partner1' / 'pass1'.
• Show assigned order dequeued from ArrayDeque FIFO queue.
• Select "Mark Order as Delivered".
• System calculates Duration between order.getPlacedAt() and deliveredAt.
• Displays SLA Verdict: [MET ✓ (Within 10 Mins)] or [BREACHED ✗].
• Appended to data/deliveries.csv!

[4:30 - 5:00] FILE PERSISTENCE & CONSOLIDATED REPORT EXPORT
• Logout -> Login as 'admin'.
• Select "System Reports & Plain-Text File Export".
• Select option 5 -> Exports consolidated stock, sales, picker, and SLA reports to reports/full_report.txt.
• Open reports/full_report.txt to show evaluator true file I/O!
```

---

## ✅ Definition of Done (Check Before Evaluation)

- [ ] All 52 Java files compile cleanly with zero errors or warnings under `javac`.
- [ ] All `data/*.csv` files exist and are loaded properly by `DataInitializer`.
- [ ] Every menu handles invalid input (typing letters when numbers expected) gracefully without crashing.
- [ ] Live simulation demonstrates 3 concurrent picker threads picking orders in real time.
- [ ] At least one substitution is shown when an item is out of stock.
- [ ] 10-minute SLA metric is calculated using `java.time.Duration`.
- [ ] Full consolidated report exports cleanly to `reports/full_report.txt`.
- [ ] Git commit history shows commits from all 4 team members across their respective branches.
- [ ] All 4 members are prepared to explain their specialized Java topics in the viva.
