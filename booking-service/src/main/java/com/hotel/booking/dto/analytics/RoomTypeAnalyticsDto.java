package com.hotel.booking.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeAnalyticsDto {
    private String roomType;
    private Long bookingCount;
    private Double totalRevenue;
    private Double averagePrice;
}