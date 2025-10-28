package com.trademaster.auth.observer;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security Alert Observer
 *
 * Monitors authentication events for suspicious patterns and security threats.
 * Implements real-time threat detection and alerting.
 *
 * Features:
 * - Failed login attempt tracking
 * - Suspicious activity detection
 * - Password change pattern analysis
 * - MFA bypass attempt monitoring
 *
 * Security Patterns Detected:
 * - Multiple failed login attempts (brute force)
 * - Password changes without MFA (when MFA was enabled)
 * - Failed verification attempts (enumeration attacks)
 * - Forced logouts (potential account takeover)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAlertObserver implements EventObserver<AuthEvent> {

    private final Map<Long, AtomicInteger> failedLoginAttempts = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> failedVerificationAttempts = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS_THRESHOLD = 3;

    @Override
    public CompletableFuture<Result<Void, String>> onEvent(AuthEvent event) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                analyzeSecurityEvent(event);
                return null;
            }).fold(
                error -> Result.failure("Security analysis failed: " + error),
                ignored -> Result.success(null)
            )
        );
    }

    /**
     * Analyze event for security threats using pattern matching
     */
    private void analyzeSecurityEvent(AuthEvent event) {
        switch (event) {
            case LoginEvent login -> analyzeLogin(login);
            case LogoutEvent logout -> analyzeLogout(logout);
            case PasswordChangeEvent passwordChange -> analyzePasswordChange(passwordChange);
            case MfaEvent mfa -> analyzeMfa(mfa);
            case VerificationEvent verification -> analyzeVerification(verification);
        }
    }

    /**
     * Analyze login event for suspicious patterns
     */
    private void analyzeLogin(LoginEvent event) {
        // Reset failed attempts counter on successful login
        failedLoginAttempts.remove(event.userId());

        // Alert if login without MFA from new device
        String mfaWarning = event.mfaUsed() ? "" : " (NO MFA)";
        String deviceWarning = event.deviceFingerprint().isEmpty() ? " (NO DEVICE FINGERPRINT)" : "";

        if (!event.mfaUsed() || event.deviceFingerprint().isEmpty()) {
            log.warn("SECURITY ALERT: Login with weak security | userId={}, ip={}{}{}, correlation={}",
                event.userId(),
                event.ipAddress(),
                mfaWarning,
                deviceWarning,
                event.correlationId());
        }
    }

    /**
     * Analyze logout event for forced terminations
     */
    private void analyzeLogout(LogoutEvent event) {
        if (event.reason() == LogoutEvent.LogoutReason.SECURITY) {
            log.warn("SECURITY ALERT: Forced security logout | userId={}, sessionId={}, correlation={}",
                event.userId(),
                event.sessionId(),
                event.correlationId());
        }
    }

    /**
     * Analyze password change for security concerns
     */
    private void analyzePasswordChange(PasswordChangeEvent event) {
        if (!event.successful()) {
            int attempts = failedLoginAttempts.computeIfAbsent(event.userId(), k -> new AtomicInteger(0))
                .incrementAndGet();

            if (attempts >= MAX_FAILED_ATTEMPTS_THRESHOLD) {
                log.error("SECURITY ALERT: Multiple failed password changes | userId={}, attempts={}, ip={}, correlation={}",
                    event.userId(),
                    attempts,
                    event.ipAddress(),
                    event.correlationId());
            }
        } else {
            failedLoginAttempts.remove(event.userId());

            // Alert on admin-initiated or forced password changes
            if (event.changeType() == PasswordChangeEvent.ChangeType.ADMIN_RESET ||
                event.changeType() == PasswordChangeEvent.ChangeType.FORCED_RESET) {

                log.warn("SECURITY ALERT: {} password change | userId={}, ip={}, correlation={}",
                    event.changeType(),
                    event.userId(),
                    event.ipAddress(),
                    event.correlationId());
            }
        }
    }

    /**
     * Analyze MFA events for bypass attempts
     */
    private void analyzeMfa(MfaEvent event) {
        if (!event.successful()) {
            log.warn("SECURITY ALERT: Failed MFA {} | userId={}, type={}, correlation={}",
                event.action(),
                event.userId(),
                event.mfaType(),
                event.correlationId());
        } else if (event.action() == MfaEvent.MfaAction.DISABLED) {
            log.warn("SECURITY ALERT: MFA disabled | userId={}, sessionId={}, correlation={}",
                event.userId(),
                event.sessionId(),
                event.correlationId());
        }
    }

    /**
     * Analyze verification events for enumeration attacks
     */
    private void analyzeVerification(VerificationEvent event) {
        if (!event.successful()) {
            int attempts = failedVerificationAttempts.computeIfAbsent(event.userId(), k -> new AtomicInteger(0))
                .incrementAndGet();

            if (attempts >= MAX_FAILED_ATTEMPTS_THRESHOLD) {
                log.error("SECURITY ALERT: Multiple failed verification attempts | userId={}, attempts={}, ip={}, correlation={}",
                    event.userId(),
                    attempts,
                    event.ipAddress(),
                    event.correlationId());
            }
        } else {
            failedVerificationAttempts.remove(event.userId());
        }
    }

    /**
     * Get failed login attempts for user
     */
    public int getFailedLoginAttempts(Long userId) {
        AtomicInteger counter = failedLoginAttempts.get(userId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Get failed verification attempts for user
     */
    public int getFailedVerificationAttempts(Long userId) {
        AtomicInteger counter = failedVerificationAttempts.get(userId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Clear security tracking for user (for testing)
     */
    public void clearUserTracking(Long userId) {
        failedLoginAttempts.remove(userId);
        failedVerificationAttempts.remove(userId);
    }
}
