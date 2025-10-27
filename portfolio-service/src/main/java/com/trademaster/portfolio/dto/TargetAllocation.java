package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Target Allocation DTO
 *
 * Defines target asset allocation for portfolio rebalancing.
 * Supports multiple allocation strategies and constraints.
 *
 * Rule #9: Immutable Records for Data Transfer Objects
 * Rule #3: Functional Programming - Immutable data structures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TargetAllocation(
    String symbol,
    BigDecimal targetPercentage,
    BigDecimal minPercentage,
    BigDecimal maxPercentage,
    String assetClass,
    String sector
) {

    /**
     * Compact constructor with validation
     * Rule #9: Validation in record compact constructors
     */
    public TargetAllocation {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (targetPercentage == null || targetPercentage.compareTo(BigDecimal.ZERO) < 0
                || targetPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Target percentage must be between 0 and 100");
        }
        if (minPercentage != null && minPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Min percentage cannot be negative");
        }
        if (maxPercentage != null && maxPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Max percentage cannot exceed 100");
        }
        if (minPercentage != null && maxPercentage != null
                && minPercentage.compareTo(maxPercentage) > 0) {
            throw new IllegalArgumentException("Min percentage cannot exceed max percentage");
        }
    }

    /**
     * Factory method for simple target allocation
     * Rule #4: Factory Pattern with functional construction
     */
    public static TargetAllocation of(String symbol, BigDecimal targetPercentage) {
        return new TargetAllocation(
            symbol,
            targetPercentage,
            targetPercentage.subtract(BigDecimal.valueOf(5)), // ±5% default tolerance
            targetPercentage.add(BigDecimal.valueOf(5)),
            "UNKNOWN",
            "UNKNOWN"
        );
    }

    /**
     * Check if current percentage is within acceptable range
     * Rule #3: Functional predicate method
     */
    public boolean isWithinRange(BigDecimal currentPercentage) {
        return currentPercentage.compareTo(minPercentage) >= 0
            && currentPercentage.compareTo(maxPercentage) <= 0;
    }

    /**
     * Calculate deviation from target
     * Rule #3: Functional calculation method
     */
    public BigDecimal calculateDeviation(BigDecimal currentPercentage) {
        return currentPercentage.subtract(targetPercentage);
    }
}
