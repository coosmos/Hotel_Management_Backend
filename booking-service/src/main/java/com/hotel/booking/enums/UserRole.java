package com.hotel.booking.enums;


public enum UserRole {
    ADMIN("Administrator with full system access"),
    MANAGER("Hotel manager with access to own hotel"),
    RECEPTIONIST("Receptionist with access to own hotel bookings"),
    GUEST("Guest user who can create and view own bookings");

    private final String description;
    UserRole(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public boolean isStaff() {
        return this == MANAGER || this == RECEPTIONIST;
    }
    public boolean hasHotelAccess() {
        return this == MANAGER || this == RECEPTIONIST || this == ADMIN;
    }
    public boolean canManageBookings() {
        return this == ADMIN || this == MANAGER || this == RECEPTIONIST;
    }
}