package com.trademaster.auth.command;

import com.trademaster.auth.dto.EmailVerificationResponse;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.service.UserVerificationService;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Verify Email Command
 *
 * Encapsulates email verification operation.
 *
 * Features:
 * - Token validation
 * - Email verification status update
 * - Expiration checking
 * - Virtual threads for async execution
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class VerifyEmailCommand implements Command<EmailVerificationResponse> {

    private final String userId;
    private final String token;
    private final UserVerificationService verificationService;

    @Override
    public CompletableFuture<Result<EmailVerificationResponse, String>> execute() {
        return CompletableFuture.supplyAsync(() ->
            verificationService.verifyEmail(userId, token)
                .mapError(error -> "Email verification failed: " + error),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
