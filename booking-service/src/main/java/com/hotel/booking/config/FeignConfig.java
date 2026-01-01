package com.hotel.booking.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    //forward authentication headers to downstream services--
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                var request = attributes.getRequest();
                // Forward all X-User-* headers
                String userId = request.getHeader("X-User-Id");
                String username = request.getHeader("X-Username");
                String email = request.getHeader("X-User-Email");
                String role = request.getHeader("X-User-Role");
                String hotelId = request.getHeader("X-Hotel-Id");
                if (userId != null) requestTemplate.header("X-User-Id", userId);
                if (username != null) requestTemplate.header("X-Username", username);
                if (email != null) requestTemplate.header("X-User-Email", email);
                if (role != null) requestTemplate.header("X-User-Role", role);
                if (hotelId != null) requestTemplate.header("X-Hotel-Id", hotelId);
            }
        };
    }
}
