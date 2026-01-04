package com.hotel.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomTypeDto {

    private String roomType; // DELUXE, SUITE, EXECUTIVE, etc.
    private BigDecimal pricePerNight;
    private Integer availableCount; // how many rooms of this type available
    private Integer maxOccupancy;
    private String description;
    private String amenities;
    private String bedType;
}