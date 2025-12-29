package com.hotel.gateway.config;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Order(-1)
@Configuration
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Set content type
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Determine status code and message
        HttpStatus status;
        String message;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "Service not found. The requested service might be down or unavailable.";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException rsException = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rsException.getStatusCode().value());
            message = rsException.getReason() != null ? rsException.getReason() : "An error occurred";
        } else if (ex instanceof io.jsonwebtoken.JwtException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Invalid or expired JWT token";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal server error: " + ex.getMessage();
        }
        response.setStatusCode(status);
        String errorJson = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}