package com.trademaster.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Service API Key Authentication Filter for Auth Service
 *
 * Provides authentication for service-to-service communication using API keys.
 * This filter runs before JWT authentication and handles internal service calls.
 *
 * Security Features:
 * - API key validation for internal services
 * - Request path filtering (only /internal/*)
 * - Audit logging for service authentication
 * - Fail-safe authentication bypass for health checks
 *
 * Integration Pattern:
 * - External access uses SecurityFacade + SecurityMediator (Zero Trust)
 * - Internal service-to-service uses this API key filter (lightweight)
 * - Service boundary security with proper role assignment
 *
 * NOTE: This filter is registered in SecurityConfig.filterChain() AND as a Spring @Component.
 * The @Component is required for dependency injection of @Value properties.
 * Do NOT add @Order annotation as that causes the filter to register twice in the filter chain.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String KONG_CONSUMER_CUSTOM_ID_HEADER = "X-Consumer-Custom-ID";
    private static final String INTERNAL_API_PATH = "/internal/";

    @Value("${trademaster.security.service.api-key:pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q}")
    private String masterServiceApiKey;

    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;

    public ServiceApiKeyFilter() {
        log.info("===============================================");
        log.info("ServiceApiKeyFilter CONSTRUCTOR CALLED!");
        log.info("===============================================");
    }

    /**
     * Determine if this filter should NOT be applied to the given request
     *
     * ✅ FUNCTIONAL PROGRAMMING: Using Optional and functional composition
     *
     * @param request the HTTP request
     * @return true if filter should be skipped, false if filter should be applied
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String servletPath = request.getServletPath();

        // Skip filter for public paths
        boolean isPublicPath = isPublicEndpoint(requestPath, servletPath);

        // Apply filter only to paths that START with /internal/ or /api/internal/
        // This prevents matching paths like /some/other/internal/path
        boolean isInternalPath = (requestPath != null && (requestPath.startsWith(INTERNAL_API_PATH) ||
                                                          requestPath.startsWith("/api" + INTERNAL_API_PATH))) ||
                                (servletPath != null && (servletPath.startsWith(INTERNAL_API_PATH) ||
                                                        servletPath.startsWith("/api" + INTERNAL_API_PATH)));

        // Skip public health endpoints (actuator endpoints only, not internal API health)
        boolean isPublicHealthEndpoint = requestPath != null && servletPath != null &&
                                       (requestPath.equals("/health") ||
                                        requestPath.equals("/actuator/health") ||
                                        servletPath.equals("/health") ||
                                        servletPath.equals("/actuator/health")) &&
                                       !requestPath.contains(INTERNAL_API_PATH);

        // Should NOT filter if it's a public path OR if it's not an internal path OR if it's a public health endpoint
        boolean shouldNotFilter = isPublicPath || !isInternalPath || isPublicHealthEndpoint;

        log.debug("ServiceApiKeyFilter.shouldNotFilter() - Path: {}, shouldNotFilter: {}",
                 requestPath, shouldNotFilter);

        return shouldNotFilter;
    }

    /**
     * Check if the path is a public endpoint that doesn't require API key authentication
     *
     * ✅ FUNCTIONAL PROGRAMMING: Using Stream API and pattern matching
     */
    private boolean isPublicEndpoint(String requestPath, String servletPath) {
        List<String> publicPatterns = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs"
        );

        return publicPatterns.stream()
            .anyMatch(pattern -> requestPath.startsWith(pattern) || servletPath.startsWith(pattern));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {

        log.info("ServiceApiKeyFilter.doFilterInternal() INVOKED!");

        String requestPath = httpRequest.getRequestURI();

        // Log all path information for debugging
        log.info("ServiceApiKeyFilter - Request URI: {}", requestPath);

        // Skip authentication if disabled (for local development)
        if (!serviceAuthEnabled) {
            log.warn("Service authentication is DISABLED - allowing internal API access");
            setServiceAuthentication("development-service");
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        String kongConsumerId = httpRequest.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = httpRequest.getHeader(KONG_CONSUMER_USERNAME_HEADER);
        String kongConsumerCustomId = httpRequest.getHeader(KONG_CONSUMER_CUSTOM_ID_HEADER);

        // Debug: Log all headers starting with X-
        log.info("ServiceApiKeyFilter Debug - All X- headers:");
        java.util.Collections.list(httpRequest.getHeaderNames()).stream()
            .filter(name -> name.toLowerCase().startsWith("x-"))
            .forEach(name -> log.info("  {}: {}", name, httpRequest.getHeader(name)));

        log.info("ServiceApiKeyFilter: API Key: {}, Kong Consumer ID: {}, Kong Consumer Username: {}, Custom ID: {}",
                 apiKey != null ? "Present" : "Missing",
                 kongConsumerId != null ? "Present" : "Missing",
                 kongConsumerUsername != null ? kongConsumerUsername : "Missing",
                 kongConsumerCustomId != null ? kongConsumerCustomId : "Missing");

        // If Kong consumer headers are present, Kong has already validated the API key
        if (StringUtils.hasText(kongConsumerId) && StringUtils.hasText(kongConsumerUsername)) {
            log.info("ServiceApiKeyFilter: Kong validated consumer '{}' (ID: {}), granting SERVICE access",
                     kongConsumerUsername, kongConsumerId);
            setServiceAuthentication(kongConsumerUsername);
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        // If no Kong headers, check if we have fallback validation enabled
        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing API key and no Kong consumer headers for internal service request: {} from {}",
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Missing service authentication");
            return;
        }

        log.warn("ServiceApiKeyFilter: No Kong consumer headers, falling back to direct validation");

        // Fallback: validate API key directly (for direct service calls)
        if (!isValidServiceApiKey(apiKey, "direct-service")) {
            log.error("Invalid API key for direct service request: {} from {}",
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Invalid service credentials");
            return;
        }

        // Set service authentication in security context
        setServiceAuthentication("direct-service");

        log.info("Service authentication successful: direct-service accessing {}", requestPath);

        chain.doFilter(httpRequest, httpResponse);
    }


    /**
     * Validate service API key
     */
    private boolean isValidServiceApiKey(String apiKey, String serviceId) {
        // For now, use master API key for all services
        // In production, you might have service-specific keys stored in database/vault
        if (!StringUtils.hasText(masterServiceApiKey)) {
            log.error("Master service API key not configured");
            return false;
        }

        // Simple validation - in production, use more sophisticated validation
        boolean isValid = masterServiceApiKey.equals(apiKey) && isKnownService(serviceId);

        if (!isValid) {
            log.error("API key validation failed for service: {}", serviceId);
        }

        return isValid;
    }

    /**
     * Check if service ID is in our known services list
     */
    private boolean isKnownService(String serviceId) {
        return List.of(
            "trading-service",
            "broker-auth-service",
            "portfolio-service",
            "notification-service",
            "risk-service",
            "user-service",
            "audit-service",
            "direct-service"
        ).contains(serviceId);
    }

    /**
     * Set service authentication in Spring Security context
     */
    private void setServiceAuthentication(String serviceId) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_SERVICE"),
            new SimpleGrantedAuthority("ROLE_INTERNAL")
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(serviceId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"SERVICE_AUTHENTICATION_FAILED\",\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis()));
    }
}