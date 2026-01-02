package com.hotel.notification.model;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
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
    private String roomStatus;
    private Integer rating;
    private String feedback;
}
