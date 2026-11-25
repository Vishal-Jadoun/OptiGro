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