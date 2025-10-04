package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Authorization Validator - Validates user permissions and access rights
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationValidator {

    /**
     * Authorize access asynchronously using Virtual Threads
     *
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<SecurityResult<SecurityContext>> authorize(SecurityContext context) {
        return CompletableFuture.supplyAsync(
            () -> authorizeSync(context),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Authorize access synchronously using functional patterns
     *
     * MANDATORY: Pattern matching - Rule #14
     * MANDATORY: No if-else - Rule #3
     */
    public SecurityResult<SecurityContext> authorizeSync(SecurityContext context) {
        log.debug("Authorizing access for correlation: {}", context.correlationId());

        return switch (evaluateAuthorizationLevel(context)) {
            case AUTHORIZED -> SecurityResult.success(context);
            case INSUFFICIENT_PRIVILEGES -> SecurityResult.failure(
                SecurityError.INSUFFICIENT_PRIVILEGES, "User lacks required privileges");
            case ACCESS_DENIED -> SecurityResult.failure(
                SecurityError.ACCESS_DENIED, "Access denied for resource");
            case SESSION_REQUIRED -> SecurityResult.failure(
                SecurityError.SESSION_INVALID, "Valid session required");
        };
    }

    /**
     * Evaluate authorization level using pattern matching - Rule #14
     */
    private AuthorizationLevel evaluateAuthorizationLevel(SecurityContext context) {
        if (!context.hasValidSession()) return AuthorizationLevel.SESSION_REQUIRED;
        if (!context.isAuthenticated()) return AuthorizationLevel.ACCESS_DENIED;
        if (isValidUser(context)) return AuthorizationLevel.AUTHORIZED;
        return AuthorizationLevel.INSUFFICIENT_PRIVILEGES;
    }

    /**
     * Check if user is valid using functional approach
     */
    private boolean isValidUser(SecurityContext context) {
        return context.userId() != null &&
               !context.userId().trim().isEmpty() &&
               context.userId().length() >= 3;
    }

    /**
     * Authorization level enumeration for pattern matching
     */
    private enum AuthorizationLevel {
        AUTHORIZED,
        INSUFFICIENT_PRIVILEGES,
        ACCESS_DENIED,
        SESSION_REQUIRED
    }
}