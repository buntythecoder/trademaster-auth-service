package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Security Audit Logger - Logs security events for compliance
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Structured Logging - Rule #15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditLogger {

    /**
     * Log access attempt asynchronously using Virtual Threads
     *
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Structured Logging - Rule #15
     */
    public <T> CompletableFuture<Void> logAccess(SecurityResult<T> result) {
        return CompletableFuture.runAsync(
            () -> logAccessSync(result),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Log access attempt synchronously with structured logging
     *
     * MANDATORY: Correlation IDs - Rule #15
     * MANDATORY: Pattern matching - Rule #14
     */
    public <T> void logAccessSync(SecurityResult<T> result) {
        switch (result) {
            case SecurityResult.Success<T> success -> logSuccessfulAccess(success);
            case SecurityResult.Failure<T> failure -> logFailedAccess(failure);
        }
    }

    /**
     * Log successful access using structured logging
     */
    private <T> void logSuccessfulAccess(SecurityResult.Success<T> success) {
        success.context().ifPresentOrElse(
            context -> log.info("Security access successful: correlation={}, userId={}, timestamp={}",
                context.correlationId(),
                context.userId(),
                LocalDateTime.now()),
            () -> log.info("Security access successful: timestamp={}", LocalDateTime.now())
        );
    }

    /**
     * Log failed access using structured logging
     */
    private <T> void logFailedAccess(SecurityResult.Failure<T> failure) {
        failure.context().ifPresentOrElse(
            context -> log.warn("Security access failed: correlation={}, userId={}, error={}, message={}, timestamp={}",
                context.correlationId(),
                context.userId(),
                failure.error(),
                failure.message(),
                LocalDateTime.now()),
            () -> log.warn("Security access failed: error={}, message={}, timestamp={}",
                failure.error(),
                failure.message(),
                LocalDateTime.now())
        );

        // Log high-severity errors as security events
        logSecurityEvent(failure);
    }

    /**
     * Log security events for monitoring and alerting
     */
    private <T> void logSecurityEvent(SecurityResult.Failure<T> failure) {
        switch (failure.error().getCategory()) {
            case AUTHENTICATION -> logAuthenticationEvent(failure);
            case AUTHORIZATION -> logAuthorizationEvent(failure);
            case RISK -> logRiskEvent(failure);
            case AVAILABILITY -> logAvailabilityEvent(failure);
            default -> logGenericSecurityEvent(failure);
        }
    }

    private <T> void logAuthenticationEvent(SecurityResult.Failure<T> failure) {
        log.error("SECURITY_EVENT: Authentication failure - error={}, message={}, severity={}",
            failure.error(),
            failure.message(),
            failure.error().getSeverity());
    }

    private <T> void logAuthorizationEvent(SecurityResult.Failure<T> failure) {
        log.error("SECURITY_EVENT: Authorization failure - error={}, message={}, severity={}",
            failure.error(),
            failure.message(),
            failure.error().getSeverity());
    }

    private <T> void logRiskEvent(SecurityResult.Failure<T> failure) {
        log.warn("SECURITY_EVENT: Risk assessment failure - error={}, message={}, severity={}",
            failure.error(),
            failure.message(),
            failure.error().getSeverity());
    }

    private <T> void logAvailabilityEvent(SecurityResult.Failure<T> failure) {
        log.error("SECURITY_EVENT: Service availability issue - error={}, message={}, severity={}",
            failure.error(),
            failure.message(),
            failure.error().getSeverity());
    }

    private <T> void logGenericSecurityEvent(SecurityResult.Failure<T> failure) {
        log.error("SECURITY_EVENT: Generic security failure - error={}, message={}, severity={}",
            failure.error(),
            failure.message(),
            failure.error().getSeverity());
    }
}