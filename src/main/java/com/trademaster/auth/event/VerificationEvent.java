package com.trademaster.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Verification Event
 *
 * Immutable event representing email and account verification operations.
 *
 * Features:
 * - Verification type (email, account)
 * - Token tracking for security
 * - IP address for audit trail
 * - Success/failure tracking
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record VerificationEvent(
    String eventId,
    Instant timestamp,
    Long userId,
    String correlationId,
    String verificationToken,
    VerificationType type,
    String ipAddress,
    boolean successful
) implements AuthEvent {

    /**
     * Verification Type
     */
    public enum VerificationType {
        EMAIL,      // Email verification
        ACCOUNT     // Full account verification
    }

    /**
     * Compact constructor with validation
     */
    public VerificationEvent {
        eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        timestamp = timestamp != null ? timestamp : Instant.now();
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        type = type != null ? type : VerificationType.EMAIL;
    }

    /**
     * Factory method for email verification
     */
    public static VerificationEvent emailVerified(Long userId, String token, String ipAddress) {
        return new VerificationEvent(
            null,
            null,
            userId,
            null,
            token,
            VerificationType.EMAIL,
            ipAddress,
            true
        );
    }

    /**
     * Factory method for failed verification
     */
    public static VerificationEvent failed(Long userId, String token, String ipAddress) {
        return new VerificationEvent(
            null,
            null,
            userId,
            null,
            token,
            VerificationType.EMAIL,
            ipAddress,
            false
        );
    }

    /**
     * Create builder instance
     */
    public static VerificationEventBuilder builder() {
        return new VerificationEventBuilder();
    }

    @Override
    public EventType eventType() {
        return type == VerificationType.EMAIL ? EventType.EMAIL_VERIFIED : EventType.ACCOUNT_VERIFIED;
    }

    /**
     * Builder for VerificationEvent
     * Provides fluent API for constructing verification events
     */
    public static class VerificationEventBuilder {
        private String eventId;
        private Instant timestamp;
        private Long userId;
        private String correlationId;
        private String verificationToken;
        private VerificationType type = VerificationType.EMAIL;
        private String ipAddress;
        private boolean successful;

        public VerificationEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public VerificationEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public VerificationEventBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public VerificationEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public VerificationEventBuilder verificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
            return this;
        }

        public VerificationEventBuilder type(VerificationType type) {
            this.type = type;
            return this;
        }

        public VerificationEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public VerificationEventBuilder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public VerificationEvent build() {
            return new VerificationEvent(
                eventId,
                timestamp,
                userId,
                correlationId,
                verificationToken,
                type,
                ipAddress,
                successful
            );
        }
    }
}
