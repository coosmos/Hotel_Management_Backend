package com.hotel.booking.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelAnalyticsDto {
    private Long hotelId;
    private String hotelName;
    private Long totalBookings;
    private Double totalRevenue;
    private Long activeBookings;
    private Double averageBookingValue;
    // constructor for JPQL query -without hotelName
    public HotelAnalyticsDto(Long hotelId, Long totalBookings, Double totalRevenue, Long activeBookings) {
        this.hotelId = hotelId;
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.activeBookings = activeBookings;
        this.averageBookingValue = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;
    }
}