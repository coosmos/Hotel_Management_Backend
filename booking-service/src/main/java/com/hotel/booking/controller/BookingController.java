package com.hotel.booking.controller;

import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.request.CheckOutRequest;
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
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private  BookingService bookingService;

    //check room availaaible for a hotel
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
       System.out.println("Checking availability for hotel");
        AvailabilityResponse response = bookingService.checkAvailability(hotelId, checkInDate, checkOutDate);
        return ResponseEntity.ok(response);
    }
    //create a new booking
    @PostMapping
    public ResponseEntity<String> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("booking created successfully");
    }
    //get booking by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking retrieved successfully"));
    }
    //get current user's bookings - user is guest
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        List<BookingResponse> responses = bookingService.getMyBookings();
        return ResponseEntity.ok(
                ApiResponse.success(responses, "User bookings retrieved successfully"));
    }
    //get all bookings for a hotel -used by manager , recpetionist , admin
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<BookingResponse>> getHotelBookings( @PathVariable Long hotelId){
        List<BookingResponse>responses=bookingService.getHotelBookings(hotelId);
        return ResponseEntity.ok(responses);
    }
    //cancel a booking
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        BookingResponse response = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok( "Booking cancelled successfully");
    }
    //check in guest  -- Manager , receptionist ,  admin
   @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkInGuest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CheckInRequest request) {
        CheckInRequest checkInRequest = request != null ? request : new CheckInRequest();
        BookingResponse response = bookingService.checkInGuest(id, checkInRequest);
        return ResponseEntity.ok( "Guest checked in successfully");
    }
    // check-out guest -- Manager , receptionist
    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOutGuest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CheckOutRequest request) {
        CheckOutRequest checkOutRequest = request != null ? request : new CheckOutRequest();
        BookingResponse response = bookingService.checkOutGuest(id, checkOutRequest);

        return ResponseEntity.ok("Guest checked out successfully");
    }
    //get today's checkins for a hotel --admin,manager ,receptionist
    @GetMapping("/hotel/{hotelId}/today-checkins")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getTodayCheckIns(
            @PathVariable Long hotelId) {
        List<BookingResponse> responses = bookingService.getTodayCheckIns(hotelId);
        return ResponseEntity.ok(
                ApiResponse.success(responses, "Today's check-ins retrieved successfully"));
    }
    //get today's checkout for a hotel --manager ,receptionist, admin
    @GetMapping("/hotel/{hotelId}/today-checkouts")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getTodayCheckOuts(
            @PathVariable Long hotelId) {
        List<BookingResponse> responses = bookingService.getTodayCheckOuts(hotelId);
        return ResponseEntity.ok(
                ApiResponse.success(responses, "Today's check-outs retrieved successfully"));
    }
}
