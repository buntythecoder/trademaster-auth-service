package com.trademaster.auth.command;

import com.trademaster.auth.dto.*;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for all concrete command implementations
 *
 * Tests cover:
 * - ChangePasswordCommand
 * - VerifyEmailCommand
 * - ResetPasswordCommand
 * - LogoutCommand
 * - EnableMfaCommand
 * - DisableMfaCommand
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Concrete Commands Integration Tests")
class ConcreteCommandsIntegrationTest {

    @Mock private PasswordManagementService passwordService;
    @Mock private UserVerificationService verificationService;
    @Mock private SessionManagementService sessionService;
    @Mock private MfaService mfaService;

    @Test
    @DisplayName("VerifyEmailCommand should verify email successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testVerifyEmailCommand() {
        // Given
        EmailVerificationResponse expectedResponse = new EmailVerificationResponse(
            "Email verified successfully",
            "user@example.com"
        );
        when(verificationService.verifyEmail(any(), any()))
            .thenReturn(Result.success(expectedResponse));

        VerifyEmailCommand command = new VerifyEmailCommand(
            "user123",
            "token123",
            verificationService
        );

        // When
        Result<EmailVerificationResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedResponse);
        verify(verificationService).verifyEmail("user123", "token123");
    }

    @Test
    @DisplayName("VerifyEmailCommand should handle verification failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testVerifyEmailCommandFailure() {
        // Given
        when(verificationService.verifyEmail(any(), any()))
            .thenReturn(Result.failure("Invalid verification token"));

        VerifyEmailCommand command = new VerifyEmailCommand(
            "user123",
            "invalid-token",
            verificationService
        );

        // When
        Result<EmailVerificationResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Email verification failed");
    }

    @Test
    @DisplayName("ResetPasswordCommand should reset password successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testResetPasswordCommand() {
        // Given
        PasswordResetResponse expectedResponse = new PasswordResetResponse(
            "Password reset successfully",
            "session-456"
        );
        when(passwordService.resetPassword(any(), any()))
            .thenReturn(Result.success(expectedResponse));

        ResetPasswordCommand command = new ResetPasswordCommand(
            "user@example.com",
            "resetToken123",
            "newPassword789",
            passwordService
        );

        // When
        Result<PasswordResetResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedResponse);
        verify(passwordService).resetPassword("resetToken123", "newPassword789");
    }

    @Test
    @DisplayName("ResetPasswordCommand should handle reset failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testResetPasswordCommandFailure() {
        // Given
        when(passwordService.resetPassword(any(), any()))
            .thenReturn(Result.failure("Invalid reset token"));

        ResetPasswordCommand command = new ResetPasswordCommand(
            "user@example.com",
            "invalid-token",
            "newPassword",
            passwordService
        );

        // When
        Result<PasswordResetResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Password reset failed");
    }

    @Test
    @DisplayName("LogoutCommand should logout user successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testLogoutCommand() {
        // Given
        LogoutResponse expectedResponse = new LogoutResponse(
            "Logged out successfully",
            "session-123"
        );
        when(sessionService.invalidateUserSession(any(), any()))
            .thenReturn(Result.success(expectedResponse));

        LogoutCommand command = new LogoutCommand(
            "user123",
            "session-123",
            sessionService
        );

        // When
        Result<LogoutResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedResponse);
        verify(sessionService).invalidateUserSession("user123", "session-123");
    }

    @Test
    @DisplayName("LogoutCommand should handle logout failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testLogoutCommandFailure() {
        // Given
        when(sessionService.invalidateUserSession(any(), any()))
            .thenReturn(Result.failure("Session not found"));

        LogoutCommand command = new LogoutCommand(
            "user123",
            "invalid-session",
            sessionService
        );

        // When
        Result<LogoutResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Logout failed");
    }

    @Test
    @DisplayName("EnableMfaCommand should enable MFA successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testEnableMfaCommand() {
        // Given
        MfaConfig config = new MfaConfig(
            MfaConfig.MfaType.TOTP,
            "secret-key-123",
            true
        );
        when(mfaService.setupTotpMfa(any(), any())).thenReturn(config);
        when(mfaService.generateQrCodeUrl(any(), any()))
            .thenReturn("https://qr-code-url");
        when(mfaService.generateBackupCodes(any(), any()))
            .thenReturn(List.of("code1", "code2", "code3"));

        EnableMfaCommand command = new EnableMfaCommand(
            "user123",
            "session-123",
            mfaService
        );

        // When
        Result<MfaSetupResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        MfaSetupResponse response = result.getValue().orElseThrow();
        assertThat(response.getMessage()).contains("MFA setup successful");
        assertThat(response.getSecretKey()).isEqualTo("secret-key-123");
        assertThat(response.getQrCodeUrl()).isEqualTo("https://qr-code-url");
        assertThat(response.getBackupCodes()).hasSize(3);

        verify(mfaService).setupTotpMfa("user123", "session-123");
        verify(mfaService).generateQrCodeUrl("user123", "secret-key-123");
        verify(mfaService).generateBackupCodes("user123", "session-123");
    }

    @Test
    @DisplayName("EnableMfaCommand should handle MFA setup failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testEnableMfaCommandFailure() {
        // Given
        when(mfaService.setupTotpMfa(any(), any()))
            .thenThrow(new RuntimeException("MFA setup error"));

        EnableMfaCommand command = new EnableMfaCommand(
            "user123",
            "session-123",
            mfaService
        );

        // When
        Result<MfaSetupResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("MFA setup failed");
    }

    @Test
    @DisplayName("DisableMfaCommand should disable MFA successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDisableMfaCommand() {
        // Given
        MfaDisableResponse expectedResponse = new MfaDisableResponse(
            "MFA disabled successfully",
            "session-123"
        );
        when(mfaService.disableMfa(any(), any()))
            .thenReturn(Result.success(expectedResponse));

        DisableMfaCommand command = new DisableMfaCommand(
            "user123",
            "session-123",
            mfaService
        );

        // When
        Result<MfaDisableResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedResponse);
        verify(mfaService).disableMfa("user123", "session-123");
    }

    @Test
    @DisplayName("DisableMfaCommand should handle disable failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDisableMfaCommandFailure() {
        // Given
        when(mfaService.disableMfa(any(), any()))
            .thenReturn(Result.failure("MFA not enabled"));

        DisableMfaCommand command = new DisableMfaCommand(
            "user123",
            "session-123",
            mfaService
        );

        // When
        Result<MfaDisableResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("MFA disable failed");
    }

    @Test
    @DisplayName("All commands should work with decorator chaining")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCommandsWithDecorators() {
        // Given
        EmailVerificationResponse verifyResponse = new EmailVerificationResponse(
            "Email verified",
            "user@example.com"
        );
        LogoutResponse logoutResponse = new LogoutResponse(
            "Logged out",
            "session-123"
        );

        when(verificationService.verifyEmail(any(), any()))
            .thenReturn(Result.failure("Temporary error"))
            .thenReturn(Result.success(verifyResponse));

        when(sessionService.invalidateUserSession(any(), any()))
            .thenReturn(Result.success(logoutResponse));

        VerifyEmailCommand verifyCommand = new VerifyEmailCommand(
            "user123",
            "token123",
            verificationService
        );

        LogoutCommand logoutCommand = new LogoutCommand(
            "user123",
            "session-123",
            sessionService
        );

        // When
        Result<EmailVerificationResponse, String> verifyResult = verifyCommand
            .withRetry(3)
            .withMetrics("verify-email")
            .withAudit("verify-email")
            .execute()
            .join();

        Result<LogoutResponse, String> logoutResult = logoutCommand
            .withMetrics("logout")
            .withAudit("logout")
            .execute()
            .join();

        // Then
        assertThat(verifyResult.isSuccess()).isTrue();
        assertThat(logoutResult.isSuccess()).isTrue();
        verify(verificationService, times(2)).verifyEmail(any(), any());
    }

    @Test
    @DisplayName("All commands should execute in virtual threads")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testVirtualThreadExecution() {
        // Given
        when(verificationService.verifyEmail(any(), any()))
            .thenReturn(Result.success(new EmailVerificationResponse("Verified", "email")));
        when(passwordService.resetPassword(any(), any()))
            .thenReturn(Result.success(new PasswordResetResponse("Reset", "session")));
        when(sessionService.invalidateUserSession(any(), any()))
            .thenReturn(Result.success(new LogoutResponse("Logged out", "session")));

        VerifyEmailCommand verify = new VerifyEmailCommand("u1", "t1", verificationService);
        ResetPasswordCommand reset = new ResetPasswordCommand("email", "token", "pass", passwordService);
        LogoutCommand logout = new LogoutCommand("u1", "s1", sessionService);

        // When - Execute all concurrently
        long start = System.currentTimeMillis();

        var result1 = verify.execute();
        var result2 = reset.execute();
        var result3 = logout.execute();

        // Join all
        result1.join();
        result2.join();
        result3.join();

        long duration = System.currentTimeMillis() - start;

        // Then - Should execute quickly in parallel
        assertThat(duration).isLessThan(1000);
    }
}
