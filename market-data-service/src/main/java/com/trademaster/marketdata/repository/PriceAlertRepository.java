package com.trademaster.marketdata.repository;

import com.trademaster.marketdata.entity.PriceAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Price Alert Repository
 * 
 * Provides data access methods for price alerts with optimized
 * queries for real-time monitoring and notification management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long>, JpaSpecificationExecutor<PriceAlert> {
    
    /**
     * Get active alerts for a user
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC, a.createdAt DESC")
    List<PriceAlert> findActiveAlertsByUser(@Param("userId") String userId);
    
    /**
     * Get active alerts for a user with pagination
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC, a.createdAt DESC")
    Page<PriceAlert> findActiveAlertsByUser(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Get all alerts for a user (any status)
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "ORDER BY a.createdAt DESC")
    Page<PriceAlert> findAllAlertsByUser(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Get alerts due for checking
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'ACTIVE' " +
           "AND a.isActive = true AND a.isTriggered = false " +
           "AND (a.nextCheckAt IS NULL OR a.nextCheckAt <= :now) " +
           "ORDER BY a.priority DESC, a.nextCheckAt ASC")
    List<PriceAlert> findAlertsDueForCheck(@Param("now") LocalDateTime now);
    
    /**
     * Get alerts due for checking by priority
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'ACTIVE' " +
           "AND a.isActive = true AND a.isTriggered = false " +
           "AND a.priority = :priority " +
           "AND (a.nextCheckAt IS NULL OR a.nextCheckAt <= :now) " +
           "ORDER BY a.nextCheckAt ASC")
    List<PriceAlert> findAlertsDueForCheckByPriority(
        @Param("priority") PriceAlert.Priority priority,
        @Param("now") LocalDateTime now);
    
    /**
     * Get alerts for specific symbol
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.symbol = :symbol " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC")
    List<PriceAlert> findActiveAlertsBySymbol(@Param("symbol") String symbol);
    
    /**
     * Get alerts for multiple symbols
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.symbol IN :symbols " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC, a.symbol ASC")
    List<PriceAlert> findActiveAlertsBySymbols(@Param("symbols") List<String> symbols);
    
    /**
     * Get user alerts for specific symbol
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.symbol = :symbol AND a.status = 'ACTIVE' " +
           "ORDER BY a.createdAt DESC")
    List<PriceAlert> findUserAlertsForSymbol(
        @Param("userId") String userId,
        @Param("symbol") String symbol);
    
    /**
     * Get triggered alerts pending notification
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'TRIGGERED' " +
           "AND a.isTriggered = true " +
           "AND (a.emailSent = false OR a.smsSent = false OR a.pushSent = false) " +
           "ORDER BY a.priority DESC, a.triggeredAt ASC")
    List<PriceAlert> findTriggeredAlertsPendingNotification();
    
    /**
     * Get recently triggered alerts
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'TRIGGERED' " +
           "AND a.triggeredAt >= :since " +
           "ORDER BY a.triggeredAt DESC")
    List<PriceAlert> findRecentlyTriggeredAlerts(@Param("since") LocalDateTime since);
    
    /**
     * Get alerts by type
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.alertType = :alertType " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC")
    List<PriceAlert> findAlertsByType(@Param("alertType") PriceAlert.AlertType alertType);
    
    /**
     * Get expired alerts
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.expiresAt IS NOT NULL " +
           "AND a.expiresAt <= :now AND a.status = 'ACTIVE' " +
           "ORDER BY a.expiresAt ASC")
    List<PriceAlert> findExpiredAlerts(@Param("now") LocalDateTime now);
    
    /**
     * Get alerts by status
     */
    List<PriceAlert> findByStatusOrderByUpdatedAtDesc(PriceAlert.AlertStatus status);
    
    /**
     * Get user alerts by status
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.status = :status " +
           "ORDER BY a.updatedAt DESC")
    List<PriceAlert> findUserAlertsByStatus(
        @Param("userId") String userId,
        @Param("status") PriceAlert.AlertStatus status);
    
    /**
     * Get high priority alerts
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.priority IN ('URGENT', 'CRITICAL') " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.priority DESC, a.createdAt ASC")
    List<PriceAlert> findHighPriorityAlerts();
    
    /**
     * Get alerts requiring immediate attention
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.priority = 'CRITICAL' " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "AND a.isTriggered = false " +
           "ORDER BY a.createdAt ASC")
    List<PriceAlert> findCriticalAlerts();
    
    /**
     * Count active alerts by user
     */
    @Query("SELECT COUNT(a) FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.status = 'ACTIVE' AND a.isActive = true")
    Long countActiveAlertsByUser(@Param("userId") String userId);
    
    /**
     * Count alerts by symbol
     */
    @Query("SELECT COUNT(a) FROM PriceAlert a WHERE a.symbol = :symbol " +
           "AND a.status = 'ACTIVE' AND a.isActive = true")
    Long countActiveAlertsBySymbol(@Param("symbol") String symbol);
    
    /**
     * Get alert statistics by user
     */
    @Query("SELECT a.status, COUNT(a) FROM PriceAlert a WHERE a.userId = :userId " +
           "GROUP BY a.status")
    List<Object[]> getUserAlertStatistics(@Param("userId") String userId);
    
    /**
     * Get alert performance metrics
     */
    @Query("SELECT a.alertType, COUNT(a), AVG(a.accuracyScore), " +
           "AVG(a.averageResponseTimeMs), SUM(a.timesTriggered) " +
           "FROM PriceAlert a WHERE a.userId = :userId " +
           "GROUP BY a.alertType")
    List<Object[]> getUserAlertPerformance(@Param("userId") String userId);
    
    /**
     * Get most active symbols (by alert count)
     */
    @Query("SELECT a.symbol, COUNT(a) FROM PriceAlert a " +
           "WHERE a.status = 'ACTIVE' AND a.isActive = true " +
           "GROUP BY a.symbol ORDER BY COUNT(a) DESC")
    List<Object[]> getMostActiveSymbols();
    
    /**
     * Get alert frequency by hour
     */
    @Query("SELECT EXTRACT(HOUR FROM a.triggeredAt), COUNT(a) " +
           "FROM PriceAlert a WHERE a.triggeredAt >= :since " +
           "GROUP BY EXTRACT(HOUR FROM a.triggeredAt) " +
           "ORDER BY EXTRACT(HOUR FROM a.triggeredAt)")
    List<Object[]> getAlertFrequencyByHour(@Param("since") LocalDateTime since);
    
    /**
     * Find alerts near trigger conditions
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'ACTIVE' " +
           "AND a.isActive = true AND a.isTriggered = false " +
           "AND a.marketPrice IS NOT NULL AND a.targetPrice IS NOT NULL " +
           "AND ABS((a.marketPrice - a.targetPrice) / a.marketPrice) <= :thresholdPercent " +
           "ORDER BY ABS((a.marketPrice - a.targetPrice) / a.marketPrice) ASC")
    List<PriceAlert> findAlertsNearTrigger(@Param("thresholdPercent") BigDecimal thresholdPercent);
    
    /**
     * Find stale alerts (not checked recently)
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.status = 'ACTIVE' " +
           "AND a.isActive = true " +
           "AND (a.lastCheckedAt IS NULL OR a.lastCheckedAt <= :cutoff) " +
           "ORDER BY a.lastCheckedAt ASC NULLS FIRST")
    List<PriceAlert> findStaleAlerts(@Param("cutoff") LocalDateTime cutoff);
    
    /**
     * Update alert status in batch
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.status = :newStatus, a.updatedAt = :now " +
           "WHERE a.id IN :alertIds")
    int updateAlertStatus(
        @Param("alertIds") List<Long> alertIds,
        @Param("newStatus") PriceAlert.AlertStatus newStatus,
        @Param("now") Instant now);
    
    /**
     * Update market price for alerts
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.marketPrice = :price, a.lastPriceUpdate = :now, " +
           "a.lastCheckedAt = :checkTime WHERE a.symbol = :symbol " +
           "AND a.status = 'ACTIVE' AND a.isActive = true")
    int updateMarketPrice(
        @Param("symbol") String symbol,
        @Param("price") BigDecimal price,
        @Param("now") Instant now,
        @Param("checkTime") LocalDateTime checkTime);
    
    /**
     * Mark alerts as expired
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.status = 'EXPIRED', a.isActive = false, " +
           "a.updatedAt = :now WHERE a.expiresAt <= :expireTime " +
           "AND a.status = 'ACTIVE'")
    int markExpiredAlerts(
        @Param("expireTime") LocalDateTime expireTime,
        @Param("now") Instant now);
    
    /**
     * Update next check time
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.nextCheckAt = :nextCheck " +
           "WHERE a.id = :alertId")
    void updateNextCheckTime(
        @Param("alertId") Long alertId,
        @Param("nextCheck") LocalDateTime nextCheck);
    
    /**
     * Increment notification attempts
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.notificationAttempts = a.notificationAttempts + 1 " +
           "WHERE a.id = :alertId")
    void incrementNotificationAttempts(@Param("alertId") Long alertId);
    
    /**
     * Mark notification as sent
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET " +
           "a.emailSent = CASE WHEN :method = 'EMAIL' THEN true ELSE a.emailSent END, " +
           "a.smsSent = CASE WHEN :method = 'SMS' THEN true ELSE a.smsSent END, " +
           "a.pushSent = CASE WHEN :method = 'PUSH' THEN true ELSE a.pushSent END " +
           "WHERE a.id = :alertId")
    void markNotificationSent(
        @Param("alertId") Long alertId,
        @Param("method") String method);
    
    /**
     * Update alert accuracy score
     */
    @Modifying
    @Query("UPDATE PriceAlert a SET a.accuracyScore = :score " +
           "WHERE a.id = :alertId")
    void updateAccuracyScore(
        @Param("alertId") Long alertId,
        @Param("score") BigDecimal score);
    
    /**
     * Custom complex query for monitoring dashboard
     */
    @Query("SELECT a.symbol, a.alertType, COUNT(a) as alertCount, " +
           "COUNT(CASE WHEN a.status = 'TRIGGERED' THEN 1 END) as triggeredCount, " +
           "AVG(a.accuracyScore) as avgAccuracy, " +
           "MAX(a.updatedAt) as lastActivity " +
           "FROM PriceAlert a WHERE a.createdAt >= :since " +
           "GROUP BY a.symbol, a.alertType " +
           "ORDER BY alertCount DESC, triggeredCount DESC")
    List<Object[]> getMonitoringStatistics(@Param("since") Instant since);
    
    /**
     * Find duplicate alerts (same user, symbol, conditions)
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.userId = :userId " +
           "AND a.symbol = :symbol AND a.alertType = :alertType " +
           "AND a.triggerCondition = :condition AND a.targetPrice = :targetPrice " +
           "AND a.status = 'ACTIVE' AND a.isActive = true " +
           "ORDER BY a.createdAt ASC")
    List<PriceAlert> findDuplicateAlerts(
        @Param("userId") String userId,
        @Param("symbol") String symbol,
        @Param("alertType") PriceAlert.AlertType alertType,
        @Param("condition") PriceAlert.TriggerCondition condition,
        @Param("targetPrice") BigDecimal targetPrice);
    
    /**
     * Get user's most frequent alert patterns
     */
    @Query("SELECT a.alertType, a.triggerCondition, COUNT(a) " +
           "FROM PriceAlert a WHERE a.userId = :userId " +
           "GROUP BY a.alertType, a.triggerCondition " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getUserAlertPatterns(@Param("userId") String userId);
    
    /**
     * Find alerts with high false positive rates
     */
    @Query("SELECT a FROM PriceAlert a WHERE a.timesTriggered > 0 " +
           "AND (a.falsePositives * 100.0 / a.timesTriggered) > :threshold " +
           "ORDER BY (a.falsePositives * 100.0 / a.timesTriggered) DESC")
    List<PriceAlert> findHighFalsePositiveAlerts(@Param("threshold") BigDecimal threshold);
    
    /**
     * Get system-wide alert health metrics
     */
    @Query("SELECT " +
           "COUNT(a) as totalAlerts, " +
           "COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) as activeAlerts, " +
           "COUNT(CASE WHEN a.isTriggered = true THEN 1 END) as triggeredAlerts, " +
           "AVG(a.averageResponseTimeMs) as avgResponseTime, " +
           "AVG(a.accuracyScore) as avgAccuracy " +
           "FROM PriceAlert a WHERE a.createdAt >= :since")
    List<Object[]> getSystemHealthMetrics(@Param("since") Instant since);
}