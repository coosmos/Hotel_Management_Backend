package com.hotel.booking.client;

import com.hotel.booking.dto.external.RoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "hotel-service",
        configuration = com.hotel.booking.config.FeignConfig.class
)
public interface HotelServiceClient {
    @GetMapping("/api/hotels/rooms/hotel/{hotelId}")
    List<RoomDto> getRoomsByHotelId(@PathVariable("hotelId") Long hotelId);
    @GetMapping("/api/hotels/rooms/{roomId}")
    RoomDto getRoomById(@PathVariable("roomId") Long roomId);
    @PatchMapping("/api/hotels/rooms/{roomId}/status")
    RoomDto updateRoomStatus(
            @PathVariable("roomId") Long roomId,
            @RequestParam("status") String status
    );
    @GetMapping("/api/hotels/rooms/hotel/{hotelId}/available")
    List<RoomDto> getAvailableRooms(@PathVariable("hotelId") Long hotelId);
}