package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Monthly P&L Summary DTO
 *
 * Immutable record representing monthly profit/loss summary.
 * Used by TransactionService for monthly P&L reporting and analytics.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 *
 * @param year Year
 * @param month Month (1-12)
 * @param realizedPnL Realized profit/loss for the month
 * @param transactionCount Number of transactions in the month
 * @param totalFees Total fees and commissions for the month
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record MonthlyPnLSummary(
    Integer year,
    Integer month,
    BigDecimal realizedPnL,
    Integer transactionCount,
    BigDecimal totalFees
) {
    /**
     * Compact constructor with validation
     */
    public MonthlyPnLSummary {
        if (year == null || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        if (transactionCount == null || transactionCount < 0) {
            throw new IllegalArgumentException("Transaction count cannot be negative");
        }
        if (realizedPnL == null) realizedPnL = BigDecimal.ZERO;
        if (totalFees == null) totalFees = BigDecimal.ZERO;
    }

    /**
     * Calculate net P&L after fees
     *
     * @return Realized P&L - total fees
     */
    public BigDecimal netPnL() {
        return realizedPnL.subtract(totalFees);
    }

    /**
     * Check if the month was profitable
     *
     * @return true if net P&L > 0
     */
    public boolean isProfitable() {
        return netPnL().compareTo(BigDecimal.ZERO) > 0;
    }
}
