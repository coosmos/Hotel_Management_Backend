package com.hotel.booking.entity;

import com.hotel.booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_hotel_id", columnList = "hotel_id"),
        @Index(name = "idx_room_id", columnList = "room_id"),
        @Index(name = "idx_check_in_date", columnList = "check_in_date"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;
    @Column(name = "total_amount", nullable = false)
    private float totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;
    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;
    @Column(name = "guest_email", nullable = false, length = 100)
    private String guestEmail;
    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;
    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;
    @Column(name = "cancelled_at")
    private LocalDate cancelledAt;
    @Column(name = "checked_in_at")
    private LocalDate checkedInAt;
    @Column(name = "checked_out_at")
    private LocalDate checkedOutAt;
    @Transient
    public int getNumberOfNights() {
        return (int) DAYS.between(checkInDate, checkOutDate);
    }
    @Transient
    public boolean isActive() {
        return status != BookingStatus.CANCELLED && status != BookingStatus.CHECKED_OUT;
    }
    public boolean overlapsWithDates(LocalDate startDate, LocalDate endDate) {
        return !checkOutDate.isBefore(startDate) && !checkInDate.isAfter(endDate);
    }
}