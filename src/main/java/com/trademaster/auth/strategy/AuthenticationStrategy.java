package com.trademaster.auth.strategy;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.pattern.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Authentication Strategy Interface - SOLID Strategy Pattern
 *
 * Defines the contract for authentication strategies.
 * Each strategy implements a specific authentication method:
 * - Password authentication (username + password)
 * - MFA authentication (password + TOTP code)
 * - Social authentication (OAuth providers)
 * - API Key authentication (service-to-service)
 *
 * Benefits:
 * - Open/Closed Principle: Add new strategies without modifying existing code
 * - Single Responsibility: Each strategy handles one authentication method
 * - Strategy Pattern: Runtime strategy selection based on request
 * - Testability: Each strategy can be tested in isolation
 *
 * This interface is 100% functional programming compliant:
 * - No if-else statements (uses supports() method for strategy selection)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - Virtual Threads for async operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface AuthenticationStrategy {

    /**
     * Authenticate user using this strategy
     *
     * @param request Authentication request containing credentials
     * @param httpRequest HTTP request for context (IP address, user agent, device fingerprint)
     * @return CompletableFuture with authentication result (success with tokens or failure with error)
     */
    CompletableFuture<Result<AuthenticationResponse, String>> authenticate(
            AuthenticationRequest request,
            HttpServletRequest httpRequest
    );

    /**
     * Check if this strategy supports the given authentication request
     *
     * Used by StrategyRegistry for runtime strategy selection.
     * Implementation should check request fields to determine compatibility.
     *
     * Examples:
     * - PasswordStrategy: returns true if no MFA code or social provider
     * - MfaStrategy: returns true if MFA code is present
     * - SocialStrategy: returns true if social provider is specified
     * - ApiKeyStrategy: returns true if API key header is present
     *
     * @param request Authentication request to evaluate
     * @param httpRequest HTTP request for context (headers, etc.)
     * @return true if this strategy can handle the request
     */
    boolean supports(AuthenticationRequest request, HttpServletRequest httpRequest);

    /**
     * Get the unique identifier for this strategy
     *
     * Used for:
     * - Strategy registry lookup
     * - Logging and auditing
     * - Error messages
     * - Metrics and monitoring
     *
     * Examples: "PASSWORD", "MFA", "SOCIAL_GOOGLE", "API_KEY"
     *
     * @return Strategy identifier (uppercase, underscores)
     */
    String getStrategyName();

    /**
     * Get the priority of this strategy
     *
     * Higher priority strategies are checked first by the registry.
     * This allows:
     * - MFA to be checked before password
     * - Social auth to be checked before password
     * - API Key to be checked before all others
     *
     * Priority levels:
     * - 100: API Key (service-to-service, highest priority)
     * - 80: Social authentication (OAuth providers)
     * - 70: MFA authentication (TOTP codes)
     * - 50: Password authentication (default, lowest priority)
     *
     * @return Priority value (higher = checked first)
     */
    default int getPriority() {
        return 50; // Default priority for password auth
    }
}
