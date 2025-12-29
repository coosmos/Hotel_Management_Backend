package com.app.service;

import com.app.dto.*;

public interface AuthService {

    AuthResponse register(GuestRegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse createUser(RegisterRequest request, String adminRole);
    void updateUsername(Long userid, String newusername);
    void changePassword(Long userid, ChangePasswordRequest request);
}