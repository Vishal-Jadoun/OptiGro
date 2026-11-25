package com.optigro.service;

import com.optigro.constants.ProductCategory;
import com.optigro.exception.OutOfStockException;
import com.optigro.models.Product;
import com.optigro.repository.ProductRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    public synchronized void reserveStock(int productId, int qty) throws OutOfStockException {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new OutOfStockException("Product not found: " + productId));
        if (!p.isSellable()) {
            throw new OutOfStockException("Product is expired and cannot be sold: " + p.getName());
        }
        if (p.getStock() < qty) {
            throw new OutOfStockException(String.format(
                    "Insufficient stock for %s. Requested: %d, Available: %d",
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
