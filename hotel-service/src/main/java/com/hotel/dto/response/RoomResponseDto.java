package com.hotel.dto.response;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDto {
    private Long id;
    private Long hotelId;
    private String hotelName; // Included for convenience
    private String roomNumber;
    private RoomType roomType;
    private BigDecimal pricePerNight;
    private Integer maxOccupancy;
    private Integer floorNumber;
    private String bedType;
    private Integer roomSize;
    private String amenities;
    private String description;
    private RoomStatus status;
    private Boolean isActive;
    private LocalDateTime statusChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
