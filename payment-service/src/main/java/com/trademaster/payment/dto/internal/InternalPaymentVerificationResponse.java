package com.trademaster.payment.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Internal Payment Verification Response DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Used by internal services to verify payment status and completion.
 * Provides essential payment details for service-to-service integration.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #16: Factory method for entity conversion
 * - Rule #18: Descriptive naming
 *
 * @param paymentId Payment transaction UUID
 * @param status Payment status (PENDING, COMPLETED, FAILED, REFUNDED)
 * @param amount Payment amount
 * @param currency Payment currency code (USD, EUR, etc.)
 * @param timestamp Response generation timestamp
 * @param correlationId Distributed tracing correlation ID
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record InternalPaymentVerificationResponse(
    String paymentId,
    String status,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    String correlationId
) {
    /**
     * Check if payment is completed
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if payment is pending
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Check if payment failed
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
