package com.trademaster.brokerauth.repository;

import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Broker Session Repository
 * 
 * Data access layer for BrokerSession entities.
 * Provides queries for session lifecycle management and monitoring.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface BrokerSessionRepository extends JpaRepository<BrokerSession, Long> {
    
    /**
     * Find session by session ID
     */
    Optional<BrokerSession> findBySessionId(String sessionId);
    
    /**
     * Find active sessions by user and broker type
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.userId = :userId AND bs.brokerType = :brokerType " +
           "AND bs.status = 'ACTIVE' ORDER BY bs.createdAt DESC")
    List<BrokerSession> findActiveSessionsByUserIdAndBrokerType(@Param("userId") Long userId, 
                                                               @Param("brokerType") BrokerType brokerType);
    
    /**
     * Find all active sessions for user
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.userId = :userId AND bs.status = 'ACTIVE' ORDER BY bs.createdAt DESC")
    List<BrokerSession> findActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * Find sessions by status
     */
    List<BrokerSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);
    
    /**
     * Find expired sessions (active but past expiry time)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.status = 'ACTIVE' AND bs.expiresAt < :now")
    List<BrokerSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * Find expired sessions (default to current time)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.status = 'ACTIVE' AND bs.expiresAt < CURRENT_TIMESTAMP")
    List<BrokerSession> findExpiredSessions();
    
    /**
     * Find sessions requiring refresh (expires within threshold)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.status = 'ACTIVE' " +
           "AND bs.encryptedRefreshToken IS NOT NULL " +
           "AND bs.expiresAt BETWEEN CURRENT_TIMESTAMP AND :refreshThreshold " +
           "ORDER BY bs.expiresAt ASC")
    List<BrokerSession> findSessionsRequiringRefresh(@Param("refreshThreshold") LocalDateTime refreshThreshold);
    
    /**
     * Find sessions by broker account
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.brokerAccount.id = :accountId ORDER BY bs.createdAt DESC")
    List<BrokerSession> findByBrokerAccountId(@Param("accountId") Long accountId);
    
    /**
     * Count active sessions per broker type
     */
    @Query("SELECT bs.brokerType, COUNT(bs) FROM BrokerSession bs WHERE bs.status = 'ACTIVE' GROUP BY bs.brokerType")
    List<Object[]> countActiveSessionsByBrokerType();
    
    /**
     * Count active sessions for user and broker type
     */
    @Query("SELECT COUNT(bs) FROM BrokerSession bs WHERE bs.userId = :userId AND bs.brokerType = :brokerType AND bs.status = 'ACTIVE'")
    long countActiveSessionsByUserAndBroker(@Param("userId") Long userId, @Param("brokerType") BrokerType brokerType);
    
    /**
     * Find sessions with high error rates
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.status = 'ACTIVE' " +
           "AND bs.apiCallsCount > 10 " +
           "AND (bs.errorCount * 100.0 / bs.apiCallsCount) > 10.0 " +
           "ORDER BY (bs.errorCount * 100.0 / bs.apiCallsCount) DESC")
    List<BrokerSession> findUnhealthySessions();
    
    /**
     * Find sessions by IP address (for security monitoring)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.clientIp = :clientIp AND bs.status = 'ACTIVE' ORDER BY bs.createdAt DESC")
    List<BrokerSession> findActiveSessionsByClientIp(@Param("clientIp") String clientIp);
    
    /**
     * Find stale sessions (not used for specified period)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.status = 'ACTIVE' " +
           "AND (bs.lastUsedAt IS NULL OR bs.lastUsedAt < :staleThreshold) " +
           "ORDER BY bs.lastUsedAt ASC NULLS FIRST")
    List<BrokerSession> findStaleSessions(@Param("staleThreshold") LocalDateTime staleThreshold);
    
    /**
     * Find sessions created in date range
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE bs.createdAt BETWEEN :startDate AND :endDate ORDER BY bs.createdAt DESC")
    List<BrokerSession> findSessionsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get session statistics by status
     */
    @Query("SELECT bs.status, COUNT(bs) FROM BrokerSession bs GROUP BY bs.status ORDER BY bs.status")
    List<Object[]> getSessionStatisticsByStatus();
    
    /**
     * Get session statistics by broker type
     */
    @Query("SELECT bs.brokerType, bs.status, COUNT(bs), AVG(bs.apiCallsCount), AVG(bs.errorCount) " +
           "FROM BrokerSession bs GROUP BY bs.brokerType, bs.status ORDER BY bs.brokerType, bs.status")
    List<Object[]> getDetailedSessionStatistics();
    
    /**
     * Find sessions for cleanup (revoked or expired sessions older than retention period)
     */
    @Query("SELECT bs FROM BrokerSession bs WHERE " +
           "(bs.status = 'REVOKED' OR bs.status = 'EXPIRED') " +
           "AND bs.updatedAt < :retentionCutoff " +
           "ORDER BY bs.updatedAt ASC")
    List<BrokerSession> findSessionsForCleanup(@Param("retentionCutoff") LocalDateTime retentionCutoff);
    
    /**
     * Update session usage statistics
     */
    @Query("UPDATE BrokerSession bs SET bs.apiCallsCount = bs.apiCallsCount + 1, " +
           "bs.lastUsedAt = :usedAt WHERE bs.sessionId = :sessionId")
    int updateUsageStats(@Param("sessionId") String sessionId, @Param("usedAt") LocalDateTime usedAt);
    
    /**
     * Update session error statistics
     */
    @Query("UPDATE BrokerSession bs SET bs.errorCount = bs.errorCount + 1, " +
           "bs.lastUsedAt = :usedAt WHERE bs.sessionId = :sessionId")
    int updateErrorStats(@Param("sessionId") String sessionId, @Param("usedAt") LocalDateTime usedAt);
    
    /**
     * Update session rate limit statistics
     */
    @Query("UPDATE BrokerSession bs SET bs.rateLimitHits = bs.rateLimitHits + 1 WHERE bs.sessionId = :sessionId")
    int updateRateLimitStats(@Param("sessionId") String sessionId);
}