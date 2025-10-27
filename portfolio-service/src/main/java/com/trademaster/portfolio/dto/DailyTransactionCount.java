package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Daily Transaction Count DTO
 *
 * Immutable record representing transaction counts and volumes for a specific date.
 * Used by TransactionService for daily transaction reporting and analytics.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 *
 * @param date Transaction date
 * @param totalTransactions Total number of transactions
 * @param tradeExecutions Number of trade executions (BUY/SELL)
 * @param cashTransactions Number of cash transactions (DEPOSIT/WITHDRAWAL)
 * @param incomeTransactions Number of income transactions (DIVIDEND/INTEREST)
 * @param totalVolume Total transaction volume
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record DailyTransactionCount(
    Instant date,
    Integer totalTransactions,
    Integer tradeExecutions,
    Integer cashTransactions,
    Integer incomeTransactions,
    BigDecimal totalVolume
) {
    /**
     * Compact constructor with validation
     */
    public DailyTransactionCount {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (totalTransactions == null || totalTransactions < 0) {
            throw new IllegalArgumentException("Total transactions cannot be negative");
        }
        if (tradeExecutions == null) tradeExecutions = 0;
        if (cashTransactions == null) cashTransactions = 0;
        if (incomeTransactions == null) incomeTransactions = 0;
        if (totalVolume == null) totalVolume = BigDecimal.ZERO;
    }
}
