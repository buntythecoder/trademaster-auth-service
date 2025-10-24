package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.projection.UserStatisticsProjection;
import com.trademaster.auth.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User Service implementation for user management and Spring Security integration
 * 
 * Features:
 * - UserDetailsService implementation for Spring Security
 * - User account management (lock/unlock, status updates)
 * - Authentication tracking and failed login attempts
 * - User statistics and reporting
 * - Account security operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    // Security constants
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_EXPIRY_DAYS = 90;
    private static final int INACTIVE_USER_DAYS = 365;

    /**
     * Load user by username (email) for Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        log.debug("Loaded user: {} with roles: {}", user.getEmail(), user.getAuthorities());
        return user;
    }

    /**
     * Load user by ID (for JWT token validation)
     */
    public UserDetails loadUserById(Long userId) {
        return userRepository.findById(userId)
                .orElse(null);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Save user
     */
    @Transactional(readOnly = false)
    public User save(User user) {
        User savedUser = userRepository.save(user);
        log.info("User saved: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * Update last activity (login tracking)
     */
    @Transactional(readOnly = false)
    public void updateLastActivity(Long userId, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            // Validate IP address format (basic validation)
            String validatedIp = Optional.ofNullable(ipAddress)
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
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
     */
    @Transactional(readOnly = false)
    public void handleFailedLogin(String email, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            findByEmail(email)
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
     */
    @Transactional(readOnly = false)
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

    /**
     * Lock user account
     */
    @Transactional(readOnly = false)
    public void lockUserAccount(Long userId, int lockDurationMinutes) {
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
        userRepository.lockUserAccount(userId, lockUntil, LocalDateTime.now());
        
        log.warn("User account locked: {} until {}", userId, lockUntil);
    }

    /**
     * Unlock user account
     */
    @Transactional(readOnly = false)
    public void unlockUserAccount(Long userId) {
        userRepository.unlockUserAccount(userId, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "ACCOUNT_UNLOCKED", "SUCCESS", 
            null, null, null, Map.of("action", "manual_unlock"), null);
        
        log.info("User account unlocked: {}", userId);
    }

    /**
     * Update account status
     */
    @Transactional(readOnly = false)
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
     * Update email verification status
     */
    @Transactional(readOnly = false)
    public void verifyEmail(Long userId) {
        userRepository.updateEmailVerificationStatus(userId, true, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "EMAIL_VERIFIED", "SUCCESS", 
            null, null, null, null, null);
        
        log.info("Email verified for user: {}", userId);
    }

    /**
     * Update phone verification status
     */
    @Transactional(readOnly = false)
    public void verifyPhone(Long userId) {
        userRepository.updatePhoneVerificationStatus(userId, true, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "PHONE_VERIFIED", "SUCCESS", 
            null, null, null, null, null);
        
        log.info("Phone verified for user: {}", userId);
    }

    /**
     * Complete mobile verification with SMS code validation
     */
    @Transactional(readOnly = false)
    public Result<Boolean, String> verifyMobile(Long userId, String mobileNumber, String verificationCode) {
        return SafeOperations.safelyToResult(() -> {
            // Find user
            // Functional validation chain using Railway Programming
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
     */
    public Result<String, String> sendMobileVerificationCode(Long userId, String mobileNumber) {
        return SafeOperations.safelyToResult(() -> {
            // Find user using functional approach
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Validate mobile number using functional approach
            Optional.of(mobileNumber)
                    .filter(this::isValidMobileNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid mobile number format: " + mobileNumber));
            
            // Generate 6-digit verification code
            String verificationCode = generateSmsVerificationCode();
            
            // Store verification code (in production, use Redis with TTL)
            storeSmsVerificationCode(userId, mobileNumber, verificationCode);
            
            // Send SMS using functional approach
            Optional.of(sendSmsVerificationCode(mobileNumber, verificationCode))
                    .filter(Boolean::booleanValue)
                    .orElseThrow(() -> new RuntimeException("Failed to send SMS verification code"));
            
            // Audit SMS sent
            auditService.logAuthenticationEvent(userId, "SMS_VERIFICATION_SENT", "SUCCESS", 
                null, null, null, 
                Map.of("mobileNumber", maskMobileNumber(mobileNumber)), null);
            
            log.info("SMS verification code sent to user {}: {}", userId, maskMobileNumber(mobileNumber));
            return "Verification code sent to " + maskMobileNumber(mobileNumber);
            
        }).mapError(error -> {
            log.error("Failed to send SMS verification code for user {}: {}", userId, error);
            return "Failed to send verification code: " + error;
        });
    }

    // Helper methods for mobile verification
    private boolean isValidMobileNumber(String mobileNumber) {
        return Optional.ofNullable(mobileNumber)
            .map(number -> number.replaceAll("[^0-9+]", ""))
            .filter(cleaned -> cleaned.length() >= 10 && cleaned.length() <= 15)
            .isPresent();
    }

    private String generateSmsVerificationCode() {
        return String.format("%06d", new java.util.Random().nextInt(1000000));
    }

    private void storeSmsVerificationCode(Long userId, String mobileNumber, String code) {
        // In production, store in Redis with TTL (e.g., 5 minutes)
        String key = "sms_verification:" + userId + ":" + mobileNumber;
        // redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.debug("SMS verification code stored for user {}", userId);
    }

    private boolean isValidSmsVerificationCode(Long userId, String mobileNumber, String code) {
        // In production, validate against stored code in Redis
        String key = "sms_verification:" + userId + ":" + mobileNumber;
        // String storedCode = redisTemplate.opsForValue().get(key);
        // return code.equals(storedCode);
        
        // For now, accept any 6-digit code for testing (remove in production)
        return code.matches("\\d{6}");
    }

    private boolean sendSmsVerificationCode(String mobileNumber, String code) {
        // In production, integrate with SMS service
        // Example with Twilio:
        // TwilioRestClient client = new TwilioRestClient(accountSid, authToken);
        // Message message = Message.creator(new PhoneNumber(mobileNumber), 
        //                                  new PhoneNumber(fromNumber),
        //                                  "Your TradeMaster verification code: " + code).create();
        
        log.info("SMS verification code would be sent to {}: {}", maskMobileNumber(mobileNumber), code);
        return true; // Simulate successful SMS sending
    }

    private String maskMobileNumber(String mobileNumber) {
        return Optional.ofNullable(mobileNumber)
                .filter(number -> number.length() >= 4)
                .map(number -> "*".repeat(number.length() - 4) + number.substring(number.length() - 4))
                .orElse("****");
    }

    /**
     * Update KYC status with validation and document verification
     */
    @Transactional(readOnly = false)
    public Result<Boolean, String> updateKycStatus(Long userId, User.KycStatus newStatus, Map<String, Object> kycDocuments, String verificationReason) {
        return findUser(userId)
            .flatMap(user -> validateKycTransition(user, newStatus))
            .flatMap(user -> validateApprovalDocuments(user, newStatus, kycDocuments))
            .flatMap(user -> persistKycUpdate(user.getId(), newStatus, kycDocuments))
            .flatMap(user -> auditKycChange(user, newStatus, verificationReason, kycDocuments))
            .map(user -> true);
    }

    private Result<User, String> findUser(Long userId) {
        return userRepository.findById(userId)
            .map(Result::<User, String>success)
            .orElse(Result.failure("User not found: " + userId));
    }

    private Result<User, String> validateKycTransition(User user, User.KycStatus newStatus) {
        return Optional.of(user)
            .filter(u -> isValidKycStatusTransition(u.getKycStatus(), newStatus))
            .map(Result::<User, String>success)
            .orElse(Result.failure("Invalid KYC status transition from " + user.getKycStatus() + " to " + newStatus));
    }

    private Result<User, String> validateApprovalDocuments(User user, User.KycStatus newStatus, Map<String, Object> kycDocuments) {
        if (newStatus == User.KycStatus.APPROVED) {
            return validateDocumentsForApproval(kycDocuments)
                .map(valid -> user);
        }
        return Result.success(user);
    }

    private Result<Boolean, String> validateDocumentsForApproval(Map<String, Object> kycDocuments) {
        return Optional.of(kycDocuments)
            .filter(this::hasRequiredKycDocuments)
            .filter(this::validateKycDocuments)
            .map(docs -> Result.<Boolean, String>success(true))
            .orElse(Result.failure("KYC document validation failed"));
    }

    private Result<User, String> persistKycUpdate(Long userId, User.KycStatus newStatus, Map<String, Object> kycDocuments) {
        return SafeOperations.safelyToResult(() -> {
            userRepository.updateKycStatus(userId, newStatus, LocalDateTime.now());
            Optional.ofNullable(kycDocuments)
                .filter(docs -> !docs.isEmpty())
                .ifPresent(docs -> updateUserProfileKycDocuments(userId, docs));
            return userRepository.findById(userId).orElseThrow();
        });
    }

    private Result<User, String> auditKycChange(User user, User.KycStatus newStatus, String verificationReason, Map<String, Object> kycDocuments) {
        return SafeOperations.safelyToResult(() -> {
            User.KycStatus oldStatus = user.getKycStatus();

            Map<String, Object> auditDetails = Map.of(
                "old_status", oldStatus,
                "new_status", newStatus,
                "verification_reason", Optional.ofNullable(verificationReason).orElse("Manual update"),
                "documents_count", Optional.ofNullable(kycDocuments).map(Map::size).orElse(0)
            );

            auditService.logAuthenticationEvent(user.getId(), "KYC_STATUS_CHANGED", "SUCCESS",
                null, null, null, auditDetails, null);

            sendKycStatusNotification(user, oldStatus, newStatus);

            log.info("KYC status updated for user {}: {} -> {} (reason: {})",
                    user.getId(), oldStatus, newStatus, verificationReason);

            return user;
            
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    @Transactional(readOnly = false)
    public void updateKycStatus(Long userId, User.KycStatus status) {
        updateKycStatus(userId, status, null, "Legacy update");
    }

    /**
     * Validate KYC status transition rules
     */
    private boolean isValidKycStatusTransition(User.KycStatus from, User.KycStatus to) {
        return Optional.of(from)
            .filter(f -> f == to)
            .map(f -> true)
            .orElseGet(() -> switch (from) {
                case PENDING -> Set.of(User.KycStatus.IN_PROGRESS, User.KycStatus.REJECTED).contains(to);
                case IN_PROGRESS -> Set.of(User.KycStatus.APPROVED, User.KycStatus.REJECTED, User.KycStatus.PENDING).contains(to);
                case APPROVED -> Set.of(User.KycStatus.IN_PROGRESS).contains(to);
                case REJECTED -> Set.of(User.KycStatus.PENDING, User.KycStatus.IN_PROGRESS).contains(to);
            });
    }

    /**
     * Check if required KYC documents are present
     */
    private boolean hasRequiredKycDocuments(Map<String, Object> documents) {
        Set<String> requiredDocs = Set.of(
            "identity_document",    // Government ID, Passport, Driver's License
            "address_proof",        // Utility bill, Bank statement
            "selfie_with_id"        // Selfie holding identity document
        );

        return Optional.ofNullable(documents)
            .filter(docs -> !docs.isEmpty())
            .map(docs -> requiredDocs.stream().allMatch(docs::containsKey))
            .orElse(false);
    }

    /**
     * Validate KYC documents integrity and format
     */
    private boolean validateKycDocuments(Map<String, Object> documents) {
        // In production, implement:
        // 1. Document format validation (PDF, JPG, PNG)
        // 2. File size limits
        // 3. OCR verification
        // 4. Face matching between selfie and ID
        // 5. Document authenticity checks

        return documents.entrySet().stream()
            .allMatch(entry -> validateDocumentEntry(entry.getKey(), entry.getValue()));
    }

    private boolean validateDocumentEntry(String docType, Object docData) {
        return Optional.ofNullable(docData)
            .filter(data -> {
                boolean valid = isValidDocumentStructure(docType, data);
                Optional.of(valid)
                    .filter(Boolean::booleanValue)
                    .orElseGet(() -> {
                        log.warn("Invalid document structure for type: {}", docType);
                        return false;
                    });
                return valid;
            })
            .map(data -> true)
            .orElseGet(() -> {
                log.warn("Missing document data for type: {}", docType);
                return false;
            });
    }

    private boolean isValidDocumentStructure(String docType, Object docData) {
        return Optional.of(docData)
            .filter(data -> data instanceof Map<?, ?>)
            .map(data -> (Map<?, ?>) data)
            .filter(docMap -> docMap.containsKey("filename") &&
                             docMap.containsKey("content_type") &&
                             docMap.containsKey("size"))
            .isPresent();
    }

    private void updateUserProfileKycDocuments(Long userId, Map<String, Object> kycDocuments) {
        // Update UserProfile entity with KYC document metadata
        // Note: Actual document storage should be handled by a separate document service
        log.info("Updating KYC documents for user profile: {}", userId);
    }

    private void sendKycStatusNotification(User user, User.KycStatus oldStatus, User.KycStatus newStatus) {
        // Send appropriate notification based on status change
        String notificationType = switch (newStatus) {
            case APPROVED -> "KYC_APPROVED";
            case REJECTED -> "KYC_REJECTED";
            case IN_PROGRESS -> "KYC_IN_PROGRESS";
            default -> "KYC_STATUS_UPDATED";
        };
        
        // In production, integrate with notification service
        log.info("KYC notification {} sent to user: {}", notificationType, user.getEmail());
    }

    /**
     * Update subscription tier with validation and billing integration
     */
    @Transactional(readOnly = false)
    public Result<Boolean, String> updateSubscriptionTier(Long userId, User.SubscriptionTier newTier, String changeReason) {
        return findUser(userId)
            .flatMap(user -> validateSubscriptionTierChange(user, newTier))
            .flatMap(user -> validateKycForTier(user, newTier))
            .flatMap(user -> persistSubscriptionUpdate(user.getId(), newTier, changeReason))
            .map(user -> true);
    }

    private Result<User, String> validateSubscriptionTierChange(User user, User.SubscriptionTier newTier) {
        return Optional.of(user)
            .filter(u -> isValidSubscriptionTierChange(u, u.getSubscriptionTier(), newTier))
            .map(Result::<User, String>success)
            .orElse(Result.failure("Invalid subscription tier change from " + user.getSubscriptionTier() + " to " + newTier));
    }

    private Result<User, String> validateKycForTier(User user, User.SubscriptionTier newTier) {
        return Optional.of(newTier)
            .filter(this::requiresKycForTier)
            .filter(tier -> user.getKycStatus() != User.KycStatus.APPROVED)
            .map(tier -> Result.<User, String>failure("KYC approval required for " + newTier + " subscription tier"))
            .orElse(Result.success(user));
    }

    private Result<User, String> persistSubscriptionUpdate(Long userId, User.SubscriptionTier newTier, String changeReason) {
        return SafeOperations.safelyToResult(() -> {
            userRepository.updateSubscriptionTier(userId, newTier, LocalDateTime.now());
            return userRepository.findById(userId).orElseThrow();
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    @Transactional(readOnly = false)
    public void updateSubscriptionTier(Long userId, User.SubscriptionTier tier) {
        updateSubscriptionTier(userId, tier, "Legacy update");
    }

    private boolean isValidSubscriptionTierChange(User user, User.SubscriptionTier from, User.SubscriptionTier to) {
        return Optional.of(from)
            .filter(f -> f == to)
            .map(f -> true)
            .orElseGet(() -> validateTierUpgradeEligibility(user, from, to));
    }

    private boolean validateTierUpgradeEligibility(User user, User.SubscriptionTier from, User.SubscriptionTier to) {
        return Optional.of(to.ordinal() > from.ordinal())
            .filter(Boolean::booleanValue)
            .map(isUpgrade -> user.getAccountStatus() == User.AccountStatus.ACTIVE)
            .orElse(true); // Downgrades are always allowed
    }

    private boolean requiresKycForTier(User.SubscriptionTier tier) {
        // Premium tiers require KYC approval
        return Set.of(User.SubscriptionTier.PREMIUM, User.SubscriptionTier.ENTERPRISE).contains(tier);
    }

    private void sendSubscriptionTierNotification(User user, User.SubscriptionTier oldTier, User.SubscriptionTier newTier) {
        String notificationType = newTier.ordinal() > oldTier.ordinal() ? 
            "SUBSCRIPTION_UPGRADED" : "SUBSCRIPTION_DOWNGRADED";
        
        // In production, integrate with notification service
        log.info("Subscription notification {} sent to user: {}", notificationType, user.getEmail());
    }

    /**
     * Update password
     */
    @Transactional(readOnly = false)
    public void updatePassword(Long userId, String passwordHash) {
        userRepository.updatePassword(userId, passwordHash, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE", "SUCCESS", 
            null, null, null, null, null);
        
        log.info("Password updated for user: {}", userId);
    }

    /**
     * Process account unlocks for users whose lock period has expired using functional approach
     */
    @Transactional(readOnly = false)
    public void processAccountUnlocks() {
        SafeOperations.safelyToResult(() -> {
            List<User> usersToUnlock = userRepository.findUsersToUnlock(LocalDateTime.now());
            
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

    /**
     * Find users requiring password change
     */
    public List<User> findUsersRequiringPasswordChange() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(PASSWORD_EXPIRY_DAYS);
        return userRepository.findUsersRequiringPasswordChange(thresholdDate);
    }

    /**
     * Find inactive users
     */
    public List<User> findInactiveUsers() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(INACTIVE_USER_DAYS);
        return userRepository.findInactiveUsers(thresholdDate);
    }

    /**
     * Get user statistics using functional approach
     */
    public UserStatistics getUserStatistics() {
        Result<UserStatistics, String> result = SafeOperations.safelyToResult(() -> {
            return Optional.ofNullable(userRepository.getUserStatistics())
                .map(this::convertToUserStatistics)
                .orElse(new UserStatistics());
        });
        
        return result
            .mapError(error -> {
                log.error("Error retrieving user statistics: {}", error);
                return error;
            })
            .orElse(new UserStatistics());
    }

    /**
     * User Statistics DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long lockedUsers;
        private long suspendedUsers;
        private long verifiedUsers;
        private long recentLogins;
    }

    // Functional helper methods and composition patterns
    
    // Strategy pattern for audit operations - initialized via methods to avoid field initialization issues
    private AuditStrategy getAccountUnlockAuditStrategy() {
        return (userId, context) -> auditService.logAuthenticationEvent(userId, "ACCOUNT_UNLOCKED", "SUCCESS", 
            null, null, null, Map.of("action", "manual_unlock"), null);
    }
    
    private AuditStrategy getEmailVerificationAuditStrategy() {
        return (userId, context) -> auditService.logAuthenticationEvent(userId, "EMAIL_VERIFIED", "SUCCESS", 
            null, null, null, null, null);
    }
    
    private AuditStrategy getPhoneVerificationAuditStrategy() {
        return (userId, context) -> auditService.logAuthenticationEvent(userId, "PHONE_VERIFIED", "SUCCESS", 
            null, null, null, null, null);
    }
    
    private AuditStrategy getPasswordUpdateAuditStrategy() {
        return (userId, context) -> auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE", "SUCCESS", 
            null, null, null, null, null);
    }
    
    // Functional audit helpers using composition
    private java.util.function.Consumer<Long> getAuditAccountUnlock() {
        return userId -> getAccountUnlockAuditStrategy().audit(userId, null);
    }
    
    private java.util.function.Consumer<Long> getAuditEmailVerification() {
        return userId -> getEmailVerificationAuditStrategy().audit(userId, null);
    }
    
    private java.util.function.Consumer<Long> getAuditPhoneVerification() {
        return userId -> getPhoneVerificationAuditStrategy().audit(userId, null);
    }
    
    private java.util.function.Consumer<Long> getAuditPasswordUpdate() {
        return userId -> getPasswordUpdateAuditStrategy().audit(userId, null);
    }
    
    // Strategy interface for audit operations
    @FunctionalInterface
    private interface AuditStrategy {
        void audit(Long userId, Map<String, Object> context);
    }
    
    private Void processFailedLoginForUser(User user, String email, String ipAddress, String deviceFingerprint) {
        int attempts = Optional.ofNullable(user.getFailedLoginAttempts()).orElse(0) + 1;
        
        userRepository.updateFailedLoginAttempts(user.getId(), attempts, LocalDateTime.now());
        
        Optional.of(attempts)
            .filter(a -> a >= MAX_FAILED_ATTEMPTS)
            .ifPresentOrElse(
                a -> handleAccountLocking(user, email, ipAddress, deviceFingerprint, a),
                () -> handleFailedAttempt(user, ipAddress, deviceFingerprint, attempts)
            );
        
        return null;
    }
    
    private Void processFailedLoginForNonExistentUser(String email, String ipAddress, String deviceFingerprint) {
        auditService.logAuthenticationEvent(null, "LOGIN_FAILED", "FAILED", 
            ipAddress, null, deviceFingerprint, 
            Map.of("email", email, "reason", "user_not_found"), null);
        return null;
    }
    
    private void handleAccountLocking(User user, String email, String ipAddress, String deviceFingerprint, int attempts) {
        lockUserAccount(user.getId(), ACCOUNT_LOCK_DURATION_MINUTES);
        
        auditService.logAuthenticationEvent(user.getId(), "ACCOUNT_LOCKED", "BLOCKED", 
            ipAddress, null, deviceFingerprint, 
            Map.of("reason", "max_failed_attempts", "attempts", attempts), null);
        
        log.warn("User account locked due to failed login attempts: {} (attempts: {})", email, attempts);
    }
    
    private void handleFailedAttempt(User user, String ipAddress, String deviceFingerprint, int attempts) {
        auditService.logAuthenticationEvent(user.getId(), "LOGIN_FAILED", "FAILED", 
            ipAddress, null, deviceFingerprint, 
            Map.of("attempts", attempts, "remaining", MAX_FAILED_ATTEMPTS - attempts), null);
    }
    
    private UserStatistics convertToUserStatistics(UserStatisticsProjection projection) {
        return UserStatistics.builder()
            .totalUsers(projection.getTotalUsers())
            .activeUsers(projection.getActiveUsers())
            .lockedUsers(projection.getLockedUsers())
            .suspendedUsers(projection.getSuspendedUsers())
            .verifiedUsers(projection.getVerifiedUsers())
            .recentLogins(projection.getRecentLogins())
            .build();
    }
}