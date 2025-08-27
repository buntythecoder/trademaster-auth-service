package com.trademaster.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * JWT Authentication Entry Point for handling authentication failures
 * 
 * This class handles cases where:
 * - No JWT token is provided
 * - JWT token is invalid or expired
 * - User is not authenticated when accessing protected resources
 * 
 * Returns standardized error responses for authentication failures.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("Unauthorized access attempt to {} from IP {}: {}", 
                request.getRequestURI(), 
                getClientIpAddress(request), 
                authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "Authentication required to access this resource");
        errorDetails.put("path", request.getRequestURI());
        
        // Add more specific error information based on the exception
        Map<String, Map<String, String>> errorMappings = Map.of(
            "TOKEN_EXPIRED", Map.of(
                "code", "TOKEN_EXPIRED",
                "description", "JWT token has expired. Please refresh your token or login again."
            ),
            "TOKEN_INVALID", Map.of(
                "code", "TOKEN_INVALID", 
                "description", "JWT token is invalid or malformed."
            ),
            "AUTHENTICATION_REQUIRED", Map.of(
                "code", "AUTHENTICATION_REQUIRED",
                "description", "Valid authentication credentials are required."
            )
        );
        
        String message = Optional.ofNullable(authException.getMessage()).orElse("");
        String errorType = Stream.of("TOKEN_EXPIRED", "TOKEN_INVALID", "AUTHENTICATION_REQUIRED")
            .filter(type -> (type.equals("TOKEN_EXPIRED") && message.contains("expired")) ||
                           (type.equals("TOKEN_INVALID") && message.contains("invalid")) ||
                           (type.equals("AUTHENTICATION_REQUIRED")))
            .findFirst()
            .orElse("AUTHENTICATION_REQUIRED");
            
        errorMappings.get(errorType).forEach(errorDetails::put);

        // Security headers
        response.setHeader("WWW-Authenticate", "Bearer realm=\"TradeMaster API\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }

    /**
     * Get client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(xff -> !xff.isEmpty())
            .map(xff -> xff.split(",")[0].trim())
            .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .filter(xri -> !xri.isEmpty())
                .orElse(request.getRemoteAddr()));
    }
}