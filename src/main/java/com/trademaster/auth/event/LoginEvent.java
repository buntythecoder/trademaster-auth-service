package com.trademaster.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Login Event
 *
 * Immutable event representing successful user authentication.
 *
 * Features:
 * - Session tracking with session ID
 * - Device fingerprint for security
 * - IP address for audit trail
 * - Login timestamp for analytics
 * - Builder pattern for fluent construction
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record LoginEvent(
    String eventId,
    Instant timestamp,
    Long userId,
    String correlationId,
    String sessionId,
    String deviceFingerprint,
    String ipAddress,
    boolean mfaUsed
) implements AuthEvent {

    /**
     * Compact constructor with validation
     */
    public LoginEvent {
        eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        timestamp = timestamp != null ? timestamp : Instant.now();
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    /**
     * Factory method for simple login events
     */
    public static LoginEvent of(Long userId, String sessionId, String ipAddress) {
        return new LoginEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            "",
            ipAddress,
            false
        );
    }

    /**
     * Factory method with MFA tracking
     */
    public static LoginEvent withMfa(
            Long userId,
            String sessionId,
            String deviceFingerprint,
            String ipAddress,
            boolean mfaUsed) {
        return new LoginEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            deviceFingerprint,
            ipAddress,
            mfaUsed
        );
    }

    /**
     * Create builder instance
     */
    public static LoginEventBuilder builder() {
        return new LoginEventBuilder();
    }

    @Override
    public EventType eventType() {
        return EventType.LOGIN;
    }

    /**
     * Builder for LoginEvent
     * Provides fluent API for constructing login events
     */
    public static class LoginEventBuilder {
        private String eventId;
        private Instant timestamp;
        private Long userId;
        private String correlationId;
        private String sessionId;
        private String deviceFingerprint = "";
        private String ipAddress;
        private boolean mfaUsed = false;

        public LoginEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public LoginEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LoginEventBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public LoginEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public LoginEventBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public LoginEventBuilder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        public LoginEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public LoginEventBuilder mfaUsed(boolean mfaUsed) {
            this.mfaUsed = mfaUsed;
            return this;
        }

        public LoginEvent build() {
            return new LoginEvent(
                eventId,
                timestamp,
                userId,
                correlationId,
                sessionId,
                deviceFingerprint,
                ipAddress,
                mfaUsed
            );
        }
    }
}
