package com.hotel.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    @Size(max = 1000, message = "Feedback cannot exceed 1000 characters")
    private String feedback;
    private Boolean lateCheckOut = false;
}