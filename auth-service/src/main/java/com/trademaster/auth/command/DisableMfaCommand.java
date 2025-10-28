package com.trademaster.auth.command;

import com.trademaster.auth.dto.MfaDisableResponse;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.service.MfaService;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Disable MFA Command
 *
 * Encapsulates Multi-Factor Authentication removal operation.
 *
 * Features:
 * - MFA verification before disable
 * - Configuration cleanup
 * - Backup codes invalidation
 * - Security audit logging
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class DisableMfaCommand implements Command<MfaDisableResponse> {

    private final String userId;
    private final String sessionId;
    private final MfaService mfaService;

    @Override
    public CompletableFuture<Result<MfaDisableResponse, String>> execute() {
        return CompletableFuture.supplyAsync(() ->
            mfaService.disableMfa(userId, sessionId)
                .mapError(error -> "MFA disable failed: " + error),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
