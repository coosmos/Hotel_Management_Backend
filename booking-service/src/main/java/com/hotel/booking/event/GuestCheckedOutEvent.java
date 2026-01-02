package com.hotel.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestCheckedOutEvent {
    private Long bookingId;
    private Long userId;
    private Long hotelId;
    private Long roomId;
    private String guestName;
    private String guestEmail;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime checkedOutAt;
    private String roomStatus; // Should be "CLEANING"
    private Integer rating;
    private String feedback;
}