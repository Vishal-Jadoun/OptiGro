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
    public InventoryMenu(InventoryService inventoryService, BinLayoutService binLayoutService, ExpiryService expiryService, ProductRepository productRepo) {
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
        int type = ConsoleScannerUtil.readIntInRange("1. Perishable (Milk/Veg)  2. Non-Perishable (Rice/Oil): ", 1, 2);
        String name = ConsoleScannerUtil.readString("Enter product name: ");
        System.out.println("Categories: 1.DAIRY 2.FRUITS 3.VEGETABLES 4.SNACKS 5.BEVERAGES 6.STAPLES");
        int catIdx = ConsoleScannerUtil.readIntInRange("Select category [1-6]: ", 1, 6);
        ProductCategory category = ProductCategory.values()[catIdx - 1];
        double price = ConsoleScannerUtil.readDouble("Enter selling price (Rs.): ");
        int stock = ConsoleScannerUtil.readInt("Enter initial stock count: ");
        int reorder = ConsoleScannerUtil.readInt("Enter reorder alert level: ");
        int newId = productRepo.nextId();
        try {
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
            var bin = binLayoutService.locate(-1); // find first empty bin
            binLayoutService.assignProduct(bin.getAisle(), bin.getShelf(), newId, 50);
            System.out.printf("✓ Assigned to Warehouse Aisle %d, Shelf %d%n", bin.getAisle(), bin.getShelf());
        } catch (IllegalArgumentException e) {
            System.out.println("[!] Invalid product values: " + e.getMessage());
        }
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
        try {
            prod.get().setStock(prod.get().getStock() + add);
            System.out.println("✓ Updated stock count: " + prod.get().getStock());
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
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
