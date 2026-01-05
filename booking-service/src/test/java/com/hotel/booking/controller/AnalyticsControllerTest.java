package com.hotel.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.dto.analytics.DashboardAnalyticsDto;
import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
import com.hotel.booking.dto.analytics.RoomTypeAnalyticsDto;
import com.hotel.booking.security.AuthorizationUtil;
import com.hotel.booking.security.UserContext;
import com.hotel.booking.enums.UserRole;
import com.hotel.booking.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private AuthorizationUtil authorizationUtil;

    private UserContext adminContext() {
        return UserContext.builder()
                .userId(1L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
    }

    private UserContext managerContext(Long hotelId) {
        return UserContext.builder()
                .userId(2L)
                .username("manager")
                .role(UserRole.MANAGER)
                .hotelId(hotelId)
                .build();
    }

    // 1. dashboard analytics - admin success
    @Test
    void getDashboardAnalytics_admin_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(adminContext());
        Mockito.when(analyticsService.getDashboardAnalytics())
                .thenReturn(DashboardAnalyticsDto.builder().totalBookings(10L).build());

        mockMvc.perform(get("/api/bookings/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings").value(10));
    }

    // 2. dashboard analytics - forbidden for guest
    @Test
    void getDashboardAnalytics_guest_forbidden() throws Exception {
        Mockito.when(authorizationUtil.getUserContext())
                .thenReturn(UserContext.builder().role(UserRole.GUEST).build());

        mockMvc.perform(get("/api/bookings/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    // 3. hotel analytics - admin
    @Test
    void getHotelAnalytics_admin_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(adminContext());
        Mockito.when(analyticsService.getHotelAnalytics())
                .thenReturn(List.of(
                        HotelAnalyticsDto.builder()
                                .hotelId(1L)
                                .hotelName("Hotel")
                                .totalBookings(5L)
                                .totalRevenue(2000.0)
                                .activeBookings(2L)
                                .averageBookingValue(400.0)
                                .build()
                ));


        mockMvc.perform(get("/api/bookings/analytics/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].hotelId").value(1L));
    }

    // 4. hotel analytics by id - manager of same hotel
    @Test
    void getHotelAnalyticsById_manager_own_hotel_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(managerContext(1L));
        Mockito.doNothing().when(authorizationUtil).verifyHotelAccess(1L);
        Mockito.when(analyticsService.getHotelAnalyticsById(1L))
                .thenReturn(
                        HotelAnalyticsDto.builder()
                                .hotelId(1L)
                                .hotelName("Hotel")
                                .totalBookings(5L)
                                .totalRevenue(2000.0)
                                .activeBookings(2L)
                                .averageBookingValue(400.0)
                                .build()
                );


        mockMvc.perform(get("/api/bookings/analytics/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hotelId").value(1L));
    }

    // 5. revenue range - admin
    @Test
    void getRevenueByDateRange_admin_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(adminContext());
        Mockito.when(analyticsService.getRevenueByDateRange(any(), any()))
                .thenReturn(List.of(new RevenueByDateDto(LocalDate.now(), 2L, 500.0)));

        mockMvc.perform(get("/api/bookings/analytics/revenue")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookingCount").value(2));
    }

    // 6. room type analytics - admin
    @Test
    void getRoomTypeAnalytics_admin_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(adminContext());
        Mockito.when(analyticsService.getRoomTypeAnalytics())
                .thenReturn(List.of(new RoomTypeAnalyticsDto("DELUXE", 3L, 3000.0, 1000.0)));

        mockMvc.perform(get("/api/bookings/analytics/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomType").value("DELUXE"));
    }

    // 7. room type analytics for hotel - manager
    @Test
    void getRoomTypeAnalyticsForHotel_manager_success() throws Exception {
        Mockito.when(authorizationUtil.getUserContext()).thenReturn(managerContext(1L));
        Mockito.doNothing().when(authorizationUtil).verifyHotelAccess(1L);
        Mockito.when(analyticsService.getRoomTypeAnalyticsForHotel(1L))
                .thenReturn(List.of(new RoomTypeAnalyticsDto("SUITE", 1L, 5000.0, 5000.0)));

        mockMvc.perform(get("/api/bookings/analytics/hotels/1/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomType").value("SUITE"));
    }

    // 8. revenue for hotel - forbidden for guest
    @Test
    void getRevenueForHotel_guest_forbidden() throws Exception {
        Mockito.when(authorizationUtil.getUserContext())
                .thenReturn(UserContext.builder().role(UserRole.GUEST).build());

        mockMvc.perform(get("/api/bookings/analytics/hotels/1/revenue")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-10"))
                .andExpect(status().isForbidden());
    }
}
