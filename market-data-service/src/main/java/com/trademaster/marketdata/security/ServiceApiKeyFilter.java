package com.trademaster.marketdata.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Service API Key Filter for internal service-to-service authentication
 *
 * This filter implements a two-tier authentication strategy:
 * 1. Primary: Kong Gateway consumer headers (X-Consumer-ID, X-Consumer-Username)
 * 2. Fallback: Direct X-API-Key header validation
 *
 * Complies with Golden Specification Section 2.2: Service API Key Filter Implementation
 * Zero Trust Architecture - validates ALL internal service requests
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Order(1) // Run before JWT filter
@Slf4j
public class ServiceApiKeyFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String INTERNAL_API_PATH = "/api/internal/";

    @Value("${trademaster.security.service.api-key:}")
    private String fallbackServiceApiKey;

    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Only process internal API requests
        if (!requestPath.startsWith(INTERNAL_API_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        // If service authentication is disabled (dev/test only)
        if (!serviceAuthEnabled) {
            log.warn("Service authentication is DISABLED for: {} - NOT RECOMMENDED FOR PRODUCTION", requestPath);
            setServiceAuthentication("dev-bypass");
            chain.doFilter(request, response);
            return;
        }

        // Primary: Check for Kong consumer headers (Kong validated API key)
        String kongConsumerId = httpRequest.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = httpRequest.getHeader(KONG_CONSUMER_USERNAME_HEADER);

        if (StringUtils.hasText(kongConsumerId) && StringUtils.hasText(kongConsumerUsername)) {
            log.info("Kong validated consumer '{}' (ID: {}), granting SERVICE access to: {}",
                    kongConsumerUsername, kongConsumerId, requestPath);
            setServiceAuthentication(kongConsumerUsername);
            chain.doFilter(request, response);
            return;
        }

        // Fallback: Direct API key validation
        String apiKey = httpRequest.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing Kong consumer headers AND X-API-Key header for: {}", requestPath);
            sendUnauthorizedResponse(httpResponse, "Missing service API key or Kong consumer headers");
            return;
        }

        // Validate fallback API key
        if (!StringUtils.hasText(fallbackServiceApiKey)) {
            log.error("No fallback API key configured - rejecting direct service call to: {}", requestPath);
            sendUnauthorizedResponse(httpResponse, "Service authentication not configured");
            return;
        }

        if (!fallbackServiceApiKey.equals(apiKey)) {
            log.error("Invalid API key for direct service request to: {}", requestPath);
            sendUnauthorizedResponse(httpResponse, "Invalid service API key");
            return;
        }

        setServiceAuthentication("direct-service-call");
        log.info("Direct API key authentication successful for: {}", requestPath);

        chain.doFilter(request, response);
    }

    /**
     * Set service authentication in Spring Security context
     * Grants ROLE_SERVICE and ROLE_INTERNAL authorities
     */
    private void setServiceAuthentication(String serviceId) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("ROLE_INTERNAL")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(serviceId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Service authentication set: serviceId={}, authorities={}", serviceId, authorities);
    }

    /**
     * Send 401 Unauthorized response with JSON error details
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\":\"SERVICE_AUTHENTICATION_FAILED\",\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis()));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("ServiceApiKeyFilter initialized - protecting paths: {}", INTERNAL_API_PATH);
        log.info("Service authentication enabled: {}", serviceAuthEnabled);
    }

    @Override
    public void destroy() {
        log.info("ServiceApiKeyFilter destroyed");
    }
}
