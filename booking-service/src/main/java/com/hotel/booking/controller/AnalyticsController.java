package com.hotel.booking.controller;

import com.hotel.booking.dto.analytics.DashboardAnalyticsDto;
import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
import com.hotel.booking.dto.analytics.RoomTypeAnalyticsDto;
import com.hotel.booking.dto.response.ApiResponse;
import com.hotel.booking.exception.UnauthorizedException;
import com.hotel.booking.security.UserContext;
import com.hotel.booking.security.AuthorizationUtil;
import com.hotel.booking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthorizationUtil authorizationUtil;

    //get dashboard analytics for admin
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardAnalyticsDto>> getDashboardAnalytics() {
        UserContext context = authorizationUtil.getUserContext();

        if (!context.isAdmin()) {
            throw new UnauthorizedException("Only admins can view dashboard analytics");
        }

        DashboardAnalyticsDto analytics = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Dashboard analytics retrieved successfully"));
    }

    //get hotel wise analytics
    @GetMapping("/hotels")
    public ResponseEntity<ApiResponse<List<HotelAnalyticsDto>>> getHotelAnalytics() {
        UserContext context = authorizationUtil.getUserContext();

        if (!context.isAdmin()) {
            throw new UnauthorizedException("Only admins can view hotel analytics");
        }

        List<HotelAnalyticsDto> analytics = analyticsService.getHotelAnalytics();
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Hotel analytics retrieved successfully"));
    }

     // get analytics for specific hotel - admin or manager of that hotel
    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelAnalyticsDto>> getHotelAnalyticsById(
            @PathVariable Long hotelId) {

        UserContext context = authorizationUtil.getUserContext();

        // admin can view all, manager can view their own hotel
        if (!context.isAdmin()) {
            if (!context.isStaff()) {
                throw new UnauthorizedException("Only admins and hotel staff can view hotel analytics");
            }
            // verify manager/receptionist belongs to this hotel
            authorizationUtil.verifyHotelAccess(hotelId);
        }

        HotelAnalyticsDto analytics = analyticsService.getHotelAnalyticsById(hotelId);
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Hotel analytics retrieved successfully"));
    }

    //get revenue by date range
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueByDateDto>>> getRevenueByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UserContext context = authorizationUtil.getUserContext();

        if (!context.isAdmin()) {
            throw new UnauthorizedException("Only admins can view revenue analytics");
        }

        List<RevenueByDateDto> analytics = analyticsService.getRevenueByDateRange(startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Revenue analytics retrieved successfully"));
    }

    //get revenue by date range for a htel
    @GetMapping("/hotels/{hotelId}/revenue")
    public ResponseEntity<ApiResponse<List<RevenueByDateDto>>> getRevenueByDateRangeForHotel(
            @PathVariable Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UserContext context = authorizationUtil.getUserContext();

        // admin can view all, manager can view their own hotel
        if (!context.isAdmin()) {
            if (!context.isStaff()) {
                throw new UnauthorizedException("Only admins and hotel staff can view revenue analytics");
            }
            authorizationUtil.verifyHotelAccess(hotelId);
        }

        List<RevenueByDateDto> analytics = analyticsService.getRevenueByDateRangeForHotel(
                hotelId, startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Revenue analytics retrieved successfully"));
    }

    //get room type analyitcs
    @GetMapping("/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypeAnalyticsDto>>> getRoomTypeAnalytics() {
        UserContext context = authorizationUtil.getUserContext();

        if (!context.isAdmin()) {
            throw new UnauthorizedException("Only admins can view room type analytics");
        }

        List<RoomTypeAnalyticsDto> analytics = analyticsService.getRoomTypeAnalytics();
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Room type analytics retrieved successfully"));
    }

    //get room type analytics for a specific hotel
    @GetMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypeAnalyticsDto>>> getRoomTypeAnalyticsForHotel(
            @PathVariable Long hotelId) {
        UserContext context = authorizationUtil.getUserContext();
        // admin can view all, manager can view their own hotel
        if (!context.isAdmin()) {
            if (!context.isStaff()) {
                throw new UnauthorizedException("Only admins and hotel staff can view room type analytics");
            }
            authorizationUtil.verifyHotelAccess(hotelId);
        }
        List<RoomTypeAnalyticsDto> analytics = analyticsService.getRoomTypeAnalyticsForHotel(hotelId);
        return ResponseEntity.ok(
                ApiResponse.success(analytics, "Room type analytics retrieved successfully"));
    }
}