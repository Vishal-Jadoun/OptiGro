package com.optigro.constants;

public enum PartnerStatus {
    ONLINE,        // Available for order assignment in the dispatch queue
    OFFLINE,       // Shift ended, not taking deliveries
    ON_DELIVERY    // Currently en route fulfilling an assigned order
}
