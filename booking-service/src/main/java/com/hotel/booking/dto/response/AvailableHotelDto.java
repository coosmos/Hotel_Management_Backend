package com.hotel.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableHotelDto {

    private Long hotelId;
    private String hotelName;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phoneNumber;
    private String email;
    private Integer totalRooms;
    private Integer availableRoomsCount; // available for the searched dates
    private String status;
    private String amenities;
}