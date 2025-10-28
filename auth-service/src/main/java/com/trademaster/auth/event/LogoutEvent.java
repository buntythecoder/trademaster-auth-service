package com.trademaster.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Logout Event
 *
 * Immutable event representing user session termination.
 *
 * Features:
 * - Session ID for tracking
 * - Device fingerprint for security
 * - IP address for audit trail
 * - Logout reason (explicit, timeout, security)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record LogoutEvent(
    String eventId,
    Instant timestamp,
    Long userId,
    String correlationId,
    String sessionId,
    String deviceFingerprint,
    String ipAddress,
    LogoutReason reason
) implements AuthEvent {

    /**
     * Logout Reason Enumeration
     */
    public enum LogoutReason {
        EXPLICIT,           // User-initiated logout (legacy)
        USER_INITIATED,     // User-initiated logout
        TIMEOUT,            // Session timeout (legacy)
        SESSION_EXPIRED,    // Session expired
        SECURITY,           // Security-forced logout (legacy)
        SECURITY_CONCERN,   // Security concern
        MFA_DISABLED,       // MFA disabled
        ADMIN_ACTION        // Admin-forced logout
    }

    /**
     * Compact constructor with validation
     */
    public LogoutEvent {
        eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        timestamp = timestamp != null ? timestamp : Instant.now();
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        reason = reason != null ? reason : LogoutReason.EXPLICIT;
    }

    /**
     * Factory method for explicit logout
     */
    public static LogoutEvent explicit(Long userId, String sessionId, String ipAddress) {
        return new LogoutEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            "",
            ipAddress,
            LogoutReason.EXPLICIT
        );
    }

    /**
     * Factory method for security logout
     */
    public static LogoutEvent security(Long userId, String sessionId, String reason) {
        return new LogoutEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            "",
            "system",
            LogoutReason.SECURITY
        );
    }

    /**
     * Factory method for session expired logout
     */
    public static LogoutEvent sessionExpired(Long userId, String sessionId, String ipAddress) {
        return new LogoutEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            "",
            ipAddress,
            LogoutReason.SESSION_EXPIRED
        );
    }

    /**
     * Create builder instance
     */
    public static LogoutEventBuilder builder() {
        return new LogoutEventBuilder();
    }

    @Override
    public EventType eventType() {
        return EventType.LOGOUT;
    }

    /**
     * Builder for LogoutEvent
     * Provides fluent API for constructing logout events
     */
    public static class LogoutEventBuilder {
        private String eventId;
        private Instant timestamp;
        private Long userId;
        private String correlationId;
        private String sessionId;
        private String deviceFingerprint = "";
        private String ipAddress;
        private LogoutReason reason = LogoutReason.EXPLICIT;

        public LogoutEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public LogoutEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LogoutEventBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public LogoutEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public LogoutEventBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public LogoutEventBuilder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        public LogoutEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public LogoutEventBuilder reason(LogoutReason reason) {
            this.reason = reason;
            return this;
        }

        public LogoutEvent build() {
            return new LogoutEvent(
                eventId,
                timestamp,
                userId,
                correlationId,
                sessionId,
                deviceFingerprint,
                ipAddress,
                reason
            );
        }
    }
}
