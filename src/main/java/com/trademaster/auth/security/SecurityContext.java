package com.trademaster.auth.security;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Security Context - Immutable security context record
 *
 * MANDATORY: Immutability - Rule #9
 * MANDATORY: Records for data holders - Rule #9
 * MANDATORY: Functional validation - Rule #3
 */
@Builder
public record SecurityContext(
    String correlationId,
    String userId,
    String ipAddress,
    String userAgent,
    String sessionId,
    LocalDateTime timestamp,
    Optional<String> deviceFingerprint,
    Optional<String> mfaToken,
    Optional<String> accessToken
) {

    /**
     * Validate security context using functional approach
     *
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: No if-else - Rule #3
     */
    public SecurityResult<SecurityContext> validate() {
        return validateCorrelationId()
            .flatMap(this::validateUserId)
            .flatMap(this::validateTimestamp)
            .map(ctx -> this);
    }

    /**
     * Check if context has valid authentication
     */
    public boolean isAuthenticated() {
        return Optional.ofNullable(userId)
            .filter(id -> !id.trim().isEmpty())
            .isPresent();
    }

    /**
     * Check if context has valid session
     */
    public boolean hasValidSession() {
        return Optional.ofNullable(sessionId)
            .filter(session -> !session.trim().isEmpty())
            .isPresent();
    }

    /**
     * Functional validation methods using Result pattern
     */
    private SecurityResult<SecurityContext> validateCorrelationId() {
        return Optional.ofNullable(correlationId)
            .filter(id -> !id.trim().isEmpty())
            .map(id -> SecurityResult.<SecurityContext>success(this))
            .orElse(SecurityResult.failure(SecurityError.CONTEXT_INVALID, "Missing correlation ID"));
    }

    private SecurityResult<SecurityContext> validateUserId(SecurityContext context) {
        return Optional.ofNullable(userId)
            .filter(id -> !id.trim().isEmpty())
            .filter(id -> id.length() >= 3)
            .map(id -> SecurityResult.<SecurityContext>success(context))
            .orElse(SecurityResult.failure(SecurityError.AUTHENTICATION_FAILED, "Invalid user ID"));
    }

    private SecurityResult<SecurityContext> validateTimestamp(SecurityContext context) {
        return Optional.ofNullable(timestamp)
            .filter(ts -> ts.isAfter(LocalDateTime.now().minusHours(24)))
            .map(ts -> SecurityResult.<SecurityContext>success(context))
            .orElse(SecurityResult.failure(SecurityError.EXPIRED_CREDENTIALS, "Invalid timestamp"));
    }

    /**
     * Builder with validation
     */
    public static class SecurityContextBuilder {
        private Optional<String> deviceFingerprint = Optional.empty();
        private Optional<String> mfaToken = Optional.empty();
        private Optional<String> accessToken = Optional.empty();

        public SecurityContextBuilder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = Optional.ofNullable(deviceFingerprint);
            return this;
        }

        public SecurityContextBuilder mfaToken(String mfaToken) {
            this.mfaToken = Optional.ofNullable(mfaToken);
            return this;
        }

        public SecurityContextBuilder accessToken(String accessToken) {
            this.accessToken = Optional.ofNullable(accessToken);
            return this;
        }
    }
}