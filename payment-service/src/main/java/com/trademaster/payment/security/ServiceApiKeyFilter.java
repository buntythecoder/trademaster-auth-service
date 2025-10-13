package com.trademaster.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service API Key Authentication Filter
 * Implements internal service authentication using API keys per Golden Specification
 *
 * Compliance:
 * - Rule 6: Zero Trust Security - API key validation for internal services
 * - Rule 10: @Slf4j for structured logging
 * - Rule 15: Correlation IDs in all log entries
 * - Rule 16: Dynamic configuration with @Value
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";

    @Value("${trademaster.security.internal-api-key:${INTERNAL_API_KEY:changeme}}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String correlationId = request.getHeader("X-Correlation-ID");

        // Only validate API key for internal endpoints
        if (!requestPath.startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        extractApiKey(request)
            .filter(this::isValidApiKey)
            .ifPresentOrElse(
                apiKey -> {
                    authenticateService(apiKey, correlationId);
                    proceedWithFilter(request, response, filterChain);
                },
                () -> {
                    log.warn("Invalid API key for internal endpoint: {} | correlation_id={}",
                            requestPath, correlationId);
                    handleUnauthorized(response);
                }
            );
    }

    private Optional<String> extractApiKey(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(API_KEY_HEADER))
                       .filter(key -> !key.isBlank());
    }

    private boolean isValidApiKey(String apiKey) {
        return validApiKey.equals(apiKey);
    }

    private void authenticateService(String apiKey, String correlationId) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));
        var authentication = new UsernamePasswordAuthenticationToken(
            "internal-service",
            apiKey,
            authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Internal service authenticated | correlation_id={}", correlationId);
    }

    private void proceedWithFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.error("Filter chain execution failed", e);
            throw new RuntimeException("Filter processing failed", e);
        }
    }

    private void handleUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
        } catch (IOException e) {
            log.error("Failed to write unauthorized response", e);
        }
    }
}
