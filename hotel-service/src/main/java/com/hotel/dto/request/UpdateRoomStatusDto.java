package com.hotel.dto.request;

import com.hotel.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomStatusDto {

    @NotNull(message = "Room status is required")
    private RoomStatus status;
    private String remarks; // Optional reason for status change
}