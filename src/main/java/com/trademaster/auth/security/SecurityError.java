package com.trademaster.auth.security;

/**
 * Security Error enumeration for type-safe error handling
 *
 * MANDATORY: Enum for error types - Rule #11
 * MANDATORY: Pattern matching support - Rule #14
 */
public enum SecurityError {
    // Authentication errors
    AUTHENTICATION_FAILED("Authentication failed"),
    INVALID_CREDENTIALS("Invalid credentials"),
    EXPIRED_CREDENTIALS("Expired credentials"),
    ACCOUNT_LOCKED("Account is locked"),
    ACCOUNT_DISABLED("Account is disabled"),

    // Authorization errors
    AUTHORIZATION_FAILED("Authorization failed"),
    INSUFFICIENT_PRIVILEGES("Insufficient privileges"),
    ACCESS_DENIED("Access denied"),

    // Risk assessment errors
    RISK_TOO_HIGH("Risk level too high"),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),

    // Context and validation errors
    CONTEXT_INVALID("Security context invalid"),
    SESSION_INVALID("Session invalid"),
    TOKEN_INVALID("Token invalid"),
    MFA_REQUIRED("MFA required"),
    MFA_INVALID("MFA code invalid"),

    // System errors
    SYSTEM_ERROR("System error"),
    OPERATION_FAILED("Operation failed"),
    CONFIGURATION_ERROR("Configuration error"),
    EXTERNAL_SERVICE_ERROR("External service error"),

    // Circuit breaker errors
    CIRCUIT_BREAKER_OPEN("Circuit breaker open"),
    SERVICE_UNAVAILABLE("Service unavailable"),
    TIMEOUT("Operation timeout");

    private final String message;

    SecurityError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Get error category for pattern matching - Rule #14
     */
    public ErrorCategory getCategory() {
        return switch (this) {
            case AUTHENTICATION_FAILED, INVALID_CREDENTIALS, EXPIRED_CREDENTIALS,
                 ACCOUNT_LOCKED, ACCOUNT_DISABLED -> ErrorCategory.AUTHENTICATION;
            case AUTHORIZATION_FAILED, INSUFFICIENT_PRIVILEGES, ACCESS_DENIED -> ErrorCategory.AUTHORIZATION;
            case RISK_TOO_HIGH, RATE_LIMIT_EXCEEDED, SUSPICIOUS_ACTIVITY -> ErrorCategory.RISK;
            case CONTEXT_INVALID, SESSION_INVALID, TOKEN_INVALID,
                 MFA_REQUIRED, MFA_INVALID -> ErrorCategory.VALIDATION;
            case CIRCUIT_BREAKER_OPEN, SERVICE_UNAVAILABLE, TIMEOUT -> ErrorCategory.AVAILABILITY;
            case SYSTEM_ERROR, OPERATION_FAILED, CONFIGURATION_ERROR,
                 EXTERNAL_SERVICE_ERROR -> ErrorCategory.SYSTEM;
        };
    }

    /**
     * Get error severity for monitoring
     */
    public ErrorSeverity getSeverity() {
        return switch (this) {
            case AUTHENTICATION_FAILED, INVALID_CREDENTIALS -> ErrorSeverity.MEDIUM;
            case AUTHORIZATION_FAILED, ACCESS_DENIED -> ErrorSeverity.HIGH;
            case ACCOUNT_LOCKED, SUSPICIOUS_ACTIVITY -> ErrorSeverity.HIGH;
            case RISK_TOO_HIGH, RATE_LIMIT_EXCEEDED -> ErrorSeverity.MEDIUM;
            case SYSTEM_ERROR, EXTERNAL_SERVICE_ERROR -> ErrorSeverity.CRITICAL;
            case CIRCUIT_BREAKER_OPEN, SERVICE_UNAVAILABLE -> ErrorSeverity.HIGH;
            default -> ErrorSeverity.LOW;
        };
    }

    public enum ErrorCategory {
        AUTHENTICATION,
        AUTHORIZATION,
        RISK,
        VALIDATION,
        AVAILABILITY,
        SYSTEM
    }

    public enum ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}