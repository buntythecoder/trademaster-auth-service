package com.trademaster.payment.service;

import com.trademaster.payment.entity.PaymentEvent;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.UserSubscription;
import com.trademaster.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Event Service
 * 
 * Service for creating and managing payment events for audit trail.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;
    
    /**
     * Create payment completed event
     */
    @Transactional
    public void createPaymentCompletedEvent(PaymentTransaction transaction, String previousStatus) {
        PaymentEvent event = PaymentEvent.paymentCompleted(transaction, previousStatus);
        paymentEventRepository.save(event);
        log.debug("Payment completed event created for transaction: {}", transaction.getId());
    }
    
    /**
     * Create payment failed event
     */
    @Transactional
    public void createPaymentFailedEvent(PaymentTransaction transaction, String previousStatus, String failureReason) {
        Map<String, Object> eventData = Map.of(
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "gateway", transaction.getPaymentGateway().name(),
                "failureReason", failureReason
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType(PaymentEvent.EventTypes.PAYMENT_FAILED)
                .eventSource(PaymentEvent.EventSources.WEBHOOK)
                .previousStatus(previousStatus)
                .newStatus(transaction.getStatus().name())
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Payment failed event created for transaction: {}", transaction.getId());
    }
    
    /**
     * Create refund completed event
     */
    @Transactional
    public void createRefundCompletedEvent(PaymentTransaction transaction, BigDecimal refundAmount) {
        Map<String, Object> eventData = Map.of(
                "refundAmount", refundAmount,
                "originalAmount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "gateway", transaction.getPaymentGateway().name()
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType(PaymentEvent.EventTypes.PAYMENT_REFUNDED)
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Refund completed event created for transaction: {}", transaction.getId());
    }
    
    /**
     * Create partial refund event
     */
    @Transactional
    public void createPartialRefundEvent(PaymentTransaction transaction, BigDecimal refundAmount) {
        Map<String, Object> eventData = Map.of(
                "refundAmount", refundAmount,
                "originalAmount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "gateway", transaction.getPaymentGateway().name(),
                "refundType", "partial"
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType(PaymentEvent.EventTypes.PAYMENT_PARTIALLY_REFUNDED)
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Partial refund event created for transaction: {}", transaction.getId());
    }
    
    /**
     * Create refund cancelled event
     */
    @Transactional
    public void createRefundCancelledEvent(PaymentTransaction transaction, String refundId, String reason) {
        Map<String, Object> eventData = Map.of(
                "refundId", refundId,
                "reason", reason,
                "originalAmount", transaction.getAmount(),
                "currency", transaction.getCurrency()
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .transaction(transaction)
                .eventType("refund.cancelled")
                .eventSource(PaymentEvent.EventSources.API)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Refund cancelled event created for transaction: {}", transaction.getId());
    }
    
    /**
     * Create subscription activated event
     */
    @Transactional
    public void createSubscriptionActivatedEvent(UserSubscription subscription, PaymentTransaction transaction) {
        PaymentEvent event = PaymentEvent.subscriptionActivated(subscription, transaction);
        paymentEventRepository.save(event);
        log.debug("Subscription activated event created for subscription: {}", subscription.getId());
    }
    
    /**
     * Create subscription cancelled event
     */
    @Transactional
    public void createSubscriptionCancelledEvent(UserSubscription subscription, String reason) {
        Map<String, Object> eventData = Map.of(
                "subscriptionId", subscription.getId().toString(),
                "planId", subscription.getSubscriptionPlan().getId().toString(),
                "planName", subscription.getSubscriptionPlan().getName(),
                "reason", reason,
                "cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd(),
                "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString()
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .subscription(subscription)
                .eventType(PaymentEvent.EventTypes.SUBSCRIPTION_CANCELLED)
                .eventSource(PaymentEvent.EventSources.API)
                .newStatus(subscription.getStatus().name())
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Subscription cancelled event created for subscription: {}", subscription.getId());
    }
    
    /**
     * Create subscription renewed event
     */
    @Transactional
    public void createSubscriptionRenewedEvent(UserSubscription subscription, PaymentTransaction transaction) {
        Map<String, Object> eventData = Map.of(
                "subscriptionId", subscription.getId().toString(),
                "transactionId", transaction.getId().toString(),
                "planId", subscription.getSubscriptionPlan().getId().toString(),
                "planName", subscription.getSubscriptionPlan().getName(),
                "amount", subscription.getAmount(),
                "billingCycle", subscription.getSubscriptionPlan().getBillingCycle().name(),
                "currentPeriodStart", subscription.getCurrentPeriodStart().toString(),
                "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString()
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .subscription(subscription)
                .transaction(transaction)
                .eventType(PaymentEvent.EventTypes.SUBSCRIPTION_RENEWED)
                .eventSource(PaymentEvent.EventSources.SCHEDULED)
                .newStatus(subscription.getStatus().name())
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Subscription renewed event created for subscription: {}", subscription.getId());
    }
    
    /**
     * Create subscription expired event
     */
    @Transactional
    public void createSubscriptionExpiredEvent(UserSubscription subscription) {
        Map<String, Object> eventData = Map.of(
                "subscriptionId", subscription.getId().toString(),
                "planId", subscription.getSubscriptionPlan().getId().toString(),
                "planName", subscription.getSubscriptionPlan().getName(),
                "expiredAt", subscription.getCurrentPeriodEnd().toString()
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .subscription(subscription)
                .eventType(PaymentEvent.EventTypes.SUBSCRIPTION_EXPIRED)
                .eventSource(PaymentEvent.EventSources.SCHEDULED)
                .previousStatus("ACTIVE")
                .newStatus(subscription.getStatus().name())
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Subscription expired event created for subscription: {}", subscription.getId());
    }
    
    /**
     * Create webhook received event
     */
    @Transactional
    public void createWebhookReceivedEvent(String eventType, String gatewayEventId, String signature, Map<String, Object> data) {
        PaymentEvent event = PaymentEvent.webhookReceived(eventType, gatewayEventId, signature, data);
        paymentEventRepository.save(event);
        log.debug("Webhook received event created for event type: {}", eventType);
    }
    
    /**
     * Create webhook processed event
     */
    @Transactional
    public void createWebhookProcessedEvent(String eventType, String gatewayEventId, String processingResult) {
        Map<String, Object> eventData = Map.of(
                "gatewayEventId", gatewayEventId,
                "processingResult", processingResult
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType(PaymentEvent.EventTypes.WEBHOOK_PROCESSED)
                .eventSource(PaymentEvent.EventSources.INTERNAL)
                .gatewayEventId(gatewayEventId)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Webhook processed event created for event type: {}", eventType);
    }
    
    /**
     * Create webhook failed event
     */
    @Transactional
    public void createWebhookFailedEvent(String eventType, String gatewayEventId, String errorMessage) {
        Map<String, Object> eventData = Map.of(
                "gatewayEventId", gatewayEventId,
                "errorMessage", errorMessage
        );
        
        PaymentEvent event = PaymentEvent.builder()
                .eventType(PaymentEvent.EventTypes.WEBHOOK_FAILED)
                .eventSource(PaymentEvent.EventSources.INTERNAL)
                .gatewayEventId(gatewayEventId)
                .eventData(eventData)
                .build();
        
        paymentEventRepository.save(event);
        log.debug("Webhook failed event created for event type: {}", eventType);
    }
}