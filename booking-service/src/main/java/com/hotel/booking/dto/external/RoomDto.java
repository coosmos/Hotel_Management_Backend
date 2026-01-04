package com.hotel.booking.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private Long hotelId;
    private String roomNumber;
    private String roomType;
    private float pricePerNight;
    private String status; // AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE
    private Boolean isActive;
    private String description;
    private Integer maxOccupancy;
    private String bedType;
    private String amenities;
}