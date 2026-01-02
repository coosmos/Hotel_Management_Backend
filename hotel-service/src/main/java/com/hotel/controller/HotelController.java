package com.hotel.controller;

import com.hotel.dto.request.HotelRequestDto;
import com.hotel.dto.response.ApiResponse;
import com.hotel.dto.response.HotelResponseDto;
import com.hotel.enums.UserRole;
import com.hotel.exception.ForbiddenException;
import com.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {

    private final HotelService hotelService;
    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponseDto>> createHotel(
            @Valid @RequestBody HotelRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("Create hotel request from user: {} with role: {}", userId, role);
        // authorization: Only ADMIN can create hotels
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ForbiddenException("Only administrators can create hotels");
        }
        HotelResponseDto hotel = hotelService.createHotel(requestDto, userId);
        ApiResponse<HotelResponseDto> response = ApiResponse.success(
                "Hotel created successfully", hotel);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    //update hotel-details   admin/manager
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDto>> updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequestDto requestDto,
                                                                     @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role,
                                                                     @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {
        log.info("Update hotel request for hotel: {} from user: {} with role: {}",
                id, userId, role);
        HotelResponseDto hotel = hotelService.updateHotel(id, requestDto, userId, role, userHotelId);
        ApiResponse<HotelResponseDto> response = ApiResponse.success(
                "Hotel updated successfully", hotel);

        return ResponseEntity.ok(response);
    }
        //get hotel by ids
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDto>> getHotelById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get hotel request for ID: {} from user: {}", id, userId);
        HotelResponseDto hotel = hotelService.getHotelById(id);
        ApiResponse<HotelResponseDto> response = ApiResponse.success(
                "Hotel retrieved successfully", hotel);

        return ResponseEntity.ok(response);
    }

   /** get all hotels--for admin */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponseDto>>> getAllHotels(
            @RequestHeader("X-User-Role") String role) {
        log.info("Get all hotels request from role: {}", role);
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ForbiddenException("Only administrators can view all hotels");
        }
        List<HotelResponseDto> hotels = hotelService.getAllHotels();
        ApiResponse<List<HotelResponseDto>> response = ApiResponse.success(
                "Hotels retrieved successfully", hotels);
        return ResponseEntity.ok(response);
    }
    //get active hotels
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<HotelResponseDto>>> getActiveHotels(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get active hotels request from user: {}", userId);

        List<HotelResponseDto> hotels = hotelService.getActiveHotels();
        ApiResponse<List<HotelResponseDto>> response = ApiResponse.success(
                "Active hotels retrieved successfully", hotels);

        return ResponseEntity.ok(response);
    }

    //search hotels
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<HotelResponseDto>>> searchHotels(
            @RequestParam(required = false) String city,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Search hotels request from user: {} with city: {}",
                userId, city);
        List<HotelResponseDto> hotels = hotelService.searchHotels(city);
        ApiResponse<List<HotelResponseDto>> response = ApiResponse.success(
                "Hotels search completed successfully", hotels);
        return ResponseEntity.ok(response);
    }

    // get my hotel manager or receptionist
    @GetMapping("/my-hotel")
    public ResponseEntity<ApiResponse<HotelResponseDto>> getMyHotel(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {
        log.info("Get my hotel request from user: {} with role: {}", userId, role);
        if (!UserRole.MANAGER.name().equals(role) && !UserRole.RECEPTIONIST.name().equals(role)) {
            throw new ForbiddenException("Only managers and receptionists can access their hotel");
        }
        if (userHotelId == null) {
            throw new ForbiddenException("No hotel assigned to this user");
        }
        HotelResponseDto hotel = hotelService.getMyHotel(userHotelId);
        ApiResponse<HotelResponseDto> response = ApiResponse.success(
                "Your hotel retrieved successfully", hotel);
        return ResponseEntity.ok(response);
    }

}