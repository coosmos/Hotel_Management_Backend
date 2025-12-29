package com.hotel.gateway.filter;

import com.hotel.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // Check if the route is secured
        if (validator.isSecured.test(request)) {
            // Check if Authorization header exists
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }
            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            // Check if header starts with "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header format", HttpStatus.UNAUTHORIZED);
            }
            // Extract token
            String token = authHeader.substring(7);
            try {
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }
                // Extract user information from token
                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                Long hotelId = jwtUtil.extractHotelId(token);
                // Add user information to request headers for downstream services
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-Username", username != null ? username : "")
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Hotel-Id", hotelId != null ? hotelId.toString() : "")
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Token validation failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        }
        return chain.filter(exchange);
    }
    @Override
    public int getOrder() {
        return -1; // High priority - execute before other filters
    }
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        String errorMessage = String.format("{\"error\": \"%s\", \"status\": %d}",
                err, httpStatus.value());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorMessage.getBytes())));
    }
}