package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Asset Allocation DTO
 *
 * Immutable record representing asset class allocation breakdown.
 * Used by PositionService.getAssetAllocation() for portfolio diversification analysis.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with factory methods
 * Rule #22: Performance <25ms for allocation calculations
 *
 * @param assetClass Asset class (EQUITY, BOND, COMMODITY, CURRENCY, CRYPTO, etc.)
 * @param value Total value in this asset class
 * @param percentage Percentage of portfolio (0-100)
 * @param positionCount Number of positions in this class
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record AssetAllocation(
    String assetClass,
    BigDecimal value,
    BigDecimal percentage,
    int positionCount
) {

    /**
     * Compact constructor with validation
     */
    public AssetAllocation {
        if (assetClass == null || assetClass.isBlank()) {
            throw new IllegalArgumentException("Asset class cannot be null or blank");
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
     * Factory method for asset allocation
     *
     * @param assetClass Asset class
     * @param value Total value
     * @param percentage Percentage
     * @param positionCount Position count
     * @return New AssetAllocation instance
     */
    public static AssetAllocation of(
            String assetClass,
            BigDecimal value,
            BigDecimal percentage,
            int positionCount) {
        return new AssetAllocation(assetClass, value, percentage, positionCount);
    }

    /**
     * Check if this asset class has significant allocation (>10%)
     *
     * @return true if allocation exceeds 10%
     */
    public boolean isSignificantAllocation() {
        return percentage.compareTo(new BigDecimal("10.0")) > 0;
    }

    /**
     * Check if this asset class is dominant (>50%)
     *
     * @return true if allocation exceeds 50%
     */
    public boolean isDominantAllocation() {
        return percentage.compareTo(new BigDecimal("50.0")) > 0;
    }

    /**
     * Calculate average value per position
     *
     * @return Average value per position
     */
    public BigDecimal averageValuePerPosition() {
        if (positionCount == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(new BigDecimal(positionCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
