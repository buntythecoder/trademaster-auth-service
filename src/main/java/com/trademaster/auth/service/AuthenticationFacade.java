package com.trademaster.auth.service;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Authentication Facade - SOLID Facade Pattern
 *
 * Unified interface for all authentication operations.
 * Coordinates between specialized authentication services:
 * - TokenManagementService: Token lifecycle
 * - UserRegistrationService: User registration
 * - PasswordManagementService: Password operations
 * - AuthenticationService: Core authentication
 *
 * This facade provides:
 * - Single entry point for authentication operations
 * - Simplified API for controllers
 * - Service coordination and orchestration
 * - Consistent error handling
 *
 * Benefits:
 * - Reduces controller complexity
 * - Hides service implementation details
 * - Enables service composition
 * - Maintains backward compatibility
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFacade {

    private final AuthenticationService authenticationService;
    private final TokenManagementService tokenManagementService;
    private final UserRegistrationService userRegistrationService;
    private final PasswordManagementService passwordManagementService;

    // ========== Authentication Operations ==========

    /**
     * Authenticate user with credentials
     *
     * @param request Authentication request containing credentials
     * @param httpRequest HTTP request for context
     * @return CompletableFuture with authentication result
     */
    public CompletableFuture<Result<AuthenticationResponse, String>> authenticate(
            AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.debug("Facade: Delegating authentication to AuthenticationService");
        return authenticationService.authenticate(request, httpRequest);
    }

    /**
     * Login user (alias for authenticate)
     *
     * @param request Authentication request
     * @param httpRequest HTTP request for context
     * @return Result with authentication response
     */
    public Result<AuthenticationResponse, String> login(
            AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.debug("Facade: Delegating login to AuthenticationService");
        return authenticationService.login(request, httpRequest);
    }

    /**
     * Logout user and invalidate session
     *
     * @param token Access token
     * @param sessionId Session identifier
     * @param ipAddress IP address
     */
    public void logout(String token, String sessionId, String ipAddress) {
        log.debug("Facade: Delegating logout to AuthenticationService");
        authenticationService.logout(token, sessionId, ipAddress);
    }

    /**
     * Complete MFA verification
     *
     * @param userId User identifier
     * @param mfaCode MFA verification code
     * @param ipAddress IP address
     * @param userAgent User agent string
     * @return Result with authentication response
     */
    public Result<AuthenticationResponse, String> completeMfaVerification(
            Long userId, String mfaCode, String ipAddress, String userAgent) {
        log.debug("Facade: Delegating MFA verification to AuthenticationService");
        return authenticationService.completeMfaVerification(userId, mfaCode, ipAddress, userAgent);
    }

    // ========== Token Operations ==========

    /**
     * Generate tokens for authenticated user
     *
     * @param user Authenticated user
     * @return CompletableFuture with authentication response
     */
    public CompletableFuture<Result<AuthenticationResponse, String>> generateTokens(User user) {
        log.debug("Facade: Delegating token generation to TokenManagementService");
        return tokenManagementService.generateTokens(user);
    }

    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken Refresh token
     * @param request HTTP request
     * @return CompletableFuture with new authentication response
     */
    public CompletableFuture<Result<AuthenticationResponse, String>> refreshAccessToken(
            String refreshToken, HttpServletRequest request) {
        log.debug("Facade: Delegating token refresh to TokenManagementService");
        return tokenManagementService.refreshAccessToken(refreshToken, request);
    }

    /**
     * Validate token and extract user information
     *
     * @param token JWT token
     * @return Result with user ID or error
     */
    public Result<Long, String> validateToken(String token) {
        log.debug("Facade: Delegating token validation to TokenManagementService");
        return tokenManagementService.validateToken(token);
    }

    /**
     * Revoke token
     *
     * @param token Token to revoke
     * @return CompletableFuture with result
     */
    public CompletableFuture<Result<Void, String>> revokeToken(String token) {
        log.debug("Facade: Delegating token revocation to TokenManagementService");
        return tokenManagementService.revokeToken(token);
    }

    /**
     * Check if token is revoked
     *
     * @param token Token to check
     * @return true if revoked
     */
    public boolean isTokenRevoked(String token) {
        log.debug("Facade: Delegating token revocation check to TokenManagementService");
        return tokenManagementService.isTokenRevoked(token);
    }

    // ========== Registration Operations ==========

    /**
     * Register new user
     *
     * @param request Registration request
     * @return CompletableFuture with created user or error
     */
    public CompletableFuture<Result<User, String>> registerUser(RegistrationRequest request) {
        log.debug("Facade: Delegating user registration to UserRegistrationService");
        return userRegistrationService.registerUser(request);
    }

    /**
     * Register new user (sync version)
     *
     * @param request Registration request
     * @param httpRequest HTTP request for context
     * @return Result with created user or error
     */
    public Result<User, String> register(RegistrationRequest request, HttpServletRequest httpRequest) {
        log.debug("Facade: Delegating user registration (sync) to UserRegistrationService");
        return userRegistrationService.register(request, httpRequest);
    }

    // ========== Password Operations ==========

    /**
     * Reset password using token
     *
     * @param token Password reset token
     * @param newPassword New password
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return CompletableFuture with result message
     */
    public CompletableFuture<Result<String, String>> resetPassword(
            String token, String newPassword, String ipAddress, String userAgent) {
        log.debug("Facade: Delegating password reset to PasswordManagementService");
        return passwordManagementService.resetPassword(token, newPassword, ipAddress, userAgent);
    }

    /**
     * Reset password using token (sync version)
     *
     * @param token Password reset token
     * @param newPassword New password
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return Result message
     */
    public String resetPasswordSync(String token, String newPassword, String ipAddress, String userAgent) {
        log.debug("Facade: Delegating password reset (sync) to PasswordManagementService");
        return passwordManagementService.resetPassword(token, newPassword, ipAddress, userAgent);
    }

    /**
     * Change password for authenticated user
     *
     * @param userId User ID
     * @param currentPassword Current password
     * @param newPassword New password
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return CompletableFuture with result message
     */
    public CompletableFuture<Result<String, String>> changePassword(
            Long userId, String currentPassword, String newPassword,
            String ipAddress, String userAgent) {
        log.debug("Facade: Delegating password change to PasswordManagementService");
        return passwordManagementService.changePassword(userId, currentPassword, newPassword, ipAddress, userAgent);
    }

    /**
     * Initiate password reset process
     *
     * @param email User email
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return CompletableFuture with result message
     */
    public CompletableFuture<Result<String, String>> initiatePasswordReset(
            String email, String ipAddress, String userAgent) {
        log.debug("Facade: Delegating password reset initiation to PasswordManagementService");
        return passwordManagementService.initiatePasswordReset(email, ipAddress, userAgent);
    }

    // ========== Composite Operations ==========

    /**
     * Register user and generate tokens in one operation
     *
     * @param request Registration request
     * @return CompletableFuture with authentication response
     */
    public CompletableFuture<Result<AuthenticationResponse, String>> registerAndGenerateTokens(
            RegistrationRequest request) {
        log.debug("Facade: Coordinating registration and token generation");
        return userRegistrationService.registerUser(request)
            .thenCompose(result -> result.match(
                user -> tokenManagementService.generateTokens(user),
                error -> CompletableFuture.completedFuture(Result.failure(error))
            ));
    }

    /**
     * Authenticate and generate tokens in one operation
     *
     * @param request Authentication request
     * @param httpRequest HTTP request
     * @return CompletableFuture with authentication response
     */
    public CompletableFuture<Result<AuthenticationResponse, String>> authenticateAndGenerateTokens(
            AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.debug("Facade: Coordinating authentication and token generation");
        return authenticationService.authenticate(request, httpRequest);
    }
}
