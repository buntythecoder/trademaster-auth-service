package com.trademaster.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Password Change Event
 *
 * Immutable event representing password modification operations.
 *
 * Features:
 * - Change type (user-initiated, admin reset, forced)
 * - IP address for security audit
 * - Timestamp for compliance
 * - Success/failure tracking
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PasswordChangeEvent(
    String eventId,
    Instant timestamp,
    Long userId,
    String correlationId,
    String sessionId,
    String ipAddress,
    ChangeType changeType,
    boolean successful
) implements AuthEvent {

    /**
     * Password Change Type
     */
    public enum ChangeType {
        USER_INITIATED,  // User changed their own password
        ADMIN_RESET,     // Admin reset user password
        FORCED_RESET,    // Security-forced password change
        FORGOT_PASSWORD  // Password reset via email
    }

    /**
     * Compact constructor with validation
     */
    public PasswordChangeEvent {
        eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        timestamp = timestamp != null ? timestamp : Instant.now();
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        changeType = changeType != null ? changeType : ChangeType.USER_INITIATED;
    }

    /**
     * Factory method for user-initiated password change
     */
    public static PasswordChangeEvent userInitiated(Long userId, String sessionId, String ipAddress, boolean successful) {
        return new PasswordChangeEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            ipAddress,
            ChangeType.USER_INITIATED,
            successful
        );
    }

    /**
     * Factory method for forgot password flow
     */
    public static PasswordChangeEvent forgotPassword(Long userId, String sessionId, String ipAddress) {
        return new PasswordChangeEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            ipAddress,
            ChangeType.FORGOT_PASSWORD,
            true
        );
    }

    /**
     * Create builder instance
     */
    public static PasswordChangeEventBuilder builder() {
        return new PasswordChangeEventBuilder();
    }

    @Override
    public EventType eventType() {
        return EventType.PASSWORD_CHANGE;
    }

    /**
     * Builder for PasswordChangeEvent
     * Provides fluent API for constructing password change events
     */
    public static class PasswordChangeEventBuilder {
        private String eventId;
        private Instant timestamp;
        private Long userId;
        private String correlationId;
        private String sessionId;
        private String ipAddress;
        private ChangeType changeType = ChangeType.USER_INITIATED;
        private boolean successful;

        public PasswordChangeEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public PasswordChangeEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public PasswordChangeEventBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PasswordChangeEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public PasswordChangeEventBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public PasswordChangeEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public PasswordChangeEventBuilder changeType(ChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public PasswordChangeEventBuilder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public PasswordChangeEvent build() {
            return new PasswordChangeEvent(
                eventId,
                timestamp,
                userId,
                correlationId,
                sessionId,
                ipAddress,
                changeType,
                successful
            );
        }
    }
}
