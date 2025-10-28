package com.trademaster.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Multi-Factor Authentication Event
 *
 * Immutable event representing MFA configuration changes.
 *
 * Features:
 * - MFA action (enabled, disabled, verified)
 * - MFA type (TOTP, SMS, Email)
 * - Session tracking
 * - Security audit trail
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record MfaEvent(
    String eventId,
    Instant timestamp,
    Long userId,
    String correlationId,
    String sessionId,
    MfaAction action,
    MfaType mfaType,
    boolean successful
) implements AuthEvent {

    /**
     * MFA Action Type
     */
    public enum MfaAction {
        ENABLED,   // MFA enabled for user
        DISABLED,  // MFA disabled for user
        VERIFIED   // MFA code verified
    }

    /**
     * MFA Type
     */
    public enum MfaType {
        TOTP,   // Time-based one-time password
        SMS,    // SMS verification
        EMAIL   // Email verification
    }

    /**
     * Compact constructor with validation
     */
    public MfaEvent {
        eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        timestamp = timestamp != null ? timestamp : Instant.now();
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        mfaType = mfaType != null ? mfaType : MfaType.TOTP;
    }

    /**
     * Factory method for MFA enabled event
     */
    public static MfaEvent enabled(Long userId, String sessionId, MfaType type) {
        return new MfaEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            MfaAction.ENABLED,
            type,
            true
        );
    }

    /**
     * Factory method for MFA disabled event
     */
    public static MfaEvent disabled(Long userId, String sessionId) {
        return new MfaEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            MfaAction.DISABLED,
            MfaType.TOTP,
            true
        );
    }

    /**
     * Factory method for MFA verification event
     */
    public static MfaEvent verified(Long userId, String sessionId, MfaType type, boolean successful) {
        return new MfaEvent(
            null,
            null,
            userId,
            null,
            sessionId,
            MfaAction.VERIFIED,
            type,
            successful
        );
    }

    /**
     * Create builder instance
     */
    public static MfaEventBuilder builder() {
        return new MfaEventBuilder();
    }

    @Override
    public EventType eventType() {
        return action == MfaAction.ENABLED ? EventType.MFA_ENABLED : EventType.MFA_DISABLED;
    }

    /**
     * Builder for MfaEvent
     * Provides fluent API for constructing MFA events
     */
    public static class MfaEventBuilder {
        private String eventId;
        private Instant timestamp;
        private Long userId;
        private String correlationId;
        private String sessionId;
        private MfaAction action;
        private MfaType mfaType = MfaType.TOTP;
        private boolean successful;

        public MfaEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public MfaEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public MfaEventBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public MfaEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public MfaEventBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public MfaEventBuilder action(MfaAction action) {
            this.action = action;
            return this;
        }

        public MfaEventBuilder mfaType(MfaType mfaType) {
            this.mfaType = mfaType;
            return this;
        }

        public MfaEventBuilder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public MfaEvent build() {
            return new MfaEvent(
                eventId,
                timestamp,
                userId,
                correlationId,
                sessionId,
                action,
                mfaType,
                successful
            );
        }
    }
}
