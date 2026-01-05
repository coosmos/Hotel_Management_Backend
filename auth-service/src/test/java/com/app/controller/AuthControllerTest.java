package com.app.controller;

import com.app.dto.*;
import com.app.enums.Role;
import com.app.exception.InvalidCredentialsException;
import com.app.exception.UserAlreadyExistsException;
import com.app.exception.UserNotFoundException;
import com.app.security.JwtUtil;
import com.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for simplicity
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private com.app.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.app.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    private GuestRegisterRequest guestRegisterRequest;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private UpdateUsernameRequest updateUsernameRequest;
    private ChangePasswordRequest changePasswordRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        // setup user response
        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setRole(Role.GUEST);
        userResponse.setFullName("Test User");
        userResponse.setPhoneNumber("1234567890");
        userResponse.setActive(true);
        userResponse.setCreatedAt(LocalDateTime.now());

        // setup auth response
        authResponse = new AuthResponse("jwt-token-123", userResponse);

        // setup guest register request
        guestRegisterRequest = new GuestRegisterRequest();
        guestRegisterRequest.setUsername("newguest");
        guestRegisterRequest.setEmail("newguest@example.com");
        guestRegisterRequest.setPassword("Password123");
        guestRegisterRequest.setFullName("New Guest");
        guestRegisterRequest.setPhoneNumber("9876543210");

        // setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123");

        // setup register request (admin creates staff)
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("manager1");
        registerRequest.setEmail("manager@example.com");
        registerRequest.setPassword("Manager123");
        registerRequest.setRole(Role.MANAGER);
        registerRequest.setHotelId(1L);
        registerRequest.setFullName("Hotel Manager");
        registerRequest.setPhoneNumber("1111111111");

        // setup update username request
        updateUsernameRequest = new UpdateUsernameRequest();
        updateUsernameRequest.setNewUsername("updatedusername");

        // setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("OldPassword123");
        changePasswordRequest.setNewPassword("NewPassword123");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void testRegister_Success() throws Exception {
        // given
        when(authService.register(any(GuestRegisterRequest.class))).thenReturn(authResponse);

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.role").value("GUEST"));

        verify(authService, times(1)).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Error - Blank Username")
    void testRegister_BlankUsername() throws Exception {
        // given
        guestRegisterRequest.setUsername("");
        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());

        verify(authService, never()).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Error - Invalid Email")
    void testRegister_InvalidEmail() throws Exception {
        // given
        guestRegisterRequest.setEmail("invalidemail");

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Error - Short Password")
    void testRegister_ShortPassword() throws Exception {
        // given
        guestRegisterRequest.setPassword("Pass1"); // less than 8 chars

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(authService, never()).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Error - Password Without Number")
    void testRegister_PasswordWithoutNumber() throws Exception {
        // given
        guestRegisterRequest.setPassword("PasswordOnly"); // no digit

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(authService, never()).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Error - Invalid Phone Number")
    void testRegister_InvalidPhoneNumber() throws Exception {
        // given
        guestRegisterRequest.setPhoneNumber("12345"); // not 10 digits

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());

        verify(authService, never()).register(any(GuestRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate Username")
    void testRegister_DuplicateUsername() throws Exception {
        // given
        when(authService.register(any(GuestRegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("username already exists"));

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRegisterRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("username already exists"));

        verify(authService, times(1)).register(any(GuestRegisterRequest.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void testLogin_Success() throws Exception {
        // given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Validation Error - Blank Username")
    void testLogin_BlankUsername() throws Exception {
        // given
        loginRequest.setUsername("");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid Credentials")
    void testLogin_InvalidCredentials() throws Exception {
        // given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("invalid username or password"));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid username or password"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // ==================== CREATE USER TESTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/auth/create-user - Success")
    void testCreateUser_Success() throws Exception {
        // given
        UserResponse managerResponse = new UserResponse();
        managerResponse.setId(2L);
        managerResponse.setUsername("manager1");
        managerResponse.setEmail("manager@example.com");
        managerResponse.setRole(Role.MANAGER);
        managerResponse.setHotelId(1L);

        when(jwtUtil.extractRole(anyString())).thenReturn("ADMIN");
        when(authService.createUser(any(RegisterRequest.class), eq("ADMIN")))
                .thenReturn(managerResponse);

        // when & then
        mockMvc.perform(post("/api/auth/create-user")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("manager1"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.hotelId").value(1));

        verify(jwtUtil, times(1)).extractRole(anyString());
        verify(authService, times(1)).createUser(any(RegisterRequest.class), eq("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/create-user - Validation Error - Missing HotelId")
    void testCreateUser_MissingHotelId() throws Exception {
        // given
        registerRequest.setHotelId(null);

        when(jwtUtil.extractRole(anyString())).thenReturn("ADMIN");

        // when & then
        mockMvc.perform(post("/api/auth/create-user")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.hotelId").exists());

        verify(authService, never()).createUser(any(RegisterRequest.class), anyString());
    }

    @Test
    @DisplayName("POST /api/auth/create-user - Non-Admin Caller")
    void testCreateUser_NonAdminCaller() throws Exception {
        // given
        when(jwtUtil.extractRole(anyString())).thenReturn("GUEST");
        when(authService.createUser(any(RegisterRequest.class), eq("GUEST")))
                .thenThrow(new SecurityException("only admin can create users"));

        // when & then
        mockMvc.perform(post("/api/auth/create-user")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("only admin can create users"));

        verify(jwtUtil, times(1)).extractRole(anyString());
        verify(authService, times(1)).createUser(any(RegisterRequest.class), eq("GUEST"));
    }

    // ==================== UPDATE USERNAME TESTS ====================

    @Test
    @DisplayName("PUT /api/auth/me/username - Success")
    void testUpdateUsername_Success() throws Exception {
        // given
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doNothing().when(authService).updateUsername(anyLong(), anyString());

        // when & then
        mockMvc.perform(put("/api/auth/me/username")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUsernameRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("username updated successfully"));

        verify(jwtUtil, times(1)).extractUserId(anyString());
        verify(authService, times(1)).updateUsername(1L, "updatedusername");
    }

    @Test
    @DisplayName("PUT /api/auth/me/username - Missing Authorization Header")
    void testUpdateUsername_MissingAuthHeader() throws Exception {
        // when & then
        mockMvc.perform(put("/api/auth/me/username")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUsernameRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Missing or invalid Authorization header"));

        verify(authService, never()).updateUsername(anyLong(), anyString());
    }

    @Test
    @DisplayName("PUT /api/auth/me/username - Validation Error - Blank Username")
    void testUpdateUsername_BlankUsername() throws Exception {
        // given
        updateUsernameRequest.setNewUsername("");

        // when & then
        mockMvc.perform(put("/api/auth/me/username")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUsernameRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newUsername").exists());

        verify(authService, never()).updateUsername(anyLong(), anyString());
    }

    @Test
    @DisplayName("PUT /api/auth/me/username - Username Already Taken")
    void testUpdateUsername_AlreadyTaken() throws Exception {
        // given
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doThrow(new UserAlreadyExistsException("Username already taken"))
                .when(authService).updateUsername(anyLong(), anyString());

        // when & then
        mockMvc.perform(put("/api/auth/me/username")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUsernameRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));

        verify(authService, times(1)).updateUsername(1L, "updatedusername");
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("PUT /api/auth/me/password - Success")
    void testChangePassword_Success() throws Exception {
        // given
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doNothing().when(authService).changePassword(anyLong(), any(ChangePasswordRequest.class));

        // when & then
        mockMvc.perform(put("/api/auth/me/password")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("password changed successfully"));

        verify(jwtUtil, times(1)).extractUserId(anyString());
        verify(authService, times(1)).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/auth/me/password - Missing Authorization Header")
    void testChangePassword_MissingAuthHeader() throws Exception {
        // when & then
        mockMvc.perform(put("/api/auth/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Missing or invalid Authorization header"));

        verify(authService, never()).changePassword(anyLong(), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/auth/me/password - Validation Error - Blank Old Password")
    void testChangePassword_BlankOldPassword() throws Exception {
        // given
        changePasswordRequest.setOldPassword("");

        // when & then
        mockMvc.perform(put("/api/auth/me/password")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.oldPassword").exists());

        verify(authService, never()).changePassword(anyLong(), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/auth/me/password - Wrong Old Password")
    void testChangePassword_WrongOldPassword() throws Exception {
        // given
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doThrow(new RuntimeException("old password doesn't match"))
                .when(authService).changePassword(anyLong(), any(ChangePasswordRequest.class));

        // when & then
        mockMvc.perform(put("/api/auth/me/password")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }
}