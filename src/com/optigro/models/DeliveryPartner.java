package com.optigro.models;

import com.optigro.constants.PartnerStatus;

import java.util.Objects;

public class DeliveryPartner {
    private final int partnerId;
    private final int userId;
    private final String name;
    private PartnerStatus status;
    private double totalRating;
    private int deliveriesDone;

    public DeliveryPartner(int partnerId, int userId, String name, PartnerStatus status,
                           double totalRating, int deliveriesDone) {
        this.partnerId = partnerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "Partner name required");
        this.status = status != null ? status : PartnerStatus.ONLINE;
        this.totalRating = Math.max(0.0, totalRating);
        this.deliveriesDone = Math.max(0, deliveriesDone);
    }

    public int getPartnerId() { return partnerId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public double getTotalRating() { return totalRating; }
    public int getDeliveriesDone() { return deliveriesDone; }

    public void addRating(int stars) {
        if (stars >= 1 && stars <= 5) {
            this.totalRating += stars;
            this.deliveriesDone++;
        }
    }

    public double getAverageRating() {
        return (deliveriesDone > 0) ? (totalRating / deliveriesDone) : 5.0;
    }

    public boolean isLowRated() {
        return deliveriesDone >= 3 && getAverageRating() < 3.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryPartner)) return false;
        DeliveryPartner partner = (DeliveryPartner) o;
        return partnerId == partner.partnerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerId);
    }

    @Override
    public String toString() {
        return String.format("Partner #%d: %-15s | Status: %-11s | Avg Rating: %-4.1f (%d reviews)",
            partnerId, name, status, getAverageRating(), deliveriesDone);
    }
}
