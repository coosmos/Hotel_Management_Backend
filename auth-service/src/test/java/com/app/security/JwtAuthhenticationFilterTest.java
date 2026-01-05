package com.app.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // clear security context before each test
        SecurityContextHolder.clearContext();

        // setup user details
        userDetails = new User(
                "testuser",
                "encodedPassword",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_GUEST"))
        );
    }

    // ==================== VALID TOKEN TESTS ====================

    @Test
    @DisplayName("Should authenticate user with valid JWT token")
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, "testuser")).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtUtil).validateToken(token, "testuser");
        verify(filterChain).doFilter(request, response);

        // verify authentication is set in security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should set correct authorities in authentication")
    void testDoFilterInternal_CorrectAuthorities() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, "testuser")).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_GUEST");
    }

    // ==================== MISSING AUTHORIZATION HEADER TESTS ====================

    @Test
    @DisplayName("Should proceed without authentication when Authorization header is missing")
    void testDoFilterInternal_MissingAuthorizationHeader() throws ServletException, IOException {
        // given - no Authorization header

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should proceed without authentication when Authorization header doesn't start with Bearer")
    void testDoFilterInternal_InvalidAuthorizationHeaderFormat() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Basic some-basic-auth-token");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should proceed without authentication when Authorization header is empty Bearer")
    void testDoFilterInternal_EmptyBearerToken() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    // ==================== INVALID TOKEN TESTS ====================

    @Test
    @DisplayName("Should not authenticate when token validation fails")
    void testDoFilterInternal_TokenValidationFails() throws ServletException, IOException {
        // given
        String token = "invalid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, "testuser")).thenReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtUtil).validateToken(token, "testuser");
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set when validation fails
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should handle malformed JWT token gracefully")
    void testDoFilterInternal_MalformedToken() throws ServletException, IOException {
        // given
        String token = "malformed-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenThrow(new MalformedJwtException("JWT is malformed"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should handle expired JWT token gracefully")
    void testDoFilterInternal_ExpiredToken() throws ServletException, IOException {
        // given
        String token = "expired-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should handle generic exception during token processing")
    void testDoFilterInternal_GenericException() throws ServletException, IOException {
        // given
        String token = "problematic-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("Unexpected error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    // ==================== ALREADY AUTHENTICATED TESTS ====================

    @Test
    @DisplayName("Should not re-authenticate when user is already authenticated")
    void testDoFilterInternal_AlreadyAuthenticated() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        // pre-set authentication in security context
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtUtil.extractUsername(token)).thenReturn("testuser");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
        verify(filterChain).doFilter(request, response);

        // verify existing authentication is preserved
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isEqualTo(existingAuth);
    }

    // ==================== NULL USERNAME TESTS ====================

    @Test
    @DisplayName("Should not authenticate when extracted username is null")
    void testDoFilterInternal_NullUsername() throws ServletException, IOException {
        // given
        String token = "token-with-null-username";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    // ==================== USER NOT FOUND TESTS ====================

    @Test
    @DisplayName("Should handle user not found exception gracefully")
    void testDoFilterInternal_UserNotFound() throws ServletException, IOException {
        // given
        String token = "valid-token-but-user-not-exists";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn("nonexistentuser");
        when(userDetailsService.loadUserByUsername("nonexistentuser"))
                .thenThrow(new RuntimeException("User not found"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("nonexistentuser");
        verify(filterChain).doFilter(request, response);

        // verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    // ==================== AUTHENTICATION DETAILS TESTS ====================

    @Test
    @DisplayName("Should set authentication details with request information")
    void testDoFilterInternal_AuthenticationDetails() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);
        request.setRemoteAddr("192.168.1.1");
        request.setRequestURI("/api/test");

        when(jwtUtil.extractUsername(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, "testuser")).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isNotNull();
    }

    // ==================== FILTER CHAIN CONTINUATION TESTS ====================

    @Test
    @DisplayName("Should always continue filter chain regardless of authentication success")
    void testDoFilterInternal_AlwaysContinuesFilterChain() throws ServletException, IOException {
        // given - valid token
        String validToken = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtUtil.extractUsername(validToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(validToken, "testuser")).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);

        // reset for next test
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        // given - invalid token
        String invalidToken = "invalid-token";
        request.addHeader("Authorization", "Bearer " + invalidToken);
        when(jwtUtil.extractUsername(invalidToken)).thenThrow(new MalformedJwtException("Invalid"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    // ==================== MULTIPLE ROLES TESTS ====================

    @Test
    @DisplayName("Should handle user with multiple authorities")
    void testDoFilterInternal_MultipleAuthorities() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails adminUser = new User(
                "admin",
                "password",
                java.util.Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
        );

        when(jwtUtil.extractUsername(token)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(adminUser);
        when(jwtUtil.validateToken(token, "admin")).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).hasSize(2);
    }
}