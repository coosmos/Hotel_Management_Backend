package com.hotel.booking.dto.response;

import com.hotel.booking.dto.external.RoomDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer totalRooms;
    private Integer availableRooms;
    private List<RoomDto> availableRoomList;
}