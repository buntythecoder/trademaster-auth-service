package com.trademaster.subscription.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Resume Request DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Used by internal services (payment-service) to resume suspended subscriptions.
 * Includes correlation ID for distributed tracing.
 *
 * Compliance:
 * - Rule #6: Zero Trust Security - service ID validation
 * - Rule #9: Immutable records
 * - Rule #15: Correlation ID tracking
 * - Rule #16: Factory methods for service-specific construction
 */
public record InternalResumeRequest(
    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    /**
     * Factory method for payment-service resume requests
     *
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalResumeRequest
     */
    public static InternalResumeRequest fromPaymentService(UUID correlationId) {
        return new InternalResumeRequest("payment-service", correlationId);
    }

    /**
     * Factory method for notification-service resume requests
     *
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalResumeRequest
     */
    public static InternalResumeRequest fromNotificationService(UUID correlationId) {
        return new InternalResumeRequest("notification-service", correlationId);
    }

    /**
     * Factory method for portfolio-service resume requests
     *
     * @param correlationId Distributed tracing correlation ID
     * @return Configured InternalResumeRequest
     */
    public static InternalResumeRequest fromPortfolioService(UUID correlationId) {
        return new InternalResumeRequest("portfolio-service", correlationId);
    }
}
