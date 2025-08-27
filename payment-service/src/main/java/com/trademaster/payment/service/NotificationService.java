package com.trademaster.payment.service;

import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.UserSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Service
 * 
 * Handles sending notifications for payment events via Kafka.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String PAYMENT_NOTIFICATION_TOPIC = "payment-notifications";
    private static final String SUBSCRIPTION_NOTIFICATION_TOPIC = "subscription-notifications";
    
    /**
     * Send payment confirmation notification
     */
    public void sendPaymentConfirmation(PaymentTransaction transaction) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "payment_confirmation",
                    "userId", transaction.getUserId().toString(),
                    "transactionId", transaction.getId().toString(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "gateway", transaction.getPaymentGateway().name(),
                    "timestamp", transaction.getProcessedAt() != null ? 
                            transaction.getProcessedAt().toString() : 
                            transaction.getCreatedAt().toString()
            );
            
            kafkaTemplate.send(PAYMENT_NOTIFICATION_TOPIC, transaction.getId().toString(), notification);
            log.info("Payment confirmation notification sent for transaction: {}", transaction.getId());
            
        } catch (Exception e) {
            log.error("Failed to send payment confirmation notification for transaction: {}", 
                    transaction.getId(), e);
        }
    }
    
    /**
     * Send payment failure notification
     */
    public void sendPaymentFailure(PaymentTransaction transaction, String failureReason) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "payment_failure",
                    "userId", transaction.getUserId().toString(),
                    "transactionId", transaction.getId().toString(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "gateway", transaction.getPaymentGateway().name(),
                    "failureReason", failureReason,
                    "timestamp", transaction.getUpdatedAt().toString()
            );
            
            kafkaTemplate.send(PAYMENT_NOTIFICATION_TOPIC, transaction.getId().toString(), notification);
            log.info("Payment failure notification sent for transaction: {}", transaction.getId());
            
        } catch (Exception e) {
            log.error("Failed to send payment failure notification for transaction: {}", 
                    transaction.getId(), e);
        }
    }
    
    /**
     * Send refund confirmation notification
     */
    public void sendRefundConfirmation(UUID userId, BigDecimal refundAmount, String currency, String refundId) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "refund_confirmation",
                    "userId", userId.toString(),
                    "refundId", refundId,
                    "amount", refundAmount,
                    "currency", currency,
                    "timestamp", java.time.Instant.now().toString()
            );
            
            kafkaTemplate.send(PAYMENT_NOTIFICATION_TOPIC, refundId, notification);
            log.info("Refund confirmation notification sent for refund: {}", refundId);
            
        } catch (Exception e) {
            log.error("Failed to send refund confirmation notification for refund: {}", refundId, e);
        }
    }
    
    /**
     * Send subscription activation notification
     */
    public void sendSubscriptionActivation(UserSubscription subscription) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "subscription_activation",
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "planId", subscription.getSubscriptionPlan().getId().toString(),
                    "planName", subscription.getSubscriptionPlan().getName(),
                    "amount", subscription.getAmount(),
                    "billingCycle", subscription.getSubscriptionPlan().getBillingCycle().name(),
                    "currentPeriodStart", subscription.getCurrentPeriodStart().toString(),
                    "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "timestamp", subscription.getActivatedAt() != null ? 
                            subscription.getActivatedAt().toString() : 
                            subscription.getCreatedAt().toString()
            );
            
            kafkaTemplate.send(SUBSCRIPTION_NOTIFICATION_TOPIC, subscription.getId().toString(), notification);
            log.info("Subscription activation notification sent for subscription: {}", subscription.getId());
            
        } catch (Exception e) {
            log.error("Failed to send subscription activation notification for subscription: {}", 
                    subscription.getId(), e);
        }
    }
    
    /**
     * Send subscription cancellation notification
     */
    public void sendSubscriptionCancellation(UserSubscription subscription, String reason) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "subscription_cancellation",
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "planName", subscription.getSubscriptionPlan().getName(),
                    "reason", reason,
                    "cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd(),
                    "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "timestamp", subscription.getCancelledAt() != null ? 
                            subscription.getCancelledAt().toString() : 
                            java.time.Instant.now().toString()
            );
            
            kafkaTemplate.send(SUBSCRIPTION_NOTIFICATION_TOPIC, subscription.getId().toString(), notification);
            log.info("Subscription cancellation notification sent for subscription: {}", subscription.getId());
            
        } catch (Exception e) {
            log.error("Failed to send subscription cancellation notification for subscription: {}", 
                    subscription.getId(), e);
        }
    }
    
    /**
     * Send subscription renewal notification
     */
    public void sendSubscriptionRenewal(UserSubscription subscription, PaymentTransaction transaction) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "subscription_renewal",
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "transactionId", transaction.getId().toString(),
                    "planName", subscription.getSubscriptionPlan().getName(),
                    "amount", subscription.getAmount(),
                    "currentPeriodStart", subscription.getCurrentPeriodStart().toString(),
                    "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "timestamp", java.time.Instant.now().toString()
            );
            
            kafkaTemplate.send(SUBSCRIPTION_NOTIFICATION_TOPIC, subscription.getId().toString(), notification);
            log.info("Subscription renewal notification sent for subscription: {}", subscription.getId());
            
        } catch (Exception e) {
            log.error("Failed to send subscription renewal notification for subscription: {}", 
                    subscription.getId(), e);
        }
    }
    
    /**
     * Send subscription renewal failure notification
     */
    public void sendSubscriptionRenewalFailure(UserSubscription subscription, String failureReason) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "subscription_renewal_failure",
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "planName", subscription.getSubscriptionPlan().getName(),
                    "amount", subscription.getAmount(),
                    "failureReason", failureReason,
                    "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "timestamp", java.time.Instant.now().toString()
            );
            
            kafkaTemplate.send(SUBSCRIPTION_NOTIFICATION_TOPIC, subscription.getId().toString(), notification);
            log.info("Subscription renewal failure notification sent for subscription: {}", subscription.getId());
            
        } catch (Exception e) {
            log.error("Failed to send subscription renewal failure notification for subscription: {}", 
                    subscription.getId(), e);
        }
    }
    
    /**
     * Send subscription expiry warning notification
     */
    public void sendSubscriptionExpiryWarning(UserSubscription subscription, int daysUntilExpiry) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "subscription_expiry_warning",
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "planName", subscription.getSubscriptionPlan().getName(),
                    "currentPeriodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "daysUntilExpiry", daysUntilExpiry,
                    "timestamp", java.time.Instant.now().toString()
            );
            
            kafkaTemplate.send(SUBSCRIPTION_NOTIFICATION_TOPIC, subscription.getId().toString(), notification);
            log.info("Subscription expiry warning notification sent for subscription: {} ({} days)", 
                    subscription.getId(), daysUntilExpiry);
            
        } catch (Exception e) {
            log.error("Failed to send subscription expiry warning notification for subscription: {}", 
                    subscription.getId(), e);
        }
    }
}