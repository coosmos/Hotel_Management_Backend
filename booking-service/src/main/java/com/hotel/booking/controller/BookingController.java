package com.hotel.booking.controller;

import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.response.ApiResponse;
import com.hotel.booking.dto.response.AvailabilityResponse;
import com.hotel.booking.dto.response.BookingResponse;
import com.hotel.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private  BookingService bookingService;

    //check room availaaible for a hotel
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(@RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
       System.out.println("Checking availability for hotel");
        AvailabilityResponse response = bookingService.checkAvailability(hotelId, checkInDate, checkOutDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Availability checked successfully"));
    }
    //create a new booking
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking created successfully"));
    }



}
