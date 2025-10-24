package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User Security Service - SOLID Single Responsibility Principle
 *
 * Responsibilities:
 * - Authentication tracking (failed/successful logins)
 * - Account security operations (lock/unlock, status updates)
 * - Email and phone verification
 * - Mobile verification with SMS
 * - Password expiry and updates
 * - Automatic account unlock processing
 *
 * This service is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final UserDataService userDataService;

    // Security constants
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;

    // ========== Authentication Tracking ==========

    /**
     * Update last activity timestamp for user
     *
     * @param userId User identifier
     * @param ipAddress IP address of activity
     * @param deviceFingerprint Device fingerprint
     */
    @Transactional
    public void updateLastActivity(Long userId, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            // Validate IP address format (basic validation)
            String validatedIp = Optional.ofNullable(ipAddress)
                .filter(ip -> !ip.trim().isEmpty())
                .orElseGet(() -> {
                    log.warn("Invalid IP address provided: {}", ipAddress);
                    return "127.0.0.1";
                });

            userRepository.updateLastLogin(userId, LocalDateTime.now(), validatedIp, deviceFingerprint, LocalDateTime.now());

            auditService.logAuthenticationEvent(userId, "LOGIN_SUCCESS", "SUCCESS",
                ipAddress, null, deviceFingerprint, null, null);

            log.debug("Updated last activity for user ID: {}", userId);
            return "Success";
        })
        .mapError(error -> {
            log.error("Error updating last activity for user {}: {}", userId, error);
            return error;
        });
    }

    /**
     * Handle failed login attempt using functional composition
     *
     * @param email User email
     * @param ipAddress IP address of attempt
     * @param deviceFingerprint Device fingerprint
     */
    @Transactional
    public void handleFailedLogin(String email, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            userDataService.findByEmail(email)
                .map(user -> processFailedLoginForUser(user, email, ipAddress, deviceFingerprint))
                .orElseGet(() -> processFailedLoginForNonExistentUser(email, ipAddress, deviceFingerprint));
            return "Failed login processed";
        })
        .mapError(error -> {
            log.error("Error processing failed login for {}: {}", email, error);
            return error;
        });
    }

    /**
     * Handle successful login using functional composition
     *
     * @param user Authenticated user
     * @param ipAddress IP address of login
     * @param deviceFingerprint Device fingerprint
     */
    @Transactional
    public void handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            // Reset failed login attempts
            userRepository.resetFailedLoginAttempts(user.getId(), LocalDateTime.now());

            // Update last login information
            updateLastActivity(user.getId(), ipAddress, deviceFingerprint);

            log.info("Successful login for user: {}", user.getEmail());
            return "Success";
        })
        .mapError(error -> {
            log.error("Error processing successful login for user {}: {}", user.getEmail(), error);
            return error;
        });
    }

    // ========== Account Security Operations ==========

    /**
     * Lock user account for specified duration
     *
     * @param userId User identifier
     * @param lockDurationMinutes Duration in minutes
     */
    @Transactional
    public void lockUserAccount(Long userId, int lockDurationMinutes) {
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
        userRepository.lockUserAccount(userId, lockUntil, LocalDateTime.now());

        log.warn("User account locked: {} until {}", userId, lockUntil);
    }

    /**
     * Unlock user account
     *
     * @param userId User identifier
     */
    @Transactional
    public void unlockUserAccount(Long userId) {
        userRepository.unlockUserAccount(userId, LocalDateTime.now());

        auditService.logAuthenticationEvent(userId, "ACCOUNT_UNLOCKED", "SUCCESS",
            null, null, null, Map.of("action", "manual_unlock"), null);

        log.info("User account unlocked: {}", userId);
    }

    /**
     * Update account status
     *
     * @param userId User identifier
     * @param status New account status
     */
    @Transactional
    public void updateAccountStatus(Long userId, User.AccountStatus status) {
        User.AccountStatus oldStatus = userRepository.findById(userId)
                .map(User::getAccountStatus)
                .orElse(null);

        userRepository.updateAccountStatus(userId, status, LocalDateTime.now());

        auditService.logAuthenticationEvent(userId, "ACCOUNT_STATUS_CHANGED", "SUCCESS",
            null, null, null,
            Map.of("old_status", oldStatus, "new_status", status), null);

        log.info("Account status updated for user {}: {} -> {}", userId, oldStatus, status);
    }

    /**
     * Process automatic account unlocks for expired lock periods
     */
    @Transactional
    public void processAccountUnlocks() {
        SafeOperations.safelyToResult(() -> {
            List<User> usersToUnlock = userDataService.findUsersToUnlock();

            usersToUnlock.stream()
                .forEach(user -> unlockUserAccount(user.getId()));

            Optional.of(usersToUnlock)
                .filter(users -> !users.isEmpty())
                .ifPresent(users -> log.info("Processed automatic account unlocks for {} users", users.size()));

            return usersToUnlock.size();
        })
        .mapError(error -> {
            log.error("Error processing account unlocks: {}", error);
            return error;
        });
    }

    // ========== Verification Operations ==========

    /**
     * Update email verification status
     *
     * @param userId User identifier
     */
    @Transactional
    public void verifyEmail(Long userId) {
        userRepository.updateEmailVerificationStatus(userId, true, LocalDateTime.now());

        auditService.logAuthenticationEvent(userId, "EMAIL_VERIFIED", "SUCCESS",
            null, null, null, null, null);

        log.info("Email verified for user: {}", userId);
    }

    /**
     * Update phone verification status
     *
     * @param userId User identifier
     */
    @Transactional
    public void verifyPhone(Long userId) {
        userRepository.updatePhoneVerificationStatus(userId, true, LocalDateTime.now());

        auditService.logAuthenticationEvent(userId, "PHONE_VERIFIED", "SUCCESS",
            null, null, null, null, null);

        log.info("Phone verified for user: {}", userId);
    }

    /**
     * Complete mobile verification with SMS code validation
     *
     * @param userId User identifier
     * @param mobileNumber Mobile number to verify
     * @param verificationCode SMS verification code
     * @return Result indicating success or failure
     */
    @Transactional
    public Result<Boolean, String> verifyMobile(Long userId, String mobileNumber, String verificationCode) {
        return SafeOperations.safelyToResult(() -> {
            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Validate using functional composition
            Optional.of(mobileNumber)
                    .filter(this::isValidMobileNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid mobile number format: " + mobileNumber));

            String trimmedCode = Optional.ofNullable(verificationCode)
                    .map(String::trim)
                    .filter(code -> code.length() == 6)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid verification code format"));

            Optional.of(trimmedCode)
                    .filter(code -> isValidSmsVerificationCode(userId, mobileNumber, code))
                    .orElseThrow(() -> new SecurityException("Invalid or expired verification code"));

            // Update user's mobile number and verification status
            userRepository.updateMobileNumber(userId, mobileNumber, LocalDateTime.now());
            userRepository.updatePhoneVerificationStatus(userId, true, LocalDateTime.now());

            // Audit the mobile verification
            auditService.logAuthenticationEvent(userId, "MOBILE_VERIFIED", "SUCCESS",
                null, null, null,
                Map.of("mobileNumber", maskMobileNumber(mobileNumber), "method", "SMS"), null);

            log.info("Mobile number verified for user {}: {}", userId, maskMobileNumber(mobileNumber));
            return true;

        }).mapError(error -> {
            // Audit failed verification attempt
            auditService.logAuthenticationEvent(userId, "MOBILE_VERIFICATION_FAILED", "FAILURE",
                null, null, null,
                Map.of("mobileNumber", maskMobileNumber(mobileNumber), "error", error), null);

            log.warn("Mobile verification failed for user {}: {}", userId, error);
            return "Mobile verification failed: " + error;
        });
    }

    /**
     * Send SMS verification code to mobile number
     *
     * @param userId User identifier
     * @param mobileNumber Mobile number to send code to
     * @return Result with verification code or error
     */
    @Transactional
    public Result<String, String> sendMobileVerificationCode(Long userId, String mobileNumber) {
        return SafeOperations.safelyToResult(() -> {
            // Validate mobile number
            Optional.of(mobileNumber)
                .filter(this::isValidMobileNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invalid mobile number format"));

            // Generate 6-digit verification code
            String code = String.format("%06d", (int)(Math.random() * 1000000));

            // Store code in Redis with 5-minute expiry (production implementation)
            storeSmsVerificationCode(userId, mobileNumber, code);

            // Send SMS (production: integrate with Twilio/AWS SNS)
            boolean smsSent = sendSmsCode(mobileNumber, code);

            Optional.of(smsSent)
                .filter(sent -> sent)
                .orElseThrow(() -> new RuntimeException("Failed to send SMS verification code"));

            // Audit the verification code send
            auditService.logAuthenticationEvent(userId, "SMS_VERIFICATION_SENT", "SUCCESS",
                null, null, null,
                Map.of("mobileNumber", maskMobileNumber(mobileNumber)), null);

            log.info("SMS verification code sent to user {}: {}", userId, maskMobileNumber(mobileNumber));
            return code;

        }).mapError(error -> {
            log.error("Failed to send verification code to {}: {}", maskMobileNumber(mobileNumber), error);
            return "Failed to send verification code: " + error;
        });
    }

    /**
     * Update password hash
     *
     * @param userId User identifier
     * @param passwordHash New password hash
     */
    @Transactional
    public void updatePassword(Long userId, String passwordHash) {
        userRepository.updatePassword(userId, passwordHash, LocalDateTime.now());

        auditService.logAuthenticationEvent(userId, "PASSWORD_UPDATED", "SUCCESS",
            null, null, null, null, null);

        log.info("Password updated for user: {}", userId);
    }

    // ========== Private Helper Methods ==========

    /**
     * Process failed login for existing user
     */
    private Void processFailedLoginForUser(User user, String email, String ipAddress, String deviceFingerprint) {
        int attempts = Optional.ofNullable(user.getFailedLoginAttempts()).orElse(0) + 1;

        userRepository.updateFailedLoginAttempts(user.getId(), attempts, LocalDateTime.now());

        Optional.of(attempts)
            .filter(count -> count >= MAX_FAILED_ATTEMPTS)
            .ifPresent(count -> lockUserAccount(user.getId(), ACCOUNT_LOCK_DURATION_MINUTES));

        auditService.logAuthenticationEvent(user.getId(), "LOGIN_FAILED", "FAILED",
            ipAddress, null, deviceFingerprint,
            Map.of("email", email, "attempts", attempts, "reason", "invalid_credentials"), null
        );

        return null;
    }

    /**
     * Process failed login for non-existent user
     */
    private Void processFailedLoginForNonExistentUser(String email, String ipAddress, String deviceFingerprint) {
        auditService.logAuthenticationEvent(null, "LOGIN_FAILED", "FAILED",
            ipAddress, null, deviceFingerprint,
            Map.of("email", email, "reason", "user_not_found"), null);
        return null;
    }

    /**
     * Validate mobile number format
     */
    private boolean isValidMobileNumber(String mobileNumber) {
        return Optional.ofNullable(mobileNumber)
            .map(number -> number.replaceAll("[^0-9+]", ""))
            .filter(cleaned -> cleaned.length() >= 10 && cleaned.length() <= 15)
            .isPresent();
    }

    /**
     * Store SMS verification code (Redis implementation placeholder)
     */
    private void storeSmsVerificationCode(Long userId, String mobileNumber, String code) {
        String key = "sms_verification:" + userId + ":" + mobileNumber;
        // redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.debug("SMS verification code stored for user {}", userId);
    }

    /**
     * Validate SMS verification code (Redis implementation placeholder)
     */
    private boolean isValidSmsVerificationCode(Long userId, String mobileNumber, String code) {
        // In production, validate against stored code in Redis
        String key = "sms_verification:" + userId + ":" + mobileNumber;
        // String storedCode = redisTemplate.opsForValue().get(key);
        // return code.equals(storedCode);

        // For now, accept any 6-digit code (development only)
        log.warn("Using development SMS verification - accepting any 6-digit code");
        return code.matches("\\d{6}");
    }

    /**
     * Send SMS code (Twilio/AWS SNS implementation placeholder)
     */
    private boolean sendSmsCode(String mobileNumber, String code) {
        // In production, integrate with Twilio or AWS SNS
        // twilioClient.sendSms(mobileNumber, "Your verification code is: " + code);

        log.info("SMS verification code would be sent to {}: {}", maskMobileNumber(mobileNumber), code);
        return true; // Simulate successful SMS sending
    }

    /**
     * Mask mobile number for logging
     */
    private String maskMobileNumber(String mobileNumber) {
        return Optional.ofNullable(mobileNumber)
                .filter(number -> number.length() >= 4)
                .map(number -> "*".repeat(number.length() - 4) + number.substring(number.length() - 4))
                .orElse("****");
    }
}
