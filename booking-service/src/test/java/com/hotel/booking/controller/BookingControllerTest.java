package com.hotel.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.response.*;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void checkAvailability_success() throws Exception {
        AvailabilityResponse response = AvailabilityResponse.builder()
                .hotelId(1L)
                .availableRooms(3)
                .totalRooms(5)
                .build();

        Mockito.when(bookingService.checkAvailability(
                Mockito.eq(1L),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(response);

        mockMvc.perform(get("/api/bookings/availability")
                        .param("hotelId", "1")
                        .param("checkInDate", "2026-01-10")
                        .param("checkOutDate", "2026-01-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value(1L))
                .andExpect(jsonPath("$.availableRooms").value(3));
    }

    @Test
    void createBooking_success() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId(1L)
                .roomType("DELUXE")
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .guestName("John")
                .guestEmail("john@test.com")
                .guestPhone("9999999999")
                .numberOfGuests(2)
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(10L)
                .status(BookingStatus.CONFIRMED)
                .build();

        Mockito.when(bookingService.createBooking(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void getBookingById_success() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(5L)
                .status(BookingStatus.CONFIRMED)
                .build();

        Mockito.when(bookingService.getBookingById(5L))
                .thenReturn(response);

        mockMvc.perform(get("/api/bookings/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5L));
    }

    @Test
    void getMyBookings_success() throws Exception {
        Mockito.when(bookingService.getMyBookings())
                .thenReturn(List.of(
                        BookingResponse.builder().id(1L).build(),
                        BookingResponse.builder().id(2L).build()
                ));

        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void cancelBooking_success() throws Exception {
        Mockito.when(bookingService.cancelBooking(1L, null))
                .thenReturn(BookingResponse.builder().id(1L).build());

        mockMvc.perform(patch("/api/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking cancelled successfully"));
    }

    @Test
    void checkInGuest_success() throws Exception {
        Mockito.when(bookingService.checkInGuest(Mockito.eq(1L), Mockito.any(CheckInRequest.class)))
                .thenReturn(BookingResponse.builder().id(1L).build());

        mockMvc.perform(patch("/api/bookings/1/check-in")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Guest checked in successfully"));
    }

    @Test
    void searchAvailableHotels_success() throws Exception {
        Mockito.when(bookingService.searchAvailableHotels(
                Mockito.eq("Delhi"),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(List.of(new AvailableHotelDto(), new AvailableHotelDto()));

        mockMvc.perform(get("/api/bookings/search-hotels")
                        .param("city", "Delhi")
                        .param("checkInDate", "2026-01-10")
                        .param("checkOutDate", "2026-01-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void updatePaymentStatus_success() throws Exception {
        Mockito.when(bookingService.updatePaymentStatus(1L, "PAID", "CASH"))
                .thenReturn(BookingResponse.builder().id(1L).build());

        mockMvc.perform(patch("/api/bookings/1/payment")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }
}
