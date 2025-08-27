package com.trademaster.payment.repository;

import com.trademaster.payment.entity.PaymentEvent;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payment Event Repository
 * 
 * Data access layer for payment event audit trail and compliance reporting.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    /**
     * Find all events for a transaction
     */
    List<PaymentEvent> findByTransactionOrderByCreatedAtDesc(PaymentTransaction transaction);
    
    /**
     * Find all events for a subscription
     */
    List<PaymentEvent> findBySubscriptionOrderByCreatedAtDesc(UserSubscription subscription);
    
    /**
     * Find events by type
     */
    List<PaymentEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    /**
     * Find events by source
     */
    List<PaymentEvent> findByEventSourceOrderByCreatedAtDesc(String eventSource);
    
    /**
     * Find unprocessed events
     */
    List<PaymentEvent> findByProcessedFalseOrderByCreatedAtAsc();
    
    /**
     * Find events requiring retry
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE " +
           "pe.processed = false AND " +
           "pe.processingAttempts < 3 AND " +
           "(pe.processingError IS NULL OR pe.processingError NOT LIKE '%permanent%')")
    List<PaymentEvent> findRetryableEvents();
    
    /**
     * Find events by gateway event ID
     */
    List<PaymentEvent> findByGatewayEventId(String gatewayEventId);
    
    /**
     * Find recent events for monitoring
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.createdAt >= :since ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findRecentEvents(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Count events by type for analytics
     */
    @Query("SELECT pe.eventType, COUNT(pe) FROM PaymentEvent pe " +
           "WHERE pe.createdAt >= :since GROUP BY pe.eventType")
    List<Object[]> countEventsByType(@Param("since") Instant since);
    
    /**
     * Count events by source for monitoring
     */
    @Query("SELECT pe.eventSource, COUNT(pe) FROM PaymentEvent pe " +
           "WHERE pe.createdAt >= :since GROUP BY pe.eventSource")
    List<Object[]> countEventsBySource(@Param("since") Instant since);
    
    /**
     * Find failed events for alerting
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE " +
           "pe.processed = false AND " +
           "pe.processingAttempts >= 3 AND " +
           "pe.createdAt >= :since")
    List<PaymentEvent> findFailedEvents(@Param("since") Instant since);
    
    /**
     * Get processing statistics
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN pe.processed = true THEN 1 END) as processedCount, " +
           "COUNT(CASE WHEN pe.processed = false THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN pe.processingAttempts >= 3 THEN 1 END) as failedCount, " +
           "AVG(EXTRACT(EPOCH FROM (pe.processedAt - pe.createdAt))) as avgProcessingTime " +
           "FROM PaymentEvent pe WHERE pe.createdAt >= :since")
    Object[] getProcessingStatistics(@Param("since") Instant since);
    
    /**
     * Find events for audit trail
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE " +
           "(pe.transaction.userId = :userId OR pe.subscription.userId = :userId) " +
           "ORDER BY pe.createdAt DESC")
    Page<PaymentEvent> findUserAuditTrail(@Param("userId") UUID userId, Pageable pageable);
    
    /**
     * Find events by date range
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE " +
           "pe.createdAt >= :startDate AND pe.createdAt <= :endDate " +
           "ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findEventsByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
    
    /**
     * Clean up old processed events
     */
    @Query("SELECT pe FROM PaymentEvent pe WHERE " +
           "pe.processed = true AND " +
           "pe.createdAt < :cutoffDate")
    List<PaymentEvent> findOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate);
}