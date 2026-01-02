package com.hotel.dto.response;

import com.hotel.enums.HotelStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponseDto {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String contactNumber;
    private String email;
    private Integer starRating;
    private String amenities;
    private HotelStatus status;
    private Integer totalRooms;
    private Integer availableRooms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}