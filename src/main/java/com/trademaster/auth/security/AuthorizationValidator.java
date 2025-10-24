package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
     * Evaluate authorization level using functional pattern
     */
    private AuthorizationLevel evaluateAuthorizationLevel(SecurityContext context) {
        return Stream.of(
                Map.entry(() -> !context.hasValidSession(), AuthorizationLevel.SESSION_REQUIRED),
                Map.entry(() -> !context.isAuthenticated(), AuthorizationLevel.ACCESS_DENIED),
                Map.entry(() -> isValidUser(context), AuthorizationLevel.AUTHORIZED)
            )
            .filter(entry -> entry.getKey().get())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(AuthorizationLevel.INSUFFICIENT_PRIVILEGES);
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