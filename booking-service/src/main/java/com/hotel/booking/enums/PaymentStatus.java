package com.hotel.booking.enums;

public enum PaymentStatus {
    PENDING("Pending"),
    PAID("Paid"),
    FAILED("Failed"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
