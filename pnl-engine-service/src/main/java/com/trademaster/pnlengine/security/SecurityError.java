package com.trademaster.pnlengine.security;

import java.time.Instant;

/**
 * Security Error Types for Zero Trust Architecture
 * 
 * MANDATORY: Java 24 + Sealed Interfaces + Functional Error Handling
 * 
 * Sealed interface hierarchy for type-safe security error handling
 * with detailed error context for audit and debugging.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Security Refactoring)
 */
public sealed interface SecurityError {
    
    /**
     * Authentication failures
     */
    record AuthenticationFailed(
        String reason,
        String userId,
        String sessionId,
        Instant attemptedAt
    ) implements SecurityError {}
    
    /**
     * Authorization failures
     */
    record AuthorizationFailed(
        String requiredAuthority,
        String requiredScope,
        String userId,
        SecurityContext.SecurityLevel requiredLevel,
        Instant deniedAt
    ) implements SecurityError {}
    
    /**
     * Session management failures
     */
    record SessionExpired(
        String sessionId,
        String userId,
        Instant expiredAt,
        Instant attemptedAt
    ) implements SecurityError {}
    
    /**
     * Rate limiting failures
     */
    record RateLimitExceeded(
        String userId,
        String endpoint,
        int attemptCount,
        int maxAttempts,
        Instant windowStart,
        Instant blockedUntil
    ) implements SecurityError {}
    
    /**
     * Risk assessment failures
     */
    record RiskAssessmentFailed(
        String userId,
        String riskFactor,
        double riskScore,
        double threshold,
        String mitigation,
        Instant assessedAt
    ) implements SecurityError {}
    
    /**
     * Input validation security failures
     */
    record InputValidationFailed(
        String field,
        String reason,
        String sanitizedValue,
        Instant validatedAt
    ) implements SecurityError {}
    
    /**
     * System-level security errors
     */
    record SystemSecurityError(
        String component,
        String errorCode,
        String message,
        Instant occurredAt
    ) implements SecurityError {}
    
    /**
     * Get error severity level
     */
    default SecuritySeverity getSeverity() {
        return switch (this) {
            case AuthenticationFailed ignored -> SecuritySeverity.HIGH;
            case AuthorizationFailed ignored -> SecuritySeverity.HIGH;
            case SessionExpired ignored -> SecuritySeverity.MEDIUM;
            case RateLimitExceeded ignored -> SecuritySeverity.MEDIUM;
            case RiskAssessmentFailed ignored -> SecuritySeverity.HIGH;
            case InputValidationFailed ignored -> SecuritySeverity.LOW;
            case SystemSecurityError ignored -> SecuritySeverity.CRITICAL;
        };
    }
    
    /**
     * Get user-friendly error message
     */
    default String getUserMessage() {
        return switch (this) {
            case AuthenticationFailed ignored -> "Authentication failed. Please log in again.";
            case AuthorizationFailed ignored -> "Access denied. Insufficient permissions.";
            case SessionExpired ignored -> "Session expired. Please log in again.";
            case RateLimitExceeded ignored -> "Too many requests. Please try again later.";
            case RiskAssessmentFailed ignored -> "Request blocked for security reasons.";
            case InputValidationFailed ignored -> "Invalid input provided.";
            case SystemSecurityError ignored -> "System security error. Please contact support.";
        };
    }
    
    /**
     * Get audit log message
     */
    default String getAuditMessage() {
        return switch (this) {
            case AuthenticationFailed(var reason, var userId, var sessionId, var attemptedAt) ->
                String.format("Authentication failed for user %s (session: %s): %s at %s", 
                    userId, sessionId, reason, attemptedAt);
            case AuthorizationFailed(var auth, var scope, var userId, var level, var deniedAt) ->
                String.format("Authorization failed for user %s: required %s/%s (level: %s) at %s",
                    userId, auth, scope, level, deniedAt);
            case SessionExpired(var sessionId, var userId, var expiredAt, var attemptedAt) ->
                String.format("Session %s for user %s expired at %s, attempted at %s",
                    sessionId, userId, expiredAt, attemptedAt);
            case RateLimitExceeded(var userId, var endpoint, var attempts, var max, var start, var blocked) ->
                String.format("Rate limit exceeded for user %s on %s: %d/%d attempts from %s, blocked until %s",
                    userId, endpoint, attempts, max, start, blocked);
            case RiskAssessmentFailed(var userId, var factor, var score, var threshold, var mitigation, var assessed) ->
                String.format("Risk assessment failed for user %s: %s score %.2f > %.2f, mitigation: %s at %s",
                    userId, factor, score, threshold, mitigation, assessed);
            case InputValidationFailed(var field, var reason, var sanitized, var validated) ->
                String.format("Input validation failed for field %s: %s, sanitized to: %s at %s",
                    field, reason, sanitized, validated);
            case SystemSecurityError(var component, var code, var message, var occurred) ->
                String.format("System security error in %s [%s]: %s at %s",
                    component, code, message, occurred);
        };
    }
    
    enum SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}