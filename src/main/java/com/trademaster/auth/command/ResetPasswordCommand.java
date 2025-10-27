package com.trademaster.auth.command;

import com.trademaster.auth.dto.PasswordResetResponse;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.service.PasswordManagementService;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Reset Password Command
 *
 * Encapsulates password reset operation with token validation.
 *
 * Features:
 * - Reset token validation
 * - Token expiration checking
 * - New password validation
 * - Security audit logging
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class ResetPasswordCommand implements Command<PasswordResetResponse> {

    private final String email;
    private final String resetToken;
    private final String newPassword;
    private final PasswordManagementService passwordService;

    @Override
    public CompletableFuture<Result<PasswordResetResponse, String>> execute() {
        return CompletableFuture.supplyAsync(() ->
            passwordService.resetPassword(resetToken, newPassword)
                .mapError(error -> "Password reset failed: " + error),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
