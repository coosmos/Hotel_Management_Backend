package com.hotel.booking.service;

import com.hotel.booking.dto.analytics.DashboardAnalyticsDto;
import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
import com.hotel.booking.dto.analytics.RoomTypeAnalyticsDto;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {
    // dashboard overview
    DashboardAnalyticsDto getDashboardAnalytics();
    // hotel-wise analytics
    List<HotelAnalyticsDto> getHotelAnalytics();
    HotelAnalyticsDto getHotelAnalyticsById(Long hotelId);
    // revenue by date
    List<RevenueByDateDto> getRevenueByDateRange(LocalDate startDate, LocalDate endDate);
    List<RevenueByDateDto> getRevenueByDateRangeForHotel(Long hotelId, LocalDate startDate, LocalDate endDate);
    // room type analytics
    List<RoomTypeAnalyticsDto> getRoomTypeAnalytics();
    List<RoomTypeAnalyticsDto> getRoomTypeAnalyticsForHotel(Long hotelId);
}