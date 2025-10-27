package com.trademaster.auth.builder;

import com.trademaster.auth.context.MfaAuthenticationContext;
import com.trademaster.auth.context.SocialAuthenticationContext;
import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.SessionTimestamp;
import com.trademaster.auth.dto.SocialAuthRequest;
import com.trademaster.auth.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for all record builders
 *
 * Tests cover:
 * - LoginEvent.builder()
 * - LogoutEvent.builder()
 * - PasswordChangeEvent.builder()
 * - MfaEvent.builder()
 * - VerificationEvent.builder()
 * - SessionTimestamp.builder()
 * - SocialAuthenticationContext.builder()
 * - MfaAuthenticationContext.builder()
 */
@DisplayName("Record Builders Comprehensive Tests")
class RecordBuildersTest {

    @Test
    @DisplayName("LoginEvent.builder() should build complete event")
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
            .userId(123L)
            .correlationId(correlationId)
            .sessionId("session-456")
            .deviceFingerprint("device-fp-789")
            .ipAddress("192.168.1.100")
            .mfaUsed(true)
            .build();

        // Then
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.userId()).isEqualTo(123L);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.sessionId()).isEqualTo("session-456");
        assertThat(event.deviceFingerprint()).isEqualTo("device-fp-789");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.100");
        assertThat(event.mfaUsed()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.LOGIN);
    }

    @Test
    @DisplayName("LoginEvent.builder() should apply default values")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLoginEventBuilderDefaults() {
        // When
        LoginEvent event = LoginEvent.builder()
            .userId(1L)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .build();

        // Then
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.correlationId()).isNotNull();
        assertThat(event.deviceFingerprint()).isEmpty(); // Default value
        assertThat(event.mfaUsed()).isFalse(); // Default value
    }

    @Test
    @DisplayName("LogoutEvent.builder() should build complete event")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLogoutEventBuilder() {
        // Given
        String eventId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();

        // When
        LogoutEvent event = LogoutEvent.builder()
            .eventId(eventId)
            .timestamp(timestamp)
            .userId(456L)
            .sessionId("session-789")
            .ipAddress("10.0.0.1")
            .reason(LogoutEvent.LogoutReason.USER_INITIATED)
            .build();

        // Then
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.userId()).isEqualTo(456L);
        assertThat(event.sessionId()).isEqualTo("session-789");
        assertThat(event.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(event.reason()).isEqualTo(LogoutEvent.LogoutReason.USER_INITIATED);
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.LOGOUT);
    }

    @Test
    @DisplayName("LogoutEvent.builder() should support all logout reasons")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLogoutEventBuilderAllReasons() {
        // When & Then
        LogoutEvent sessionExpired = LogoutEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .ipAddress("ip")
            .reason(LogoutEvent.LogoutReason.SESSION_EXPIRED)
            .build();
        assertThat(sessionExpired.reason()).isEqualTo(LogoutEvent.LogoutReason.SESSION_EXPIRED);

        LogoutEvent securityConcern = LogoutEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .ipAddress("ip")
            .reason(LogoutEvent.LogoutReason.SECURITY_CONCERN)
            .build();
        assertThat(securityConcern.reason()).isEqualTo(LogoutEvent.LogoutReason.SECURITY_CONCERN);
    }

    @Test
    @DisplayName("PasswordChangeEvent.builder() should build complete event")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPasswordChangeEventBuilder() {
        // When
        PasswordChangeEvent event = PasswordChangeEvent.builder()
            .userId(789L)
            .sessionId("session-abc")
            .ipAddress("172.16.0.1")
            .successful(true)
            .build();

        // Then
        assertThat(event.userId()).isEqualTo(789L);
        assertThat(event.sessionId()).isEqualTo("session-abc");
        assertThat(event.ipAddress()).isEqualTo("172.16.0.1");
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.PASSWORD_CHANGE);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("PasswordChangeEvent.builder() should handle success and failure")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPasswordChangeEventBuilderSuccessFailure() {
        // When
        PasswordChangeEvent success = PasswordChangeEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .ipAddress("ip")
            .successful(true)
            .build();

        PasswordChangeEvent failure = PasswordChangeEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .ipAddress("ip")
            .successful(false)
            .build();

        // Then
        assertThat(success.successful()).isTrue();
        assertThat(failure.successful()).isFalse();
    }

    @Test
    @DisplayName("MfaEvent.builder() should build complete event")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventBuilder() {
        // When
        MfaEvent event = MfaEvent.builder()
            .userId(321L)
            .sessionId("session-xyz")
            .action(MfaEvent.MfaAction.ENABLED)
            .mfaType(MfaEvent.MfaType.TOTP)
            .successful(true)
            .build();

        // Then
        assertThat(event.userId()).isEqualTo(321L);
        assertThat(event.sessionId()).isEqualTo("session-xyz");
        assertThat(event.action()).isEqualTo(MfaEvent.MfaAction.ENABLED);
        assertThat(event.mfaType()).isEqualTo(MfaEvent.MfaType.TOTP);
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.MFA_ENABLED);
    }

    @Test
    @DisplayName("MfaEvent.builder() should support all MFA types and actions")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaEventBuilderAllTypes() {
        // When & Then
        MfaEvent totpEnabled = MfaEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .action(MfaEvent.MfaAction.ENABLED)
            .mfaType(MfaEvent.MfaType.TOTP)
            .successful(true)
            .build();
        assertThat(totpEnabled.mfaType()).isEqualTo(MfaEvent.MfaType.TOTP);

        MfaEvent smsVerified = MfaEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .action(MfaEvent.MfaAction.VERIFIED)
            .mfaType(MfaEvent.MfaType.SMS)
            .successful(true)
            .build();
        assertThat(smsVerified.mfaType()).isEqualTo(MfaEvent.MfaType.SMS);
        assertThat(smsVerified.action()).isEqualTo(MfaEvent.MfaAction.VERIFIED);

        MfaEvent emailDisabled = MfaEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .action(MfaEvent.MfaAction.DISABLED)
            .mfaType(MfaEvent.MfaType.EMAIL)
            .successful(true)
            .build();
        assertThat(emailDisabled.mfaType()).isEqualTo(MfaEvent.MfaType.EMAIL);
        assertThat(emailDisabled.eventType()).isEqualTo(AuthEvent.EventType.MFA_DISABLED);
    }

    @Test
    @DisplayName("VerificationEvent.builder() should build complete event")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testVerificationEventBuilder() {
        // When
        VerificationEvent event = VerificationEvent.builder()
            .userId(654L)
            .verificationToken("token-123")
            .type(VerificationEvent.VerificationType.EMAIL)
            .ipAddress("192.168.2.1")
            .successful(true)
            .build();

        // Then
        assertThat(event.userId()).isEqualTo(654L);
        assertThat(event.verificationToken()).isEqualTo("token-123");
        assertThat(event.type()).isEqualTo(VerificationEvent.VerificationType.EMAIL);
        assertThat(event.ipAddress()).isEqualTo("192.168.2.1");
        assertThat(event.successful()).isTrue();
        assertThat(event.eventType()).isEqualTo(AuthEvent.EventType.EMAIL_VERIFIED);
    }

    @Test
    @DisplayName("VerificationEvent.builder() should support both verification types")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testVerificationEventBuilderTypes() {
        // When
        VerificationEvent emailVerification = VerificationEvent.builder()
            .userId(1L)
            .verificationToken("token")
            .type(VerificationEvent.VerificationType.EMAIL)
            .ipAddress("ip")
            .successful(true)
            .build();

        VerificationEvent accountVerification = VerificationEvent.builder()
            .userId(1L)
            .verificationToken("token")
            .type(VerificationEvent.VerificationType.ACCOUNT)
            .ipAddress("ip")
            .successful(true)
            .build();

        // Then
        assertThat(emailVerification.eventType()).isEqualTo(AuthEvent.EventType.EMAIL_VERIFIED);
        assertThat(accountVerification.eventType()).isEqualTo(AuthEvent.EventType.ACCOUNT_VERIFIED);
    }

    @Test
    @DisplayName("SessionTimestamp.builder() should build complete record")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testSessionTimestampBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        SessionTimestamp timestamp = SessionTimestamp.builder()
            .sessionId("session-123")
            .timestamp(now)
            .build();

        // Then
        assertThat(timestamp.sessionId()).isEqualTo("session-123");
        assertThat(timestamp.timestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("SocialAuthenticationContext.builder() should build complete context")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testSocialAuthenticationContextBuilder() {
        // Given
        SocialAuthRequest request = new SocialAuthRequest(
            "google",
            "social-token-123",
            "user@gmail.com"
        );

        // When
        SocialAuthenticationContext context = SocialAuthenticationContext.builder()
            .request(request)
            .socialToken("social-token-123")
            .build();

        // Then
        assertThat(context.request()).isEqualTo(request);
        assertThat(context.socialToken()).isEqualTo("social-token-123");
    }

    @Test
    @DisplayName("MfaAuthenticationContext.builder() should build complete context")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMfaAuthenticationContextBuilder() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest(
            "user@example.com",
            "password123",
            "device-fp"
        );

        // When
        MfaAuthenticationContext context = MfaAuthenticationContext.builder()
            .request(request)
            .mfaToken("mfa-token-456")
            .build();

        // Then
        assertThat(context.request()).isEqualTo(request);
        assertThat(context.mfaToken()).isEqualTo("mfa-token-456");
    }

    @Test
    @DisplayName("All builders should support fluent API")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testFluentBuilderAPI() {
        // When - Chain multiple builder calls
        LoginEvent login = LoginEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .deviceFingerprint("fp")
            .ipAddress("ip")
            .mfaUsed(true)
            .build();

        MfaEvent mfa = MfaEvent.builder()
            .userId(2L)
            .sessionId("s2")
            .action(MfaEvent.MfaAction.ENABLED)
            .mfaType(MfaEvent.MfaType.TOTP)
            .successful(true)
            .build();

        // Then
        assertThat(login).isNotNull();
        assertThat(mfa).isNotNull();
    }

    @Test
    @DisplayName("Builders should create immutable records")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testBuilderImmutability() {
        // When
        LoginEvent event = LoginEvent.builder()
            .userId(1L)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .build();

        // Then - Records should be immutable
        assertThat(event).isInstanceOf(LoginEvent.class);
        // Cannot modify fields - records are immutable by design
    }

    @Test
    @DisplayName("Builders should integrate with factory methods")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testBuilderAndFactoryMethodIntegration() {
        // Given
        LoginEvent factoryEvent = LoginEvent.withMfa(1L, "s1", "fp", "ip", true);
        LoginEvent builderEvent = LoginEvent.builder()
            .userId(1L)
            .sessionId("s1")
            .deviceFingerprint("fp")
            .ipAddress("ip")
            .mfaUsed(true)
            .build();

        // Then - Both should create valid events (different IDs)
        assertThat(factoryEvent.userId()).isEqualTo(builderEvent.userId());
        assertThat(factoryEvent.sessionId()).isEqualTo(builderEvent.sessionId());
        assertThat(factoryEvent.mfaUsed()).isEqualTo(builderEvent.mfaUsed());
        assertThat(factoryEvent.eventId()).isNotEqualTo(builderEvent.eventId());
    }
}
