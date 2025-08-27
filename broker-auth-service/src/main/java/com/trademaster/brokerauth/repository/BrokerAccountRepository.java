package com.trademaster.brokerauth.repository;

import com.trademaster.brokerauth.entity.BrokerAccount;
import com.trademaster.brokerauth.enums.BrokerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Broker Account Repository
 * 
 * Data access layer for BrokerAccount entities.
 * Provides queries for user-broker account relationships and statistics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface BrokerAccountRepository extends JpaRepository<BrokerAccount, Long> {
    
    /**
     * Find broker account by user and broker type
     */
    Optional<BrokerAccount> findByUserIdAndBrokerType(Long userId, BrokerType brokerType);
    
    /**
     * Find all active broker accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.userId = :userId AND ba.isActive = true ORDER BY ba.createdAt DESC")
    List<BrokerAccount> findActiveAccountsByUserId(@Param("userId") Long userId);
    
    /**
     * Find all broker accounts for user (including inactive)
     */
    List<BrokerAccount> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find accounts by broker type
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.brokerType = :brokerType AND ba.isActive = true ORDER BY ba.createdAt DESC")
    List<BrokerAccount> findActiveAccountsByBrokerType(@Param("brokerType") BrokerType brokerType);
    
    /**
     * Find verified accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.userId = :userId AND ba.isActive = true AND ba.isVerified = true ORDER BY ba.createdAt DESC")
    List<BrokerAccount> findVerifiedAccountsByUserId(@Param("userId") Long userId);
    
    /**
     * Check if user has account for broker type
     */
    @Query("SELECT CASE WHEN COUNT(ba) > 0 THEN true ELSE false END FROM BrokerAccount ba " +
           "WHERE ba.userId = :userId AND ba.brokerType = :brokerType AND ba.isActive = true")
    boolean hasActiveAccount(@Param("userId") Long userId, @Param("brokerType") BrokerType brokerType);
    
    /**
     * Count active accounts per user
     */
    @Query("SELECT COUNT(ba) FROM BrokerAccount ba WHERE ba.userId = :userId AND ba.isActive = true")
    long countActiveAccountsByUserId(@Param("userId") Long userId);
    
    /**
     * Find accounts with high error rates (poor health)
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.isActive = true " +
           "AND ba.totalConnections > 10 " +
           "AND (ba.failedConnections * 100.0 / ba.totalConnections) > 20.0 " +
           "ORDER BY (ba.failedConnections * 100.0 / ba.totalConnections) DESC")
    List<BrokerAccount> findUnhealthyAccounts();
    
    /**
     * Find accounts not verified for more than specified days
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.isActive = true AND ba.isVerified = false " +
           "AND ba.createdAt < :cutoffDate ORDER BY ba.createdAt ASC")
    List<BrokerAccount> findUnverifiedAccountsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find accounts with recent connection activity
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.isActive = true " +
           "AND ba.lastConnectionAt >= :sinceDate ORDER BY ba.lastConnectionAt DESC")
    List<BrokerAccount> findAccountsWithRecentActivity(@Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * Get account statistics by broker type
     */
    @Query("SELECT ba.brokerType, COUNT(ba) as total, " +
           "SUM(CASE WHEN ba.isActive = true THEN 1 ELSE 0 END) as active, " +
           "SUM(CASE WHEN ba.isVerified = true THEN 1 ELSE 0 END) as verified, " +
           "AVG(ba.successfulConnections * 100.0 / NULLIF(ba.totalConnections, 0)) as avgSuccessRate " +
           "FROM BrokerAccount ba GROUP BY ba.brokerType ORDER BY ba.brokerType")
    List<Object[]> getAccountStatisticsByBrokerType();
    
    /**
     * Find accounts requiring attention (unverified, high error rate, or old)
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.isActive = true AND (" +
           "ba.isVerified = false OR " +
           "(ba.totalConnections > 5 AND ba.failedConnections * 100.0 / ba.totalConnections > 30.0) OR " +
           "ba.lastConnectionAt < :staleThreshold) " +
           "ORDER BY ba.lastConnectionAt ASC")
    List<BrokerAccount> findAccountsRequiringAttention(@Param("staleThreshold") LocalDateTime staleThreshold);
    
    /**
     * Update last connection time and statistics
     */
    @Query("UPDATE BrokerAccount ba SET ba.lastConnectionAt = :connectionTime, " +
           "ba.totalConnections = ba.totalConnections + 1, " +
           "ba.successfulConnections = ba.successfulConnections + :successIncrement, " +
           "ba.failedConnections = ba.failedConnections + :failureIncrement " +
           "WHERE ba.id = :accountId")
    int updateConnectionStats(@Param("accountId") Long accountId,
                             @Param("connectionTime") LocalDateTime connectionTime,
                             @Param("successIncrement") int successIncrement,
                             @Param("failureIncrement") int failureIncrement);
}