package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Transaction Summary DTO
 *
 * Immutable record representing comprehensive transaction summary for a date range.
 * Used by TransactionService for reporting and analytics.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 * Rule #22: Performance <200ms for summary generation
 *
 * @param portfolioId Portfolio identifier
 * @param fromDate Summary start date
 * @param toDate Summary end date
 * @param totalTransactions Total number of transactions
 * @param tradeExecutions Number of trade executions
 * @param cashTransactions Number of cash transactions
 * @param dividendPayments Number of dividend payments
 * @param totalBuyVolume Total value of buy transactions
 * @param totalSellVolume Total value of sell transactions
 * @param realizedPnL Total realized P&L
 * @param totalFees Total fees and commissions
 * @param netCashFlow Net cash flow (in - out)
 * @param dividendIncome Total dividend income
 * @param interestIncome Total interest income
 * @param symbolsTraded Number of unique symbols traded
 * @param averageTradeSize Average trade value
 * @param generatedAt Report generation timestamp
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TransactionSummary(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    Integer totalTransactions,
    Integer tradeExecutions,
    Integer cashTransactions,
    Integer dividendPayments,
    BigDecimal totalBuyVolume,
    BigDecimal totalSellVolume,
    BigDecimal realizedPnL,
    BigDecimal totalFees,
    BigDecimal netCashFlow,
    BigDecimal dividendIncome,
    BigDecimal interestIncome,
    Integer symbolsTraded,
    BigDecimal averageTradeSize,
    Instant generatedAt
) {

    /**
     * Compact constructor with functional validation - eliminates all if-statements with Optional
     */
    public TransactionSummary {
        // Validate required fields - eliminates if-statements with Optional.filter().orElseThrow()
        Optional.ofNullable(portfolioId)
            .filter(id -> id > 0)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio ID must be positive"));

        Optional.ofNullable(fromDate)
            .orElseThrow(() -> new IllegalArgumentException("From date cannot be null"));

        Optional.ofNullable(toDate)
            .orElseThrow(() -> new IllegalArgumentException("To date cannot be null"));

        Optional.of(fromDate)
            .filter(from -> !from.isAfter(toDate))
            .orElseThrow(() -> new IllegalArgumentException("From date must be before or equal to date"));

        Optional.ofNullable(totalTransactions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Total transactions cannot be negative"));

        Optional.ofNullable(tradeExecutions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Trade executions cannot be negative"));

        Optional.ofNullable(cashTransactions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Cash transactions cannot be negative"));

        Optional.ofNullable(dividendPayments)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Dividend payments cannot be negative"));

        // Default null values - eliminates if-statements with Optional.ofNullable().orElse()
        totalBuyVolume = Optional.ofNullable(totalBuyVolume).orElse(BigDecimal.ZERO);
        totalSellVolume = Optional.ofNullable(totalSellVolume).orElse(BigDecimal.ZERO);
        realizedPnL = Optional.ofNullable(realizedPnL).orElse(BigDecimal.ZERO);
        totalFees = Optional.ofNullable(totalFees).orElse(BigDecimal.ZERO);
        netCashFlow = Optional.ofNullable(netCashFlow).orElse(BigDecimal.ZERO);
        dividendIncome = Optional.ofNullable(dividendIncome).orElse(BigDecimal.ZERO);
        interestIncome = Optional.ofNullable(interestIncome).orElse(BigDecimal.ZERO);
        symbolsTraded = Optional.ofNullable(symbolsTraded).orElse(0);
        averageTradeSize = Optional.ofNullable(averageTradeSize).orElse(BigDecimal.ZERO);
        generatedAt = Optional.ofNullable(generatedAt).orElseGet(Instant::now);
    }

    /**
     * Factory method for zero summary
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return TransactionSummary with all zero values
     */
    public static TransactionSummary zero(Long portfolioId, Instant fromDate, Instant toDate) {
        return new TransactionSummary(
            portfolioId,
            fromDate,
            toDate,
            0, 0, 0, 0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            BigDecimal.ZERO,
            Instant.now()
        );
    }

    /**
     * Factory method for creating transaction summary
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @param totalTransactions Total transactions
     * @param tradeExecutions Trade executions
     * @param cashTransactions Cash transactions
     * @param dividendPayments Dividend payments
     * @param totalBuyVolume Buy volume
     * @param totalSellVolume Sell volume
     * @param realizedPnL Realized P&L
     * @param totalFees Total fees
     * @param netCashFlow Net cash flow
     * @param dividendIncome Dividend income
     * @param interestIncome Interest income
     * @param symbolsTraded Symbols traded
     * @param averageTradeSize Average trade size
     * @return New TransactionSummary instance
     */
    public static TransactionSummary of(
            Long portfolioId,
            Instant fromDate,
            Instant toDate,
            Integer totalTransactions,
            Integer tradeExecutions,
            Integer cashTransactions,
            Integer dividendPayments,
            BigDecimal totalBuyVolume,
            BigDecimal totalSellVolume,
            BigDecimal realizedPnL,
            BigDecimal totalFees,
            BigDecimal netCashFlow,
            BigDecimal dividendIncome,
            BigDecimal interestIncome,
            Integer symbolsTraded,
            BigDecimal averageTradeSize) {
        return new TransactionSummary(
            portfolioId,
            fromDate,
            toDate,
            totalTransactions,
            tradeExecutions,
            cashTransactions,
            dividendPayments,
            totalBuyVolume,
            totalSellVolume,
            realizedPnL,
            totalFees,
            netCashFlow,
            dividendIncome,
            interestIncome,
            symbolsTraded,
            averageTradeSize,
            Instant.now()
        );
    }

    /**
     * Calculate total trading volume
     *
     * @return Total buy + sell volume
     */
    public BigDecimal totalTradingVolume() {
        return totalBuyVolume.add(totalSellVolume);
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
     * Calculate total income (dividends + interest)
     *
     * @return Total income
     */
    public BigDecimal totalIncome() {
        return dividendIncome.add(interestIncome);
    }

    /**
     * Check if period was profitable
     *
     * @return true if net P&L > 0
     */
    public boolean isProfitable() {
        return netPnL().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculate average transactions per day - eliminates if-statement with Optional
     *
     * @return Average transactions per day
     */
    public BigDecimal averageTransactionsPerDay() {
        long daysBetween = java.time.Duration.between(fromDate, toDate).toDays() + 1;
        return Optional.of(daysBetween)
            .filter(days -> days > 0)
            .map(days -> new BigDecimal(totalTransactions).divide(
                new BigDecimal(days),
                2,
                java.math.RoundingMode.HALF_UP
            ))
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate fee percentage of trading volume - eliminates if-statement with Optional
     *
     * @return Fees as percentage of trading volume
     */
    public BigDecimal feePercentage() {
        BigDecimal totalVolume = totalTradingVolume();
        return Optional.of(totalVolume)
            .filter(volume -> volume.compareTo(BigDecimal.ZERO) != 0)
            .map(volume -> totalFees
                .divide(volume, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")))
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Get number of days in period
     *
     * @return Number of days
     */
    public long daysInPeriod() {
        return java.time.Duration.between(fromDate, toDate).toDays() + 1;
    }
}
