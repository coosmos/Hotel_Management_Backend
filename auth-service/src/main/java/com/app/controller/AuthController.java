package com.app.controller;

import com.app.dto.AuthResponse;
import com.app.dto.LoginRequest;
import com.app.dto.RegisterRequest;
import com.app.dto.UserResponse;
import com.app.security.JwtUtil;
import com.app.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request,
                                                   HttpServletRequest httpRequest) {
        // extract role from jwt token
        String token = httpRequest.getHeader("Authorization").substring(7);
        String role = jwtUtil.extractRole(token);

        UserResponse response = authService.createUser(request, role);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}