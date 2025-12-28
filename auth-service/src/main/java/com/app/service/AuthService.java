package com.app.service;

import com.app.dto.AuthResponse;
import com.app.dto.LoginRequest;
import com.app.dto.RegisterRequest;
import com.app.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse createUser(RegisterRequest request, String adminRole);
}