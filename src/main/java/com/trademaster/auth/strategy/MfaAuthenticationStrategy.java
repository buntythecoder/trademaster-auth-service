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
import com.trademaster.auth.service.MfaService;
import com.trademaster.auth.service.UserSecurityService;
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
 * MFA Authentication Strategy - SOLID Single Responsibility
 *
 * Handles Multi-Factor Authentication (MFA) using TOTP codes.
 * Requires both password and valid TOTP code for authentication.
 *
 * Authentication Flow:
 * 1. Validate MFA code presence
 * 2. Find user by email
 * 3. Authenticate password with Spring Security
 * 4. Verify TOTP code with MfaService
 * 5. Generate access + refresh tokens
 * 6. Update last activity
 * 7. Audit MFA authentication event
 *
 * Security Features:
 * - Two-factor authentication (password + TOTP)
 * - Time-based one-time passwords (TOTP)
 * - 30-second time window validation
 * - Failed MFA attempt tracking
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
public class MfaAuthenticationStrategy implements AuthenticationStrategy {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceFingerprintService deviceFingerprintService;
    private final AuditService auditService;
    private final MfaService mfaService;
    private final UserSecurityService userSecurityService;

    private static final String STRATEGY_NAME = "MFA";
    private static final int STRATEGY_PRIORITY = 70; // Higher priority than password

    @Override
    public CompletableFuture<Result<AuthenticationResponse, String>> authenticate(
            AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        log.debug("MFA authentication initiated for user: {}", request.getEmail());

        return CompletableFuture.supplyAsync(() ->
            createMfaAuthPipeline(request, httpRequest),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    @Override
    public boolean supports(AuthenticationRequest request, HttpServletRequest httpRequest) {
        // MFA strategy supports requests WITH MFA code present
        return Optional.ofNullable(request)
            .flatMap(req -> Optional.ofNullable(req.getMfaCode()))
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .isPresent();
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public int getPriority() {
        return STRATEGY_PRIORITY;
    }

    // ========== MFA Authentication Pipeline ==========

    /**
     * Functional MFA authentication pipeline using railway-oriented programming
     */
    private Result<AuthenticationResponse, String> createMfaAuthPipeline(
            AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        AuthenticationContext context = new AuthenticationContext(request, httpRequest);

        return validateMfaCodePresence().apply(context)
            .flatMap(findUserForMfa())
            .flatMap(authenticatePasswordForMfa())
            .flatMap(verifyMfaCode())
            .flatMap(generateMfaTokens())
            .map(auditMfaAuthentication());
    }

    /**
     * MFA code presence validation using Optional
     */
    private Function<AuthenticationContext, Result<AuthenticationContext, String>> validateMfaCodePresence() {
        return context -> Optional.ofNullable(context.request().getMfaCode())
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .map(code -> Result.<AuthenticationContext, String>success(context))
            .orElse(Result.<AuthenticationContext, String>failure("MFA code is required"));
    }

    /**
     * User lookup for MFA using Optional chains
     */
    private Function<Result<AuthenticationContext, String>, Result<MfaAuthContext, String>> findUserForMfa() {
        return result -> result.flatMap(context ->
            userRepository.findByEmailIgnoreCase(context.request().getEmail())
                .map(user -> Result.<MfaAuthContext, String>success(
                    new MfaAuthContext(context, user)))
                .orElseGet(() -> {
                    // Log failed MFA attempt for non-existent user
                    auditService.logAuthenticationEvent(
                        null,
                        "MFA_LOGIN_FAILED",
                        "FAILURE",
                        getClientIpAddress(context.httpRequest()),
                        getUserAgent(context.httpRequest()),
                        deviceFingerprintService.generateFingerprint(context.httpRequest()),
                        Map.of("reason", "user_not_found", "email", context.request().getEmail()),
                        null
                    );
                    return Result.<MfaAuthContext, String>failure("User not found");
                })
        );
    }

    /**
     * Password authentication for MFA using SafeOperations
     */
    private Function<Result<MfaAuthContext, String>, Result<MfaAuthContext, String>> authenticatePasswordForMfa() {
        return result -> result.flatMap(mfaContext ->
            SafeOperations.safelyToResult(() -> {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        mfaContext.authContext().request().getEmail(),
                        mfaContext.authContext().request().getPassword()
                    )
                );
                return authentication.isAuthenticated();
            })
            .flatMap(authenticated -> authenticated
                ? Result.success(mfaContext)
                : handlePasswordAuthenticationFailure(mfaContext))
        );
    }

    /**
     * Handle password authentication failure for MFA
     */
    private Result<MfaAuthContext, String> handlePasswordAuthenticationFailure(MfaAuthContext mfaContext) {
        userSecurityService.handleFailedLogin(
            mfaContext.user().getEmail(),
            getClientIpAddress(mfaContext.authContext().httpRequest()),
            deviceFingerprintService.generateFingerprint(mfaContext.authContext().httpRequest())
        );

        auditService.logAuthenticationEvent(
            mfaContext.user().getId(),
            "MFA_PASSWORD_VERIFICATION_FAILED",
            "FAILURE",
            getClientIpAddress(mfaContext.authContext().httpRequest()),
            getUserAgent(mfaContext.authContext().httpRequest()),
            deviceFingerprintService.generateFingerprint(mfaContext.authContext().httpRequest()),
            Map.of("reason", "invalid_password"),
            null
        );

        return Result.failure("Invalid credentials");
    }

    /**
     * MFA code verification using functional validation
     */
    private Function<Result<MfaAuthContext, String>, Result<MfaAuthContext, String>> verifyMfaCode() {
        return result -> result.flatMap(mfaContext -> {
            String mfaCode = mfaContext.authContext().request().getMfaCode().trim();
            Result<Boolean, String> mfaValidation = mfaService.verifyMfaCode(
                mfaContext.user().getId().toString(),
                mfaCode,
                "auth-session"
            );

            return mfaValidation.isSuccess() && mfaValidation.getValue().orElse(false)
                ? Result.<MfaAuthContext, String>success(mfaContext)
                : handleMfaVerificationFailure(mfaContext, "Invalid MFA code");
        });
    }

    /**
     * Handle MFA verification failure with audit logging
     */
    private Result<MfaAuthContext, String> handleMfaVerificationFailure(
            MfaAuthContext mfaContext,
            String errorMessage) {

        auditService.logAuthenticationEvent(
            mfaContext.user().getId(),
            "MFA_VERIFICATION_FAILED",
            "FAILURE",
            getClientIpAddress(mfaContext.authContext().httpRequest()),
            getUserAgent(mfaContext.authContext().httpRequest()),
            deviceFingerprintService.generateFingerprint(mfaContext.authContext().httpRequest()),
            Map.of("reason", errorMessage),
            null
        );

        log.warn("MFA verification failed for user {}: {}", mfaContext.user().getId(), errorMessage);

        return Result.failure(errorMessage);
    }

    /**
     * Generate MFA tokens using functional approach
     */
    private Function<Result<MfaAuthContext, String>, Result<TokenGenerationContext, String>> generateMfaTokens() {
        return result -> result.flatMap(mfaContext ->
            SafeOperations.safelyToResult(() -> {
                String deviceFingerprint = deviceFingerprintService.generateFingerprint(
                    mfaContext.authContext().httpRequest());
                String accessToken = jwtTokenProvider.generateToken(
                    mfaContext.user().getEmail(),
                    mfaContext.user().getId(),
                    deviceFingerprint,
                    false
                );
                String refreshToken = jwtTokenProvider.generateToken(
                    mfaContext.user().getEmail(),
                    mfaContext.user().getId(),
                    deviceFingerprint,
                    true
                );

                // Update user activity
                userSecurityService.updateLastActivity(
                    mfaContext.user().getId(),
                    getClientIpAddress(mfaContext.authContext().httpRequest()),
                    deviceFingerprint
                );

                AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .deviceFingerprint(deviceFingerprint)
                    .requiresMfa(false) // MFA already verified
                    .build();

                return new TokenGenerationContext(mfaContext, response);
            })
        );
    }

    /**
     * Audit MFA authentication success
     */
    private Function<TokenGenerationContext, AuthenticationResponse> auditMfaAuthentication() {
        return context -> {
            // Handle successful MFA login
            userSecurityService.handleSuccessfulLogin(
                context.mfaContext().user(),
                getClientIpAddress(context.mfaContext().authContext().httpRequest()),
                context.response().getDeviceFingerprint()
            );

            // Audit event
            auditService.logAuthenticationEvent(
                context.mfaContext().user().getId(),
                "MFA_LOGIN_SUCCESS",
                "SUCCESS",
                getClientIpAddress(context.mfaContext().authContext().httpRequest()),
                getUserAgent(context.mfaContext().authContext().httpRequest()),
                context.response().getDeviceFingerprint(),
                Map.of("strategy", STRATEGY_NAME, "mfaType", "TOTP"),
                null
            );

            log.info("MFA authentication successful for user: {} (strategy: {})",
                context.mfaContext().user().getEmail(), STRATEGY_NAME);

            return context.response();
        };
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
     * MFA authentication context record - immutable data holder
     */
    private record MfaAuthContext(
        AuthenticationContext authContext,
        User user
    ) {}

    /**
     * Token generation context record - immutable data holder
     */
    private record TokenGenerationContext(
        MfaAuthContext mfaContext,
        AuthenticationResponse response
    ) {}
}
