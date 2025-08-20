package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            userRepository.updateLastLogin(userId, LocalDateTime.now(), inetAddress, deviceFingerprint, LocalDateTime.now());
            
            auditService.logAuthenticationEvent(userId, "LOGIN_SUCCESS", "SUCCESS", 
                ipAddress, null, deviceFingerprint, null, null);
            
            log.debug("Updated last activity for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error updating last activity for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Handle failed login attempt
     */
    @Transactional
    public void handleFailedLogin(String email, String ipAddress, String deviceFingerprint) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            
            userRepository.updateFailedLoginAttempts(user.getId(), attempts, LocalDateTime.now());
            
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                lockUserAccount(user.getId(), ACCOUNT_LOCK_DURATION_MINUTES);
                
                auditService.logAuthenticationEvent(user.getId(), "ACCOUNT_LOCKED", "BLOCKED", 
                    ipAddress, null, deviceFingerprint, 
                    Map.of("reason", "max_failed_attempts", "attempts", attempts), null);
                
                log.warn("User account locked due to failed login attempts: {} (attempts: {})", email, attempts);
            } else {
                auditService.logAuthenticationEvent(user.getId(), "LOGIN_FAILED", "FAILED", 
                    ipAddress, null, deviceFingerprint, 
                    Map.of("attempts", attempts, "remaining", MAX_FAILED_ATTEMPTS - attempts), null);
            }
        } else {
            // Log failed attempt for non-existent user (security measure)
            auditService.logAuthenticationEvent(null, "LOGIN_FAILED", "FAILED", 
                ipAddress, null, deviceFingerprint, 
                Map.of("email", email, "reason", "user_not_found"), null);
        }
    }

    /**
     * Handle successful login
     */
    @Transactional
    public void handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint) {
        // Reset failed login attempts
        userRepository.resetFailedLoginAttempts(user.getId(), LocalDateTime.now());
        
        // Update last login information
        updateLastActivity(user.getId(), ipAddress, deviceFingerprint);
        
        log.info("Successful login for user: {}", user.getEmail());
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
     * Update KYC status
     */
    @Transactional
    public void updateKycStatus(Long userId, User.KycStatus status) {
        User.KycStatus oldStatus = userRepository.findById(userId)
                .map(User::getKycStatus)
                .orElse(null);
        
        userRepository.updateKycStatus(userId, status, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "KYC_STATUS_CHANGED", "SUCCESS", 
            null, null, null, 
            Map.of("old_status", oldStatus, "new_status", status), null);
        
        log.info("KYC status updated for user {}: {} -> {}", userId, oldStatus, status);
    }

    /**
     * Update subscription tier
     */
    @Transactional
    public void updateSubscriptionTier(Long userId, User.SubscriptionTier tier) {
        User.SubscriptionTier oldTier = userRepository.findById(userId)
                .map(User::getSubscriptionTier)
                .orElse(null);
        
        userRepository.updateSubscriptionTier(userId, tier, LocalDateTime.now());
        
        auditService.logAuthenticationEvent(userId, "SUBSCRIPTION_TIER_CHANGED", "SUCCESS", 
            null, null, null, 
            Map.of("old_tier", oldTier, "new_tier", tier), null);
        
        log.info("Subscription tier updated for user {}: {} -> {}", userId, oldTier, tier);
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
     * Process account unlocks for users whose lock period has expired
     */
    @Transactional
    public void processAccountUnlocks() {
        List<User> usersToUnlock = userRepository.findUsersToUnlock(LocalDateTime.now());
        
        for (User user : usersToUnlock) {
            unlockUserAccount(user.getId());
        }
        
        if (!usersToUnlock.isEmpty()) {
            log.info("Processed automatic account unlocks for {} users", usersToUnlock.size());
        }
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
     * Get user statistics
     */
    public UserStatistics getUserStatistics() {
        Object[] stats = userRepository.getUserStatistics();
        if (stats != null && stats.length >= 6) {
            return UserStatistics.builder()
                .totalUsers(((Number) stats[0]).longValue())
                .activeUsers(((Number) stats[1]).longValue())
                .lockedUsers(((Number) stats[2]).longValue())
                .suspendedUsers(((Number) stats[3]).longValue())
                .verifiedUsers(((Number) stats[4]).longValue())
                .recentLogins(((Number) stats[5]).longValue())
                .build();
        }
        return new UserStatistics();
    }

    /**
     * User Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long lockedUsers;
        private long suspendedUsers;
        private long verifiedUsers;
        private long recentLogins;
    }
}