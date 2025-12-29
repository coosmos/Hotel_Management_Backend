package com.hotel.dto.request;

import com.hotel.enums.HotelStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelRequestDto {

    @NotBlank(message = "Hotel name is required")
    @Size(min = 3, max = 200, message = "Hotel name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @Pattern(regexp = "^[0-9]{5,10}$", message = "Invalid pincode format")
    private String pincode;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid contact number format")
    private String contactNumber;

    @Email(message = "Invalid email format")
    private String email;

    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating cannot exceed 5")
    private Integer starRating;

    private String amenities; // Comma-separated

    private String imageUrl;

    private HotelStatus status;
}