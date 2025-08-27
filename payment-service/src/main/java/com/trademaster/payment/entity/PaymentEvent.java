package com.trademaster.payment.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Event Entity
 * 
 * Audit trail for all payment-related events and state changes.
 * Essential for compliance and debugging payment flows.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "payment_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private PaymentTransaction transaction;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private UserSubscription subscription;
    
    // Event Details
    @NotBlank
    @Size(max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // payment.created, payment.succeeded, subscription.updated, etc.
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "event_source", nullable = false, length = 50)
    private String eventSource; // webhook, api, internal
    
    // Event Data
    @Type(JsonType.class)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;
    
    @Size(max = 50)
    @Column(name = "previous_status", length = 50)
    private String previousStatus;
    
    @Size(max = 50)
    @Column(name = "new_status", length = 50)
    private String newStatus;
    
    // Gateway Information
    @Size(max = 255)
    @Column(name = "gateway_event_id")
    private String gatewayEventId;
    
    @Size(max = 500)
    @Column(name = "gateway_signature")
    private String gatewaySignature;
    
    // Processing Information
    @Builder.Default
    @Column(nullable = false)
    private Boolean processed = false;
    
    @Builder.Default
    @Column(name = "processing_attempts", nullable = false)
    private Integer processingAttempts = 0;
    
    @Size(max = 1000)
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;
    
    // Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    // Helper methods
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }
    
    public void markAsProcessed(String result) {
        markAsProcessed();
        if (eventData != null) {
            eventData.put("processing_result", result);
        }
    }
    
    public void recordProcessingAttempt(String error) {
        this.processingAttempts++;
        this.processingError = error;
    }
    
    public boolean hasMaxAttemptsExceeded() {
        return processingAttempts >= 3;
    }
    
    public boolean isRetryable() {
        return !processed && !hasMaxAttemptsExceeded() && 
               (processingError == null || !processingError.contains("permanent"));
    }
    
    public String getEventDescription() {
        return String.format("%s: %s -> %s", eventType, previousStatus, newStatus);
    }
    
    // Event type constants
    public static class EventTypes {
        public static final String PAYMENT_CREATED = "payment.created";
        public static final String PAYMENT_PROCESSING = "payment.processing";
        public static final String PAYMENT_COMPLETED = "payment.completed";
        public static final String PAYMENT_FAILED = "payment.failed";
        public static final String PAYMENT_CANCELLED = "payment.cancelled";
        public static final String PAYMENT_REFUNDED = "payment.refunded";
        public static final String PAYMENT_PARTIALLY_REFUNDED = "payment.partially_refunded";
        
        public static final String SUBSCRIPTION_CREATED = "subscription.created";
        public static final String SUBSCRIPTION_ACTIVATED = "subscription.activated";
        public static final String SUBSCRIPTION_CANCELLED = "subscription.cancelled";
        public static final String SUBSCRIPTION_EXPIRED = "subscription.expired";
        public static final String SUBSCRIPTION_RENEWED = "subscription.renewed";
        
        public static final String WEBHOOK_RECEIVED = "webhook.received";
        public static final String WEBHOOK_PROCESSED = "webhook.processed";
        public static final String WEBHOOK_FAILED = "webhook.failed";
    }
    
    // Event source constants
    public static class EventSources {
        public static final String WEBHOOK = "webhook";
        public static final String API = "api";
        public static final String INTERNAL = "internal";
        public static final String SCHEDULED = "scheduled";
        public static final String ADMIN = "admin";
    }
    
    // Factory methods for common events
    public static PaymentEvent paymentCreated(PaymentTransaction transaction, Map<String, Object> data) {
        return PaymentEvent.builder()
                .transaction(transaction)
                .eventType(EventTypes.PAYMENT_CREATED)
                .eventSource(EventSources.API)
                .eventData(data)
                .newStatus(transaction.getStatus().name())
                .build();
    }
    
    public static PaymentEvent paymentCompleted(PaymentTransaction transaction, String previousStatus) {
        return PaymentEvent.builder()
                .transaction(transaction)
                .eventType(EventTypes.PAYMENT_COMPLETED)
                .eventSource(EventSources.WEBHOOK)
                .previousStatus(previousStatus)
                .newStatus(transaction.getStatus().name())
                .eventData(Map.of(
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "gateway", transaction.getPaymentGateway().name()
                ))
                .build();
    }
    
    public static PaymentEvent subscriptionActivated(UserSubscription subscription, PaymentTransaction transaction) {
        return PaymentEvent.builder()
                .subscription(subscription)
                .transaction(transaction)
                .eventType(EventTypes.SUBSCRIPTION_ACTIVATED)
                .eventSource(EventSources.INTERNAL)
                .newStatus(subscription.getStatus().name())
                .eventData(Map.of(
                    "plan_id", subscription.getSubscriptionPlan().getId(),
                    "plan_name", subscription.getSubscriptionPlan().getName(),
                    "amount", subscription.getAmount(),
                    "billing_cycle", subscription.getSubscriptionPlan().getBillingCycle().name()
                ))
                .build();
    }
    
    public static PaymentEvent webhookReceived(String eventType, String gatewayEventId, 
                                             String signature, Map<String, Object> data) {
        return PaymentEvent.builder()
                .eventType(EventTypes.WEBHOOK_RECEIVED)
                .eventSource(EventSources.WEBHOOK)
                .gatewayEventId(gatewayEventId)
                .gatewaySignature(signature)
                .eventData(data)
                .build();
    }
}