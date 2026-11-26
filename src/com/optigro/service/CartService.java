package com.optigro.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CartService {
    private final InventoryService inventoryService;
   
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
        Map<Integer, Integer> cart = carts.get(customerId);
        return (cart != null) ? Collections.unmodifiableMap(new HashMap<>(cart)) : Collections.emptyMap();
    }

    public void clearCart(int customerId) {
        carts.remove(customerId);
    }

    public boolean isEmpty(int customerId) {
        Map<Integer, Integer> cart = carts.get(customerId);
        return cart == null || cart.isEmpty();
    }
}
