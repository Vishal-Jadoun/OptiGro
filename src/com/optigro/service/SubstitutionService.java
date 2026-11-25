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

    public Optional<Product> suggestSubstitute(Product outOfStockProduct, int qty) {
        List<Product> similar = inventoryService.findSimilar(outOfStockProduct);
        return similar.stream()
            .filter(p -> p.getStock() >= qty)
            .min(Comparator.comparingDouble(p -> Math.abs(p.getPrice() - outOfStockProduct.getPrice())));
    }
}