package com.optigro.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Delivery {
    private final int deliveryId;
    private final int orderId;
    private final int partnerId;
    private final LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private boolean metSla;
    private int rating;

    public Delivery(int deliveryId, int orderId, int partnerId) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.partnerId = partnerId;
        this.dispatchedAt = LocalDateTime.now();
        this.deliveredAt = null;
        this.metSla = false;
        this.rating = 0;
    }

    public int getDeliveryId() { return deliveryId; }
    public int getOrderId() { return orderId; }
    public int getPartnerId() { return partnerId; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public boolean isMetSla() { return metSla; }
    public void setMetSla(boolean metSla) { this.metSla = metSla; }
    public int getRating() { return rating; }

    public void setRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Delivery)) return false;
        Delivery delivery = (Delivery) o;
        return deliveryId == delivery.deliveryId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId);
    }

    @Override
    public String toString() {
        return String.format("Delivery #%d | Order #%d | Partner #%d | Met SLA: %b | Rating: %s",
            deliveryId, orderId, partnerId, metSla, (rating > 0 ? rating + "★" : "Pending"));
    }
}
