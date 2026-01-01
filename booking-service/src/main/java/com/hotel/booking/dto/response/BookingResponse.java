package com.hotel.booking.dto.response;

import com.hotel.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long userId;
    private Long hotelId;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private Integer numberOfGuests;
    private Integer numberOfNights;
    private LocalDate cancelledAt;
    private LocalDate checkedInAt;
    private LocalDate checkedOutAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
