package com.optigro.constants;

public enum OrderStatus {
    PLACED,
    PICKING,
    PACKED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public OrderStatus next() {
        switch (this) {
            case PLACED:           return PICKING;
            case PICKING:          return PACKED;
            case PACKED:           return OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY: return DELIVERED;
            default:
                throw new IllegalStateException("No valid next status after terminal state: " + this);
        }
    }

    public boolean canCancel() {
        return this == PLACED || this == PICKING;
    }
}
