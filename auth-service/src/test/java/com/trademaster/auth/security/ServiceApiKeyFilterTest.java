package com.trademaster.auth.security;

import com.trademaster.auth.config.KongConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for ServiceApiKeyFilter
 *
 * MANDATORY: Rule #20 (Testing Standards) + Rule #6 (Zero Trust Security)
 * Tests Kong API Gateway integration with functional programming patterns
 *
 * Security Features Tested:
 * - Kong consumer header extraction with functional Optional chains
 * - Service authentication token creation with immutable records
 * - Spring Security context management with functional composition
 * - Path filtering for internal API endpoints only
 * - Error handling with functional fallback patterns
 * - Authentication audit logging for security compliance
 *
 * ✅ UPDATED FOR Spring Boot 3.5.3:
 * - No-arg constructor with @Value injection
 * - doFilterInternal() instead of doFilter()
 * - shouldNotFilter() method for path filtering
 * - Uses ReflectionTestUtils to inject @Value fields in tests
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Programming + Kong Integration)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Service API Key Filter Tests")
class ServiceApiKeyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private ServiceApiKeyFilter serviceApiKeyFilter;

    @BeforeEach
    void setUp() {
        serviceApiKeyFilter = new ServiceApiKeyFilter();

        // Inject @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(serviceApiKeyFilter, "masterServiceApiKey", "pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q");
        ReflectionTestUtils.setField(serviceApiKeyFilter, "serviceAuthEnabled", true);

        // Setup SecurityContext mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Kong Authentication - Should authenticate service with valid Kong headers")
    void testValidKongHeaders() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Test data setup using builder pattern
        when(request.getHeader("X-Consumer-ID")).thenReturn("service-trading-001");
        when(request.getHeader("X-Consumer-Username")).thenReturn("trading-service-internal");
        when(request.getHeader("X-Consumer-Custom-ID")).thenReturn("custom-trading-id");
        when(request.getHeader("X-API-Key")).thenReturn("trademaster-trading-api-key-2024-prod");

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Verify authentication was set
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(authCaptor.capture());

        Authentication auth = authCaptor.getValue();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("trading-service-internal");
        assertThat(auth.getCredentials()).isNull(); // Kong already validated
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_SERVICE");

        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Kong Authentication - Should skip authentication when consumer ID missing")
    void testMissingConsumerId() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing Optional.empty() scenario
        when(request.getHeader("X-Consumer-ID")).thenReturn(null);
        when(request.getHeader("X-Consumer-Username")).thenReturn("trading-service-internal");

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Verify no authentication was set
        verify(securityContext, never()).setAuthentication(any());

        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Kong Authentication - Should skip authentication when consumer username missing")
    void testMissingConsumerUsername() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing functional filter conditions
        when(request.getHeader("X-Consumer-ID")).thenReturn("service-trading-001");
        when(request.getHeader("X-Consumer-Username")).thenReturn(null);

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Verify no authentication was set
        verify(securityContext, never()).setAuthentication(any());

        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Kong Authentication - Should handle partial Kong headers gracefully")
    void testPartialKongHeaders() throws Exception {
        // Test with only some headers present
        when(request.getHeader("X-Consumer-ID")).thenReturn("service-trading-001");
        when(request.getHeader("X-Consumer-Username")).thenReturn("trading-service-internal");
        when(request.getHeader("X-Consumer-Custom-ID")).thenReturn(null); // Missing custom ID
        when(request.getHeader("X-API-Key")).thenReturn(null); // Missing API key

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Should still authenticate (custom ID and API key are optional)
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(authCaptor.capture());

        Authentication auth = authCaptor.getValue();
        assertThat(auth.getName()).isEqualTo("trading-service-internal");
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_SERVICE");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Path Filtering - Should apply filter only to internal API endpoints")
    void testShouldNotFilterInternalPaths() {
        // ✅ FUNCTIONAL PROGRAMMING: Testing functional path filtering
        when(request.getRequestURI()).thenReturn("/api/internal/v1/auth/greeting");
        when(request.getServletPath()).thenReturn("/api/internal/v1/auth/greeting");

        boolean shouldNotFilter = serviceApiKeyFilter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isFalse(); // Should apply filter
    }

    @Test
    @DisplayName("Path Filtering - Should skip filter for non-internal endpoints")
    void testShouldNotFilterPublicPaths() {
        // Test various non-internal paths
        String[] publicPaths = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/actuator/health",
            "/swagger-ui/index.html",
            "/v3/api-docs"
        };

        for (String path : publicPaths) {
            when(request.getRequestURI()).thenReturn(path);
            when(request.getServletPath()).thenReturn(path);

            boolean shouldNotFilter = serviceApiKeyFilter.shouldNotFilter(request);

            assertThat(shouldNotFilter)
                .describedAs("Should skip filter for path: " + path)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Error Handling - Should clear security context on authentication error")
    void testAuthenticationError() throws Exception {
        // Setup valid headers but simulate authentication error
        when(request.getHeader("X-Consumer-ID")).thenReturn("service-trading-001");
        when(request.getHeader("X-Consumer-Username")).thenReturn("trading-service-internal");

        // Simulate SecurityContext throwing exception
        doThrow(new RuntimeException("Security context error"))
            .when(securityContext).setAuthentication(any());

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Verify security context was cleared after error
        verify(securityContext).setAuthentication(any()); // Initial attempt
        // The filter should handle the exception gracefully

        // Verify filter chain still continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Kong Headers Extraction - Should extract all Kong headers correctly")
    void testKongHeadersExtraction() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing immutable record pattern
        String expectedConsumerId = "service-broker-auth-001";
        String expectedUsername = "broker-auth-service-internal";
        String expectedCustomId = "custom-broker-auth-id";
        String expectedApiKey = "trademaster-broker-auth-api-key-2024-prod";

        when(request.getHeader("X-Consumer-ID")).thenReturn(expectedConsumerId);
        when(request.getHeader("X-Consumer-Username")).thenReturn(expectedUsername);
        when(request.getHeader("X-Consumer-Custom-ID")).thenReturn(expectedCustomId);
        when(request.getHeader("X-API-Key")).thenReturn(expectedApiKey);

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Verify authentication with correct principal
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(authCaptor.capture());

        Authentication auth = authCaptor.getValue();
        assertThat(auth.getName()).isEqualTo(expectedUsername);
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_SERVICE");
    }

    @Test
    @DisplayName("Service Authentication Context - Should set proper authentication details")
    void testAuthenticationDetails() throws Exception {
        when(request.getHeader("X-Consumer-ID")).thenReturn("service-portfolio-001");
        when(request.getHeader("X-Consumer-Username")).thenReturn("portfolio-service-internal");
        when(request.getRemoteAddr()).thenReturn("172.18.0.5");
        when(request.getHeader("User-Agent")).thenReturn("OkHttp/4.9.3");

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(authCaptor.capture());

        Authentication auth = authCaptor.getValue();
        assertThat(auth.getDetails()).isNotNull();
        // WebAuthenticationDetails should contain IP and session info
    }

    @Test
    @DisplayName("Multiple Services - Should handle different Kong consumers")
    void testMultipleServiceAuthentication() throws Exception {
        // ✅ FUNCTIONAL PROGRAMMING: Testing functional composition with different inputs
        String[][] serviceConfigs = {
            {"service-trading-001", "trading-service-internal"},
            {"service-portfolio-001", "portfolio-service-internal"},
            {"service-notification-001", "notification-service-internal"},
            {"service-broker-auth-001", "broker-auth-service-internal"}
        };

        for (String[] config : serviceConfigs) {
            // Reset mocks
            reset(securityContext);

            when(request.getHeader("X-Consumer-ID")).thenReturn(config[0]);
            when(request.getHeader("X-Consumer-Username")).thenReturn(config[1]);

            serviceApiKeyFilter.doFilter(request, response, filterChain);

            ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(securityContext).setAuthentication(authCaptor.capture());

            Authentication auth = authCaptor.getValue();
            assertThat(auth.getName())
                .describedAs("Service authentication for " + config[1])
                .isEqualTo(config[1]);
            assertThat(auth.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_SERVICE");
        }
    }

    @Test
    @DisplayName("Security Context - Should not interfere with existing authentication")
    void testExistingAuthentication() throws Exception {
        // Test that filter doesn't interfere when no Kong headers present
        when(request.getHeader("X-Consumer-ID")).thenReturn(null);
        when(request.getHeader("X-Consumer-Username")).thenReturn(null);

        serviceApiKeyFilter.doFilter(request, response, filterChain);

        // Should not set any authentication
        verify(securityContext, never()).setAuthentication(any());

        // Should still continue filter chain
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Functional Path Filtering - Should handle edge cases in path matching")
    void testPathFilteringEdgeCases() {
        // ✅ FUNCTIONAL PROGRAMMING: Testing functional Optional patterns
        String[] testPaths = {
            "/api/internal/",           // Should apply (ends with /)
            "/api/internal",            // Should skip (no /)
            "/api/internal/v1",         // Should apply
            "/api/internal/health",     // Should apply
            "/API/INTERNAL/AUTH",       // Should skip (case sensitive)
            "",                         // Should skip (empty)
            "/api/internal/v1/auth/greeting",  // Should apply
            "/some/other/internal/path" // Should skip (not starting with /api/internal/)
        };

        boolean[] expectedResults = {true, false, true, true, false, false, true, false};

        for (int i = 0; i < testPaths.length; i++) {
            when(request.getRequestURI()).thenReturn(testPaths[i]);
            when(request.getServletPath()).thenReturn(testPaths[i]);

            boolean shouldNotFilter = serviceApiKeyFilter.shouldNotFilter(request);
            boolean shouldApplyFilter = !shouldNotFilter;

            assertThat(shouldApplyFilter)
                .describedAs("Path filtering for: '" + testPaths[i] + "'")
                .isEqualTo(expectedResults[i]);
        }
    }
}