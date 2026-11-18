# 👤 Member 1 (M1) — Vishal (Team Lead): Inventory & Store Layout

> **Role:** Inventory & Store Layout — The Data Foundation  
> **Branch:** `m1/inventory`  
> **Files Owned:** 13 Java files + `data/products.csv`  
> **GitHub:** [Vishal-Jadoun/OptiGro](https://github.com/Vishal-Jadoun/OptiGro.git)

---

## 📌 Overview

You are building the **foundation layer** of the entire project. Every other member depends on your `Product` hierarchy, `BaseRepository<T>`, `InventoryService`, and `ConsoleScannerUtil`. Your code must be published **first** (Day 1) so the team can build on top of it.

**As the Team Lead, you also handle:**
- GitHub repository setup (branch protection, initial structure, collaborator permissions)
- Creating and managing member branches: `m1/inventory`, `m2/customer-orders`, `m3/picking-engine`, `m4/delivery-platform`
- Resolving merge conflicts during sprint integrations
- Final integration testing before college submission
- Ensuring all 4 members' code compiles together cleanly with `javac`

---

## 📁 Your Complete File & Data Structure

```text
src/
└── com/
    └── optigro/
        ├── constants/
        │   └── ProductCategory.java          ← Enum: DAIRY, FRUITS, VEGETABLES, SNACKS, BEVERAGES, STAPLES
        ├── models/
        │   ├── Product.java                  ← Abstract base class (encapsulation, equals/hashCode by id)
        │   ├── PerishableProduct.java        ← Extends Product, adds expiryDate (LocalDate)
        │   ├── NonPerishableProduct.java     ← Extends Product, adds shelfLifeMonths
        │   └── Bin.java                      ← Aisle/shelf/productId/capacity (equals/hashCode by aisle+shelf)
        ├── repository/
        │   ├── BaseRepository.java           ← Generic interface BaseRepository<T>
        │   └── ProductRepository.java        ← ConcurrentHashMap-backed + CSV file persistence (load/save)
        ├── service/
        │   ├── InventoryService.java         ← Stock management, Set<ProductCategory> active categories, search, reserve/release
        │   ├── ExpiryService.java            ← TreeMap-based expiry tracking & headMap range queries
        │   └── BinLayoutService.java         ← 2D Bin[][] grid, locate, distance, printGrid
        ├── controller/
        │   └── InventoryMenu.java            ← Admin menu for inventory operations
        ├── exception/
        │   └── OutOfStockException.java      ← Custom checked exception when stock insufficient
        └── util/
            └── ConsoleScannerUtil.java        ← Safe input utility (PUBLISH DAY 1)

data/
└── products.csv                              ← CSV file persistence for catalog & live stock counts
```

---

## 🔧 Detailed File-by-File Work Breakdown

### 1. `ProductCategory.java` — Enum
**Path:** `src/com/optigro/constants/ProductCategory.java`

```java
package com.optigro.constants;

public enum ProductCategory {
    DAIRY, FRUITS, VEGETABLES, SNACKS, BEVERAGES, STAPLES;
}
```

**What to do:**
- Define all 6 product categories.
- Keep it clean and accessible.
- Used by M2's `PricingService` for category-based GST rates (5% staples/dairy, 0% fresh fruits/veg, 12% packaged goods).
- Used by your `InventoryService` for category filtering and active category `Set` generation.

---

### 2. `Product.java` — Abstract Base Class (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Product.java`

**What to do:**
1. Mark the class `abstract`.
2. Fields (all `private` for encapsulation):
   - `private final int id;`
   - `private final String name;`
   - `private final ProductCategory category;`
   - `private final double price;`
   - `private int stock;`
   - `private final int reorderLevel;`
3. Protected constructor that validates:
   - `price > 0`
   - `stock >= 0`
   - `reorderLevel >= 0`
4. All getters, setter only for `stock` (with validation `if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");`).
5. Two abstract methods:
   - `public abstract boolean isSellable();` — Perishable checks expiry date against `LocalDate.now()`; Non-perishable returns `true`.
   - `public abstract String getStorageInfo();` — returns storage instructions description.
6. Override `toString()` with formatted output: `"1001 | Milk | Rs.55.0 | stock 20"`.
7. **Override `equals(Object o)` and `hashCode()` contract:**
   - Must compare `id` field using `java.util.Objects`.
   - Critical for HashMaps and Sets so products with the same ID are recognized as identical objects.

```java
package com.optigro.models;

import com.optigro.constants.ProductCategory;
import java.util.Objects;

public abstract class Product {
    private final int id;
    private final String name;
    private final ProductCategory category;
    private final double price;
    private int stock;
    private final int reorderLevel;

    protected Product(int id, String name, ProductCategory category,
                      double price, int stock, int reorderLevel) {
        if (price <= 0) throw new IllegalArgumentException("Price must be positive");
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
        if (reorderLevel < 0) throw new IllegalArgumentException("Reorder level cannot be negative");
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.reorderLevel = reorderLevel;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
        this.stock = stock;
    }
    public int getReorderLevel() { return reorderLevel; }

    public abstract boolean isSellable();
    public abstract String getStorageInfo();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%d | %-20s | Rs.%-6.2f | Stock: %-4d", id, name, price, stock);
    }
}
```

**OOP Concepts demonstrated:** Abstraction, Encapsulation, Inheritance setup, `equals/hashCode` contract.

**Integration points:**
- M2 uses `Product` through `InventoryService.getProduct()` and `searchByName()`.
- M3 uses `Product` through `InventoryService.findSimilar()` for substitutions.
- Aditya (M4) accesses product information through service layers for inventory reporting.

---

### 3. `PerishableProduct.java` — Concrete Subclass
**Path:** `src/com/optigro/models/PerishableProduct.java`

**What to do:**
1. `extends Product`.
2. Additional field: `private final LocalDate expiryDate;`.
3. Constructor calls `super(...)` and validates `expiryDate != null`.
4. `getExpiryDate()` getter.
5. Implement `isSellable()`: return `!LocalDate.now().isAfter(expiryDate)`.
6. Implement `getStorageInfo()`: return `"Chilled - expires " + expiryDate`.

```java
package com.optigro.models;

import com.optigro.constants.ProductCategory;
import java.time.LocalDate;
import java.util.Objects;

public class PerishableProduct extends Product {
    private final LocalDate expiryDate;

    public PerishableProduct(int id, String name, ProductCategory category, double price,
                             int stock, int reorderLevel, LocalDate expiryDate) {
        super(id, name, category, price, stock, reorderLevel);
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null");
    }

    public LocalDate getExpiryDate() { return expiryDate; }

    @Override
    public boolean isSellable() {
        return !LocalDate.now().isAfter(expiryDate);
    }

    @Override
    public String getStorageInfo() {
        return "Chilled storage - expires " + expiryDate;
    }
}
```

**Key details:**
- Use `java.time.LocalDate` (never obsolete `java.util.Date`).
- Used by `ExpiryService` for TreeMap date queries.

---

### 4. `NonPerishableProduct.java` — Concrete Subclass
**Path:** `src/com/optigro/models/NonPerishableProduct.java`

**What to do:**
1. `extends Product`.
2. Additional field: `private final int shelfLifeMonths;`.
3. Constructor calls `super(...)` and validates `shelfLifeMonths > 0`.
4. Getter for `shelfLifeMonths`.
5. Implement `isSellable()`: always return `true`.
6. Implement `getStorageInfo()`: return `"Shelf stable - " + shelfLifeMonths + " months"`.

```java
package com.optigro.models;

import com.optigro.constants.ProductCategory;

public class NonPerishableProduct extends Product {
    private final int shelfLifeMonths;

    public NonPerishableProduct(int id, String name, ProductCategory category, double price,
                                int stock, int reorderLevel, int shelfLifeMonths) {
        super(id, name, category, price, stock, reorderLevel);
        if (shelfLifeMonths <= 0) throw new IllegalArgumentException("Shelf life must be positive");
        this.shelfLifeMonths = shelfLifeMonths;
    }

    public int getShelfLifeMonths() { return shelfLifeMonths; }

    @Override
    public boolean isSellable() {
        return true;
    }

    @Override
    public String getStorageInfo() {
        return "Ambient shelf stable - " + shelfLifeMonths + " months shelf life";
    }
}
```

---

### 5. `Bin.java` — Store Grid Cell (with `equals` & `hashCode`)
**Path:** `src/com/optigro/models/Bin.java`

**What to do:**
1. Fields: `aisle` (int), `shelf` (int), `productId` (int), `capacity` (int).
2. Constructor, all getters, setter for `productId`.
3. `toString()` → `"[A2-S3: Product #1001 (cap: 50)]"`.
4. **Override `equals(Object o)` and `hashCode()`:**
   - Two Bins are identical if they share the exact same `aisle` and `shelf` coordinates.

```java
package com.optigro.models;

import java.util.Objects;

public class Bin {
    private final int aisle;
    private final int shelf;
    private int productId;
    private final int capacity;

    public Bin(int aisle, int shelf, int productId, int capacity) {
        this.aisle = aisle;
        this.shelf = shelf;
        this.productId = productId;
        this.capacity = capacity;
    }

    public int getAisle() { return aisle; }
    public int getShelf() { return shelf; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getCapacity() { return capacity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bin)) return false;
        Bin bin = (Bin) o;
        return aisle == bin.aisle && shelf == bin.shelf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aisle, shelf);
    }

    @Override
    public String toString() {
        return String.format("[Aisle %d, Shelf %d -> Product #%d (cap: %d)]",
            aisle, shelf, productId, capacity);
    }
}
```

**Used by:** `BinLayoutService` for the 2D grid and by M3's `PickRoutePlanner`.

---

### 6. `BaseRepository.java` — Generic Interface
**Path:** `src/com/optigro/repository/BaseRepository.java`

```java
package com.optigro.repository;

import java.util.List;
import java.util.Optional;

public interface BaseRepository<T> {
    void save(T entity);
    Optional<T> findById(int id);
    List<T> findAll();
    boolean deleteById(int id);
    int nextId();
}
```

> ⚠️ **PUBLISH THIS ON DAY 1** — All other members implement this interface for their repositories.

**OOP Concepts:** Generics, Interface abstraction.

---

### 7. `ProductRepository.java` — Repository with CSV Persistence
**Path:** `src/com/optigro/repository/ProductRepository.java`

**What to do:**
1. `implements BaseRepository<Product>`.
2. Internal storage: `ConcurrentHashMap<Integer, Product>` (thread-safe for concurrent picker worker threads).
3. ID counter: `AtomicInteger` starting at 1000.
4. Implement all 5 methods from interface:
   - `save(Product p)` → `store.put(p.getId(), p);`
   - `findById(int id)` → `Optional.ofNullable(store.get(id));`
   - `findAll()` → `new ArrayList<>(store.values());`
   - `deleteById(int id)` → `store.remove(id) != null;`
   - `nextId()` → `counter.incrementAndGet();`
5. Add extra method: `List<Product> findByCategory(ProductCategory cat)` using Streams filter.
6. **File Persistence (CSV): `loadFromCsv()` and `saveToCsv()`:**
   - Satisfies Rubric Component 4 (File Persistence).

```java
package com.optigro.repository;

import com.optigro.constants.ProductCategory;
import com.optigro.models.NonPerishableProduct;
import com.optigro.models.PerishableProduct;
import com.optigro.models.Product;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProductRepository implements BaseRepository<Product> {
    private final ConcurrentHashMap<Integer, Product> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    public void save(Product p) {
        store.put(p.getId(), p);
    }

    @Override
    public Optional<Product> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
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

    public List<Product> findByCategory(ProductCategory cat) {
        return store.values().stream()
            .filter(p -> p.getCategory() == cat)
            .collect(Collectors.toList());
    }

    // ==========================================
    // File Persistence: Read data/products.csv
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
                if (parts.length < 8) continue;

                int id = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                ProductCategory category = ProductCategory.valueOf(parts[2].trim());
                double price = Double.parseDouble(parts[3].trim());
                int stock = Integer.parseInt(parts[4].trim());
                int reorder = Integer.parseInt(parts[5].trim());
                String type = parts[6].trim();

                Product product;
                if ("PERISHABLE".equalsIgnoreCase(type)) {
                    LocalDate expiry = LocalDate.parse(parts[7].trim());
                    product = new PerishableProduct(id, name, category, price, stock, reorder, expiry);
                } else {
                    int shelfLife = Integer.parseInt(parts[7].trim());
                    product = new NonPerishableProduct(id, name, category, price, stock, reorder, shelfLife);
                }

                save(product);
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }

    // ==========================================
    // File Persistence: Save data/products.csv
    // ==========================================
    public void saveToCsv(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("# id,name,category,price,stock,reorderLevel,type,extra\n");
            for (Product p : findAll()) {
                if (p instanceof PerishableProduct) {
                    PerishableProduct pp = (PerishableProduct) p;
                    writer.write(String.format("%d,%s,%s,%.2f,%d,%d,PERISHABLE,%s%n",
                        pp.getId(), pp.getName(), pp.getCategory().name(),
                        pp.getPrice(), pp.getStock(), pp.getReorderLevel(), pp.getExpiryDate()));
                } else if (p instanceof NonPerishableProduct) {
                    NonPerishableProduct np = (NonPerishableProduct) p;
                    writer.write(String.format("%d,%s,%s,%.2f,%d,%d,NON_PERISHABLE,%d%n",
                        np.getId(), np.getName(), np.getCategory().name(),
                        np.getPrice(), np.getStock(), np.getReorderLevel(), np.getShelfLifeMonths()));
                }
            }
        }
    }
}
```

---

### 8. `InventoryService.java` — Core Business Logic & `Set` Usage
**Path:** `src/com/optigro/service/InventoryService.java`

**This is your most critical service.** Every other member calls these methods.

**Constructor:**
```java
public InventoryService(ProductRepository productRepo) {
    this.productRepo = productRepo;
}
```

**Methods to implement (FROZEN CONTRACT):**

| Method | Logic | Notes |
|---|---|---|
| `Optional<Product> getProduct(int productId)` | Delegate to repo | M2, M3, M4 call this |
| `List<Product> searchByName(String keyword)` | Stream filter: `name.toLowerCase().contains(keyword.toLowerCase())` | Harnoor (M2) uses for search |
| `List<Product> getByCategory(ProductCategory category)` | Stream filter by category | Harnoor (M2) uses for browsing |
| **`Set<ProductCategory> getActiveCategories()`** | **`HashSet<ProductCategory>`** containing categories with `stock > 0` | **Demonstrates `Set` interface** |
| `boolean isAvailable(int productId, int qty)` | Check product exists, `isSellable()`, and `stock >= qty` | Harnoor's (M2) CartService validates with this |
| `void reserveStock(int productId, int qty)` | **`synchronized`** — reduce stock, throw `OutOfStockException` if insufficient | Thread-safe for M3 |
| `void releaseStock(int productId, int qty)` | **`synchronized`** — increase stock (for cancelled orders) | Called on order cancel |
| `List<Product> getLowStock()` | Filter: `stock <= reorderLevel` | Admin report |
| `List<Product> findSimilar(Product original)` | Same category, in stock, `isSellable()`, price within ±25% | Kritika's (M3) SubstitutionService uses this |

**Thread safety and `Set` implementation:**
```java
package com.optigro.service;

import com.optigro.constants.ProductCategory;
import com.optigro.exception.OutOfStockException;
import com.optigro.models.Product;
import com.optigro.repository.ProductRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InventoryService {
    private final ProductRepository productRepo;

    public InventoryService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public Optional<Product> getProduct(int productId) {
        return productRepo.findById(productId);
    }

    public List<Product> searchByName(String keyword) {
        String lower = keyword.toLowerCase().trim();
        return productRepo.findAll().stream()
            .filter(p -> p.getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public List<Product> getByCategory(ProductCategory category) {
        return productRepo.findByCategory(category);
    }

    // Demonstrates Set / HashSet: returns unique active categories
    public Set<ProductCategory> getActiveCategories() {
        Set<ProductCategory> active = new HashSet<>();
        for (Product p : productRepo.findAll()) {
            if (p.isSellable() && p.getStock() > 0) {
                active.add(p.getCategory());
            }
        }
        return Collections.unmodifiableSet(active);
    }

    public boolean isAvailable(int productId, int qty) {
        return productRepo.findById(productId)
            .map(p -> p.isSellable() && p.getStock() >= qty)
            .orElse(false);
    }

    // Synchronized to prevent overselling race conditions
    public synchronized void reserveStock(int productId, int qty) throws OutOfStockException {
        Product p = productRepo.findById(productId)
            .orElseThrow(() -> new OutOfStockException("Product not found: " + productId));
        if (!p.isSellable()) {
            throw new OutOfStockException("Product is expired and cannot be sold: " + p.getName());
        }
        if (p.getStock() < qty) {
            throw new OutOfStockException(String.format("Insufficient stock for %s. Requested: %d, Available: %d",
                p.getName(), qty, p.getStock()));
        }
        p.setStock(p.getStock() - qty);
    }

    public synchronized void releaseStock(int productId, int qty) {
        productRepo.findById(productId).ifPresent(p -> p.setStock(p.getStock() + qty));
    }

    public List<Product> getLowStock() {
        return productRepo.findAll().stream()
            .filter(p -> p.getStock() <= p.getReorderLevel())
            .collect(Collectors.toList());
    }

    public List<Product> findSimilar(Product original) {
        double minPrice = original.getPrice() * 0.75;
        double maxPrice = original.getPrice() * 1.25;
        return productRepo.findAll().stream()
            .filter(p -> p.getId() != original.getId())
            .filter(p -> p.getCategory() == original.getCategory())
            .filter(Product::isSellable)
            .filter(p -> p.getStock() > 0)
            .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
            .collect(Collectors.toList());
    }

    public String generateStockReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== INVENTORY STOCK REPORT =====\n");
        sb.append(String.format("%-6s %-25s %-12s %-8s %-8s %-10s%n",
            "ID", "Name", "Category", "Price", "Stock", "Status"));
        sb.append("-".repeat(75)).append("\n");
        for (Product p : productRepo.findAll()) {
            String status = p.getStock() <= p.getReorderLevel() ? "LOW" : "OK";
            sb.append(String.format("%-6d %-25s %-12s Rs.%-5.0f %-8d %-10s%n",
                p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(), status));
        }
        sb.append("\nTotal Products: ").append(productRepo.findAll().size());
        sb.append("\nLow Stock Items: ").append(getLowStock().size());
        return sb.toString();
    }
}
```

---

### 9. `ExpiryService.java` — Expiry Tracking with `TreeMap`
**Path:** `src/com/optigro/service/ExpiryService.java`

**What to do:**
1. Constructor takes `ProductRepository`.
2. Maintain/build a `TreeMap<LocalDate, List<PerishableProduct>>` grouping perishable products by date.
3. Methods:
   - `List<PerishableProduct> expiringWithinDays(int days)`: Uses `headMap(LocalDate.now().plusDays(days + 1))` to collect all products expiring within N days.
   - `List<PerishableProduct> getExpired()`: Filters for `LocalDate.now().isAfter(expiryDate)`.

```java
package com.optigro.service;

import com.optigro.models.PerishableProduct;
import com.optigro.repository.ProductRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExpiryService {
    private final ProductRepository productRepo;

    public ExpiryService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    private TreeMap<LocalDate, List<PerishableProduct>> buildExpiryMap() {
        TreeMap<LocalDate, List<PerishableProduct>> map = new TreeMap<>();
        for (var p : productRepo.findAll()) {
            if (p instanceof PerishableProduct) {
                PerishableProduct pp = (PerishableProduct) p;
                map.computeIfAbsent(pp.getExpiryDate(), k -> new ArrayList<>()).add(pp);
            }
        }
        return map;
    }

    public List<PerishableProduct> expiringWithinDays(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days + 1);
        TreeMap<LocalDate, List<PerishableProduct>> map = buildExpiryMap();
        List<PerishableProduct> result = new ArrayList<>();
        // headMap gives efficient sorted range view up to threshold date
        for (List<PerishableProduct> list : map.headMap(threshold).values()) {
            for (PerishableProduct pp : list) {
                if (pp.isSellable()) {
                    result.add(pp);
                }
            }
        }
        return result;
    }

    public List<PerishableProduct> getExpired() {
        LocalDate today = LocalDate.now();
        return productRepo.findAll().stream()
            .filter(p -> p instanceof PerishableProduct)
            .map(p -> (PerishableProduct) p)
            .filter(pp -> today.isAfter(pp.getExpiryDate()))
            .collect(Collectors.toList());
    }
}
```

**Why TreeMap?** Logarithmic O(log N) sorted keys, natural date ordering, and `headMap()` provides submap views for range queries.

---

### 10. `BinLayoutService.java` — 2D Store Grid
**Path:** `src/com/optigro/service/BinLayoutService.java`

**What to do:**
1. Maintain a 2D array: `Bin[][] grid` (e.g. 4 aisles × 5 shelves = 20 bins).
2. Constructor takes `InventoryService`.
3. Methods:
   - `Bin locate(int productId)`: loops through grid to find the bin containing the productId.
   - `int distance(Bin a, Bin b)`: Manhattan distance: `Math.abs(a.getAisle() - b.getAisle()) + Math.abs(a.getShelf() - b.getShelf())`.
   - `void printGrid()`: displays a 2D table representing warehouse shelves.
   - `void assignProduct(int aisle, int shelf, int productId, int capacity)`.
   - `void autoAssignBins()`: automatically maps all products into the grid slots at boot.

```java
package com.optigro.service;

import com.optigro.models.Bin;

public class BinLayoutService {
    private static final int AISLES = 4;
    private static final int SHELVES = 5;
    private final Bin[][] grid = new Bin[AISLES][SHELVES];
    private final InventoryService inventoryService;

    public BinLayoutService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        for (int a = 0; a < AISLES; a++) {
            for (int s = 0; s < SHELVES; s++) {
                grid[a][s] = new Bin(a, s, -1, 50); // -1 = empty bin
            }
        }
    }

    public void assignProduct(int aisle, int shelf, int productId, int capacity) {
        if (aisle >= 0 && aisle < AISLES && shelf >= 0 && shelf < SHELVES) {
            grid[aisle][shelf] = new Bin(aisle, shelf, productId, capacity);
        }
    }

    public void autoAssignBins() {
        int aisle = 0;
        int shelf = 0;
        for (var product : inventoryService.searchByName("")) {
            assignProduct(aisle, shelf, product.getId(), 50);
            shelf++;
            if (shelf >= SHELVES) {
                shelf = 0;
                aisle++;
                if (aisle >= AISLES) break;
            }
        }
    }

    public Bin locate(int productId) {
        for (int a = 0; a < AISLES; a++) {
            for (int s = 0; s < SHELVES; s++) {
                if (grid[a][s].getProductId() == productId) {
                    return grid[a][s];
                }
            }
        }
        return new Bin(0, 0, productId, 50); // fallback origin
    }

    public int distance(Bin a, Bin b) {
        return Math.abs(a.getAisle() - b.getAisle()) + Math.abs(a.getShelf() - b.getShelf());
    }

    public void printGrid() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        OPTIGRO WAREHOUSE BIN GRID                              ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-8s | %-12s | %-12s | %-12s | %-12s | %-12s ║%n",
            "Aisle", "Shelf 0", "Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════════╣");

        for (int a = 0; a < AISLES; a++) {
            StringBuilder row = new StringBuilder(String.format("║ Aisle %-2d |", a));
            for (int s = 0; s < SHELVES; s++) {
                int pid = grid[a][s].getProductId();
                String name = "EMPTY";
                if (pid != -1) {
                    var prod = inventoryService.getProduct(pid);
                    name = prod.map(p -> p.getName().length() > 10 ? p.getName().substring(0, 10) : p.getName())
                               .orElse("ID:" + pid);
                }
                row.append(String.format(" %-12s |", name));
            }
            row.setLength(row.length() - 1);
            row.append("║");
            System.out.println(row);
        }
        System.out.println("╚════════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

---

### 11. `InventoryMenu.java` — Admin Menu Controller
**Path:** `src/com/optigro/controller/InventoryMenu.java`

```java
package com.optigro.controller;

import com.optigro.constants.ProductCategory;
import com.optigro.models.NonPerishableProduct;
import com.optigro.models.PerishableProduct;
import com.optigro.models.Product;
import com.optigro.repository.ProductRepository;
import com.optigro.service.BinLayoutService;
import com.optigro.service.ExpiryService;
import com.optigro.service.InventoryService;
import com.optigro.util.ConsoleScannerUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

public class InventoryMenu {
    private final InventoryService inventoryService;
    private final BinLayoutService binLayoutService;
    private final ExpiryService expiryService;
    private final ProductRepository productRepo;

    public InventoryMenu(InventoryService inventoryService,
                         BinLayoutService binLayoutService,
                         ExpiryService expiryService,
                         ProductRepository productRepo) {
        this.inventoryService = inventoryService;
        this.binLayoutService = binLayoutService;
        this.expiryService = expiryService;
        this.productRepo = productRepo;
    }

    public void show() {
        while (true) {
            System.out.println("\n===== INVENTORY MANAGEMENT (Admin) =====");
            System.out.println("1. Add Product");
            System.out.println("2. Update Stock");
            System.out.println("3. Remove Product");
            System.out.println("4. Search Products");
            System.out.println("5. View Active Categories (Set)");
            System.out.println("6. View Store Grid (2D Array)");
            System.out.println("7. Expiry Alerts (TreeMap)");
            System.out.println("8. Low Stock Alert");
            System.out.println("9. Save Catalog to CSV (File Persistence)");
            System.out.println("10. Back to Main Menu");

            int choice = ConsoleScannerUtil.readIntInRange("Choose an option [1-10]: ", 1, 10);
            switch (choice) {
                case 1: addProductFlow(); break;
                case 2: updateStockFlow(); break;
                case 3: removeProductFlow(); break;
                case 4: searchProductFlow(); break;
                case 5: showActiveCategoriesFlow(); break;
                case 6: binLayoutService.printGrid(); break;
                case 7: showExpiryAlertsFlow(); break;
                case 8: showLowStockFlow(); break;
                case 9: saveCsvFlow(); break;
                case 10: return;
            }
        }
    }

    private void addProductFlow() {
        System.out.println("\n--- Add New Product ---");
        int type = ConsoleScannerUtil.readIntInRange("1. Perishable (e.g. Milk/Veg)  2. Non-Perishable (e.g. Rice/Oil): ", 1, 2);
        String name = ConsoleScannerUtil.readString("Enter product name: ");
        
        System.out.println("Categories: 1.DAIRY 2.FRUITS 3.VEGETABLES 4.SNACKS 5.BEVERAGES 6.STAPLES");
        int catIdx = ConsoleScannerUtil.readIntInRange("Select category [1-6]: ", 1, 6);
        ProductCategory category = ProductCategory.values()[catIdx - 1];

        double price = ConsoleScannerUtil.readDouble("Enter selling price (Rs.): ");
        int stock = ConsoleScannerUtil.readInt("Enter initial stock count: ");
        int reorder = ConsoleScannerUtil.readInt("Enter reorder alert level: ");
        int newId = productRepo.nextId();

        Product product;
        if (type == 1) {
            LocalDate expiry = ConsoleScannerUtil.readDate("Enter expiry date");
            product = new PerishableProduct(newId, name, category, price, stock, reorder, expiry);
        } else {
            int shelfLife = ConsoleScannerUtil.readInt("Enter shelf life in months: ");
            product = new NonPerishableProduct(newId, name, category, price, stock, reorder, shelfLife);
        }

        productRepo.save(product);
        System.out.println("✓ Product created successfully! Assigned ID: " + newId);
        
        // Auto-assign to store grid
        var bin = binLayoutService.locate(-1); // find first empty
        binLayoutService.assignProduct(bin.getAisle(), bin.getShelf(), newId, 50);
        System.out.printf("✓ Assigned to Warehouse Aisle %d, Shelf %d%n", bin.getAisle(), bin.getShelf());
    }

    private void updateStockFlow() {
        int id = ConsoleScannerUtil.readInt("Enter Product ID to update: ");
        var prod = inventoryService.getProduct(id);
        if (prod.isEmpty()) {
            System.out.println("[!] Product not found.");
            return;
        }
        System.out.println("Current stock: " + prod.get().getStock());
        int add = ConsoleScannerUtil.readInt("Enter additional stock to add: ");
        prod.get().setStock(prod.get().getStock() + add);
        System.out.println("✓ Updated stock count: " + prod.get().getStock());
    }

    private void removeProductFlow() {
        int id = ConsoleScannerUtil.readInt("Enter Product ID to delete: ");
        if (productRepo.deleteById(id)) {
            System.out.println("✓ Product #" + id + " removed from catalog.");
        } else {
            System.out.println("[!] Product not found.");
        }
    }

    private void searchProductFlow() {
        String kw = ConsoleScannerUtil.readString("Enter search keyword: ");
        var list = inventoryService.searchByName(kw);
        if (list.isEmpty()) {
            System.out.println("No matching products found.");
            return;
        }
        System.out.printf("%-6s %-25s %-12s %-8s %-6s%n", "ID", "Name", "Category", "Price", "Stock");
        System.out.println("-".repeat(60));
        for (var p : list) {
            System.out.printf("%-6d %-25s %-12s Rs.%-6.2f %-6d%n",
                p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock());
        }
    }

    private void showActiveCategoriesFlow() {
        Set<ProductCategory> active = inventoryService.getActiveCategories();
        System.out.println("\n--- Active Categories In Stock (Set/HashSet Demonstration) ---");
        System.out.println("Unique Categories: " + active.size());
        for (var cat : active) {
            System.out.println(" • " + cat.name());
        }
    }

    private void showExpiryAlertsFlow() {
        int days = ConsoleScannerUtil.readIntInRange("Check items expiring within how many days? (e.g. 7): ", 1, 60);
        var expiring = expiryService.expiringWithinDays(days);
        System.out.println("\n--- Expiry Alerts (TreeMap Range Query) ---");
        if (expiring.isEmpty()) {
            System.out.println("No items expiring within " + days + " days.");
        } else {
            for (var pp : expiring) {
                System.out.printf("[!] ALERT: %s (ID: %d) expires on %s! Stock remaining: %d%n",
                    pp.getName(), pp.getId(), pp.getExpiryDate(), pp.getStock());
            }
        }
    }

    private void showLowStockFlow() {
        var low = inventoryService.getLowStock();
        System.out.println("\n--- Low Stock Alerts ---");
        if (low.isEmpty()) {
            System.out.println("All product stocks are above reorder thresholds.");
        } else {
            for (var p : low) {
                System.out.printf("[!] REORDER NEEDED: %s (ID: %d) -> Current: %d, Reorder Level: %d%n",
                    p.getName(), p.getId(), p.getStock(), p.getReorderLevel());
            }
        }
    }

    private void saveCsvFlow() {
        try {
            productRepo.saveToCsv("data/products.csv");
            System.out.println("✓ Catalog and stock successfully saved to data/products.csv!");
        } catch (IOException e) {
            System.out.println("[!] Error saving CSV: " + e.getMessage());
        }
    }
}
```

---

### 12. `OutOfStockException.java` — Custom Exception
**Path:** `src/com/optigro/exception/OutOfStockException.java`

```java
package com.optigro.exception;

public class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}
```

---

### 13. `ConsoleScannerUtil.java` — Safe Console Input Utility
**Path:** `src/com/optigro/util/ConsoleScannerUtil.java`

> ⚠️ **PUBLISH THIS ON DAY 1** — Every member uses this for all console input.

```java
package com.optigro.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleScannerUtil {
    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleScannerUtil() { }

    public static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a whole number.");
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("[!] Please choose a number between " + min + " and " + max + ".");
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a valid decimal number.");
            }
        }
    }

    public static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            try {
                System.out.print(prompt + " (YYYY-MM-DD): ");
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date format. Please use YYYY-MM-DD (e.g. 2026-11-28).");
            }
        }
    }

    public static boolean confirm(String prompt) {
        System.out.print(prompt + " (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }
}
```

---

## 🎯 Team Lead Responsibilities

### GitHub Setup (Day 1)
1. Initialize repository and push structure.
2. Publish `ConsoleScannerUtil.java`, `BaseRepository.java`, and `ProductCategory.java`.
3. Create `data/` directory with starter CSV files:
   - `data/products.csv`
   - `data/customers.csv`
   - `data/pickers.csv`
   - `data/users.csv`
   - `data/partners.csv`
4. Create and push 4 member branches:
   ```cmd
   git checkout -b m1/inventory
   git push origin m1/inventory
   git checkout -b m2/customer-orders
   git push origin m2/customer-orders
   git checkout -b m3/picking-engine
   git push origin m3/picking-engine
   git checkout -b m4/delivery-platform
   git push origin m4/delivery-platform
   ```
5. Add all 3 teammates as collaborators on GitHub.

### Integration Management
- **End of Sprint 1:** Merge all models and repositories to `main`.
- **End of Sprint 2:** Merge all services to `main`.
- **End of Sprint 3:** Final integration of menus + App.java.
- **End of Sprint 4:** Final polish, documentation, submission run.

---

## 📋 Sprint-wise Task Checklist

### Sprint 0 (Day 1) — Setup
- [ ] Push empty structure and starter `data/*.csv` files.
- [ ] Publish `ConsoleScannerUtil.java` (safe input).
- [ ] Publish `BaseRepository.java` (generic interface).
- [ ] Publish `ProductCategory.java` (enum).

### Sprint 1 — Models & Repositories
- [ ] `Product.java` (abstract, `equals` & `hashCode` by `id`).
- [ ] `PerishableProduct.java` (extends Product, expiryDate).
- [ ] `NonPerishableProduct.java` (extends Product, shelfLifeMonths).
- [ ] `Bin.java` (`equals` & `hashCode` by `aisle + shelf`).
- [ ] `ProductRepository.java` (`ConcurrentHashMap`, `AtomicInteger`, `loadFromCsv()`, `saveToCsv()`).
- [ ] Verify `data/products.csv` loads 20+ items cleanly into repo.

### Sprint 2 — Services
- [ ] `InventoryService.java` (synchronized stock reserve/release, `getActiveCategories()` returning `Set`).
- [ ] `ExpiryService.java` (TreeMap, `expiringWithinDays()`, `getExpired()`).
- [ ] `BinLayoutService.java` (Bin[][] grid, locate, distance, printGrid).
- [ ] `OutOfStockException.java`.

### Sprint 3 — Menu & Integration
- [ ] `InventoryMenu.java` (all 10 options working).
- [ ] Verify CSV persistence: add product via menu → save to CSV → restart console → product still present!
- [ ] Pair code review with Harnoor (M2) (leave 5+ review comments on GitHub PR).

### Sprint 4 — Hardening & Docs
- [ ] Defensive input check on all menu prompts.
- [ ] Create ER diagram + data dictionary (your documentation deliverable).
- [ ] Author "My Module" page (1 page).
- [ ] Fill OOP concepts map rows.
- [ ] Teach-back prep: Abstraction, Generics, 2D Arrays, TreeMap, `synchronized`, `Set`, CSV File Persistence.

---

## 🎓 Viva Preparation

### Your Java Topics to Master
1. **Abstraction & Polymorphism:** Why is `Product` abstract? Why not a single class with a boolean `isPerishable`? (Different storage rules, different date calculations, avoids messy if-else checks, allows open-closed principle).
2. **The `equals()` and `hashCode()` Contract:** What is the contract? (If `a.equals(b)` is true, `a.hashCode() == b.hashCode()` MUST be true. If you override `equals()` without `hashCode()`, putting a Product in a `HashMap` or `HashSet` will fail to retrieve it because the default hash code uses memory location).
3. **Generics (`BaseRepository<T>`):** What do generics provide? (Compile-time type checking and reusable CRUD operations without hazardous runtime type casting).
4. **`Set` Interface (`getActiveCategories()`):** Why return a `Set<ProductCategory>` instead of `List`? (A `Set` guarantees no duplicates mathematically in O(1) time without looping over results).
5. **Thread Synchronization:** Why is `reserveStock()` `synchronized`? (Multiple picker worker threads could simultaneously observe stock = 1, both approve the pick, and reduce stock to -1, creating a race condition and overselling).
6. **File Persistence:** How does `data/products.csv` persistence work? Why use `try-with-resources`? (`try (BufferedReader br = ...)` auto-closes streams even on exceptions, avoiding file descriptor resource leaks).
