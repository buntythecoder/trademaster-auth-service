package com.trademaster.auth.controller;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Unit Tests for InternalAuthController
 *
 * MANDATORY: Rule #20 (Testing Standards) - >80% coverage with functional test builders
 * Tests all internal API endpoints with Kong service authentication
 *
 * Features Tested:
 * - Health check endpoint (no auth required)
 * - Service greeting with API key authentication
 * - User validation for service-to-service calls
 * - User profile retrieval with Optional chains
 * - JWT token validation with functional patterns
 * - Statistics collection with real service integration
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Kong Integration + Functional Programming)
 */
@WebMvcTest(InternalAuthController.class)
@DisplayName("Internal Auth Controller Tests")
class InternalAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private SessionManagementService sessionService;

    @MockBean
    private SecurityAuditService auditService;

    @MockBean
    private AuthenticationService authenticationService;

    // ✅ FUNCTIONAL PROGRAMMING: Test data builders using functional patterns
    private User createTestUser() {
        return User.builder()
            .id("test-user-123")
            .email("test@trademaster.com")
            .enabled(true)
            .accountExpired(false)
            .mfaEnabled(true)
            .lastLoginDate(LocalDateTime.now().minusHours(2))
            .roles(Set.of("ROLE_USER", "ROLE_TRADER"))
            .authorities(List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_TRADER")
            ))
            .build();
    }

    private UserDetails createTestUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
            .username("test@trademaster.com")
            .password("encoded-password")
            .authorities("ROLE_USER", "ROLE_TRADER")
            .build();
    }

    @BeforeEach
    void setupMocks() {
        // Setup default mock responses for statistics
        when(userService.getActiveUsersCount()).thenReturn(1250L);
        when(userService.getTodayLoginCount()).thenReturn(3450L);
        when(userService.getMfaEnabledUsersCount()).thenReturn(892L);
        when(sessionService.getTrustedDevicesCount()).thenReturn(2150L);
        when(auditService.getTodaySecurityAlertsCount()).thenReturn(3L);

        when(authenticationService.isJwtOperational()).thenReturn(true);
        when(userService.isMfaOperational()).thenReturn(true);
        when(sessionService.isDeviceManagementOperational()).thenReturn(true);
        when(auditService.isAuditOperational()).thenReturn(true);

        when(sessionService.getAverageResponseTime()).thenReturn(45.2);
        when(auditService.getSuccessRate()).thenReturn(99.5);
    }

    @Test
    @DisplayName("Health Check - Should return service status without authentication")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/internal/v1/auth/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.service").value("auth-service"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.internal_api").value("available"))
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.authentication").value("service-api-key-enabled"))
            .andExpect(jsonPath("$.capabilities").isArray())
            .andExpect(jsonPath("$.capabilities[0]").value("user-authentication"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("Service Greeting - Should authenticate with SERVICE role")
    void testServiceGreeting() throws Exception {
        mockMvc.perform(get("/api/internal/v1/auth/greeting"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Hello from Auth Service Internal API!"))
            .andExpect(jsonPath("$.service").value("auth-service"))
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.role").value("SERVICE"))
            .andExpect(jsonPath("$.kong_integration").value("working"));
    }

    @Test
    @DisplayName("Service Greeting - Should reject without SERVICE role")
    void testServiceGreetingUnauthorized() throws Exception {
        mockMvc.perform(get("/api/internal/v1/auth/greeting"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("Internal Status - Should return authenticated service status")
    void testInternalStatus() throws Exception {
        mockMvc.perform(get("/api/internal/v1/auth/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("auth-service"))
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.message").value("Auth service is running and authenticated"))
            .andExpect(jsonPath("$.features").isArray())
            .andExpect(jsonPath("$.features[0]").value("JWT"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("User Validation - Should validate active user with functional Optional chain")
    void testValidateUserSuccess() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Using Optional for test setup
        User testUser = createTestUser();
        when(userService.findByUserId("test-user-123")).thenReturn(Optional.of(testUser));
        when(sessionService.isDeviceTrusted("test-user-123")).thenReturn(true);

        mockMvc.perform(get("/api/internal/v1/auth/users/test-user-123/validate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("test-user-123"))
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.mfaEnabled").value(true))
            .andExpect(jsonPath("$.deviceTrusted").value(true));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("User Validation - Should handle user not found with Optional chain")
    void testValidateUserNotFound() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing Optional.empty() scenario
        when(userService.findByUserId("nonexistent-user")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/v1/auth/users/nonexistent-user/validate"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("User Profile - Should return user profile with functional data mapping")
    void testGetUserProfile() throws Exception {
        User testUser = createTestUser();
        when(userService.findByUserId("test-user-123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/internal/v1/auth/users/test-user-123/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("test-user-123"))
            .andExpect(jsonPath("$.email").value("test@trademaster.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("JWT Token Validation - Should validate token with functional Optional chain")
    void testValidateTokenSuccess() throws Exception {
        UserDetails userDetails = createTestUserDetails();
        when(authenticationService.validateJwtToken("valid-jwt-token"))
            .thenReturn(Optional.of(userDetails));
        when(authenticationService.extractExpirationFromToken("valid-jwt-token"))
            .thenReturn(LocalDateTime.now().plusHours(1));

        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("token", "valid-jwt-token")
        );

        mockMvc.perform(post("/api/internal/v1/auth/tokens/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.userId").value("test@trademaster.com"))
            .andExpect(jsonPath("$.email").value("test@trademaster.com"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.issuer").value("trademaster-auth-service"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("JWT Token Validation - Should handle invalid token with functional fallback")
    void testValidateTokenInvalid() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing Optional.empty() for invalid token
        when(authenticationService.validateJwtToken("invalid-token"))
            .thenReturn(Optional.empty());

        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("token", "invalid-token")
        );

        mockMvc.perform(post("/api/internal/v1/auth/tokens/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.reason").value("Invalid token"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("Statistics - Should return service statistics with functional data collection")
    void testGetStatistics() throws Exception {
        mockMvc.perform(get("/api/internal/v1/auth/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active_users").value(1250))
            .andExpect(jsonPath("$.total_logins_today").value(3450))
            .andExpect(jsonPath("$.mfa_enabled_users").value(892))
            .andExpect(jsonPath("$.trusted_devices").value(2150))
            .andExpect(jsonPath("$.security_alerts").value(3))
            .andExpect(jsonPath("$.features.JWT").value("OPERATIONAL"))
            .andExpect(jsonPath("$.features.MFA").value("OPERATIONAL"))
            .andExpect(jsonPath("$.service_health").value("UP"))
            .andExpect(jsonPath("$.performance_metrics.avg_response_time_ms").value(45.2))
            .andExpect(jsonPath("$.performance_metrics.success_rate_percent").value(99.5));
    }

    @Test
    @DisplayName("All Internal Endpoints - Should require SERVICE role authentication")
    void testAuthenticationRequired() throws Exception {
        // Test all protected endpoints require authentication
        mockMvc.perform(get("/api/internal/v1/auth/greeting"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/v1/auth/status"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/v1/auth/users/test/validate"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/v1/auth/users/test/profile"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/internal/v1/auth/tokens/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/v1/auth/statistics"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    @DisplayName("Service Statistics - Should handle service degradation scenarios")
    void testStatisticsServiceDegradation() throws Exception {
        // Test degraded service scenarios
        when(authenticationService.isJwtOperational()).thenReturn(false);
        when(userService.isMfaOperational()).thenReturn(false);

        mockMvc.perform(get("/api/internal/v1/auth/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.features.JWT").value("DEGRADED"))
            .andExpect(jsonPath("$.features.MFA").value("DEGRADED"))
            .andExpect(jsonPath("$.service_health").value("UP"));
    }
}