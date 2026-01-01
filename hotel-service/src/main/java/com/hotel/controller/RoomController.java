package com.hotel.controller;

import com.hotel.dto.request.RoomRequestDto;
import com.hotel.dto.request.UpdateRoomStatusDto;
import com.hotel.dto.response.ApiResponse;
import com.hotel.dto.response.RoomResponseDto;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hotels/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
        //create a new room admin / manager
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponseDto>> createRoom(
            @Valid @RequestBody RoomRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        log.info("Create room request for hotel: {} from user: {} with role: {}",
                requestDto.getHotelId(), userId, role);

        RoomResponseDto room = roomService.createRoom(requestDto, userId, role, userHotelId);
        ApiResponse<RoomResponseDto> response = ApiResponse.success(
                "Room created successfully", room);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    //update room admin / manager
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponseDto>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        log.info("Update room request for room: {} from user: {} with role: {}",
                id, userId, role);

        RoomResponseDto room = roomService.updateRoom(id, requestDto, userId, role, userHotelId);
        ApiResponse<RoomResponseDto> response = ApiResponse.success(
                "Room updated successfully", room);

        return ResponseEntity.ok(response);
    }

    // update room status admin / manager
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomResponseDto>> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomStatusDto statusDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        log.info("Update room status request for room: {} to status: {} from user: {}",
                id, statusDto.getStatus(), userId);

        RoomResponseDto room = roomService.updateRoomStatus(id, statusDto, userId, role, userHotelId);
        ApiResponse<RoomResponseDto> response = ApiResponse.success(
                "Room status updated successfully", room);

        return ResponseEntity.ok(response);
    }
    // get room by id all users
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponseDto>> getRoomById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get room request for ID: {} from user: {}", id, userId);

        RoomResponseDto room = roomService.getRoomById(id);
        ApiResponse<RoomResponseDto> response = ApiResponse.success(
                "Room retrieved successfully", room);

        return ResponseEntity.ok(response);
    }
     // Get all rooms for a hotel (all authenticated users)
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<RoomResponseDto>>> getRoomsByHotelId(
            @PathVariable Long hotelId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get rooms request for hotel: {} from user: {}", hotelId, userId);

        List<RoomResponseDto> rooms = roomService.getRoomsByHotelId(hotelId);
        ApiResponse<List<RoomResponseDto>> response = ApiResponse.success(
                "Rooms retrieved successfully", rooms);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<RoomResponseDto>>> getAvailableRoomsByHotelId(
            @PathVariable Long hotelId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get available rooms request for hotel: {} from user: {}", hotelId, userId);

        List<RoomResponseDto> rooms = roomService.getAvailableRoomsByHotelId(hotelId);
        ApiResponse<List<RoomResponseDto>> response = ApiResponse.success(
                "Available rooms retrieved successfully", rooms);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/hotel/{hotelId}/search")
    public ResponseEntity<ApiResponse<List<RoomResponseDto>>> searchRooms(
            @PathVariable Long hotelId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Search rooms request for hotel: {} from user: {} with filters", hotelId, userId);

        List<RoomResponseDto> rooms = roomService.searchRooms(
                hotelId, status, roomType, minPrice, maxPrice);
        ApiResponse<List<RoomResponseDto>> response = ApiResponse.success(
                "Room search completed successfully", rooms);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        log.info("Delete room request for room: {} from user: {} with role: {}",
                id, userId, role);

        roomService.deleteRoom(id, userId, role, userHotelId);
        ApiResponse<Void> response = ApiResponse.success(
                "Room deleted successfully", null);

        return ResponseEntity.ok(response);
    }
}