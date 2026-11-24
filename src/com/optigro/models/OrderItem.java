package com.optigro.models;

import java.util.Objects;

public class OrderItem {
    private final int productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(int productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    public double getLineTotal() {
        return quantity * unitPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem item = (OrderItem) o;
        return productId == item.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return String.format("%-20s x%d @ Rs.%.2f = Rs.%.2f",
            productName, quantity, unitPrice, getLineTotal());
    }
}
