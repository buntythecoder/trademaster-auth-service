package com.trademaster.payment.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Internal Refund Initiation Response DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Provides refund processing status and details.
 * Used by Trading Service for automated refund workflows.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #18: Descriptive naming
 *
 * @param refundId Refund transaction UUID
 * @param status Refund status (PENDING, COMPLETED, FAILED)
 * @param amount Refund amount
 * @param currency Refund currency code (USD, EUR, etc.)
 * @param timestamp Response generation timestamp
 * @param correlationId Distributed tracing correlation ID
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record InternalRefundInitiationResponse(
    String refundId,
    String status,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    String correlationId
) {
    /**
     * Check if refund is completed
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if refund is pending
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
}
