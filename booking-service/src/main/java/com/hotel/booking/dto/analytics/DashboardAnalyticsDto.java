package com.hotel.booking.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsDto {
    private Long totalBookings;
    private Double totalRevenue;
    private Long activeBookings; // CONFIRMED + CHECKED_IN
    private Long completedBookings; // CHECKED_OUT
    private Long cancelledBookings;
    private Long todayCheckIns;
    private Long todayCheckOuts;
    private Long pendingPayments;
    private Double averageBookingValue;
}