package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Position Metrics DTO
 *
 * Immutable record representing aggregated position metrics for analytics.
 * Used by PositionService.calculatePositionMetrics() for performance analysis.
 *
 * NOTE: This is a DTO class, different from PortfolioMetrics (Micrometer metrics collector).
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <50ms for metric calculations
 *
 * @param totalPositions Total number of positions (open + closed)
 * @param openPositions Number of currently open positions
 * @param totalMarketValue Total market value of all open positions
 * @param totalCost Total cost basis of all positions
 * @param totalUnrealizedPnL Total unrealized P&L
 * @param totalRealizedPnL Total realized P&L
 * @param averageConcentration Average position concentration (%)
 * @param largestPositionPercent Largest single position as percentage
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PositionMetrics(
    int totalPositions,
    int openPositions,
    BigDecimal totalMarketValue,
    BigDecimal totalCost,
    BigDecimal totalUnrealizedPnL,
    BigDecimal totalRealizedPnL,
    BigDecimal averageConcentration,
    BigDecimal largestPositionPercent
) {

    /**
     * Compact constructor with validation
     */
    public PositionMetrics {
        if (totalPositions < 0) {
            throw new IllegalArgumentException("Total positions cannot be negative");
        }
        if (openPositions < 0 || openPositions > totalPositions) {
            throw new IllegalArgumentException("Open positions must be between 0 and total positions");
        }
        if (totalMarketValue == null) {
            throw new IllegalArgumentException("Total market value cannot be null");
        }
        if (totalCost == null) {
            throw new IllegalArgumentException("Total cost cannot be null");
        }
        if (totalUnrealizedPnL == null) {
            throw new IllegalArgumentException("Total unrealized P&L cannot be null");
        }
        if (totalRealizedPnL == null) {
            throw new IllegalArgumentException("Total realized P&L cannot be null");
        }
        if (averageConcentration == null) {
            throw new IllegalArgumentException("Average concentration cannot be null");
        }
        if (largestPositionPercent == null) {
            throw new IllegalArgumentException("Largest position percent cannot be null");
        }
    }

    /**
     * Factory method for zero metrics
     *
     * @return PositionMetrics with all zero values
     */
    public static PositionMetrics zero() {
        return new PositionMetrics(
            0, 0,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    /**
     * Factory method for position metrics
     *
     * @param totalPositions Total positions
     * @param openPositions Open positions
     * @param totalMarketValue Total market value
     * @param totalCost Total cost
     * @param totalUnrealizedPnL Total unrealized P&L
     * @param totalRealizedPnL Total realized P&L
     * @param averageConcentration Average concentration
     * @param largestPositionPercent Largest position percent
     * @return New PositionMetrics instance
     */
    public static PositionMetrics of(
            int totalPositions,
            int openPositions,
            BigDecimal totalMarketValue,
            BigDecimal totalCost,
            BigDecimal totalUnrealizedPnL,
            BigDecimal totalRealizedPnL,
            BigDecimal averageConcentration,
            BigDecimal largestPositionPercent) {
        return new PositionMetrics(
            totalPositions, openPositions,
            totalMarketValue, totalCost,
            totalUnrealizedPnL, totalRealizedPnL,
            averageConcentration, largestPositionPercent
        );
    }

    /**
     * Calculate total P&L (realized + unrealized)
     *
     * @return Total P&L
     */
    public BigDecimal totalPnL() {
        return totalRealizedPnL.add(totalUnrealizedPnL);
    }

    /**
     * Calculate return on investment percentage
     *
     * @return ROI percentage
     */
    public BigDecimal returnOnInvestment() {
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPnL()
            .divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Check if portfolio is diversified (avg concentration < 5%)
     *
     * @return true if well diversified
     */
    public boolean isDiversified() {
        return averageConcentration.compareTo(new BigDecimal("5.0")) < 0;
    }

    /**
     * Check if portfolio has high concentration risk (largest > 20%)
     *
     * @return true if high concentration risk
     */
    public boolean hasHighConcentrationRisk() {
        return largestPositionPercent.compareTo(new BigDecimal("20.0")) > 0;
    }
}
