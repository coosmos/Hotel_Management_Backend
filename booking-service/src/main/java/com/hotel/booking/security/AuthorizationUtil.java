package com.hotel.booking.security;

import com.hotel.booking.enums.UserRole;
import com.hotel.booking.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class AuthorizationUtil {

    public UserContext getUserContext() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new UnauthorizedException("No request context available");
        }
        HttpServletRequest request = attributes.getRequest();
        String userIdStr = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String email = request.getHeader("X-User-Email");
        String roleStr = request.getHeader("X-User-Role");
        String hotelIdStr = request.getHeader("X-Hotel-Id");
        if (userIdStr == null || roleStr == null) {
            throw new UnauthorizedException("Missing authentication headers");
        }
        try {
            Long userId = Long.parseLong(userIdStr);
            UserRole role = UserRole.valueOf(roleStr);
            Long hotelId = hotelIdStr != null && !hotelIdStr.isEmpty()
                    ? Long.parseLong(hotelIdStr) : null;

            return UserContext.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .role(role)
                    .hotelId(hotelId)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing user context: {}", e.getMessage());
            throw new UnauthorizedException("Invalid authentication headers");
        }
    }
    public void verifyHotelAccess(Long hotelId) {
        UserContext context = getUserContext();
        // admin has access to all hotels
        if (context.isAdmin()) {
            return;
        }
        // Staff can only access their own hotel
        if (context.isStaff()) {
            if (context.getHotelId() == null || !context.getHotelId().equals(hotelId)) {
                throw new UnauthorizedException(
                        "You don't have permission to access this hotel's data");
            }
            return;
        }
        // guest cannot access hotel-level operations
        throw new UnauthorizedException("Guests cannot access hotel operations");
    }
    public void verifyBookingAccess(Long bookingUserId, Long bookingHotelId) {
        UserContext context = getUserContext();
        // ADMIN has access to all bookings
        if (context.isAdmin()) {
            return;
        }
        // Staff can access bookings for their hotel
        if (context.isStaff()) {
            if (context.getHotelId() == null || !context.getHotelId().equals(bookingHotelId)) {
                throw new UnauthorizedException(
                        "You can only access bookings for your hotel");
            }
            return;
        }
        // GUEST can only access their own bookings
        if (context.isGuest()) {
            if (!context.getUserId().equals(bookingUserId)) {
                throw new UnauthorizedException("You can only access your own bookings");
            }
            return;
        }
        throw new UnauthorizedException("Access denied");
    }
}