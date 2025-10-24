package com.trademaster.auth.strategy;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.UserRepository;
import com.trademaster.auth.security.DeviceFingerprintService;
import com.trademaster.auth.security.JwtTokenProvider;
import com.trademaster.auth.service.AuditService;
import com.trademaster.auth.service.UserSecurityService;
import com.trademaster.auth.validator.AuthenticationValidators;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Password Authentication Strategy - SOLID Single Responsibility
 *
 * Handles traditional username/password authentication.
 * This is the default authentication method with the lowest priority.
 *
 * Authentication Flow:
 * 1. Validate request (email + password required)
 * 2. Find user by email
 * 3. Authenticate credentials with Spring Security
 * 4. Check account status (enabled, not locked, not expired)
 * 5. Generate access + refresh tokens
 * 6. Update last activity
 * 7. Audit authentication event
 *
 * Security Features:
 * - Bcrypt password hashing
 * - Failed login tracking
 * - Account locking after 5 failed attempts
 * - Device fingerprinting
 * - Comprehensive audit logging
 *
 * This strategy is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API)
 * - Virtual Threads for async operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceFingerprintService deviceFingerprintService;
    private final AuditService auditService;
    private final UserSecurityService userSecurityService;

    private static final String STRATEGY_NAME = "PASSWORD";
    private static final int STRATEGY_PRIORITY = 50; // Lowest priority (default)

    @Override
    public CompletableFuture<Result<AuthenticationResponse, String>> authenticate(
            AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Password authentication initiated for user: {}", request.getEmail());

        return CompletableFuture.supplyAsync(() ->
            createPasswordAuthPipeline(request, httpRequest),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    @Override
    public boolean supports(AuthenticationRequest request, HttpServletRequest httpRequest) {
        // Password strategy supports requests WITHOUT MFA code and WITHOUT social provider
        // This is the fallback/default authentication method
        return Optional.ofNullable(request)
            .map(req -> Optional.ofNullable(req.getMfaCode()).isEmpty() &&
                        Optional.ofNullable(req.getSocialProvider()).isEmpty() &&
                        Optional.ofNullable(req.getEmail()).isPresent() &&
                        Optional.ofNullable(req.getPassword()).isPresent())
            .orElse(false);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public int getPriority() {
        return STRATEGY_PRIORITY;
    }

    // ========== Authentication Pipeline ==========

    /**
     * Functional authentication pipeline using railway-oriented programming
     */
    private Result<AuthenticationResponse, String> createPasswordAuthPipeline(
            AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        AuthenticationContext context = new AuthenticationContext(request, httpRequest);

        return validateAuthenticationRequest().apply(context)
            .flatMap(findUserByEmail())
            .flatMap(authenticateCredentials())
            .flatMap(checkAccountStatus())
            .flatMap(generateTokens())
            .map(auditAuthentication());
    }

    /**
     * Authentication request validation
     */
    private Function<AuthenticationContext, Result<AuthenticationContext, String>> validateAuthenticationRequest() {
        return context -> AuthenticationValidators.validateAuthenticationRequest
            .validate(context.request())
            .map(validRequest -> context);
    }

    /**
     * User lookup using Optional - replaces if-else
     */
    private Function<Result<AuthenticationContext, String>, Result<AuthenticatedUserContext, String>> findUserByEmail() {
        return result -> result.flatMap(context ->
            userRepository.findByEmailIgnoreCase(context.request().getEmail())
                .map(user -> Result.<AuthenticatedUserContext, String>success(
                    new AuthenticatedUserContext(context, user)))
                .orElseGet(() -> {
                    // Log failed login for non-existent user
                    userSecurityService.handleFailedLogin(
                        context.request().getEmail(),
                        getClientIpAddress(context.httpRequest()),
                        deviceFingerprintService.generateFingerprint(context.httpRequest())
                    );
                    return Result.<AuthenticatedUserContext, String>failure("User not found");
                })
        );
    }

    /**
     * Credential authentication using SafeOperations - replaces try-catch
     */
    private Function<Result<AuthenticatedUserContext, String>, Result<AuthenticatedUserContext, String>> authenticateCredentials() {
        return result -> result.flatMap(context ->
            SafeOperations.safelyToResult(() -> {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        context.authContext().request().getEmail(),
                        context.authContext().request().getPassword()
                    )
                );
                return authentication.isAuthenticated();
            })
            .flatMap(authenticated -> authenticated
                ? Result.success(context)
                : handleAuthenticationFailure(context))
        );
    }

    /**
     * Handle authentication failure with proper logging
     */
    private Result<AuthenticatedUserContext, String> handleAuthenticationFailure(AuthenticatedUserContext context) {
        userSecurityService.handleFailedLogin(
            context.user().getEmail(),
            getClientIpAddress(context.authContext().httpRequest()),
            deviceFingerprintService.generateFingerprint(context.authContext().httpRequest())
        );
        return Result.failure("Invalid credentials");
    }

    /**
     * Account status validation using functional chains
     */
    private Function<Result<AuthenticatedUserContext, String>, Result<AuthenticatedUserContext, String>> checkAccountStatus() {
        return result -> result.flatMap(context ->
            validateAccountEnabled(context.user())
                .flatMap(this::validateAccountNonLocked)
                .flatMap(this::validateAccountNonExpired)
                .map(user -> context)
        );
    }

    /**
     * Token generation using function composition
     */
    private Function<Result<AuthenticatedUserContext, String>, Result<TokenGenerationContext, String>> generateTokens() {
        return result -> result.flatMap(context ->
            SafeOperations.safelyToResult(() -> {
                String deviceFingerprint = deviceFingerprintService.generateFingerprint(
                    context.authContext().httpRequest());
                String accessToken = jwtTokenProvider.generateToken(
                    context.user().getEmail(),
                    context.user().getId(),
                    deviceFingerprint,
                    false
                );
                String refreshToken = jwtTokenProvider.generateToken(
                    context.user().getEmail(),
                    context.user().getId(),
                    deviceFingerprint,
                    true
                );

                // Update last activity
                userSecurityService.updateLastActivity(
                    context.user().getId(),
                    getClientIpAddress(context.authContext().httpRequest()),
                    deviceFingerprint
                );

                AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .deviceFingerprint(deviceFingerprint)
                    .requiresMfa(context.user().isMfaEnabled())
                    .build();

                return new TokenGenerationContext(context, response);
            })
        );
    }

    /**
     * Authentication auditing using functional approach
     */
    private Function<TokenGenerationContext, AuthenticationResponse> auditAuthentication() {
        return context -> {
            // Handle successful login
            userSecurityService.handleSuccessfulLogin(
                context.userContext().user(),
                getClientIpAddress(context.userContext().authContext().httpRequest()),
                context.response().getDeviceFingerprint()
            );

            // Audit event
            auditService.logAuthenticationEvent(
                context.userContext().user().getId(),
                "PASSWORD_LOGIN_SUCCESS",
                "SUCCESS",
                getClientIpAddress(context.userContext().authContext().httpRequest()),
                getUserAgent(context.userContext().authContext().httpRequest()),
                context.response().getDeviceFingerprint(),
                Map.of("strategy", STRATEGY_NAME, "tokenType", context.response().getTokenType()),
                null
            );

            log.info("Password authentication successful for user: {} (strategy: {})",
                context.userContext().user().getEmail(), STRATEGY_NAME);

            return context.response();
        };
    }

    // ========== Validation Helpers ==========

    private Result<User, String> validateAccountEnabled(User user) {
        return Optional.of(user.isEnabled())
            .filter(enabled -> enabled)
            .map(enabled -> Result.<User, String>success(user))
            .orElse(Result.<User, String>failure("Account is disabled"));
    }

    private Result<User, String> validateAccountNonLocked(User user) {
        return Optional.of(user.isAccountNonLocked())
            .filter(unlocked -> unlocked)
            .map(unlocked -> Result.<User, String>success(user))
            .orElse(Result.<User, String>failure("Account is locked"));
    }

    private Result<User, String> validateAccountNonExpired(User user) {
        return Optional.of(user.isAccountNonExpired())
            .filter(notExpired -> notExpired)
            .map(notExpired -> Result.<User, String>success(user))
            .orElse(Result.<User, String>failure("Account is expired"));
    }

    // ========== Utility Methods ==========

    private String getClientIpAddress(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(ip -> !ip.isEmpty())
            .orElse(Optional.ofNullable(request.getRemoteAddr()).orElse("unknown"));
    }

    private String getUserAgent(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown");
    }

    // ========== Immutable Context Records (Rule #9) ==========

    /**
     * Authentication context record - immutable data holder
     */
    private record AuthenticationContext(
        AuthenticationRequest request,
        HttpServletRequest httpRequest
    ) {}

    /**
     * Authenticated user context record - immutable data holder
     */
    private record AuthenticatedUserContext(
        AuthenticationContext authContext,
        User user
    ) {}

    /**
     * Token generation context record - immutable data holder
     */
    private record TokenGenerationContext(
        AuthenticatedUserContext userContext,
        AuthenticationResponse response
    ) {}
}
