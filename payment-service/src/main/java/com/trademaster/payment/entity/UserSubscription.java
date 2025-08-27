package com.trademaster.payment.entity;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.SubscriptionStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * User Subscription Entity
 * 
 * Represents an active user subscription with billing information
 * and lifecycle management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    
    // Billing Information
    @NotNull
    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;
    
    @NotNull
    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;
    
    @Column(name = "next_billing_date")
    private Instant nextBillingDate;
    
    // Trial Information
    @Column(name = "trial_start")
    private Instant trialStart;
    
    @Column(name = "trial_end")
    private Instant trialEnd;
    
    @Builder.Default
    @Column(name = "is_trial_used", nullable = false)
    private Boolean isTrialUsed = false;
    
    // Pricing Information
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    // Gateway Information
    @Size(max = 255)
    @Column(name = "gateway_subscription_id")
    private String gatewaySubscriptionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway")
    private PaymentGateway paymentGateway;
    
    // Cancellation Information
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    @Size(max = 1000)
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Builder.Default
    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = false;
    
    // Additional metadata
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "activated_at")
    private Instant activatedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        
        if (currentPeriodStart == null) {
            currentPeriodStart = now;
        }
        
        if (currentPeriodEnd == null && subscriptionPlan != null) {
            currentPeriodEnd = calculateNextBillingDate(currentPeriodStart);
        }
        
        if (nextBillingDate == null && status == SubscriptionStatus.ACTIVE) {
            nextBillingDate = currentPeriodEnd;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
    
    public boolean isCancelled() {
        return status == SubscriptionStatus.CANCELLED;
    }
    
    public boolean isExpired() {
        return status == SubscriptionStatus.EXPIRED || 
               (currentPeriodEnd != null && currentPeriodEnd.isBefore(Instant.now()));
    }
    
    public boolean isInTrial() {
        if (trialStart == null || trialEnd == null) {
            return false;
        }
        
        Instant now = Instant.now();
        return now.isAfter(trialStart) && now.isBefore(trialEnd) && 
               status == SubscriptionStatus.ACTIVE;
    }
    
    public boolean hasTrial() {
        return subscriptionPlan != null && subscriptionPlan.hasTrial();
    }
    
    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.activatedAt = Instant.now();
        if (nextBillingDate == null) {
            this.nextBillingDate = currentPeriodEnd;
        }
    }
    
    public void cancel(String reason, boolean immediately) {
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
        
        if (immediately) {
            this.status = SubscriptionStatus.CANCELLED;
            this.nextBillingDate = null;
        } else {
            this.cancelAtPeriodEnd = true;
        }
    }
    
    public void suspend(String reason) {
        this.status = SubscriptionStatus.SUSPENDED;
        this.cancellationReason = reason;
    }
    
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.nextBillingDate = null;
    }
    
    public void renewPeriod() {
        if (subscriptionPlan != null) {
            this.currentPeriodStart = this.currentPeriodEnd;
            this.currentPeriodEnd = calculateNextBillingDate(this.currentPeriodStart);
            this.nextBillingDate = this.currentPeriodEnd;
            
            if (this.cancelAtPeriodEnd) {
                cancel("Cancelled at period end", true);
            }
        }
    }
    
    public long getDaysUntilExpiry() {
        if (currentPeriodEnd == null) {
            return 0;
        }
        
        return java.time.Duration.between(Instant.now(), currentPeriodEnd).toDays();
    }
    
    public boolean requiresPayment() {
        return !subscriptionPlan.isFree() && isActive() && !isInTrial();
    }
    
    private Instant calculateNextBillingDate(Instant from) {
        if (subscriptionPlan == null) {
            return from.plus(java.time.Duration.ofDays(30)); // Default 30 days
        }
        
        return switch (subscriptionPlan.getBillingCycle()) {
            case MONTHLY -> from.plus(java.time.Duration.ofDays(30));
            case QUARTERLY -> from.plus(java.time.Duration.ofDays(90));
            case ANNUAL -> from.plus(java.time.Duration.ofDays(365));
        };
    }
    
    public String getDisplayStatus() {
        if (isInTrial()) {
            return "Trial (expires " + java.time.LocalDate.ofInstant(trialEnd, java.time.ZoneOffset.UTC) + ")";
        }
        
        return switch (status) {
            case ACTIVE -> "Active";
            case CANCELLED -> "Cancelled";
            case EXPIRED -> "Expired";
            case SUSPENDED -> "Suspended";
            case INACTIVE -> "Inactive";
        };
    }
}