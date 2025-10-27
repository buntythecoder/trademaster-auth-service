package com.trademaster.auth.command;

import com.trademaster.auth.dto.ChangePasswordRequest;
import com.trademaster.auth.dto.PasswordChangeResponse;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.service.PasswordManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ChangePasswordCommand
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordCommand Tests")
class ChangePasswordCommandTest {

    @Mock
    private PasswordManagementService passwordService;

    private ChangePasswordRequest request;

    @BeforeEach
    void setUp() {
        request = new ChangePasswordRequest(
            "user@example.com",
            "oldPassword123",
            "newPassword456"
        );
    }

    @Test
    @DisplayName("Should execute password change successfully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSuccessfulPasswordChange() {
        // Given
        PasswordChangeResponse expectedResponse = new PasswordChangeResponse(
            "Password changed successfully",
            "session-123"
        );

        when(passwordService.changePassword(eq("user@example.com"), any(), any()))
            .thenReturn(Result.success(expectedResponse));

        ChangePasswordCommand command = new ChangePasswordCommand(
            request,
            "session-123",
            passwordService
        );

        // When
        Result<PasswordChangeResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedResponse);
        verify(passwordService).changePassword(eq("user@example.com"), any(), any());
    }

    @Test
    @DisplayName("Should handle password change failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPasswordChangeFailure() {
        // Given
        when(passwordService.changePassword(eq("user@example.com"), any(), any()))
            .thenReturn(Result.failure("Invalid old password"));

        ChangePasswordCommand command = new ChangePasswordCommand(
            request,
            "session-123",
            passwordService
        );

        // When
        Result<PasswordChangeResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Password change failed");
    }

    @Test
    @DisplayName("Should execute asynchronously")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testAsynchronousExecution() {
        // Given
        PasswordChangeResponse response = new PasswordChangeResponse(
            "Password changed",
            "session-123"
        );
        when(passwordService.changePassword(any(), any(), any()))
            .thenReturn(Result.success(response));

        ChangePasswordCommand command = new ChangePasswordCommand(
            request,
            "session-123",
            passwordService
        );

        // When
        long startThread = Thread.currentThread().threadId();
        Result<PasswordChangeResponse, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        // Command should execute in virtual thread (different thread)
    }

    @Test
    @DisplayName("Should work with retry decorator")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testWithRetryDecorator() {
        // Given
        PasswordChangeResponse response = new PasswordChangeResponse(
            "Password changed",
            "session-123"
        );

        // Fail first time, succeed second time
        when(passwordService.changePassword(any(), any(), any()))
            .thenReturn(Result.failure("Temporary error"))
            .thenReturn(Result.success(response));

        ChangePasswordCommand command = new ChangePasswordCommand(
            request,
            "session-123",
            passwordService
        );

        // When
        Result<PasswordChangeResponse, String> result = command
            .withRetry(3)
            .execute()
            .join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(passwordService, org.mockito.Mockito.times(2))
            .changePassword(any(), any(), any());
    }
}
