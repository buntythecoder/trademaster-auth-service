package com.trademaster.subscription.dto.internal;

import com.trademaster.subscription.enums.SubscriptionTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Tier Change Request DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Used by internal services (payment-service) to change subscription tiers.
 * Includes correlation ID for distributed tracing and effective date control.
 *
 * Compliance:
 * - Rule #6: Zero Trust Security - service ID validation
 * - Rule #9: Immutable records
 * - Rule #15: Correlation ID tracking
 * - Rule #16: Factory methods for service-specific construction
 */
public record InternalTierChangeRequest(
    @NotNull(message = "New tier is required")
    SubscriptionTier newTier,

    @NotBlank(message = "Effective date is required")
    String effectiveDate,

    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    /**
     * Factory method for payment-service tier change requests (immediate)
     *
     * @param newTier Target subscription tier
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalTierChangeRequest with immediate effect
     */
    public static InternalTierChangeRequest fromPaymentService(
            SubscriptionTier newTier,
            UUID correlationId) {
        return new InternalTierChangeRequest(newTier, "immediate", "payment-service", correlationId);
    }

    /**
     * Factory method for payment-service tier change requests (scheduled)
     *
     * @param newTier Target subscription tier
     * @param effectiveDate When to apply the tier change (ISO-8601 format or "immediate")
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalTierChangeRequest with scheduled effect
     */
    public static InternalTierChangeRequest fromPaymentServiceScheduled(
            SubscriptionTier newTier,
            String effectiveDate,
            UUID correlationId) {
        return new InternalTierChangeRequest(newTier, effectiveDate, "payment-service", correlationId);
    }

    /**
     * Factory method for notification-service tier change requests
     *
     * @param newTier Target subscription tier
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalTierChangeRequest
     */
    public static InternalTierChangeRequest fromNotificationService(
            SubscriptionTier newTier,
            UUID correlationId) {
        return new InternalTierChangeRequest(newTier, "immediate", "notification-service", correlationId);
    }

    /**
     * Factory method for portfolio-service tier change requests
     *
     * @param newTier Target subscription tier
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalTierChangeRequest
     */
    public static InternalTierChangeRequest fromPortfolioService(
            SubscriptionTier newTier,
            UUID correlationId) {
        return new InternalTierChangeRequest(newTier, "immediate", "portfolio-service", correlationId);
    }
}
