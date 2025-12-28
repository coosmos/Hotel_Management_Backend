package com.app.service;

import com.app.dto.AuthResponse;
import com.app.dto.LoginRequest;
import com.app.dto.RegisterRequest;
import com.app.dto.UserResponse;
import com.app.entity.User;
import com.app.enums.Role;
import com.app.exception.InvalidCredentialsException;
import com.app.exception.UserAlreadyExistsException;
import com.app.repository.UserRepository;
import com.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username already exists");
        }

        // check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email already exists");
        }

        // validate hotel id for manager and receptionist
        if ((request.getRole() == Role.MANAGER || request.getRole() == Role.RECEPTIONIST)
                && request.getHotelId() == null) {
            throw new IllegalArgumentException("hotel id is required for manager and receptionist roles");
        }

        // create user entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setHotelId(request.getHotelId());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setActive(true);

        // save user
        User savedUser = userRepository.save(user);

        // generate jwt token
        String token = jwtUtil.generateToken(savedUser);

        // create response
        UserResponse userResponse = mapToUserResponse(savedUser);
        return new AuthResponse(token, userResponse);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("invalid username or password"));

        // check if user is active
        if (!user.getActive()) {
            throw new InvalidCredentialsException("user account is inactive");
        }

        // verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("invalid username or password");
        }

        // generate jwt token
        String token = jwtUtil.generateToken(user);

        // create response
        UserResponse userResponse = mapToUserResponse(user);
        return new AuthResponse(token, userResponse);
    }

    // helper method to map user entity to user response dto
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setHotelId(user.getHotelId());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setActive(user.getActive());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}