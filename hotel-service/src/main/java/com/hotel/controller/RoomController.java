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
    @PostMapping
    public ResponseEntity<RoomResponseDto> createRoom(
            @Valid @RequestBody RoomRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        RoomResponseDto room =
                roomService.createRoom(requestDto, userId, role, userHotelId);

        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDto> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        return ResponseEntity.ok(
                roomService.updateRoom(id, requestDto, userId, role, userHotelId)
        );
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomResponseDto> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomStatusDto statusDto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {
        return ResponseEntity.ok(
                roomService.updateRoomStatus(id, statusDto, userId, role, userHotelId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDto> getRoomById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(roomService.getRoomById(id));
    }
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<RoomResponseDto>> getRoomsByHotelId(
            @PathVariable Long hotelId,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(
                roomService.getRoomsByHotelId(hotelId)
        );
    }
    @GetMapping("/hotel/{hotelId}/available")
    public ResponseEntity<List<RoomResponseDto>> getAvailableRoomsByHotelId(
            @PathVariable Long hotelId,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(
                roomService.getAvailableRoomsByHotelId(hotelId)
        );
    }
    @GetMapping("/hotel/{hotelId}/search")
    public ResponseEntity<List<RoomResponseDto>> searchRooms(
            @PathVariable Long hotelId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(
                roomService.searchRooms(hotelId, status, roomType, minPrice, maxPrice)
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hotel-Id", required = false) Long userHotelId) {

        roomService.deleteRoom(id, userId, role, userHotelId);
        return ResponseEntity.noContent().build();
    }
}
