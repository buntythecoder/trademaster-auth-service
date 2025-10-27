package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Exchange Distribution DTO
 *
 * Immutable record representing exchange-based distribution breakdown.
 * Used by PositionService.getExchangeDistribution() for geographic diversification analysis.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <25ms for distribution calculations
 *
 * @param exchange Exchange name (NSE, BSE, NYSE, NASDAQ, etc.)
 * @param value Total value on this exchange
 * @param percentage Percentage of portfolio (0-100)
 * @param positionCount Number of positions on this exchange
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record ExchangeDistribution(
    String exchange,
    BigDecimal value,
    BigDecimal percentage,
    int positionCount
) {

    /**
     * Compact constructor with validation
     */
    public ExchangeDistribution {
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("Exchange cannot be null or blank");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage must be non-negative");
        }
        if (positionCount < 0) {
            throw new IllegalArgumentException("Position count cannot be negative");
        }
    }

    /**
     * Factory method for exchange distribution
     *
     * @param exchange Exchange name
     * @param value Total value
     * @param percentage Percentage
     * @param positionCount Position count
     * @return New ExchangeDistribution instance
     */
    public static ExchangeDistribution of(
            String exchange,
            BigDecimal value,
            BigDecimal percentage,
            int positionCount) {
        return new ExchangeDistribution(exchange, value, percentage, positionCount);
    }

    /**
     * Check if this exchange has significant exposure (>15%)
     *
     * @return true if exposure exceeds 15%
     */
    public boolean isSignificantExposure() {
        return percentage.compareTo(new BigDecimal("15.0")) > 0;
    }

    /**
     * Check if this exchange is dominant (>60%)
     *
     * @return true if exposure exceeds 60%
     */
    public boolean isDominantExchange() {
        return percentage.compareTo(new BigDecimal("60.0")) > 0;
    }

    /**
     * Calculate average value per position on this exchange
     *
     * @return Average value per position
     */
    public BigDecimal averageValuePerPosition() {
        if (positionCount == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(new BigDecimal(positionCount), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Check if this is an Indian exchange (NSE or BSE)
     *
     * @return true if NSE or BSE
     */
    public boolean isIndianExchange() {
        return "NSE".equalsIgnoreCase(exchange) || "BSE".equalsIgnoreCase(exchange);
    }

    /**
     * Check if this is a US exchange (NYSE or NASDAQ)
     *
     * @return true if NYSE or NASDAQ
     */
    public boolean isUSExchange() {
        return "NYSE".equalsIgnoreCase(exchange) || "NASDAQ".equalsIgnoreCase(exchange);
    }
}
