package com.hotel.notification.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckInReminderEvent {

    private Long bookingId;
    private Long userId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private Long hotelId;
    private String hotelName;
    private Long roomId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
