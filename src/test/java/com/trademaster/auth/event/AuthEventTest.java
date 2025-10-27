package com.trademaster.auth.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for AuthEvent sealed hierarchy
 *
 * Tests cover:
 * - Event creation with builders
 * - Factory methods
 * - Default value initialization
 * - Pattern matching exhaustiveness
 * - Event type classification
 */
@DisplayName("AuthEvent Sealed Hierarchy Tests")
class AuthEventTest {

    @Test
    @DisplayName("Should create LoginEvent with factory method")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLoginEventFactoryMethod() {
        // When
        LoginEvent event = LoginEvent.withMfa(
            1L,
            "session-123",
            "device-fingerprint",
            "192.168.1.1",
            true
        );

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.sessionId()).isEqualTo("session-123");
        assertThat(event.deviceFingerprint()).isEqualTo("device-fingerprint");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(event.mfaUsed()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.LOGIN);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create LoginEvent with builder")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLoginEventBuilder() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();

        // When
        LoginEvent event = LoginEvent.builder()
            .eventId(eventId)
            .timestamp(timestamp)
            .userId(1L)
            .correlationId(correlationId)
            .sessionId("session-123")
            .deviceFingerprint("device-fp")
            .ipAddress("192.168.1.1")
            .mfaUsed(true)
            .build();

        // Then
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.sessionId()).isEqualTo("session-123");
        assertThat(event.mfaUsed()).isTrue();
    }

    @Test
    @DisplayName("Should create LogoutEvent with factory method")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLogoutEventFactoryMethod() {
        // When
        LogoutEvent event = LogoutEvent.sessionExpired(1L, "session-123", "192.168.1.1");

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.sessionId()).isEqualTo("session-123");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(event.reason()).isEqualTo(LogoutEvent.LogoutReason.SESSION_EXPIRED);
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.LOGOUT);
    }

    @Test
    @DisplayName("Should create LogoutEvent with builder")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLogoutEventBuilder() {
        // When
        LogoutEvent event = LogoutEvent.builder()
            .userId(1L)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .reason(LogoutEvent.LogoutReason.USER_INITIATED)
            .build();

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.reason()).isEqualTo(LogoutEvent.LogoutReason.USER_INITIATED);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create PasswordChangeEvent with factory method")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPasswordChangeEventFactoryMethod() {
        // When
        PasswordChangeEvent event = PasswordChangeEvent.userInitiated(
            1L,
            "session-123",
            "192.168.1.1",
            true
        );

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.sessionId()).isEqualTo("session-123");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.PASSWORD_CHANGE);
    }

    @Test
    @DisplayName("Should create PasswordChangeEvent with builder")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPasswordChangeEventBuilder() {
        // When
        PasswordChangeEvent event = PasswordChangeEvent.builder()
            .userId(1L)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .successful(false)
            .build();

        // Then
        assertThat(event.successful()).isFalse();
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("Should create MfaEvent with factory method for enabled")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventEnabledFactoryMethod() {
        // When
        MfaEvent event = MfaEvent.enabled(1L, "session-123", MfaEvent.MfaType.TOTP);

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.sessionId()).isEqualTo("session-123");
        assertThat(event.action()).isEqualTo(MfaEvent.MfaAction.ENABLED);
        assertThat(event.mfaType()).isEqualTo(MfaEvent.MfaType.TOTP);
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.MFA_ENABLED);
    }

    @Test
    @DisplayName("Should create MfaEvent with factory method for disabled")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventDisabledFactoryMethod() {
        // When
        MfaEvent event = MfaEvent.disabled(1L, "session-123");

        // Then
        assertThat(event.action()).isEqualTo(MfaEvent.MfaAction.DISABLED);
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.MFA_DISABLED);
    }

    @Test
    @DisplayName("Should create MfaEvent with factory method for verified")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventVerifiedFactoryMethod() {
        // When
        MfaEvent event = MfaEvent.verified(1L, "session-123", MfaEvent.MfaType.SMS, true);

        // Then
        assertThat(event.action()).isEqualTo(MfaEvent.MfaAction.VERIFIED);
        assertThat(event.mfaType()).isEqualTo(MfaEvent.MfaType.SMS);
        assertThat(event.successful()).isTrue();
    }

    @Test
    @DisplayName("Should create MfaEvent with builder")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventBuilder() {
        // When
        MfaEvent event = MfaEvent.builder()
            .userId(1L)
            .sessionId("session-123")
            .action(MfaEvent.MfaAction.VERIFIED)
            .mfaType(MfaEvent.MfaType.EMAIL)
            .successful(true)
            .build();

        // Then
        assertThat(event.action()).isEqualTo(MfaEvent.MfaAction.VERIFIED);
        assertThat(event.mfaType()).isEqualTo(MfaEvent.MfaType.EMAIL);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("Should create VerificationEvent with factory method for email verified")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testVerificationEventEmailVerifiedFactoryMethod() {
        // When
        VerificationEvent event = VerificationEvent.emailVerified(
            1L,
            "token-123",
            "192.168.1.1"
        );

        // Then
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.verificationToken()).isEqualTo("token-123");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(event.type()).isEqualTo(VerificationEvent.VerificationType.EMAIL);
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.EMAIL_VERIFIED);
    }

    @Test
    @DisplayName("Should create VerificationEvent with factory method for failed")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testVerificationEventFailedFactoryMethod() {
        // When
        VerificationEvent event = VerificationEvent.failed(
            1L,
            "token-123",
            "192.168.1.1"
        );

        // Then
        assertThat(event.successful()).isFalse();
    }

    @Test
    @DisplayName("Should create VerificationEvent with builder")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testVerificationEventBuilder() {
        // When
        VerificationEvent event = VerificationEvent.builder()
            .userId(1L)
            .verificationToken("token-123")
            .type(VerificationEvent.VerificationType.ACCOUNT)
            .ipAddress("192.168.1.1")
            .successful(true)
            .build();

        // Then
        assertThat(event.type()).isEqualTo(VerificationEvent.VerificationType.ACCOUNT);
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.ACCOUNT_VERIFIED);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("Should generate unique event IDs for different events")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testUniqueEventIds() {
        // When
        LoginEvent event1 = LoginEvent.withMfa(1L, "session-1", "fp-1", "ip-1", true);
        LoginEvent event2 = LoginEvent.withMfa(1L, "session-1", "fp-1", "ip-1", true);

        // Then
        assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
    }

    @Test
    @DisplayName("Should generate unique correlation IDs for different events")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testUniqueCorrelationIds() {
        // When
        LoginEvent event1 = LoginEvent.withMfa(1L, "session-1", "fp-1", "ip-1", true);
        LoginEvent event2 = LoginEvent.withMfa(1L, "session-1", "fp-1", "ip-1", true);

        // Then
        assertThat(event1.correlationId()).isNotEqualTo(event2.correlationId());
    }

    @Test
    @DisplayName("Should handle pattern matching exhaustively")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExhaustivePatternMatching() {
        // Given
        AuthEvent loginEvent = LoginEvent.withMfa(1L, "s1", "fp", "ip", true);
        AuthEvent logoutEvent = LogoutEvent.sessionExpired(1L, "s1", "ip");
        AuthEvent passwordEvent = PasswordChangeEvent.userInitiated(1L, "s1", "ip", true);
        AuthEvent mfaEvent = MfaEvent.enabled(1L, "s1", MfaEvent.MfaType.TOTP);
        AuthEvent verificationEvent = VerificationEvent.emailVerified(1L, "t1", "ip");

        // When & Then - Pattern matching should be exhaustive
        assertThat(getEventDescription(loginEvent)).isEqualTo("Login event");
        assertThat(getEventDescription(logoutEvent)).isEqualTo("Logout event");
        assertThat(getEventDescription(passwordEvent)).isEqualTo("Password change event");
        assertThat(getEventDescription(mfaEvent)).isEqualTo("MFA event");
        assertThat(getEventDescription(verificationEvent)).isEqualTo("Verification event");
    }

    private String getEventDescription(AuthEvent event) {
        return switch (event) {
            case LoginEvent login -> "Login event";
            case LogoutEvent logout -> "Logout event";
            case PasswordChangeEvent password -> "Password change event";
            case MfaEvent mfa -> "MFA event";
            case VerificationEvent verification -> "Verification event";
        };
    }

    @Test
    @DisplayName("Should apply default values in compact constructor")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCompactConstructorDefaults() {
        // When
        LoginEvent event = new LoginEvent(
            null,  // Should generate eventId
            null,  // Should generate timestamp
            1L,
            null,  // Should generate correlationId
            "session-123",
            "device-fp",
            "192.168.1.1",
            true
        );

        // Then
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.correlationId()).isNotNull();
    }
}
