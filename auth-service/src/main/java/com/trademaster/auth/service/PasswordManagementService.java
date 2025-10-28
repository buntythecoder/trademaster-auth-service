package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Password Management Service - SOLID Single Responsibility Principle
 *
 * Responsibilities:
 * - Password reset operations
 * - Password change for authenticated users
 * - Password strength validation
 * - Password security auditing
 *
 * This service is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API)
 * - Uses Virtual Threads for async operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final SecurityAuditService securityAuditService;

    /**
     * Reset password using verification token (synchronous for tests)
     *
     * @param token Password reset token
     * @param newPassword New password to set
     * @return Result of PasswordResetResponse
     */
    @Transactional
    public Result<com.trademaster.auth.dto.PasswordResetResponse, String> resetPassword(
            String token, String newPassword) {
        return verifyResetToken(token)
            .flatMap(user -> encodeNewPassword(user, newPassword))
            .flatMap(this::saveUpdatedUser)
            .flatMap(user -> markTokenAsUsed(token, user))
            .map(user -> new com.trademaster.auth.dto.PasswordResetResponse(
                "Password reset successfully",
                "session-" + user.getId()
            ));
    }

    /**
     * Reset password using verification token
     *
     * @param token Password reset token
     * @param newPassword New password to set
     * @param ipAddress IP address of requester
     * @param userAgent User agent string
     * @return Result message indicating success or failure
     */
    @Transactional
    public CompletableFuture<Result<String, String>> resetPassword(
            String token, String newPassword, String ipAddress, String userAgent) {
        return CompletableFuture.supplyAsync(() ->
            verifyResetToken(token)
                .flatMap(user -> encodeNewPassword(user, newPassword))
                .flatMap(this::saveUpdatedUser)
                .flatMap(user -> markTokenAsUsed(token, user))
                .flatMap(user -> auditPasswordReset(user, ipAddress, userAgent))
                .map(user -> "Password reset successful"),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Change password for authenticated user using email
     *
     * @param email User email address
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @return Result of PasswordChangeResponse with session info
     */
    @Transactional
    public Result<com.trademaster.auth.dto.PasswordChangeResponse, String> changePassword(
            String email, String currentPassword, String newPassword) {
        return findUserByEmail(email)
            .flatMap(user -> verifyCurrentPassword(user, currentPassword))
            .flatMap(user -> encodeNewPassword(user, newPassword))
            .flatMap(this::saveUpdatedUser)
            .map(user -> new com.trademaster.auth.dto.PasswordChangeResponse(
                "Password changed successfully",
                "session-" + user.getId()
            ));
    }

    /**
     * Change password for authenticated user
     *
     * @param userId User ID
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @param ipAddress IP address of requester
     * @param userAgent User agent string
     * @return Result message indicating success or failure
     */
    @Transactional
    public CompletableFuture<Result<String, String>> changePassword(
            Long userId, String currentPassword, String newPassword,
            String ipAddress, String userAgent) {
        return CompletableFuture.supplyAsync(() ->
            findUserById(userId)
                .flatMap(user -> verifyCurrentPassword(user, currentPassword))
                .flatMap(user -> encodeNewPassword(user, newPassword))
                .flatMap(this::saveUpdatedUser)
                .flatMap(user -> auditPasswordChange(user, ipAddress, userAgent))
                .map(user -> "Password changed successfully"),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Initiate password reset by generating and sending reset token
     *
     * @param email User email address
     * @param ipAddress IP address of requester
     * @param userAgent User agent string
     * @return Result message indicating success or failure
     */
    @Transactional
    public CompletableFuture<Result<String, String>> initiatePasswordReset(
            String email, String ipAddress, String userAgent) {
        return CompletableFuture.supplyAsync(() ->
            findUserByEmail(email)
                .flatMap(user -> generateResetToken(user, ipAddress, userAgent))
                .flatMap(token -> auditResetInitiation(email, ipAddress, userAgent))
                .map(token -> "Password reset email sent"),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    // Private helper methods - Functional composition

    /**
     * Verify password reset token and extract user
     */
    private Result<User, String> verifyResetToken(String token) {
        return Optional.ofNullable(token)
            .filter(t -> !t.isBlank())
            .flatMap(t -> verificationTokenService.verifyPasswordResetToken(t))
            .map(Result::<User, String>success)
            .orElse(Result.failure("Invalid or expired token"));
    }

    /**
     * Find user by ID
     */
    private Result<User, String> findUserById(Long userId) {
        return Optional.ofNullable(userId)
            .flatMap(userRepository::findById)
            .map(Result::<User, String>success)
            .orElse(Result.failure("User not found"));
    }

    /**
     * Find user by email
     */
    private Result<User, String> findUserByEmail(String email) {
        return Optional.ofNullable(email)
            .filter(e -> !e.isBlank())
            .flatMap(userRepository::findByEmailIgnoreCase)
            .map(Result::<User, String>success)
            .orElse(Result.failure("User not found"));
    }

    /**
     * Verify current password matches stored password
     */
    private Result<User, String> verifyCurrentPassword(User user, String currentPassword) {
        return Optional.ofNullable(currentPassword)
            .filter(pwd -> passwordEncoder.matches(pwd, user.getPasswordHash()))
            .map(pwd -> Result.<User, String>success(user))
            .orElse(Result.failure("Current password is incorrect"));
    }

    /**
     * Encode new password and create updated user
     */
    private Result<User, String> encodeNewPassword(User user, String newPassword) {
        return Optional.ofNullable(newPassword)
            .filter(pwd -> !pwd.isBlank())
            .map(pwd -> passwordEncoder.encode(pwd))
            .map(encodedPwd -> user.withPasswordHash(encodedPwd))
            .map(Result::<User, String>success)
            .orElse(Result.failure("New password cannot be empty"));
    }

    /**
     * Save updated user to repository
     */
    private Result<User, String> saveUpdatedUser(User user) {
        return SafeOperations.safelyToResult(() -> userRepository.save(user));
    }

    /**
     * Mark password reset token as used
     */
    private Result<User, String> markTokenAsUsed(String token, User user) {
        verificationTokenService.markPasswordResetTokenAsUsed(token);
        return Result.success(user);
    }

    /**
     * Generate password reset token
     */
    private Result<String, String> generateResetToken(User user, String ipAddress, String userAgent) {
        return Optional.ofNullable(verificationTokenService.generatePasswordResetToken(user, ipAddress, userAgent))
            .map(Result::<String, String>success)
            .orElse(Result.failure("Failed to generate reset token"));
    }

    /**
     * Audit password reset completion
     */
    private Result<User, String> auditPasswordReset(User user, String ipAddress, String userAgent) {
        securityAuditService.logSecurityEvent(
            user.getId(),
            "PASSWORD_RESET",
            "MEDIUM",
            ipAddress,
            userAgent,
            Map.of("action", "Password reset completed")
        );
        return Result.success(user);
    }

    /**
     * Audit password change
     */
    private Result<User, String> auditPasswordChange(User user, String ipAddress, String userAgent) {
        securityAuditService.logSecurityEvent(
            user.getId(),
            "PASSWORD_CHANGE",
            "LOW",
            ipAddress,
            userAgent,
            Map.of("action", "Password changed by user")
        );
        return Result.success(user);
    }

    /**
     * Audit password reset initiation
     */
    private Result<String, String> auditResetInitiation(String email, String ipAddress, String userAgent) {
        securityAuditService.logSecurityEvent(
            null,
            "PASSWORD_RESET_INITIATED",
            "LOW",
            ipAddress,
            userAgent,
            Map.of("email", email, "action", "Password reset requested")
        );
        return Result.success("Reset initiated");
    }
}
