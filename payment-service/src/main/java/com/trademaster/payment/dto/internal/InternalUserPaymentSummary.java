package com.trademaster.payment.dto.internal;

import java.time.Instant;
import java.util.Optional;

/**
 * Internal User Payment Summary DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Provides aggregated payment information for a specific user.
 * Used by Subscription Service to verify payment eligibility and history.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #11: Railway programming with Optional
 * - Rule #18: Descriptive naming
 *
 * @param userId User UUID
 * @param totalPayments Total number of payment transactions
 * @param lastPayment Most recent payment details (nullable)
 * @param timestamp Response generation timestamp
 * @param correlationId Distributed tracing correlation ID
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record InternalUserPaymentSummary(
    String userId,
    int totalPayments,
    InternalLastPayment lastPayment,
    Instant timestamp,
    String correlationId
) {
    /**
     * Get last payment as Optional
     *
     * @return Optional containing last payment or empty if none exists
     */
    public Optional<InternalLastPayment> getLastPaymentOptional() {
        return Optional.ofNullable(lastPayment);
    }

    /**
     * Check if user has any payments
     *
     * @return true if totalPayments > 0
     */
    public boolean hasPayments() {
        return totalPayments > 0;
    }
}
