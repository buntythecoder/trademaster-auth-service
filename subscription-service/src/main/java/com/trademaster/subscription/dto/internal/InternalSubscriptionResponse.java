package com.trademaster.subscription.dto.internal;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal Subscription Response DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Used by internal services to receive subscription data from subscription-service.
 * Includes all essential subscription details for service-to-service integration.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #11: Railway programming with Optional
 * - Rule #16: Factory method for entity conversion
 */
public record InternalSubscriptionResponse(
    UUID id,
    UUID userId,
    SubscriptionTier tier,
    SubscriptionStatus status,
    BigDecimal monthlyPrice,
    BigDecimal billingAmount,
    String currency,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime nextBillingDate,
    Boolean autoRenewal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime cancelledAt,
    String cancellationReason,
    LocalDateTime upgradedDate,
    LocalDateTime activatedDate
) {
    /**
     * Factory method to convert Subscription entity to internal response
     *
     * @param subscription Source subscription entity
     * @return Internal response with all subscription details
     */
    public static InternalSubscriptionResponse fromSubscription(Subscription subscription) {
        return new InternalSubscriptionResponse(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getMonthlyPrice(),
            subscription.getBillingAmount(),
            subscription.getCurrency(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getNextBillingDate(),
            subscription.getAutoRenewal(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            subscription.getCancelledAt(),
            subscription.getCancellationReason(),
            subscription.getUpgradedDate(),
            subscription.getActivatedDate()
        );
    }

    /**
     * Check if subscription is currently active
     *
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return SubscriptionStatus.ACTIVE.equals(status);
    }

    /**
     * Check if subscription is suspended
     *
     * @return true if status is SUSPENDED
     */
    public boolean isSuspended() {
        return SubscriptionStatus.SUSPENDED.equals(status);
    }

    /**
     * Check if subscription is cancelled
     *
     * @return true if status is CANCELLED
     */
    public boolean isCancelled() {
        return SubscriptionStatus.CANCELLED.equals(status);
    }

    /**
     * Check if subscription is in trial period
     *
     * @return true if status is TRIAL
     */
    public boolean isTrial() {
        return SubscriptionStatus.TRIAL.equals(status);
    }

    /**
     * Get cancellation details if subscription is cancelled
     *
     * @return Optional containing cancellation timestamp and reason
     */
    public Optional<CancellationDetails> getCancellationDetails() {
        return Optional.ofNullable(cancelledAt)
            .map(timestamp -> new CancellationDetails(timestamp, cancellationReason));
    }

    /**
     * Check if subscription was upgraded
     *
     * @return true if upgrade date is present
     */
    public boolean wasUpgraded() {
        return upgradedDate != null;
    }

    /**
     * Cancellation details record
     */
    public record CancellationDetails(
        LocalDateTime cancelledAt,
        String reason
    ) {}
}
