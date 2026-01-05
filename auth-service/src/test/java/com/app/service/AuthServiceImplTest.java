package com.app.service;

import com.app.dto.*;
import com.app.entity.User;
import com.app.enums.Role;
import com.app.exception.InvalidCredentialsException;
import com.app.exception.UserAlreadyExistsException;
import com.app.exception.UserNotFoundException;
import com.app.repository.UserRepository;
import com.app.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private GuestRegisterRequest guestRegisterRequest;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");
        testUser.setRole(Role.GUEST);
        testUser.setFullName("Test User");
        testUser.setPhoneNumber("1234567890");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

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

        // setup register request (for admin creating staff)
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("manager1");
        registerRequest.setEmail("manager@example.com");
        registerRequest.setPassword("Manager123");
        registerRequest.setRole(Role.MANAGER);
        registerRequest.setHotelId(1L);
        registerRequest.setFullName("Hotel Manager");
        registerRequest.setPhoneNumber("1111111111");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register guest successfully")
    void testRegister_Success() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // when
        AuthResponse response = authService.register(guestRegisterRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getRole()).isEqualTo(Role.GUEST);

        // verify interactions
        verify(userRepository).existsByUsername("newguest");
        verify(userRepository).existsByEmail("newguest@example.com");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(any(User.class));

        // verify saved user has GUEST role (forced)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(Role.GUEST);
        assertThat(savedUser.getHotelId()).isNull();
        assertThat(savedUser.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when username already exists during registration")
    void testRegister_DuplicateUsername() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(guestRegisterRequest)
        );

        assertThat(exception.getMessage()).isEqualTo("username already exists");
        verify(userRepository).existsByUsername("newguest");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists during registration")
    void testRegister_DuplicateEmail() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(guestRegisterRequest)
        );

        assertThat(exception.getMessage()).isEqualTo("email already exists");
        verify(userRepository).existsByUsername("newguest");
        verify(userRepository).existsByEmail("newguest@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        // given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // when
        AuthResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("Password123", "encodedPassword123");
        verify(jwtUtil).generateToken(testUser);
    }

    @Test
    @DisplayName("Should throw exception when username not found during login")
    void testLogin_UsernameNotFound() {
        // given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // when & then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertThat(exception.getMessage()).isEqualTo("invalid username or password");
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLogin_WrongPassword() {
        // given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // when & then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertThat(exception.getMessage()).isEqualTo("invalid username or password");
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("Password123", "encodedPassword123");
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user account is inactive")
    void testLogin_InactiveUser() {
        // given
        testUser.setActive(false);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        // when & then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertThat(exception.getMessage()).isEqualTo("user account is inactive");
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // ==================== CREATE USER TESTS ====================

    @Test
    @DisplayName("Should create MANAGER user successfully by admin")
    void testCreateUser_ManagerSuccess() {
        // given
        User managerUser = new User();
        managerUser.setId(2L);
        managerUser.setUsername("manager1");
        managerUser.setRole(Role.MANAGER);
        managerUser.setHotelId(1L);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(managerUser);

        // when
        UserResponse response = authService.createUser(registerRequest, "ADMIN");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.MANAGER);
        assertThat(response.getHotelId()).isEqualTo(1L);

        verify(userRepository).existsByUsername("manager1");
        verify(userRepository).existsByEmail("manager@example.com");
        verify(passwordEncoder).encode("Manager123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create RECEPTIONIST user successfully by admin")
    void testCreateUser_ReceptionistSuccess() {
        // given
        registerRequest.setRole(Role.RECEPTIONIST);
        User receptionistUser = new User();
        receptionistUser.setId(3L);
        receptionistUser.setRole(Role.RECEPTIONIST);
        receptionistUser.setHotelId(1L);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(receptionistUser);

        // when
        UserResponse response = authService.createUser(registerRequest, "ADMIN");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.RECEPTIONIST);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to create user")
    void testCreateUser_NonAdminCaller() {
        // when & then
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> authService.createUser(registerRequest, "GUEST")
        );

        assertThat(exception.getMessage()).isEqualTo("only admin can create users");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to create ADMIN role")
    void testCreateUser_AdminRoleNotAllowed() {
        // given
        registerRequest.setRole(Role.ADMIN);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.createUser(registerRequest, "ADMIN")
        );

        assertThat(exception.getMessage()).isEqualTo("can only create MANAGER or RECEPTIONIST roles");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to create GUEST role via admin endpoint")
    void testCreateUser_GuestRoleNotAllowed() {
        // given
        registerRequest.setRole(Role.GUEST);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.createUser(registerRequest, "ADMIN")
        );

        assertThat(exception.getMessage()).isEqualTo("can only create MANAGER or RECEPTIONIST roles");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when hotelId is missing for staff user")
    void testCreateUser_MissingHotelId() {
        // given
        registerRequest.setHotelId(null);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.createUser(registerRequest, "ADMIN")
        );

        assertThat(exception.getMessage()).isEqualTo("hotel id is required for manager and receptionist roles");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists during user creation")
    void testCreateUser_DuplicateUsername() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.createUser(registerRequest, "ADMIN")
        );

        assertThat(exception.getMessage()).isEqualTo("username already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists during user creation")
    void testCreateUser_DuplicateEmail() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.createUser(registerRequest, "ADMIN")
        );

        assertThat(exception.getMessage()).isEqualTo("email already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== UPDATE USERNAME TESTS ====================

    @Test
    @DisplayName("Should update username successfully")
    void testUpdateUsername_Success() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.updateUsername(1L, "newusername");

        // then
        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsernameAndIdNot("newusername", 1L);
        verify(userRepository).save(testUser);
        assertThat(testUser.getUsername()).isEqualTo("newusername");
    }

    @Test
    @DisplayName("Should throw exception when user not found during username update")
    void testUpdateUsername_UserNotFound() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authService.updateUsername(999L, "newusername")
        );

        assertThat(exception.getMessage()).isEqualTo("User not found");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when new username already taken")
    void testUpdateUsername_UsernameAlreadyTaken() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.updateUsername(1L, "existingusername")
        );

        assertThat(exception.getMessage()).isEqualTo("Username already taken");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("Should change password successfully")
    void testChangePassword_Success() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123", "NewPassword123");
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.changePassword(1L, request);

        // then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("OldPassword123", "encodedPassword123");
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found during password change")
    void testChangePassword_UserNotFound() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123", "NewPassword123");
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.changePassword(999L, request)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when old password is incorrect")
    void testChangePassword_WrongOldPassword() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("WrongOldPassword", "NewPassword123");
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.changePassword(1L, request)
        );

        assertThat(exception.getMessage()).isEqualTo("old password doesn't match");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}