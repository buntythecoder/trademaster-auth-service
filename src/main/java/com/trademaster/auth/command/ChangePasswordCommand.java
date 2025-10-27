package com.trademaster.auth.command;

import com.trademaster.auth.dto.ChangePasswordRequest;
import com.trademaster.auth.dto.PasswordChangeResponse;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.RetryCommandDecorator;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.service.PasswordManagementService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Change Password Command
 *
 * Encapsulates password change operation with validation and security.
 *
 * Features:
 * - Old password verification
 * - New password validation
 * - Audit trail logging
 * - Virtual threads for async execution
 * - Retry decorator support
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class ChangePasswordCommand implements Command<PasswordChangeResponse> {

    private final ChangePasswordRequest request;
    private final String sessionId;
    private final PasswordManagementService passwordService;
    private Command<PasswordChangeResponse> decorated;

    @Override
    public CompletableFuture<Result<PasswordChangeResponse, String>> execute() {
        return Optional.ofNullable(decorated)
            .map(Command::execute)
            .orElseGet(() -> CompletableFuture.supplyAsync(() ->
                passwordService.changePassword(
                    request.email(),
                    request.currentPassword(),
                    request.newPassword()
                ).mapError(error -> "Password change failed: " + error),
                Executors.newVirtualThreadPerTaskExecutor()
            ));
    }

    /**
     * Decorate with retry capability
     *
     * @param maxAttempts Maximum number of retry attempts
     * @return This command with retry decorator
     */
    public ChangePasswordCommand withRetry(int maxAttempts) {
        this.decorated = new RetryCommandDecorator<>(this, maxAttempts);
        return this;
    }
}
