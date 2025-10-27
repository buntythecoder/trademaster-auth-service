package com.trademaster.auth.command;

import com.trademaster.auth.dto.MfaSetupResponse;
import com.trademaster.auth.entity.MfaConfiguration.MfaType;
import com.trademaster.auth.pattern.Command;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import com.trademaster.auth.service.MfaService;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

/**
 * Enable MFA Command
 *
 * Encapsulates Multi-Factor Authentication setup operation.
 *
 * Features:
 * - TOTP secret generation
 * - QR code generation
 * - Backup codes creation
 * - Security audit logging
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
public class EnableMfaCommand implements Command<MfaSetupResponse> {

    private final String userId;
    private final String sessionId;
    private final MfaService mfaService;

    @Override
    public CompletableFuture<Result<MfaSetupResponse, String>> execute() {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                var config = mfaService.setupTotpMfa(userId, sessionId);
                var qrCodeUrl = mfaService.generateQrCodeUrl(userId, config.secretKey());
                var backupCodes = mfaService.generateBackupCodes(userId, sessionId);

                return MfaSetupResponse.builder()
                    .message("MFA setup successful")
                    .mfaType(config.mfaType().toString())
                    .secretKey(config.secretKey())
                    .qrCodeUrl(qrCodeUrl)
                    .backupCodes(backupCodes)
                    .build();
            }).mapError(error -> "MFA setup failed: " + error)
        );
    }
}
