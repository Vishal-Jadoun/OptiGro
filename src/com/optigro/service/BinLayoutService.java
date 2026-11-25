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
                grid[a][s] = new Bin(a, s, -1, 50); // -1 means empty bin
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
        return new Bin(0, 0, productId, 50); // safe fallback origin
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
