package com.trademaster.portfolio.dto;

/**
 * Holding Period DTO
 *
 * Immutable record representing holding period statistics for a position.
 * Used by PositionService.getAverageHoldingPeriod() for investment timeframe analysis.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <25ms for period calculations
 *
 * @param symbol Trading symbol
 * @param averageDays Average holding period in days
 * @param minDays Minimum holding period in days
 * @param maxDays Maximum holding period in days
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record HoldingPeriod(
    String symbol,
    long averageDays,
    long minDays,
    long maxDays
) {

    /**
     * Compact constructor with validation
     */
    public HoldingPeriod {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (averageDays < 0) {
            throw new IllegalArgumentException("Average days cannot be negative");
        }
        if (minDays < 0) {
            throw new IllegalArgumentException("Min days cannot be negative");
        }
        if (maxDays < 0) {
            throw new IllegalArgumentException("Max days cannot be negative");
        }
        if (minDays > maxDays) {
            throw new IllegalArgumentException("Min days cannot exceed max days");
        }
    }

    /**
     * Factory method for holding period
     *
     * @param symbol Trading symbol
     * @param averageDays Average days
     * @param minDays Min days
     * @param maxDays Max days
     * @return New HoldingPeriod instance
     */
    public static HoldingPeriod of(
            String symbol,
            long averageDays,
            long minDays,
            long maxDays) {
        return new HoldingPeriod(symbol, averageDays, minDays, maxDays);
    }

    /**
     * Check if this is a short-term holding (<30 days)
     *
     * @return true if average holding < 30 days
     */
    public boolean isShortTerm() {
        return averageDays < 30;
    }

    /**
     * Check if this is a medium-term holding (30-365 days)
     *
     * @return true if average holding between 30-365 days
     */
    public boolean isMediumTerm() {
        return averageDays >= 30 && averageDays <= 365;
    }

    /**
     * Check if this is a long-term holding (>365 days)
     *
     * @return true if average holding > 365 days
     */
    public boolean isLongTerm() {
        return averageDays > 365;
    }

    /**
     * Calculate holding period volatility (range)
     *
     * @return Difference between max and min days
     */
    public long holdingVolatility() {
        return maxDays - minDays;
    }

    /**
     * Get investment style based on holding period
     *
     * @return Investment style (DAY_TRADER, SWING_TRADER, POSITION_TRADER, LONG_TERM_INVESTOR)
     */
    public String investmentStyle() {
        return switch ((int) averageDays) {
            case int days when days < 1 -> "DAY_TRADER";
            case int days when days < 7 -> "SWING_TRADER";
            case int days when days < 90 -> "POSITION_TRADER";
            default -> "LONG_TERM_INVESTOR";
        };
    }

    /**
     * Calculate average holding period in weeks
     *
     * @return Average holding period in weeks
     */
    public long averageWeeks() {
        return averageDays / 7;
    }

    /**
     * Calculate average holding period in months (approximate)
     *
     * @return Average holding period in months
     */
    public long averageMonths() {
        return averageDays / 30;
    }

    /**
     * Calculate average holding period in years (approximate)
     *
     * @return Average holding period in years
     */
    public long averageYears() {
        return averageDays / 365;
    }
}
