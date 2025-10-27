package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Valuation Result DTO
 *
 * Immutable record representing comprehensive portfolio valuation with P&L breakdown.
 * Used by PnLCalculationService for real-time portfolio valuation and performance tracking.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 * Rule #22: Performance <50ms for valuation calculations
 *
 * @param portfolioId Portfolio identifier
 * @param totalValue Total portfolio value (cash + positions)
 * @param cashBalance Available cash balance
 * @param positionsValue Total value of all positions
 * @param unrealizedPnl Unrealized profit/loss on open positions
 * @param realizedPnl Realized profit/loss from closed positions
 * @param dayPnl Day profit/loss (today's change)
 * @param totalReturn Total return percentage
 * @param positionsCount Number of open positions
 * @param valuationTime Valuation timestamp
 * @param calculationTimeMs Calculation time in milliseconds
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PortfolioValuationResult(
    Long portfolioId,
    BigDecimal totalValue,
    BigDecimal cashBalance,
    BigDecimal positionsValue,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal dayPnl,
    BigDecimal totalReturn,
    Integer positionsCount,
    Instant valuationTime,
    Long calculationTimeMs
) {
    /**
     * Compact constructor with validation
     */
    public PortfolioValuationResult {
        if (portfolioId == null || portfolioId <= 0) {
            throw new IllegalArgumentException("Portfolio ID must be positive");
        }
        if (totalValue == null) totalValue = BigDecimal.ZERO;
        if (cashBalance == null) cashBalance = BigDecimal.ZERO;
        if (positionsValue == null) positionsValue = BigDecimal.ZERO;
        if (unrealizedPnl == null) unrealizedPnl = BigDecimal.ZERO;
        if (realizedPnl == null) realizedPnl = BigDecimal.ZERO;
        if (dayPnl == null) dayPnl = BigDecimal.ZERO;
        if (totalReturn == null) totalReturn = BigDecimal.ZERO;
        if (positionsCount == null) positionsCount = 0;
        if (valuationTime == null) valuationTime = Instant.now();
        if (calculationTimeMs == null) calculationTimeMs = 0L;
    }

    /**
     * Check if portfolio is profitable
     *
     * @return true if total return > 0
     */
    public boolean isProfitable() {
        return totalReturn.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculate total P&L (realized + unrealized)
     *
     * @return Total profit/loss
     */
    public BigDecimal totalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }

    /**
     * Check if valuation is recent (within last minute)
     *
     * @return true if valuation is recent
     */
    public boolean isRecent() {
        return Instant.now().minusSeconds(60).isBefore(valuationTime);
    }

    /**
     * Check if calculation was fast (< 50ms)
     *
     * @return true if calculation time < 50ms
     */
    public boolean isFastCalculation() {
        return calculationTimeMs < 50;
    }
}
