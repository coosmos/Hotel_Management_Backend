package com.hotel.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    // Public endpoints that don't require JWT authentication
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/",
            "/eureka/"
    );
    // Predicate to check if the request is for a secured endpoint
    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                return openApiEndpoints.stream().noneMatch(path::startsWith);
    };
}