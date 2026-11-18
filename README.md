# 🛒 OptiGro — 10-Minute Grocery Delivery Logistics Engine

A modular, production-ready, pure Java 11+ terminal console application modeling quick-commerce dark store operations (inspired by Zepto / Blinkit).

---

## 📌 Project Overview
* **Architecture:** 4-Layer Architecture (`Controller` → `Service` → `Repository` → `Model`)
* **Environment:** Pure Java Standard Library (Java 11+), no external frameworks or build tools required
* **Repository:** [https://github.com/Vishal-Jadoun/OptiGro.git](https://github.com/Vishal-Jadoun/OptiGro.git)
* **Team Structure:** 4 Team Members, 13 Java files + CSV persistence files each with equal full-stack layer ownership

---

## 🎯 Course Rubric Compliance (25CSE0202 — 25 Marks)

| Evaluation Component | Marks | Implementation in OptiGro |
|---|---|---|
| **1. Problem Statement** | 3 | Real-world 10-minute grocery delivery logistics engine with inventory, routing, picking, and dispatch. |
| **2. Objectives & Scope** | 2 | Clear scope: 20+ products, 6 categories, multithreaded picker threads, 10-minute SLA validation, and order state machine. |
| **3. Project Design & Prototype** | 9 | Sensible 4-tier package structure (`com.optigro.*`), 52 classes, robust CLI menus, and demo simulation. |
| **4. Integration of Course Concepts** | 6 | **100% Complete:** OOP (Inheritance, Polymorphism, `equals/hashCode`), Collections (`List`, `Map`, `Set`, `TreeMap`, `ArrayDeque`, `PriorityBlockingQueue`), Concurrency (`Runnable`, `ExecutorService`, `synchronized`), **File Persistence (`data/*.csv` via `BufferedReader`/`BufferedWriter`)**, and Streams API. |
| **5. Code Quality & Evidence** | 5 | Clean code conventions, Git history across 4 member branches, zero compiler warnings, and full documentation. |

---

## 👥 Equal Team Ownership & Modules

```text
┌─────────┬──────────────────────────┬────────────────────────────┬─────────────────────────────┐
│ Member  │ equals() & hashCode()    │ Set / HashSet              │ File Persistence (CSV)      │
├─────────┼──────────────────────────┼────────────────────────────┼─────────────────────────────┤
│ M1      │ Product & Bin            │ Active categories Set      │ products.csv (read & write) │
│ M2      │ Customer, Order, Item    │ Used coupons Set           │ orders.csv (read & write)   │
│ M3      │ Picker, Task, Sub        │ Picked items Set           │ pickers.csv (read & write)  │
│ M4      │ User, Partner, Delivery  │ Locked accounts Set        │ users.csv (read & write)    │
└─────────┴──────────────────────────┴────────────────────────────┴─────────────────────────────┘
```

| Member | Domain & Slice | Core Focus & Java Mastery | Branch Name | Guide |
|---|---|---|---|---|
| **M1 (Vishal - Lead)** | **Inventory & Store Layout** | Abstraction, Generics, `ConcurrentHashMap`, `synchronized`, 2D Bin Grid, `TreeMap` Expiry, `getActiveCategories()` (`Set`), `products.csv` persistence | `m1/inventory` | [M1 Guide](docs/M1_Vishal_Inventory_StoreLayout.md) |
| **M2 (Harnoor)** | **Customer & Orders** | State Machine Enum, Streams API, `usedCoupons` (`Set`), `Consumer<Order>` callback, `orders.csv` persistence, ASCII bills | `m2/customer-orders` | [M2 Guide (Harnoor)](docs/M2_Harnoor_Customer_Orders.md) |
| **M3 (Kritika)** | **Picking Engine** | Concurrency (`Runnable`, `ExecutorService`), `PriorityBlockingQueue`, `Comparable` vs `Comparator`, `pickedProductIds` (`Set`), `DataInitializer` master CSV bootstrapper | `m3/picking-engine` | [M3 Guide (Kritika)](docs/M3_Kritika_Picking_Engine.md) |
| **M4 (Aditya)** | **Delivery & Platform** | Security/Auth, `ArrayDeque` Queue, SLA duration checking (`java.time.Duration`), `lockedUsernames` (`Set`), `deliveries.csv` persistence, Dependency Injection in `App.java` | `m4/delivery-platform` | [M4 Guide (Aditya)](docs/M4_Aditya_Delivery_Platform.md) |

---

## 📚 Complete Documentation Suite

All detailed specifications, workflows, architecture diagrams, and viva guides are in the [`docs/`](docs/) folder:

1. **[📋 Master Integration Plan (`docs/MASTER_PLAN.md`)](docs/MASTER_PLAN.md)**
   * Complete 52-file architecture, dependency graph, frozen integration contracts, sprint schedules, and end-to-end 5-minute evaluator demo script.
2. **[🧾 Git & GitHub Cheatsheet (`docs/GIT_CHEATSHEET.md`)](docs/GIT_CHEATSHEET.md)**
   * Beginner-friendly handbook covering cloning, daily branch workflows, push/pull routines, and step-by-step merge conflict resolution.
3. **[👤 M1 (Vishal) Work Guide (`docs/M1_Vishal_Inventory_StoreLayout.md`)](docs/M1_Vishal_Inventory_StoreLayout.md)**
   * Foundation models, Generic `BaseRepository`, thread-safe `InventoryService`, 2D `BinLayoutService`, admin inventory menu, team lead setup & merge responsibilities.
4. **[👤 M2 Work Guide (`docs/M2_Harnoor_Customer_Orders.md`)](docs/M2_Harnoor_Customer_Orders.md)**
   * Shopping cart, GST calculation with Streams, discount coupons, `OrderStatus` state machine, order placement with stock rollback, ASCII tables & invoice rendering.
5. **[👤 M3 Work Guide (`docs/M3_Kritika_Picking_Engine.md`)](docs/M3_Kritika_Picking_Engine.md)**
   * Multithreaded picking workers, priority queue ordering, snake-routing optimization through warehouse aisles, intelligent out-of-stock substitutions, and master `DataInitializer.java`.
6. **[👤 M4 Work Guide (`docs/M4_Aditya_Delivery_Platform.md`)](docs/M4_Aditya_Delivery_Platform.md)**
   * Role-based login and brute-force lockout, FIFO delivery partner dispatch, strict 10-minute SLA validation, report generation with file export, and master manual wiring in `App.java`.

---

## ⚡ How to Build and Run

### Windows (PowerShell or Command Prompt)
```cmd
# Navigate to the project root
cd X:\SEM3\Clone\OptiGro

# Collect all Java source files
dir /s /b src\*.java > sources.txt

# Compile cleanly into out/ directory
javac -d out @sources.txt

# Launch the application
java -cp out com.optigro.App
```

### macOS / Linux
```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.optigro.App
```

---

## 🔑 Demo Login Credentials

The system seeds all mock entities from `data/*.csv` at boot time:

| Role | Username | Password | Notes |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Full access to stock, store grid, expiry alerts, reports, and Live Simulation |
| **Customer** | `cust1` | `pass1` | Pre-funded wallet balance, order history, coupon application |
| **Picker** | `picker1` | `pass1` | Warehouse pick task processing and snake route visualization |
| **Delivery Partner** | `partner1` | `pass1` | Order dispatch acceptance, status toggling, SLA and rating verification |
