package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Position Statistics DTO
 *
 * Immutable record representing comprehensive position performance statistics.
 * Used by PositionService.getPositionStatistics() for portfolio performance analysis.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <50ms for statistics calculations
 *
 * @param totalPositions Total number of positions (open + closed)
 * @param openPositions Number of currently open positions
 * @param closedPositions Number of closed positions
 * @param profitablePositions Number of positions with positive P&L
 * @param losingPositions Number of positions with negative P&L
 * @param winRate Win rate percentage (0-100)
 * @param totalRealizedPnL Total realized P&L
 * @param totalUnrealizedPnL Total unrealized P&L
 * @param largestGain Largest single position gain
 * @param largestLoss Largest single position loss (absolute value)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PositionStatistics(
    int totalPositions,
    int openPositions,
    int closedPositions,
    int profitablePositions,
    int losingPositions,
    BigDecimal winRate,
    BigDecimal totalRealizedPnL,
    BigDecimal totalUnrealizedPnL,
    BigDecimal largestGain,
    BigDecimal largestLoss
) {

    /**
     * Compact constructor with functional validation - eliminates all if-statements with Optional
     */
    public PositionStatistics {
        // Validate integer fields - eliminates if-statements with Optional.of().filter().orElseThrow()
        Optional.of(totalPositions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Total positions cannot be negative"));

        Optional.of(openPositions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Open positions cannot be negative"));

        Optional.of(closedPositions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Closed positions cannot be negative"));

        Optional.of(profitablePositions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Profitable positions cannot be negative"));

        Optional.of(losingPositions)
            .filter(count -> count >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Losing positions cannot be negative"));

        // Validate BigDecimal fields - eliminates if-statements with Optional.ofNullable().filter().orElseThrow()
        Optional.ofNullable(winRate)
            .filter(rate -> rate.compareTo(BigDecimal.ZERO) >= 0)
            .orElseThrow(() -> new IllegalArgumentException("Win rate must be non-negative"));

        Optional.ofNullable(totalRealizedPnL)
            .orElseThrow(() -> new IllegalArgumentException("Total realized P&L cannot be null"));

        Optional.ofNullable(totalUnrealizedPnL)
            .orElseThrow(() -> new IllegalArgumentException("Total unrealized P&L cannot be null"));

        Optional.ofNullable(largestGain)
            .orElseThrow(() -> new IllegalArgumentException("Largest gain cannot be null"));

        Optional.ofNullable(largestLoss)
            .orElseThrow(() -> new IllegalArgumentException("Largest loss cannot be null"));
    }

    /**
     * Factory method for zero statistics
     *
     * @return PositionStatistics with all zero values
     */
    public static PositionStatistics zero() {
        return new PositionStatistics(
            0, 0, 0, 0, 0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }

    /**
     * Factory method for position statistics
     *
     * @param totalPositions Total positions
     * @param openPositions Open positions
     * @param closedPositions Closed positions
     * @param profitablePositions Profitable positions
     * @param losingPositions Losing positions
     * @param winRate Win rate
     * @param totalRealizedPnL Total realized P&L
     * @param totalUnrealizedPnL Total unrealized P&L
     * @param largestGain Largest gain
     * @param largestLoss Largest loss
     * @return New PositionStatistics instance
     */
    public static PositionStatistics of(
            int totalPositions,
            int openPositions,
            int closedPositions,
            int profitablePositions,
            int losingPositions,
            BigDecimal winRate,
            BigDecimal totalRealizedPnL,
            BigDecimal totalUnrealizedPnL,
            BigDecimal largestGain,
            BigDecimal largestLoss) {
        return new PositionStatistics(
            totalPositions, openPositions, closedPositions,
            profitablePositions, losingPositions,
            winRate, totalRealizedPnL, totalUnrealizedPnL,
            largestGain, largestLoss
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
     * Calculate average P&L per position - eliminates if-statement with Optional
     *
     * @return Average P&L per position
     */
    public BigDecimal averagePnLPerPosition() {
        return Optional.of(totalPositions)
            .filter(count -> count > 0)
            .map(count -> totalPnL().divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Check if portfolio is profitable (total P&L > 0)
     *
     * @return true if profitable
     */
    public boolean isProfitable() {
        return totalPnL().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if win rate is healthy (>50%)
     *
     * @return true if win rate exceeds 50%
     */
    public boolean hasHealthyWinRate() {
        return winRate.compareTo(new BigDecimal("50.0")) > 0;
    }

    /**
     * Check if win rate is excellent (>70%)
     *
     * @return true if win rate exceeds 70%
     */
    public boolean hasExcellentWinRate() {
        return winRate.compareTo(new BigDecimal("70.0")) > 0;
    }

    /**
     * Calculate profit factor (total gains / total losses) - eliminates if-statement with Optional
     * Returns 0 if no losses
     *
     * @return Profit factor
     */
    public BigDecimal profitFactor() {
        return Optional.of(largestLoss)
            .filter(loss -> loss.compareTo(BigDecimal.ZERO) != 0)
            .map(loss -> largestGain.divide(loss, 2, java.math.RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Get performance rating based on win rate and profitability - eliminates if-else chain with Optional
     *
     * @return Performance rating (EXCELLENT, GOOD, FAIR, POOR)
     */
    public String performanceRating() {
        return Optional.of(hasExcellentWinRate() && isProfitable())
            .filter(Boolean::booleanValue)
            .map(excellent -> "EXCELLENT")
            .or(() -> Optional.of(hasHealthyWinRate() && isProfitable())
                .filter(Boolean::booleanValue)
                .map(good -> "GOOD"))
            .or(() -> Optional.of(isProfitable())
                .filter(Boolean::booleanValue)
                .map(fair -> "FAIR"))
            .orElse("POOR");
    }
}
