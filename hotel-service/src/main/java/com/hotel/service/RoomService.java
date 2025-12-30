// RoomService.java (Interface)
package com.hotel.service;

import com.hotel.dto.request.RoomRequestDto;
import com.hotel.dto.request.UpdateRoomStatusDto;
import com.hotel.dto.response.RoomResponseDto;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.math.BigDecimal;
import java.util.List;

public interface RoomService {
    RoomResponseDto createRoom(RoomRequestDto requestDto, Long userId, String role, Long userHotelId);
    RoomResponseDto updateRoom(Long roomId, RoomRequestDto requestDto, Long userId, String role, Long userHotelId);
    RoomResponseDto updateRoomStatus(Long roomId, UpdateRoomStatusDto statusDto, Long userId, String role, Long userHotelId);
    RoomResponseDto getRoomById(Long roomId);
    List<RoomResponseDto> getRoomsByHotelId(Long hotelId);
    List<RoomResponseDto> getAvailableRoomsByHotelId(Long hotelId);
    List<RoomResponseDto> searchRooms(Long hotelId, RoomStatus status, RoomType roomType, BigDecimal minPrice, BigDecimal maxPrice);
    void deleteRoom(Long roomId, Long userId, String role, Long userHotelId);
}
