package com.trademaster.auth.observer;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Audit Log Observer
 *
 * Observes all authentication events and creates comprehensive audit trail.
 * Implements structured logging for compliance and security monitoring.
 *
 * Features:
 * - Structured audit logging with correlation IDs
 * - Compliance-ready event tracking
 * - Pattern matching for event-specific details
 * - Async logging with virtual threads
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogObserver implements EventObserver<AuthEvent> {

    @Override
    public CompletableFuture<Result<Void, String>> onEvent(AuthEvent event) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                logAuditEvent(event);
                return null;
            }).fold(
                error -> Result.failure("Audit logging failed: " + error),
                ignored -> Result.success(null)
            )
        );
    }

    /**
     * Log audit event using pattern matching
     */
    private void logAuditEvent(AuthEvent event) {
        switch (event) {
            case LoginEvent login -> logLogin(login);
            case LogoutEvent logout -> logLogout(logout);
            case PasswordChangeEvent passwordChange -> logPasswordChange(passwordChange);
            case MfaEvent mfa -> logMfa(mfa);
            case VerificationEvent verification -> logVerification(verification);
        }
    }

    /**
     * Log login event with security details
     */
    private void logLogin(LoginEvent event) {
        log.info("AUDIT: User login | userId={}, sessionId={}, ip={}, mfa={}, correlation={}, timestamp={}",
            event.userId(),
            event.sessionId(),
            event.ipAddress(),
            event.mfaUsed(),
            event.correlationId(),
            event.timestamp());
    }

    /**
     * Log logout event with session termination reason
     */
    private void logLogout(LogoutEvent event) {
        log.info("AUDIT: User logout | userId={}, sessionId={}, reason={}, correlation={}, timestamp={}",
            event.userId(),
            event.sessionId(),
            event.reason(),
            event.correlationId(),
            event.timestamp());
    }

    /**
     * Log password change with security context
     */
    private void logPasswordChange(PasswordChangeEvent event) {
        log.info("AUDIT: Password change | userId={}, type={}, ip={}, successful={}, correlation={}, timestamp={}",
            event.userId(),
            event.changeType(),
            event.ipAddress(),
            event.successful(),
            event.correlationId(),
            event.timestamp());
    }

    /**
     * Log MFA event with configuration details
     */
    private void logMfa(MfaEvent event) {
        log.info("AUDIT: MFA {} | userId={}, type={}, sessionId={}, successful={}, correlation={}, timestamp={}",
            event.action(),
            event.userId(),
            event.mfaType(),
            event.sessionId(),
            event.successful(),
            event.correlationId(),
            event.timestamp());
    }

    /**
     * Log verification event with token tracking
     */
    private void logVerification(VerificationEvent event) {
        log.info("AUDIT: {} verification | userId={}, ip={}, successful={}, correlation={}, timestamp={}",
            event.type(),
            event.userId(),
            event.ipAddress(),
            event.successful(),
            event.correlationId(),
            event.timestamp());
    }
}
