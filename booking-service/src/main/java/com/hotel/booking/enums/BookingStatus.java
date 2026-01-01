package com.hotel.booking.enums;


public enum BookingStatus {
    PENDING("Pending", "Booking is pending confirmation"),
    CONFIRMED("Confirmed", "Booking is confirmed"),
    CHECKED_IN("Checked In", "Guest has checked in"),
    CHECKED_OUT("Checked Out", "Guest has checked out"),
    CANCELLED("Cancelled", "Booking has been cancelled");
    private final String displayName;
    private final String description;
    BookingStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    public String getDisplayName() {
        return displayName;
    }
    public String getDescription() {
        return description;
    }
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;}
    public boolean canCheckIn() {return this == CONFIRMED;}
    public boolean canCheckOut() {
        return this == CHECKED_IN;
    }
}