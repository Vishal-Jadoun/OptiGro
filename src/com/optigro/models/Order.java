package com.optigro.models;

import com.optigro.constants.OrderStatus;
import com.optigro.constants.PaymentMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private final int orderId;
    private final int customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final PaymentMode paymentMode;
    private final double subtotal;
    private final double gst;
    private final double deliveryFee;
    private final double discount;
    private final double total;
    private final LocalDateTime placedAt;

    public Order(int orderId, int customerId, List<OrderItem> items, PaymentMode paymentMode,
                 double subtotal, double gst, double deliveryFee, double discount, double total) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.paymentMode = paymentMode;
        this.subtotal = subtotal;
        this.gst = gst;
        this.deliveryFee = deliveryFee;
        this.discount = discount;
        this.total = total;
        this.status = OrderStatus.PLACED;
        this.placedAt = LocalDateTime.now();
    }

    public int getOrderId() { return orderId; }
    public int getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            if (!status.canCancel()) {
                throw new IllegalStateException("Cannot cancel order in status: " + status);
            }
            this.status = OrderStatus.CANCELLED;
            return;
        }
        // State machine progression check
        if (this.status.next() != newStatus) {
            throw new IllegalStateException(String.format("Invalid transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public double getSubtotal() { return subtotal; }
    public double getGst() { return gst; }
    public double getDeliveryFee() { return deliveryFee; }
    public double getDiscount() { return discount; }
    public double getTotal() { return total; }
    public LocalDateTime getPlacedAt() { return placedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return orderId == order.orderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("Order #%d | Customer #%d | Total: Rs.%.2f | Status: %s | Time: %s",
            orderId, customerId, total, status, placedAt);
    }
}
