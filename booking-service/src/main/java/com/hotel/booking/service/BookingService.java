package com.hotel.booking.service;

import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.request.CheckOutRequest;
import com.hotel.booking.dto.response.AvailabilityResponse;
import com.hotel.booking.dto.response.BookingResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    AvailabilityResponse checkAvailability(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);
    BookingResponse createBooking(BookingCreateRequest request);
    BookingResponse getBookingById(Long bookingId);
    List<BookingResponse> getMyBookings();
    List<BookingResponse> getHotelBookings(Long hotelId);
    List<BookingResponse> getAllBookings();
    BookingResponse cancelBooking(Long bookingId, String reason);
    BookingResponse checkInGuest(Long bookingId, CheckInRequest request);
    BookingResponse checkOutGuest(Long bookingId, CheckOutRequest request);
    List<BookingResponse> getTodayCheckIns(Long hotelId);
    List<BookingResponse> getTodayCheckOuts(Long hotelId);
}