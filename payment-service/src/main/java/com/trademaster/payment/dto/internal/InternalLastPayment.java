package com.trademaster.payment.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Internal Last Payment Details DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Represents the most recent payment transaction for a user.
 * Used as part of UserPaymentSummary for quick payment history access.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #18: Descriptive naming
 *
 * @param amount Payment amount
 * @param currency Payment currency code (USD, EUR, etc.)
 * @param date Payment transaction date
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record InternalLastPayment(
    BigDecimal amount,
    String currency,
    Instant date
) {}
