package com.hotel.booking.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String specialRequests;
}