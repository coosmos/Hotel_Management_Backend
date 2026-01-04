package com.hotel.booking.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelDto {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phoneNumber;
    private String email;
    private Integer totalRooms;
    private Integer availableRooms;
    private String status; // ACTIVE, INACTIVE, UNDER_MAINTENANCE
    private String amenities;
}