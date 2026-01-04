package com.hotel.booking.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByDateDto {
    private LocalDate date;
    private Long bookingCount;
    private Double revenue;
}