package com.hotel.booking.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    // If called from HTTP request, forward headers from gateway
                    var request = attributes.getRequest();
                    String userId = request.getHeader("X-User-Id");
                    String username = request.getHeader("X-Username");
                    String email = request.getHeader("X-User-Email");
                    String role = request.getHeader("X-User-Role");
                    String hotelId = request.getHeader("X-Hotel-Id");

                    if (userId != null) template.header("X-User-Id", userId);
                    if (username != null) template.header("X-Username", username);
                    if (email != null) template.header("X-User-Email", email);
                    if (role != null) template.header("X-User-Role", role);
                    if (hotelId != null) template.header("X-Hotel-Id", hotelId);
                } else {
                    // If called from scheduler/background job, use system headers
                    template.header("X-User-Id", "0");
                    template.header("X-Username", "system");
                    template.header("X-User-Email", "system@hotel.com");
                    template.header("X-User-Role", "ADMIN");
                }
            }
        };
    }
}