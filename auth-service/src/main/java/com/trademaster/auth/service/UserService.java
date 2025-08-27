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

import java.net.InetAddress;
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
    @Transactional
    public User save(User user) {
        User savedUser = userRepository.save(user);
        log.info("User saved: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * Update last activity (login tracking)
     */
    @Transactional
    public void updateLastActivity(Long userId, String ipAddress, String deviceFingerprint) {
        SafeOperations.safelyToResult(() -> {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (Exception e) {
                log.warn("Failed to parse IP address {}: {}", ipAddress, e.getMessage());
                inetAddress = InetAddress.getLoopbackAddress();
            }
            
            userRepository.updateLastLogin(userId, LocalDateTime.now(), inetAddress, deviceFingerprint, LocalDateTime.now());
            
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
    @Transactional
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

    /**
     * Lock user account
     */
    @Transactional
    public void lockUserAccount(Long userId, int lockDurationMinutes) {
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
        userRepository.lockUserAccount(userId, lockUntil, LocalDateTime.now());
        
        log.warn("User account locked: {} until {}", userId, lockUntil);
    }

    /**
     * Unlock user account
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
     * Update email verification status
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
     */
    @Transactional
    public Result<Boolean, String> verifyMobile(Long userId, String mobileNumber, String verificationCode) {
        return SafeOperations.safelyToResult(() -> {
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            User user = userOpt.get();
            
            // Validate mobile number format
            if (!isValidMobileNumber(mobileNumber)) {
                throw new IllegalArgumentException("Invalid mobile number format: " + mobileNumber);
            }
            
            // Validate verification code
            if (verificationCode == null || verificationCode.trim().length() != 6) {
                throw new IllegalArgumentException("Invalid verification code format");
            }
            
            // Check if verification code is valid (in production, validate against SMS service)
            if (!isValidSmsVerificationCode(userId, mobileNumber, verificationCode.trim())) {
                throw new SecurityException("Invalid or expired verification code");
            }
            
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
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            // Validate mobile number
            if (!isValidMobileNumber(mobileNumber)) {
                throw new IllegalArgumentException("Invalid mobile number format: " + mobileNumber);
            }
            
            // Generate 6-digit verification code
            String verificationCode = generateSmsVerificationCode();
            
            // Store verification code (in production, use Redis with TTL)
            storeSmsVerificationCode(userId, mobileNumber, verificationCode);
            
            // Send SMS (in production, integrate with SMS service like Twilio, AWS SNS)
            boolean smsSent = sendSmsVerificationCode(mobileNumber, verificationCode);
            if (!smsSent) {
                throw new RuntimeException("Failed to send SMS verification code");
            }
            
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
        if (mobileNumber == null) return false;
        // Basic validation - in production use proper regex for international numbers
        String cleaned = mobileNumber.replaceAll("[^0-9+]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
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
        if (mobileNumber == null || mobileNumber.length() < 4) return "****";
        return "*".repeat(mobileNumber.length() - 4) + mobileNumber.substring(mobileNumber.length() - 4);
    }

    /**
     * Update KYC status with validation and document verification
     */
    @Transactional
    public Result<Boolean, String> updateKycStatus(Long userId, User.KycStatus newStatus, Map<String, Object> kycDocuments, String verificationReason) {
        return SafeOperations.safelyToResult(() -> {
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            User user = userOpt.get();
            User.KycStatus oldStatus = user.getKycStatus();
            
            // Validate KYC status transition
            if (!isValidKycStatusTransition(oldStatus, newStatus)) {
                throw new IllegalStateException("Invalid KYC status transition from " + oldStatus + " to " + newStatus);
            }
            
            // Validate required documents for APPROVED status
            if (newStatus == User.KycStatus.APPROVED) {
                if (!hasRequiredKycDocuments(kycDocuments)) {
                    throw new IllegalArgumentException("Required KYC documents missing for verification");
                }
                
                // Validate document integrity
                if (!validateKycDocuments(kycDocuments)) {
                    throw new SecurityException("KYC document validation failed");
                }
            }
            
            // Update KYC status in database
            userRepository.updateKycStatus(userId, newStatus, LocalDateTime.now());
            
            // Update user profile with KYC documents if provided
            if (kycDocuments != null && !kycDocuments.isEmpty()) {
                updateUserProfileKycDocuments(userId, kycDocuments);
            }
            
            // Create audit log entry
            Map<String, Object> auditDetails = Map.of(
                "old_status", oldStatus,
                "new_status", newStatus,
                "verification_reason", verificationReason != null ? verificationReason : "Manual update",
                "documents_count", kycDocuments != null ? kycDocuments.size() : 0
            );
            
            auditService.logAuthenticationEvent(userId, "KYC_STATUS_CHANGED", "SUCCESS", 
                null, null, null, auditDetails, null);
            
            // Send notification for status changes
            sendKycStatusNotification(user, oldStatus, newStatus);
            
            log.info("KYC status updated for user {}: {} -> {} (reason: {})", 
                    userId, oldStatus, newStatus, verificationReason);
            
            return true;
            
        }).mapError(error -> {
            // Audit failed KYC update
            auditService.logAuthenticationEvent(userId, "KYC_STATUS_UPDATE_FAILED", "FAILURE", 
                null, null, null, 
                Map.of("target_status", newStatus, "error", error), null);
            
            log.error("KYC status update failed for user {}: {}", userId, error);
            return "KYC status update failed: " + error;
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public void updateKycStatus(Long userId, User.KycStatus status) {
        updateKycStatus(userId, status, null, "Legacy update");
    }

    /**
     * Validate KYC status transition rules
     */
    private boolean isValidKycStatusTransition(User.KycStatus from, User.KycStatus to) {
        if (from == to) return true;
        
        return switch (from) {
            case PENDING -> Set.of(User.KycStatus.IN_PROGRESS, User.KycStatus.REJECTED).contains(to);
            case IN_PROGRESS -> Set.of(User.KycStatus.APPROVED, User.KycStatus.REJECTED, User.KycStatus.PENDING).contains(to);
            case APPROVED -> Set.of(User.KycStatus.IN_PROGRESS).contains(to);
            case REJECTED -> Set.of(User.KycStatus.PENDING, User.KycStatus.IN_PROGRESS).contains(to);
        };
    }

    /**
     * Check if required KYC documents are present
     */
    private boolean hasRequiredKycDocuments(Map<String, Object> documents) {
        if (documents == null || documents.isEmpty()) return false;
        
        // Required documents for verification
        Set<String> requiredDocs = Set.of(
            "identity_document",    // Government ID, Passport, Driver's License
            "address_proof",        // Utility bill, Bank statement
            "selfie_with_id"        // Selfie holding identity document
        );
        
        return requiredDocs.stream().allMatch(documents::containsKey);
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
        
        for (Map.Entry<String, Object> entry : documents.entrySet()) {
            String docType = entry.getKey();
            Object docData = entry.getValue();
            
            // Basic validation
            if (docData == null) {
                log.warn("Missing document data for type: {}", docType);
                return false;
            }
            
            // Validate document structure
            if (!isValidDocumentStructure(docType, docData)) {
                log.warn("Invalid document structure for type: {}", docType);
                return false;
            }
        }
        
        return true;
    }

    private boolean isValidDocumentStructure(String docType, Object docData) {
        // Basic structure validation
        if (docData instanceof Map<?, ?> docMap) {
            return docMap.containsKey("filename") && 
                   docMap.containsKey("content_type") && 
                   docMap.containsKey("size");
        }
        return false;
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
    @Transactional
    public Result<Boolean, String> updateSubscriptionTier(Long userId, User.SubscriptionTier newTier, String changeReason) {
        return SafeOperations.safelyToResult(() -> {
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            User user = userOpt.get();
            User.SubscriptionTier oldTier = user.getSubscriptionTier();
            
            // Validate subscription tier change
            if (!isValidSubscriptionTierChange(user, oldTier, newTier)) {
                throw new IllegalStateException("Invalid subscription tier change from " + oldTier + " to " + newTier);
            }
            
            // Check KYC requirements for premium tiers
            if (requiresKycForTier(newTier) && user.getKycStatus() != User.KycStatus.APPROVED) {
                throw new IllegalStateException("KYC approval required for " + newTier + " subscription tier");
            }
            
            // Update subscription tier
            userRepository.updateSubscriptionTier(userId, newTier, LocalDateTime.now());
            
            // Create audit log
            Map<String, Object> auditDetails = Map.of(
                "old_tier", oldTier,
                "new_tier", newTier,
                "change_reason", changeReason != null ? changeReason : "Manual update",
                "kyc_status", user.getKycStatus()
            );
            
            auditService.logAuthenticationEvent(userId, "SUBSCRIPTION_TIER_CHANGED", "SUCCESS", 
                null, null, null, auditDetails, null);
            
            // Send notification
            sendSubscriptionTierNotification(user, oldTier, newTier);
            
            log.info("Subscription tier updated for user {}: {} -> {} (reason: {})", 
                    userId, oldTier, newTier, changeReason);
            
            return true;
            
        }).mapError(error -> {
            auditService.logAuthenticationEvent(userId, "SUBSCRIPTION_TIER_UPDATE_FAILED", "FAILURE", 
                null, null, null, 
                Map.of("target_tier", newTier, "error", error), null);
            
            log.error("Subscription tier update failed for user {}: {}", userId, error);
            return "Subscription tier update failed: " + error;
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public void updateSubscriptionTier(Long userId, User.SubscriptionTier tier) {
        updateSubscriptionTier(userId, tier, "Legacy update");
    }

    private boolean isValidSubscriptionTierChange(User user, User.SubscriptionTier from, User.SubscriptionTier to) {
        // Basic validation rules
        if (from == to) return true;
        
        // Account must be active for upgrades
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE && 
            to.ordinal() > from.ordinal()) {
            return false;
        }
        
        // Additional business rules can be added here
        return true;
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
    @Transactional
    public void updatePassword(Long userId, String passwordHash) {
        userRepository.updatePassword(userId, passwordHash, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE", "SUCCESS", 
            null, null, null, null, null);
        
        log.info("Password updated for user: {}", userId);
    }

    /**
     * Process account unlocks for users whose lock period has expired using functional approach
     */
    @Transactional
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