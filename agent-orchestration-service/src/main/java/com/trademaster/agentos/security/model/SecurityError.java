package com.trademaster.agentos.security.model;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable security error representation for Zero Trust security violations.
 */
public record SecurityError(
    ErrorType type,
    String message,
    String details,
    String correlationId,
    Instant timestamp,
    Map<String, Object> metadata
) {
    public SecurityError {
        timestamp = timestamp != null ? timestamp : Instant.now();
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }
    
    /**
     * Security error types
     */
    public enum ErrorType {
        AUTHENTICATION_FAILED("Authentication failed"),
        AUTHORIZATION_DENIED("Authorization denied"),
        TOKEN_EXPIRED("Security token expired"),
        TOKEN_INVALID("Security token invalid"),
        INSUFFICIENT_PRIVILEGES("Insufficient privileges"),
        RISK_THRESHOLD_EXCEEDED("Risk threshold exceeded"),
        RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
        INVALID_SESSION("Invalid or expired session"),
        SECURITY_VIOLATION("Security policy violation"),
        INPUT_VALIDATION_FAILED("Input validation failed"),
        BLOCKED_IP("IP address blocked"),
        SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
        MFA_REQUIRED("Multi-factor authentication required"),
        ACCESS_DENIED("Access denied by security policy");
        
        private final String defaultMessage;
        
        ErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
    
    // Factory methods for common errors
    public static SecurityError authenticationFailed(String details, String correlationId) {
        return new SecurityError(
            ErrorType.AUTHENTICATION_FAILED,
            ErrorType.AUTHENTICATION_FAILED.getDefaultMessage(),
            details,
            correlationId,
            Instant.now(),
            Map.of()
        );
    }
    
    public static SecurityError authorizationDenied(String resource, String correlationId) {
        return new SecurityError(
            ErrorType.AUTHORIZATION_DENIED,
            ErrorType.AUTHORIZATION_DENIED.getDefaultMessage(),
            "Access denied to resource: " + resource,
            correlationId,
            Instant.now(),
            Map.of("resource", resource)
        );
    }
    
    public static SecurityError tokenExpired(String correlationId) {
        return new SecurityError(
            ErrorType.TOKEN_EXPIRED,
            ErrorType.TOKEN_EXPIRED.getDefaultMessage(),
            "Security token has expired",
            correlationId,
            Instant.now(),
            Map.of()
        );
    }
    
    public static SecurityError riskThresholdExceeded(double riskScore, String correlationId) {
        return new SecurityError(
            ErrorType.RISK_THRESHOLD_EXCEEDED,
            ErrorType.RISK_THRESHOLD_EXCEEDED.getDefaultMessage(),
            String.format("Risk score %.2f exceeds threshold", riskScore),
            correlationId,
            Instant.now(),
            Map.of("riskScore", riskScore)
        );
    }
    
    public static SecurityError rateLimitExceeded(String userId, String correlationId) {
        return new SecurityError(
            ErrorType.RATE_LIMIT_EXCEEDED,
            ErrorType.RATE_LIMIT_EXCEEDED.getDefaultMessage(),
            "Rate limit exceeded for user",
            correlationId,
            Instant.now(),
            Map.of("userId", userId)
        );
    }
}