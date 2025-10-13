package com.trademaster.payment.entity;

import com.trademaster.payment.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Entity - Simplified for Testing
 * Represents subscription billing cycle with payment details
 *
 * Compliance:
 * - Rule 9: Entity with proper Lombok annotations
 * - Rule 16: No hardcoded values
 * - Rule 20: Builder pattern for immutable creation
 *
 * Usage:
 * - Integration testing
 * - Subscription billing workflows
 * - Recurring payment processing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_user_id", columnList = "user_id"),
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_next_billing", columnList = "next_billing_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * User who owns this subscription
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Subscription plan identifier
     */
    @Column(name = "plan_id", nullable = false, length = 100)
    private String planId;

    /**
     * Payment method used for billing
     */
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    /**
     * Subscription amount per billing cycle
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217)
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Current subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /**
     * Billing cycle frequency (MONTHLY, QUARTERLY, ANNUAL)
     */
    @Column(name = "billing_cycle", length = 20)
    private String billingCycle;

    /**
     * Next billing date for automatic payment
     */
    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * Subscription start date
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Subscription end date (if cancelled)
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Audit trail: Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit trail: Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callbacks for audit timestamps
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
