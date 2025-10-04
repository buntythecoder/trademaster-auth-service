package com.trademaster.auth.filter;

import com.trademaster.auth.constants.AuthConstants;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Correlation ID Filter - Automatic correlation ID injection
 *
 * MANDATORY: Rule #15 - Structured Logging with Correlation IDs
 *
 * Features:
 * - Automatic correlation ID generation for all requests
 * - Header propagation for distributed tracing
 * - MDC cleanup for Virtual Threads
 * - Request/response correlation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Order(1) // Execute first in filter chain
@Slf4j
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID using functional approach
            String correlationId = Optional.ofNullable(httpRequest.getHeader(AuthConstants.CORRELATION_ID_HEADER))
                    .filter(id -> !id.trim().isEmpty())
                    .orElseGet(() -> UUID.randomUUID().toString());

            // Set correlation ID in MDC for current Virtual Thread
            MDC.put(AuthConstants.CORRELATION_ID, correlationId);

            // Add correlation ID to response headers for client tracking
            httpResponse.setHeader(AuthConstants.CORRELATION_ID_HEADER, correlationId);

            // Add request context to MDC
            setRequestContext(httpRequest);

            log.debug("Request started with correlation ID: {}", correlationId);

            // Continue with filter chain
            chain.doFilter(request, response);

        } finally {
            // Critical: Clear MDC for Virtual Thread reuse
            String correlationId = MDC.get(AuthConstants.CORRELATION_ID);
            log.debug("Request completed with correlation ID: {}", correlationId);
            MDC.clear();
        }
    }

    /**
     * Set request context in MDC using functional patterns
     */
    private void setRequestContext(HttpServletRequest request) {
        // Use Optional patterns instead of if-else for Rule #3 compliance
        Optional.ofNullable(request.getRemoteAddr())
                .filter(ip -> !ip.trim().isEmpty())
                .ifPresent(ip -> MDC.put(AuthConstants.IP_ADDRESS_FIELD, ip));

        Optional.ofNullable(request.getHeader("User-Agent"))
                .filter(ua -> !ua.trim().isEmpty())
                .map(this::sanitizeUserAgent)
                .ifPresent(ua -> MDC.put(AuthConstants.USER_AGENT_FIELD, ua));

        Optional.ofNullable(request.getRequestURI())
                .ifPresent(uri -> MDC.put("request_uri", uri));

        Optional.ofNullable(request.getMethod())
                .ifPresent(method -> MDC.put("request_method", method));
    }

    /**
     * Sanitize user agent using functional approach
     */
    private String sanitizeUserAgent(String userAgent) {
        return Optional.ofNullable(userAgent)
                .filter(ua -> ua.length() <= AuthConstants.MAX_USER_AGENT_LENGTH)
                .orElseGet(() -> userAgent.substring(0, AuthConstants.MAX_USER_AGENT_LENGTH));
    }
}