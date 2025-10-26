package com.trademaster.subscription.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Suspend Request DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Used by internal services (payment-service) to suspend subscriptions.
 * Includes correlation ID for distributed tracing.
 *
 * Compliance:
 * - Rule #6: Zero Trust Security - service ID validation
 * - Rule #9: Immutable records
 * - Rule #15: Correlation ID tracking
 * - Rule #16: Factory methods for service-specific construction
 */
public record InternalSuspendRequest(
    @NotBlank(message = "Suspension reason is required")
    String reason,

    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    /**
     * Factory method for payment-service suspend requests
     *
     * @param reason Suspension reason
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalSuspendRequest
     */
    public static InternalSuspendRequest fromPaymentService(String reason, UUID correlationId) {
        return new InternalSuspendRequest(reason, "payment-service", correlationId);
    }

    /**
     * Factory method for notification-service suspend requests
     *
     * @param reason Suspension reason
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalSuspendRequest
     */
    public static InternalSuspendRequest fromNotificationService(String reason, UUID correlationId) {
        return new InternalSuspendRequest(reason, "notification-service", correlationId);
    }

    /**
     * Factory method for portfolio-service suspend requests
     *
     * @param reason Suspension reason
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalSuspendRequest
     */
    public static InternalSuspendRequest fromPortfolioService(String reason, UUID correlationId) {
        return new InternalSuspendRequest(reason, "portfolio-service", correlationId);
    }
}
