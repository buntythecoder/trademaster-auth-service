package com.trademaster.marketdata.security;

/**
 * Security Error Types for Zero Trust operations
 *
 * Represents all possible security failures in functional error handling.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum SecurityError {
    AUTHENTICATION_FAILED("Authentication failed"),
    AUTHORIZATION_FAILED("User lacks required permissions"),
    INVALID_TOKEN("Invalid or expired security token"),
    RISK_THRESHOLD_EXCEEDED("Operation blocked by risk assessment"),
    AUDIT_LOG_FAILED("Failed to record security audit"),
    INVALID_SECURITY_CONTEXT("Security context is invalid or incomplete"),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded for this operation"),
    IP_BLOCKED("IP address is blocked by security policy"),
    SUSPICIOUS_ACTIVITY("Operation blocked due to suspicious activity");

    private final String message;

    SecurityError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Create error with additional context
     */
    public String withContext(String context) {
        return message + ": " + context;
    }
}
