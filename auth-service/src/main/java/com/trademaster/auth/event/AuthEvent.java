package com.trademaster.auth.event;

import java.time.Instant;

/**
 * Authentication Event - Base Event Type
 *
 * Sealed interface for all authentication-related events in the system.
 * Uses Java 24 sealed types for exhaustive pattern matching and type safety.
 *
 * Event Types:
 * - LoginEvent: User authentication events
 * - LogoutEvent: Session termination events
 * - PasswordChangeEvent: Password modification events
 * - MfaEvent: Multi-factor authentication events
 * - VerificationEvent: Email/account verification events
 *
 * All events are immutable records with:
 * - Unique event ID for correlation
 * - Timestamp for audit trail
 * - User ID for security tracking
 * - Correlation ID for distributed tracing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface AuthEvent permits
    LoginEvent,
    LogoutEvent,
    PasswordChangeEvent,
    MfaEvent,
    VerificationEvent {

    /**
     * Unique event identifier
     */
    String eventId();

    /**
     * Event timestamp
     */
    Instant timestamp();

    /**
     * User associated with this event
     */
    Long userId();

    /**
     * Correlation ID for distributed tracing
     */
    String correlationId();

    /**
     * Event type for pattern matching
     */
    EventType eventType();

    /**
     * Event Type Enumeration
     */
    enum EventType {
        LOGIN,
        LOGOUT,
        PASSWORD_CHANGE,
        MFA_ENABLED,
        MFA_DISABLED,
        EMAIL_VERIFIED,
        ACCOUNT_VERIFIED
    }
}
