package com.trademaster.userprofile.repository;

import com.trademaster.userprofile.entity.ChangeType;
import com.trademaster.userprofile.entity.EntityType;
import com.trademaster.userprofile.entity.ProfileAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileAuditLogRepository extends JpaRepository<ProfileAuditLog, UUID> {
    
    /**
     * Find all audit logs for a user profile
     */
    Page<ProfileAuditLog> findByUserProfileIdOrderByCreatedAtDesc(UUID userProfileId, Pageable pageable);
    
    /**
     * Find audit logs by change type
     */
    List<ProfileAuditLog> findByUserProfileIdAndChangeTypeOrderByCreatedAtDesc(
        UUID userProfileId, 
        ChangeType changeType
    );
    
    /**
     * Find audit logs by entity type
     */
    List<ProfileAuditLog> findByUserProfileIdAndEntityTypeOrderByCreatedAtDesc(
        UUID userProfileId, 
        EntityType entityType
    );
    
    /**
     * Find audit logs for specific entity
     */
    List<ProfileAuditLog> findByEntityIdOrderByCreatedAtDesc(UUID entityId);
    
    /**
     * Find audit logs by user who made changes
     */
    Page<ProfileAuditLog> findByChangedByOrderByCreatedAtDesc(UUID changedBy, Pageable pageable);
    
    /**
     * Find audit logs within date range
     */
    Page<ProfileAuditLog> findByUserProfileIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID userProfileId, 
        Instant startDate, 
        Instant endDate, 
        Pageable pageable
    );
    
    /**
     * Find recent activity for a user
     */
    @Query("SELECT pal FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId AND " +
           "pal.createdAt >= :since " +
           "ORDER BY pal.createdAt DESC")
    List<ProfileAuditLog> findRecentActivity(
        @Param("userProfileId") UUID userProfileId, 
        @Param("since") Instant since
    );
    
    /**
     * Count changes by type for a user
     */
    @Query("SELECT pal.changeType, COUNT(pal) FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId " +
           "GROUP BY pal.changeType")
    List<Object[]> countChangesByType(@Param("userProfileId") UUID userProfileId);
    
    /**
     * Find login/logout events
     */
    @Query("SELECT pal FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId AND " +
           "pal.changeType IN ('LOGIN', 'LOGOUT') " +
           "ORDER BY pal.createdAt DESC")
    Page<ProfileAuditLog> findSecurityEvents(@Param("userProfileId") UUID userProfileId, Pageable pageable);
    
    /**
     * Find KYC-related events
     */
    @Query("SELECT pal FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId AND " +
           "pal.changeType IN ('KYC_SUBMIT', 'KYC_VERIFY') " +
           "ORDER BY pal.createdAt DESC")
    List<ProfileAuditLog> findKycEvents(@Param("userProfileId") UUID userProfileId);
    
    /**
     * Find changes from specific IP address
     */
    List<ProfileAuditLog> findByIpAddressOrderByCreatedAtDesc(InetAddress ipAddress);
    
    /**
     * Find suspicious activity (multiple changes from different IPs)
     */
    @Query("SELECT pal FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId AND " +
           "pal.createdAt >= :since AND " +
           "pal.ipAddress IS NOT NULL " +
           "GROUP BY pal.ipAddress " +
           "HAVING COUNT(DISTINCT pal.ipAddress) > :threshold")
    List<ProfileAuditLog> findSuspiciousActivity(
        @Param("userProfileId") UUID userProfileId,
        @Param("since") Instant since,
        @Param("threshold") int threshold
    );
    
    /**
     * Get user activity summary
     */
    @Query("SELECT " +
           "COUNT(pal) as totalActions, " +
           "COUNT(DISTINCT pal.ipAddress) as uniqueIps, " +
           "MIN(pal.createdAt) as firstActivity, " +
           "MAX(pal.createdAt) as lastActivity " +
           "FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId")
    Object[] getActivitySummary(@Param("userProfileId") UUID userProfileId);
    
    /**
     * Find most active users
     */
    @Query("SELECT pal.userProfile.id, COUNT(pal) as actionCount FROM ProfileAuditLog pal " +
           "WHERE pal.createdAt >= :since " +
           "GROUP BY pal.userProfile.id " +
           "ORDER BY actionCount DESC")
    Page<Object[]> findMostActiveUsers(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Count actions by date (for analytics)
     */
    @Query("SELECT DATE(pal.createdAt), COUNT(pal) FROM ProfileAuditLog pal " +
           "WHERE pal.createdAt >= :since " +
           "GROUP BY DATE(pal.createdAt) " +
           "ORDER BY DATE(pal.createdAt)")
    List<Object[]> countActionsByDate(@Param("since") Instant since);
    
    /**
     * Delete old audit logs (data retention)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProfileAuditLog pal WHERE " +
           "pal.createdAt < :cutoffDate")
    int deleteAuditLogsOlderThan(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find failed login attempts
     */
    @Query("SELECT pal FROM ProfileAuditLog pal WHERE " +
           "pal.changeType = 'LOGIN' AND " +
           "JSON_EXTRACT(pal.newValues, '$.success') = false AND " +
           "pal.createdAt >= :since " +
           "ORDER BY pal.createdAt DESC")
    List<ProfileAuditLog> findFailedLoginAttempts(@Param("since") Instant since);
    
    /**
     * Count events by IP address (security monitoring)
     */
    @Query("SELECT pal.ipAddress, COUNT(pal) FROM ProfileAuditLog pal " +
           "WHERE pal.createdAt >= :since AND pal.ipAddress IS NOT NULL " +
           "GROUP BY pal.ipAddress " +
           "HAVING COUNT(pal) > :threshold " +
           "ORDER BY COUNT(pal) DESC")
    List<Object[]> findSuspiciousIpActivity(
        @Param("since") Instant since,
        @Param("threshold") long threshold
    );
    
    /**
     * Get change frequency for a user (how often they update profile)
     */
    @Query("SELECT COUNT(pal) as changeCount, " +
           "EXTRACT(EPOCH FROM (MAX(pal.createdAt) - MIN(pal.createdAt))) / COUNT(pal) as avgIntervalSeconds " +
           "FROM ProfileAuditLog pal WHERE " +
           "pal.userProfile.id = :userProfileId AND " +
           "pal.changeType IN ('CREATE', 'UPDATE')")
    Object[] getChangeFrequency(@Param("userProfileId") UUID userProfileId);
}