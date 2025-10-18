package com.trademaster.marketdata.security;

import com.trademaster.common.functional.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Security Mediator for Zero Trust operations
 *
 * Coordinates authentication validation, authorization, risk assessment, and audit logging.
 * Works with Spring Security but adds additional zero-trust validation layers.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityMediator {

    private static final Set<String> BLOCKED_IPS = ConcurrentHashMap.newKeySet();
    private static final Duration MAX_TOKEN_AGE = Duration.ofMinutes(30);

    /**
     * Mediate secure access with full zero-trust validation chain
     */
    public <T> Result<T, SecurityError> mediateAccess(
            SecurityContext context,
            Supplier<T> operation) {

        return validateAuthentication(context)
            .flatMap(this::validateTimestamp)
            .flatMap(this::assessRisk)
            .flatMap(ctx -> executeOperation(ctx, operation))
            .map(result -> auditSuccess(context, result))
            .onFailure(error -> auditFailure(context, error));
    }

    /**
     * Mediate access with specific role requirement
     */
    public <T> Result<T, SecurityError> mediateAccessWithRole(
            SecurityContext context,
            String requiredRole,
            Supplier<T> operation) {

        return validateAuthentication(context)
            .flatMap(this::validateTimestamp)
            .flatMap(ctx -> validateAuthorization(ctx, requiredRole))
            .flatMap(this::assessRisk)
            .flatMap(ctx -> executeOperation(ctx, operation))
            .map(result -> auditSuccess(context, result))
            .onFailure(error -> auditFailure(context, error));
    }

    /**
     * Validate authentication context
     */
    private Result<SecurityContext, SecurityError> validateAuthentication(SecurityContext context) {
        if (context == null) {
            log.warn("Security validation failed: null context");
            return Result.failure(SecurityError.INVALID_SECURITY_CONTEXT);
        }

        if (context.userId() == null || context.userId().isBlank()) {
            log.warn("Security validation failed: missing user ID");
            return Result.failure(SecurityError.AUTHENTICATION_FAILED);
        }

        if (context.roles() == null || context.roles().isEmpty()) {
            log.warn("Security validation failed: no roles for user {}", context.userId());
            return Result.failure(SecurityError.AUTHENTICATION_FAILED);
        }

        log.debug("Authentication validated for user: {}", context.userId());
        return Result.success(context);
    }

    /**
     * Validate request timestamp is not too old
     */
    private Result<SecurityContext, SecurityError> validateTimestamp(SecurityContext context) {
        Duration age = Duration.between(context.requestTime(), Instant.now());

        if (age.compareTo(MAX_TOKEN_AGE) > 0) {
            log.warn("Security validation failed: expired token for user {} (age: {})",
                context.userId(), age);
            return Result.failure(SecurityError.INVALID_TOKEN);
        }

        return Result.success(context);
    }

    /**
     * Validate authorization for specific role
     */
    private Result<SecurityContext, SecurityError> validateAuthorization(
            SecurityContext context,
            String requiredRole) {

        if (!context.hasRole(requiredRole)) {
            log.warn("Authorization failed: user {} lacks role {}",
                context.userId(), requiredRole);
            return Result.failure(SecurityError.AUTHORIZATION_FAILED);
        }

        log.debug("Authorization validated: user {} has role {}",
            context.userId(), requiredRole);
        return Result.success(context);
    }

    /**
     * Assess risk based on IP address and suspicious activity patterns
     */
    private Result<SecurityContext, SecurityError> assessRisk(SecurityContext context) {
        // Check if IP is blocked
        if (context.ipAddress() != null && BLOCKED_IPS.contains(context.ipAddress())) {
            log.warn("Risk assessment failed: blocked IP {} for user {}",
                context.ipAddress(), context.userId());
            return Result.failure(SecurityError.IP_BLOCKED);
        }

        // Additional risk checks can be added here (rate limiting, suspicious patterns, etc.)
        log.debug("Risk assessment passed for user: {}", context.userId());
        return Result.success(context);
    }

    /**
     * Execute operation with error handling
     */
    private <T> Result<T, SecurityError> executeOperation(
            SecurityContext context,
            Supplier<T> operation) {

        return Result.safely(
            operation::get,
            ex -> {
                log.error("Operation execution failed for user {}: {}",
                    context.userId(), ex.getMessage(), ex);
                return SecurityError.SUSPICIOUS_ACTIVITY;
            }
        );
    }

    /**
     * Audit successful operation
     */
    private <T> T auditSuccess(SecurityContext context, T result) {
        log.info("Security audit: SUCCESS - user={}, roles={}, ip={}, result={}",
            context.userId(),
            context.roles(),
            context.ipAddress(),
            result.getClass().getSimpleName());
        return result;
    }

    /**
     * Audit failed operation
     */
    private void auditFailure(SecurityContext context, SecurityError error) {
        log.warn("Security audit: FAILURE - user={}, roles={}, ip={}, error={}",
            context.userId(),
            context.roles(),
            context.ipAddress(),
            error.getMessage());
    }

    /**
     * Block an IP address
     */
    public void blockIp(String ipAddress) {
        BLOCKED_IPS.add(ipAddress);
        log.warn("IP address blocked: {}", ipAddress);
    }

    /**
     * Unblock an IP address
     */
    public void unblockIp(String ipAddress) {
        BLOCKED_IPS.remove(ipAddress);
        log.info("IP address unblocked: {}", ipAddress);
    }
}
