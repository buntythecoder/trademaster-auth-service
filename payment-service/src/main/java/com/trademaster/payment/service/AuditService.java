package com.trademaster.payment.service;

import com.trademaster.payment.entity.PaymentEvent;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Service
 * 
 * Comprehensive audit logging for payment operations and compliance reporting.
 * Maintains detailed audit trail for financial regulations and internal monitoring.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final PaymentEventRepository paymentEventRepository;
    
    // Payment Transaction Audit Events
    
    @Transactional
    public void logPaymentInitiated(UUID userId, UUID transactionId, BigDecimal amount, String currency) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "amount", amount,
                "currency", currency,
                "action", "payment_initiated"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment.initiated")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment initiated for user: {} amount: {} {}", userId, amount, currency);
    }
    
    @Transactional
    public void logPaymentCompleted(PaymentTransaction transaction) {
        Map<String, Object> eventData = Map.of(
                "transactionId", transaction.getId().toString(),
                "userId", transaction.getUserId().toString(),
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "gateway", transaction.getPaymentGateway().name(),
                "gatewayTransactionId", transaction.getGatewayTransactionId() != null ? 
                        transaction.getGatewayTransactionId() : "",
                "action", "payment_completed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType("payment.completed")
                .eventSource(PaymentEvent.EventSources.WEBHOOK)
                .eventData(eventData)
                .newStatus(transaction.getStatus().name())
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment completed for transaction: {}", transaction.getId());
    }
    
    @Transactional
    public void logPaymentFailed(PaymentTransaction transaction, String failureReason) {
        Map<String, Object> eventData = Map.of(
                "transactionId", transaction.getId().toString(),
                "userId", transaction.getUserId().toString(),
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "gateway", transaction.getPaymentGateway().name(),
                "failureReason", failureReason,
                "action", "payment_failed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType("payment.failed")
                .eventSource(PaymentEvent.EventSources.WEBHOOK)
                .eventData(eventData)
                .newStatus(transaction.getStatus().name())
                .build();
        
        paymentEventRepository.save(event);
        log.warn("Audit: Payment failed for transaction: {} - {}", transaction.getId(), failureReason);
    }
    
    // Refund Audit Events
    
    @Transactional
    public void logRefundProcessed(UUID userId, UUID transactionId, BigDecimal refundAmount, 
                                  boolean isFullRefund, String refundId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "transactionId", transactionId.toString(),
                "refundAmount", refundAmount,
                "isFullRefund", isFullRefund,
                "refundId", refundId != null ? refundId : "",
                "action", "refund_processed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("refund.processed")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Refund processed for transaction: {} amount: {}", transactionId, refundAmount);
    }
    
    @Transactional
    public void logRefundFailed(UUID userId, UUID transactionId, BigDecimal refundAmount, String errorMessage) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "transactionId", transactionId.toString(),
                "refundAmount", refundAmount,
                "errorMessage", errorMessage,
                "action", "refund_failed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("refund.failed")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.warn("Audit: Refund failed for transaction: {} - {}", transactionId, errorMessage);
    }
    
    @Transactional
    public void logRefundCancelled(UUID userId, UUID transactionId, String refundId, String reason) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "transactionId", transactionId.toString(),
                "refundId", refundId,
                "reason", reason,
                "action", "refund_cancelled"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("refund.cancelled")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Refund cancelled for transaction: {} - {}", transactionId, reason);
    }
    
    // Payment Method Audit Events
    
    @Transactional
    public void logPaymentMethodAdded(UUID userId, UUID paymentMethodId, PaymentMethod paymentMethodType) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "paymentMethodType", paymentMethodType.name(),
                "action", "payment_method_added"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.added")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment method added for user: {} type: {}", userId, paymentMethodType);
    }
    
    @Transactional
    public void logPaymentMethodRemoved(UUID userId, UUID paymentMethodId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "action", "payment_method_removed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.removed")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment method removed for user: {}", userId);
    }
    
    @Transactional
    public void logPaymentMethodDefaultSet(UUID userId, UUID paymentMethodId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "action", "payment_method_default_set"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.default_set")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Default payment method set for user: {}", userId);
    }
    
    @Transactional
    public void logPaymentMethodVerified(UUID userId, UUID paymentMethodId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "action", "payment_method_verified"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.verified")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment method verified for user: {}", userId);
    }
    
    @Transactional
    public void logPaymentMethodExpired(UUID userId, UUID paymentMethodId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "action", "payment_method_expired"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.expired")
                .eventSource(PaymentEvent.EventSources.SCHEDULED)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment method expired for user: {}", userId);
    }
    
    @Transactional
    public void logPaymentMethodInactive(UUID userId, UUID paymentMethodId) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "paymentMethodId", paymentMethodId.toString(),
                "action", "payment_method_inactive"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("payment_method.inactive")
                .eventSource(PaymentEvent.EventSources.SCHEDULED)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Payment method marked as inactive for user: {}", userId);
    }
    
    // Subscription Audit Events
    
    @Transactional
    public void logSubscriptionCreated(UUID userId, UUID subscriptionId, UUID planId, BigDecimal amount) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "planId", planId.toString(),
                "amount", amount,
                "action", "subscription_created"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("subscription.created")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Subscription created for user: {} plan: {}", userId, planId);
    }
    
    @Transactional
    public void logSubscriptionCancelled(UUID userId, UUID subscriptionId, String reason) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "reason", reason,
                "action", "subscription_cancelled"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("subscription.cancelled")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Subscription cancelled for user: {} - {}", userId, reason);
    }
    
    @Transactional
    public void logSubscriptionRenewed(UUID userId, UUID subscriptionId, BigDecimal amount) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "amount", amount,
                "action", "subscription_renewed"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("subscription.renewed")
                .eventSource(PaymentEvent.EventSources.SCHEDULED)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.info("Audit: Subscription renewed for user: {} amount: {}", userId, amount);
    }
    
    // Security Audit Events
    
    @Transactional
    public void logSecurityViolation(String violationType, String details, String ipAddress, String userAgent) {
        Map<String, Object> eventData = Map.of(
                "violationType", violationType,
                "details", details,
                "ipAddress", ipAddress != null ? ipAddress : "unknown",
                "userAgent", userAgent != null ? userAgent : "unknown",
                "action", "security_violation"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("security.violation")
                .eventSource(PaymentEvent.EventSources.INTERNAL)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.warn("Audit: Security violation - {} from {}", violationType, ipAddress);
    }
    
    @Transactional
    public void logSuspiciousActivity(UUID userId, String activityType, String details) {
        Map<String, Object> eventData = Map.of(
                "userId", userId.toString(),
                "activityType", activityType,
                "details", details,
                "action", "suspicious_activity"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType("security.suspicious_activity")
                .eventSource(PaymentEvent.EventSources.INTERNAL)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.warn("Audit: Suspicious activity for user: {} - {}", userId, activityType);
    }
    
    // Audit Query Methods
    
    /**
     * Get user audit trail
     */
    public Page<PaymentEvent> getUserAuditTrail(UUID userId, Pageable pageable) {
        return paymentEventRepository.findUserAuditTrail(userId, pageable);
    }
    
    /**
     * Get audit events by date range
     */
    public List<PaymentEvent> getAuditEventsByDateRange(Instant startDate, Instant endDate) {
        return paymentEventRepository.findEventsByDateRange(startDate, endDate);
    }
    
    /**
     * Get processing statistics
     */
    public Map<String, Object> getProcessingStatistics(Instant since) {
        Object[] stats = paymentEventRepository.getProcessingStatistics(since);
        
        if (stats != null && stats.length >= 4) {
            Map<String, Object> result = new HashMap<>();
            result.put("processedCount", stats[0] != null ? ((Number) stats[0]).longValue() : 0L);
            result.put("pendingCount", stats[1] != null ? ((Number) stats[1]).longValue() : 0L);
            result.put("failedCount", stats[2] != null ? ((Number) stats[2]).longValue() : 0L);
            result.put("avgProcessingTime", stats[3] != null ? ((Number) stats[3]).doubleValue() : 0.0);
            return result;
        }
        
        return Map.of(
                "processedCount", 0L,
                "pendingCount", 0L,
                "failedCount", 0L,
                "avgProcessingTime", 0.0
        );
    }
    
    /**
     * Get event count by type
     */
    public List<Object[]> getEventCountByType(Instant since) {
        return paymentEventRepository.countEventsByType(since);
    }
    
    /**
     * Get event count by source
     */
    public List<Object[]> getEventCountBySource(Instant since) {
        return paymentEventRepository.countEventsBySource(since);
    }
    
    /**
     * Get failed events for alerting
     */
    public List<PaymentEvent> getFailedEvents(Instant since) {
        return paymentEventRepository.findFailedEvents(since);
    }
    
    /**
     * Cleanup old processed events
     */
    @Transactional
    public int cleanupOldEvents(int daysToRetain) {
        Instant cutoffDate = Instant.now().minus(daysToRetain, java.time.temporal.ChronoUnit.DAYS);
        List<PaymentEvent> oldEvents = paymentEventRepository.findOldProcessedEvents(cutoffDate);
        
        int deletedCount = oldEvents.size();
        paymentEventRepository.deleteAll(oldEvents);
        
        log.info("Audit cleanup: Deleted {} old processed events older than {} days", deletedCount, daysToRetain);
        return deletedCount;
    }
}