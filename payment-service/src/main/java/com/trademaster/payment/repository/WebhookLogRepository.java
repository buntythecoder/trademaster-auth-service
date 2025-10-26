package com.trademaster.payment.repository;

import com.trademaster.payment.entity.WebhookLog;
import com.trademaster.payment.enums.PaymentGateway;
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
 * Webhook Log Repository
 * 
 * Data access layer for webhook logging, debugging, and audit purposes.
 * Essential for payment gateway integration monitoring and compliance.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    /**
     * Find webhooks by gateway
     */
    List<WebhookLog> findByGatewayOrderByReceivedAtDesc(PaymentGateway gateway);
    
    /**
     * Find webhooks by event type
     */
    List<WebhookLog> findByEventTypeOrderByReceivedAtDesc(String eventType);
    
    /**
     * Find webhooks by gateway and event type
     */
    List<WebhookLog> findByGatewayAndEventTypeOrderByReceivedAtDesc(
            PaymentGateway gateway, String eventType);
    
    /**
     * Find unprocessed webhooks
     */
    List<WebhookLog> findByProcessedFalseOrderByReceivedAtAsc();
    
    /**
     * Find failed webhooks that need retry
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.processed = false AND " +
           "wl.signatureVerified = true AND " +
           "wl.processingAttempts < 3")
    List<WebhookLog> findRetryableWebhooks();
    
    /**
     * Find webhooks with signature verification failures
     */
    List<WebhookLog> findBySignatureVerifiedFalseOrderByReceivedAtDesc();
    
    /**
     * Find webhooks by webhook ID (may return duplicates if no unique constraint)
     */
    List<WebhookLog> findByWebhookId(String webhookId);

    /**
     * Find webhook by gateway and webhook ID for idempotency check
     * Returns Optional for unique constraint enforcement
     */
    java.util.Optional<WebhookLog> findByGatewayAndWebhookId(
            PaymentGateway gateway,
            String webhookId
    );
    
    /**
     * Find recent webhooks for monitoring
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE wl.receivedAt >= :since ORDER BY wl.receivedAt DESC")
    Page<WebhookLog> findRecentWebhooks(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Find webhooks by processing status
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "(CASE " +
           "WHEN wl.signatureVerified = false THEN 'SIGNATURE_FAILED' " +
           "WHEN wl.processed = true AND wl.responseStatus >= 200 AND wl.responseStatus < 300 THEN 'SUCCESS' " +
           "WHEN wl.processed = true THEN 'FAILED' " +
           "WHEN wl.processingAttempts >= 3 THEN 'MAX_RETRIES_EXCEEDED' " +
           "ELSE 'PENDING' END) = :status " +
           "ORDER BY wl.receivedAt DESC")
    List<WebhookLog> findByProcessingStatus(@Param("status") String status);
    
    /**
     * Get webhook processing statistics
     */
    @Query("SELECT " +
           "wl.gateway, " +
           "COUNT(wl) as totalCount, " +
           "COUNT(CASE WHEN wl.processed = true THEN 1 END) as processedCount, " +
           "COUNT(CASE WHEN wl.signatureVerified = false THEN 1 END) as signatureFailures, " +
           "COUNT(CASE WHEN wl.processingAttempts >= 3 THEN 1 END) as maxRetriesCount, " +
           "AVG(EXTRACT(EPOCH FROM (wl.processedAt - wl.receivedAt))) as avgProcessingTime " +
           "FROM WebhookLog wl " +
           "WHERE wl.receivedAt >= :since " +
           "GROUP BY wl.gateway")
    List<Object[]> getProcessingStatistics(@Param("since") Instant since);
    
    /**
     * Count webhooks by event type
     */
    @Query("SELECT wl.eventType, COUNT(wl) FROM WebhookLog wl " +
           "WHERE wl.receivedAt >= :since " +
           "GROUP BY wl.eventType " +
           "ORDER BY COUNT(wl) DESC")
    List<Object[]> countWebhooksByEventType(@Param("since") Instant since);
    
    /**
     * Count webhooks by gateway
     */
    @Query("SELECT wl.gateway, COUNT(wl) FROM WebhookLog wl " +
           "WHERE wl.receivedAt >= :since " +
           "GROUP BY wl.gateway")
    List<Object[]> countWebhooksByGateway(@Param("since") Instant since);
    
    /**
     * Find webhooks with errors
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.processingError IS NOT NULL AND " +
           "wl.receivedAt >= :since " +
           "ORDER BY wl.receivedAt DESC")
    List<WebhookLog> findWebhooksWithErrors(@Param("since") Instant since);
    
    /**
     * Find successful webhooks
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.processed = true AND " +
           "wl.responseStatus >= 200 AND wl.responseStatus < 300 " +
           "ORDER BY wl.receivedAt DESC")
    Page<WebhookLog> findSuccessfulWebhooks(Pageable pageable);
    
    /**
     * Find slow processing webhooks
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.processed = true AND " +
           "EXTRACT(EPOCH FROM (wl.processedAt - wl.receivedAt)) > :thresholdSeconds " +
           "ORDER BY (wl.processedAt - wl.receivedAt) DESC")
    List<WebhookLog> findSlowProcessingWebhooks(@Param("thresholdSeconds") long thresholdSeconds);
    
    /**
     * Find duplicate webhooks by webhook ID
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.webhookId IN (SELECT wl2.webhookId FROM WebhookLog wl2 " +
           "WHERE wl2.webhookId IS NOT NULL " +
           "GROUP BY wl2.webhookId HAVING COUNT(wl2) > 1) " +
           "ORDER BY wl.webhookId, wl.receivedAt")
    List<WebhookLog> findDuplicateWebhooks();
    
    /**
     * Clean up old processed webhooks
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.processed = true AND " +
           "wl.receivedAt < :cutoffDate " +
           "ORDER BY wl.receivedAt")
    List<WebhookLog> findOldProcessedWebhooks(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find webhooks by date range
     */
    @Query("SELECT wl FROM WebhookLog wl WHERE " +
           "wl.receivedAt >= :startDate AND wl.receivedAt <= :endDate " +
           "ORDER BY wl.receivedAt DESC")
    List<WebhookLog> findWebhooksByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
    
    /**
     * Get webhook health metrics
     */
    @Query("SELECT " +
           "COUNT(wl) as totalWebhooks, " +
           "COUNT(CASE WHEN wl.processed = true THEN 1 END) as processedWebhooks, " +
           "COUNT(CASE WHEN wl.signatureVerified = true THEN 1 END) as verifiedWebhooks, " +
           "COUNT(CASE WHEN wl.responseStatus >= 200 AND wl.responseStatus < 300 THEN 1 END) as successfulWebhooks, " +
           "AVG(CASE WHEN wl.processed = true THEN EXTRACT(EPOCH FROM (wl.processedAt - wl.receivedAt)) END) as avgProcessingTime, " +
           "MAX(CASE WHEN wl.processed = true THEN EXTRACT(EPOCH FROM (wl.processedAt - wl.receivedAt)) END) as maxProcessingTime " +
           "FROM WebhookLog wl " +
           "WHERE wl.receivedAt >= :since")
    Object[] getWebhookHealthMetrics(@Param("since") Instant since);
}