package com.trademaster.auth.command;

import com.trademaster.auth.dto.LogoutResponse;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.service.SessionManagementService;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Logout Command
 *
 * Encapsulates user logout operation with session invalidation.
 *
 * Features:
 * - Session invalidation
 * - Token revocation
 * - Device fingerprint cleanup
 * - Audit trail logging
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class LogoutCommand implements Command<LogoutResponse> {

    private final String userId;
    private final String sessionId;
    private final SessionManagementService sessionService;

    @Override
    public CompletableFuture<Result<LogoutResponse, String>> execute() {
        return CompletableFuture.supplyAsync(() ->
            sessionService.invalidateUserSession(userId, sessionId)
                .mapError(error -> "Logout failed: " + error),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
