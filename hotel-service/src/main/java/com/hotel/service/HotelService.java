
// HotelService.java (Interface)
package com.hotel.service;

import com.hotel.dto.request.HotelRequestDto;
import com.hotel.dto.response.HotelResponseDto;
import com.hotel.enums.HotelStatus;

import java.util.List;

public interface HotelService {
    HotelResponseDto createHotel(HotelRequestDto requestDto, Long userId);
    HotelResponseDto updateHotel(Long hotelId, HotelRequestDto requestDto, Long userId, String role, Long userHotelId);
    HotelResponseDto getHotelById(Long hotelId);
    List<HotelResponseDto> getAllHotels();
    List<HotelResponseDto> getActiveHotels();
    List<HotelResponseDto> searchHotels(String city, Integer minRating);
    HotelResponseDto getMyHotel(Long hotelId);
    void deleteHotel(Long hotelId, Long userId, String role);
    void updateHotelRoomCounts(Long hotelId);
}