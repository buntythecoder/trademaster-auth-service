package com.trademaster.auth.observer;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Metrics Observer
 *
 * Tracks authentication metrics for monitoring, analytics, and performance analysis.
 * Integrates with Prometheus via Micrometer for real-time metrics collection.
 *
 * Features:
 * - Login/logout event counting
 * - Password change tracking
 * - MFA usage metrics
 * - Verification success rates
 * - Pattern matching for metric categorization
 *
 * Metrics Collected:
 * - auth.login.total - Total login events
 * - auth.login.mfa - MFA-enabled logins
 * - auth.logout.total - Total logout events
 * - auth.password.change - Password change events
 * - auth.mfa.enabled - MFA enabled events
 * - auth.mfa.disabled - MFA disabled events
 * - auth.verification.success - Successful verifications
 * - auth.verification.failure - Failed verifications
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MetricsObserver implements EventObserver<AuthEvent> {

    private final MeterRegistry meterRegistry;

    @Override
    public CompletableFuture<Result<Void, String>> onEvent(AuthEvent event) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                recordMetrics(event);
                return null;
            }).fold(
                error -> Result.failure("Metrics recording failed: " + error),
                ignored -> Result.success(null)
            )
        );
    }

    /**
     * Record metrics using pattern matching
     */
    private void recordMetrics(AuthEvent event) {
        switch (event) {
            case LoginEvent login -> recordLoginMetrics(login);
            case LogoutEvent logout -> recordLogoutMetrics(logout);
            case PasswordChangeEvent passwordChange -> recordPasswordChangeMetrics(passwordChange);
            case MfaEvent mfa -> recordMfaMetrics(mfa);
            case VerificationEvent verification -> recordVerificationMetrics(verification);
        }
    }

    /**
     * Record login metrics
     */
    private void recordLoginMetrics(LoginEvent event) {
        incrementCounter("auth.login.total",
            "ip", event.ipAddress(),
            "mfa", String.valueOf(event.mfaUsed()));

        if (event.mfaUsed()) {
            incrementCounter("auth.login.mfa",
                "ip", event.ipAddress());
        }

        log.debug("Metrics recorded: login event for userId={}", event.userId());
    }

    /**
     * Record logout metrics
     */
    private void recordLogoutMetrics(LogoutEvent event) {
        incrementCounter("auth.logout.total",
            "reason", event.reason().name());

        log.debug("Metrics recorded: logout event for userId={}, reason={}",
            event.userId(), event.reason());
    }

    /**
     * Record password change metrics
     */
    private void recordPasswordChangeMetrics(PasswordChangeEvent event) {
        incrementCounter("auth.password.change",
            "type", event.changeType().name(),
            "successful", String.valueOf(event.successful()));

        log.debug("Metrics recorded: password change for userId={}, type={}, successful={}",
            event.userId(), event.changeType(), event.successful());
    }

    /**
     * Record MFA metrics
     */
    private void recordMfaMetrics(MfaEvent event) {
        String metricName = event.action() == MfaEvent.MfaAction.ENABLED
            ? "auth.mfa.enabled"
            : "auth.mfa.disabled";

        incrementCounter(metricName,
            "type", event.mfaType().name(),
            "successful", String.valueOf(event.successful()));

        log.debug("Metrics recorded: MFA {} for userId={}, type={}",
            event.action(), event.userId(), event.mfaType());
    }

    /**
     * Record verification metrics
     */
    private void recordVerificationMetrics(VerificationEvent event) {
        String metricName = event.successful()
            ? "auth.verification.success"
            : "auth.verification.failure";

        incrementCounter(metricName,
            "type", event.type().name());

        log.debug("Metrics recorded: verification {} for userId={}, type={}",
            event.successful() ? "success" : "failure",
            event.userId(),
            event.type());
    }

    /**
     * Increment counter with tags
     */
    private void incrementCounter(String name, String... tags) {
        Counter.builder(name)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
}
