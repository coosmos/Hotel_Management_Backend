package com.hotel.booking.security;

import com.hotel.booking.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private Long userId;
    private String username;
    private String email;
    private UserRole role;
    private Long hotelId; // Nullable for admin and guest
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
    public boolean isStaff() {
        return this.role == UserRole.MANAGER || this.role == UserRole.RECEPTIONIST;
    }
    public boolean isGuest() {
        return this.role == UserRole.GUEST;
    }
    public boolean belongsToHotel(Long hotelId) {
        return this.hotelId != null && this.hotelId.equals(hotelId);
    }
    public boolean canManageBookings() {return isAdmin() || isStaff();}
}
