package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Position Concentration Analysis DTO
 *
 * Immutable record representing concentration risk data for a single position.
 * Used by PositionService.calculateConcentrationRisk() to analyze portfolio diversification.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <10ms for risk calculations
 *
 * @param symbol Trading symbol
 * @param concentrationPercent Percentage of portfolio value (0-100)
 * @param marketValue Current market value
 * @param riskLevel Risk classification (LOW, MEDIUM, HIGH, CRITICAL)
 * @param exchange Stock exchange
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PositionConcentration(
    String symbol,
    BigDecimal concentrationPercent,
    BigDecimal marketValue,
    String riskLevel,
    String exchange
) {

    /**
     * Compact constructor with validation
     */
    public PositionConcentration {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (concentrationPercent == null || concentrationPercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Concentration percent must be non-negative");
        }
        if (marketValue == null) {
            throw new IllegalArgumentException("Market value cannot be null");
        }
        if (riskLevel == null || riskLevel.isBlank()) {
            throw new IllegalArgumentException("Risk level cannot be null or blank");
        }
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("Exchange cannot be null or blank");
        }
    }

    /**
     * Factory method for position concentration
     *
     * @param symbol Trading symbol
     * @param concentrationPercent Concentration percentage
     * @param marketValue Market value
     * @param riskLevel Risk level
     * @param exchange Exchange
     * @return New PositionConcentration instance
     */
    public static PositionConcentration of(
            String symbol,
            BigDecimal concentrationPercent,
            BigDecimal marketValue,
            String riskLevel,
            String exchange) {
        return new PositionConcentration(symbol, concentrationPercent, marketValue, riskLevel, exchange);
    }

    /**
     * Check if position has high concentration risk
     *
     * @return true if concentration exceeds 10%
     */
    public boolean isHighConcentration() {
        return concentrationPercent.compareTo(new BigDecimal("10.0")) > 0;
    }

    /**
     * Check if position has critical concentration risk
     *
     * @return true if concentration exceeds 20%
     */
    public boolean isCriticalConcentration() {
        return concentrationPercent.compareTo(new BigDecimal("20.0")) > 0;
    }
}
