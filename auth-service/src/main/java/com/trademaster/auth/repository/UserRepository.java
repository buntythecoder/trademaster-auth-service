package com.trademaster.auth.repository;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.projection.UserStatisticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations
 * 
 * Provides data access methods for user management including:
 * - Authentication queries
 * - Account status management
 * - Security tracking
 * - KYC and subscription management
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find users by account status
     */
    List<User> findByAccountStatus(User.AccountStatus accountStatus);

    /**
     * Find users by KYC status
     */
    List<User> findByKycStatus(User.KycStatus kycStatus);

    /**
     * Find users by subscription tier
     */
    List<User> findBySubscriptionTier(User.SubscriptionTier subscriptionTier);

    /**
     * Find users with failed login attempts above threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    List<User> findUsersWithFailedLoginAttempts(@Param("threshold") int threshold);

    /**
     * Find users with locked accounts that should be unlocked
     */
    @Query("SELECT u FROM User u WHERE u.accountStatus = 'LOCKED' AND u.accountLockedUntil < :currentTime")
    List<User> findUsersToUnlock(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find users requiring password change
     */
    @Query("SELECT u FROM User u WHERE u.passwordChangedAt IS NULL OR u.passwordChangedAt < :thresholdDate")
    List<User> findUsersRequiringPasswordChange(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * Find inactive users (haven't logged in for specified days)
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :thresholdDate")
    List<User> findInactiveUsers(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * Update failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, 
                                  @Param("attempts") int attempts, 
                                  @Param("currentTime") LocalDateTime currentTime);

    /**
     * Reset failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLockedUntil = NULL, u.updatedAt = :currentTime WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update last login information
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress, u.deviceFingerprint = :deviceFingerprint, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, 
                        @Param("loginTime") LocalDateTime loginTime,
                        @Param("ipAddress") java.net.InetAddress ipAddress,
                        @Param("deviceFingerprint") String deviceFingerprint,
                        @Param("currentTime") LocalDateTime currentTime);

    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = 'LOCKED', u.accountLockedUntil = :lockUntil, u.updatedAt = :currentTime WHERE u.id = :userId")
    void lockUserAccount(@Param("userId") Long userId, 
                        @Param("lockUntil") LocalDateTime lockUntil, 
                        @Param("currentTime") LocalDateTime currentTime);

    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = 'ACTIVE', u.accountLockedUntil = NULL, u.failedLoginAttempts = 0, u.updatedAt = :currentTime WHERE u.id = :userId")
    void unlockUserAccount(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update account status
     */
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateAccountStatus(@Param("userId") Long userId, 
                           @Param("status") User.AccountStatus status, 
                           @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update email verification status
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = :verified, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateEmailVerificationStatus(@Param("userId") Long userId, 
                                     @Param("verified") boolean verified, 
                                     @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update phone verification status
     */
    @Modifying
    @Query("UPDATE User u SET u.phoneVerified = :verified, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updatePhoneVerificationStatus(@Param("userId") Long userId, 
                                     @Param("verified") boolean verified, 
                                     @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update mobile number
     */
    @Modifying
    @Query("UPDATE User u SET u.mobileNumber = :mobileNumber, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateMobileNumber(@Param("userId") Long userId, 
                          @Param("mobileNumber") String mobileNumber, 
                          @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update KYC status
     */
    @Modifying
    @Query("UPDATE User u SET u.kycStatus = :status, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateKycStatus(@Param("userId") Long userId, 
                        @Param("status") User.KycStatus status, 
                        @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update subscription tier
     */
    @Modifying
    @Query("UPDATE User u SET u.subscriptionTier = :tier, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updateSubscriptionTier(@Param("userId") Long userId, 
                              @Param("tier") User.SubscriptionTier tier, 
                              @Param("currentTime") LocalDateTime currentTime);

    /**
     * Update password hash and password changed timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.passwordChangedAt = :currentTime, u.updatedAt = :currentTime WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, 
                       @Param("passwordHash") String passwordHash, 
                       @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find users created between dates
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Count users by subscription tier
     */
    @Query("SELECT u.subscriptionTier, COUNT(u) FROM User u GROUP BY u.subscriptionTier")
    List<Object[]> countUsersBySubscriptionTier();

    /**
     * Count users by KYC status
     */
    @Query("SELECT u.kycStatus, COUNT(u) FROM User u GROUP BY u.kycStatus")
    List<Object[]> countUsersByKycStatus();

    /**
     * Find users with MFA enabled
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.mfaConfigurations mfa WHERE mfa.isEnabled = true")
    List<User> findUsersWithMfaEnabled();

    /**
     * Find recently active users (logged in within specified days)
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :thresholdDate")
    List<User> findRecentlyActiveUsers(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * Custom query for user statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as totalUsers,
            COUNT(CASE WHEN account_status = 'ACTIVE' THEN 1 END) as activeUsers,
            COUNT(CASE WHEN account_status = 'LOCKED' THEN 1 END) as lockedUsers,
            COUNT(CASE WHEN account_status = 'SUSPENDED' THEN 1 END) as suspendedUsers,
            COUNT(CASE WHEN email_verified = true THEN 1 END) as verifiedUsers,
            COUNT(CASE WHEN last_login_at >= NOW() - INTERVAL '30 days' THEN 1 END) as recentLogins
        FROM users
        """, nativeQuery = true)
    UserStatisticsProjection getUserStatistics();
}