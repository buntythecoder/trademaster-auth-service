package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Dividend Income Summary DTO
 *
 * Immutable record representing dividend income summary for a specific symbol.
 * Used by TransactionService for dividend income reporting and analytics.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 *
 * @param symbol Stock symbol
 * @param paymentCount Number of dividend payments
 * @param totalDividends Total dividend amount received
 * @param averageDividendPerShare Average dividend per share
 * @param firstPayment Date of first dividend payment
 * @param lastPayment Date of last dividend payment
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record DividendIncomeSummary(
    String symbol,
    Integer paymentCount,
    BigDecimal totalDividends,
    BigDecimal averageDividendPerShare,
    Instant firstPayment,
    Instant lastPayment
) {
    /**
     * Compact constructor with validation
     */
    public DividendIncomeSummary {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (paymentCount == null || paymentCount < 0) {
            throw new IllegalArgumentException("Payment count cannot be negative");
        }
        if (totalDividends == null) totalDividends = BigDecimal.ZERO;
        if (averageDividendPerShare == null) averageDividendPerShare = BigDecimal.ZERO;
    }

    /**
     * Check if there are any dividend payments
     *
     * @return true if payment count > 0
     */
    public boolean hasDividends() {
        return paymentCount > 0 && totalDividends.compareTo(BigDecimal.ZERO) > 0;
    }
}
