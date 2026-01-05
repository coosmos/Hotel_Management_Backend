package com.hotel.booking.service;

import com.hotel.booking.client.HotelServiceClient;
import com.hotel.booking.dto.analytics.DashboardAnalyticsDto;
import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
import com.hotel.booking.dto.analytics.RoomTypeAnalyticsDto;
import com.hotel.booking.dto.external.HotelDto;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.dto.response.ApiResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.repository.BookingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private Booking booking;

    @BeforeEach
    void setup() {
        booking = Booking.builder()
                .hotelId(1L)
                .roomId(101L)
                .totalAmount(2000f)
                .status(BookingStatus.CONFIRMED)
                .build();

        booking.setId(1L); // <-- THIS is the correct way
    }

    // 1️⃣ Dashboard analytics
    @Test
    void getDashboardAnalytics_success() {
        when(bookingRepository.getTotalBookings()).thenReturn(10L);
        when(bookingRepository.getTotalRevenue()).thenReturn(50000.0);
        when(bookingRepository.getActiveBookings()).thenReturn(3L);
        when(bookingRepository.getCompletedBookings()).thenReturn(5L);
        when(bookingRepository.getCancelledBookings()).thenReturn(2L);
        when(bookingRepository.getTodayCheckInsCount(any())).thenReturn(1L);
        when(bookingRepository.getTodayCheckOutsCount(any())).thenReturn(1L);
        when(bookingRepository.getPendingPayments()).thenReturn(2L);
        when(bookingRepository.getAverageBookingValue()).thenReturn(5000.0);

        DashboardAnalyticsDto dto = analyticsService.getDashboardAnalytics();

        assertEquals(10L, dto.getTotalBookings());
        assertEquals(50000.0, dto.getTotalRevenue());
        assertEquals(3L, dto.getActiveBookings());
        assertEquals(2L, dto.getCancelledBookings());
    }

    // 2️⃣ Hotel analytics (with hotel name)
    @Test
    void getHotelAnalytics_success() {
        HotelAnalyticsDto ha = new HotelAnalyticsDto(1L, 5L, 20000.0, 2L);
        when(bookingRepository.getRevenueByHotel()).thenReturn(List.of(ha));

        HotelDto hotel = new HotelDto();
        hotel.setId(1L);
        hotel.setName("Taj Hotel");

        when(hotelServiceClient.getHotelByIdWrapped(1L))
                .thenReturn(ApiResponse.success(hotel, "ok"));

        List<HotelAnalyticsDto> result = analyticsService.getHotelAnalytics();

        assertEquals(1, result.size());
        assertEquals("Taj Hotel", result.get(0).getHotelName());
    }

    // 3️⃣ Hotel analytics fallback when hotel-service fails
    @Test
    void getHotelAnalytics_hotelServiceFails_fallbackName() {
        HotelAnalyticsDto ha = new HotelAnalyticsDto(2L, 0L, 0.0, 0L);
        when(bookingRepository.getRevenueByHotel()).thenReturn(List.of(ha));
        when(hotelServiceClient.getHotelByIdWrapped(2L))
                .thenThrow(new RuntimeException("service down"));

        List<HotelAnalyticsDto> result = analyticsService.getHotelAnalytics();

        assertEquals("Hotel #2", result.get(0).getHotelName());
    }

    // 4️⃣ Get analytics by hotel id (present)
    @Test
    void getHotelAnalyticsById_found() {
        when(bookingRepository.getRevenueByHotel())
                .thenReturn(List.of(new HotelAnalyticsDto(1L, 4L, 10000.0, 1L)));

        HotelDto hotel = new HotelDto();
        hotel.setId(1L);
        hotel.setName("Oberoi");

        when(hotelServiceClient.getHotelByIdWrapped(1L))
                .thenReturn(ApiResponse.success(hotel, "ok"));

        HotelAnalyticsDto dto = analyticsService.getHotelAnalyticsById(1L);

        assertEquals("Oberoi", dto.getHotelName());
        assertEquals(4L, dto.getTotalBookings());
    }

    // 5️⃣ Revenue by date range
    @Test
    void getRevenueByDateRange_success() {
        List<RevenueByDateDto> data = List.of(
                new RevenueByDateDto(LocalDate.now(), 2L, 4000.0)
        );

        when(bookingRepository.getRevenueByDateRange(any(), any()))
                .thenReturn(data);

        List<RevenueByDateDto> result =
                analyticsService.getRevenueByDateRange(LocalDate.now().minusDays(5), LocalDate.now());

        assertEquals(1, result.size());
        assertEquals(4000.0, result.get(0).getRevenue());
    }

    // 6️⃣ Room type analytics (global)
    @Test
    void getRoomTypeAnalytics_success() {
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        RoomDto room = new RoomDto();
        room.setRoomType("DELUXE");

        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);

        List<RoomTypeAnalyticsDto> result = analyticsService.getRoomTypeAnalytics();

        assertEquals(1, result.size());
        assertEquals("DELUXE", result.get(0).getRoomType());
        assertEquals(1L, result.get(0).getBookingCount());
    }

    // 7️⃣ Room type analytics ignores cancelled bookings
    @Test
    void getRoomTypeAnalytics_ignoresCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<RoomTypeAnalyticsDto> result = analyticsService.getRoomTypeAnalytics();

        assertTrue(result.isEmpty());
    }

    // 8️⃣ Room type analytics for hotel
    @Test
    void getRoomTypeAnalyticsForHotel_success() {
        when(bookingRepository.findByHotelIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(booking));

        RoomDto room = new RoomDto();
        room.setRoomType("SUITE");

        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);

        List<RoomTypeAnalyticsDto> result =
                analyticsService.getRoomTypeAnalyticsForHotel(1L);

        assertEquals(1, result.size());
        assertEquals("SUITE", result.get(0).getRoomType());
    }
}
