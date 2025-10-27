package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Functional accumulator for monthly P&L calculations
 * 
 * Immutable record that accumulates profit/loss data by month following
 * functional programming principles with no side effects.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Functional Programming)
 */
public record MonthlyPnLAccumulator(
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal tradingVolume,
    BigDecimal fees,
    Integer transactionCount,
    Instant firstTransaction,
    Instant lastTransaction
) {
    
    /**
     * Create new empty accumulator
     */
    public MonthlyPnLAccumulator() {
        this(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            Instant.now(),
            Instant.now()
        );
    }
    
    /**
     * Add transaction data to accumulator
     */
    public MonthlyPnLAccumulator add(
        BigDecimal realizedPnlDelta,
        BigDecimal unrealizedPnlDelta,
        BigDecimal volume,
        BigDecimal feesAmount,
        Instant transactionTime
    ) {
        return new MonthlyPnLAccumulator(
            this.realizedPnl.add(realizedPnlDelta),
            this.unrealizedPnl.add(unrealizedPnlDelta),
            this.tradingVolume.add(volume),
            this.fees.add(feesAmount),
            this.transactionCount + 1,
            transactionTime.isBefore(this.firstTransaction) ? transactionTime : this.firstTransaction,
            transactionTime.isAfter(this.lastTransaction) ? transactionTime : this.lastTransaction
        );
    }
    
    /**
     * Combine two accumulators
     */
    public static MonthlyPnLAccumulator combine(MonthlyPnLAccumulator a, MonthlyPnLAccumulator b) {
        return new MonthlyPnLAccumulator(
            a.realizedPnl.add(b.realizedPnl),
            a.unrealizedPnl.add(b.unrealizedPnl),
            a.tradingVolume.add(b.tradingVolume),
            a.fees.add(b.fees),
            a.transactionCount + b.transactionCount,
            a.firstTransaction.isBefore(b.firstTransaction) ? a.firstTransaction : b.firstTransaction,
            a.lastTransaction.isAfter(b.lastTransaction) ? a.lastTransaction : b.lastTransaction
        );
    }
    
    /**
     * Convert to summary record.
     *
     * Pattern: Record construction with functional defaults
     * Rule #9: Immutable record construction
     *
     * @param portfolioId Portfolio identifier
     * @param year Year for the summary
     * @param month Month for the summary
     * @return Complete monthly PnL summary
     */
    public MonthlyPnLSummary toSummary(Long portfolioId, Integer year, Integer month) {
        return new MonthlyPnLSummary(
            portfolioId,
            java.time.YearMonth.of(year, month),
            BigDecimal.ZERO,  // openingValue - not tracked by accumulator
            BigDecimal.ZERO,  // closingValue - not tracked by accumulator
            BigDecimal.ZERO,  // monthlyReturn - not tracked by accumulator
            this.realizedPnl,
            this.unrealizedPnl,
            this.realizedPnl.add(this.unrealizedPnl),
            BigDecimal.ZERO,  // netDeposits - not tracked by accumulator
            0,  // tradingDays - not tracked by accumulator
            BigDecimal.ZERO,  // averageDailyPnL - not tracked by accumulator
            null,  // bestDay - not tracked by accumulator
            null,  // worstDay - not tracked by accumulator
            0,  // winningDays - not tracked by accumulator
            0,  // losingDays - not tracked by accumulator
            java.util.List.of(),  // topPerformers - not tracked by accumulator
            java.util.List.of(),  // topLosers - not tracked by accumulator
            BigDecimal.ZERO,  // monthlyBeta - not tracked by accumulator
            BigDecimal.ZERO   // sharpeRatio - not tracked by accumulator
        );
    }
}