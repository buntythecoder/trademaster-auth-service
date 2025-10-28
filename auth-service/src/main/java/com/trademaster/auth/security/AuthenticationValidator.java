package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Authentication Validator - Validates authentication credentials
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Zero Trust - Rule #6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationValidator {

    // Authentication constants
    private static final int MIN_USER_ID_LENGTH = 3;
    private static final int MAX_SESSION_AGE_HOURS = 24;

    /**
     * Validate authentication context using functional approach
     *
     * MANDATORY: No if-else - Rule #3
     * MANDATORY: Result pattern - Rule #11
     */
    public SecurityResult<SecurityContext> validate(SecurityContext context) {
        log.debug("Validating authentication for correlation: {}", context.correlationId());

        return validateBasicContext(context)
            .flatMap(this::validateUserAuthentication)
            .flatMap(this::validateSessionValidity)
            .flatMap(this::validateTimestamp);
    }

    /**
     * Basic context validation using functional chains
     */
    private SecurityResult<SecurityContext> validateBasicContext(SecurityContext context) {
        return Optional.ofNullable(context.correlationId())
            .filter(id -> !id.trim().isEmpty())
            .map(id -> SecurityResult.<SecurityContext>success(context))
            .orElse(SecurityResult.failure(SecurityError.CONTEXT_INVALID, "Missing correlation ID"));
    }

    /**
     * User authentication validation using pattern matching - Rule #14
     */
    private SecurityResult<SecurityContext> validateUserAuthentication(SecurityContext context) {
        return switch (evaluateAuthenticationStatus(context)) {
            case VALID -> SecurityResult.success(context);
            case MISSING_USER_ID -> SecurityResult.failure(SecurityError.AUTHENTICATION_FAILED, "User ID missing");
            case INVALID_USER_ID -> SecurityResult.failure(SecurityError.INVALID_CREDENTIALS, "User ID invalid");
            case NO_SESSION -> SecurityResult.failure(SecurityError.AUTHENTICATION_FAILED, "No valid session");
        };
    }

    /**
     * Session validity validation using functional approach
     */
    private SecurityResult<SecurityContext> validateSessionValidity(SecurityContext context) {
        return Optional.of(context.hasValidSession())
            .filter(valid -> valid)
            .map(valid -> SecurityResult.<SecurityContext>success(context))
            .orElse(SecurityResult.failure(SecurityError.AUTHENTICATION_FAILED, "Invalid session"));
    }

    /**
     * Timestamp validation using functional chains
     */
    private SecurityResult<SecurityContext> validateTimestamp(SecurityContext context) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(MAX_SESSION_AGE_HOURS);

        return Optional.ofNullable(context.timestamp())
            .filter(ts -> ts.isAfter(cutoff))
            .map(ts -> SecurityResult.<SecurityContext>success(context))
            .orElse(SecurityResult.failure(SecurityError.EXPIRED_CREDENTIALS, "Session expired"));
    }

    /**
     * Authentication status evaluation using functional pattern
     */
    private AuthenticationStatus evaluateAuthenticationStatus(SecurityContext context) {
        return Stream.of(
                new ValidationRule(() -> !context.isAuthenticated(), AuthenticationStatus.MISSING_USER_ID),
                new ValidationRule(() -> context.userId().length() < MIN_USER_ID_LENGTH, AuthenticationStatus.INVALID_USER_ID),
                new ValidationRule(() -> !context.hasValidSession(), AuthenticationStatus.NO_SESSION)
            )
            .filter(rule -> rule.condition().get())
            .map(ValidationRule::status)
            .findFirst()
            .orElse(AuthenticationStatus.VALID);
    }

    /**
     * Validation rule record for functional validation chains
     */
    private record ValidationRule(Supplier<Boolean> condition, AuthenticationStatus status) {}

    /**
     * Authentication status enumeration for pattern matching
     */
    private enum AuthenticationStatus {
        VALID,
        MISSING_USER_ID,
        INVALID_USER_ID,
        NO_SESSION
    }
}