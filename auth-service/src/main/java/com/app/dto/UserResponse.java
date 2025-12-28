package com.app.dto;

import com.app.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private Long hotelId;
    private String fullName;
    private String phoneNumber;
    private Boolean active;
    private LocalDateTime createdAt;
}