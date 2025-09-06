package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Asset Allocation Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents asset class allocation breakdown within a portfolio showing
 * distribution across different asset categories like equity, derivatives,
 * mutual funds, bonds, etc.
 * 
 * Used for portfolio analysis, risk assessment, and diversification monitoring.
 * Helps users understand their investment distribution and make informed
 * rebalancing decisions.
 * 
 * @param assetClass Asset class category (EQUITY, DERIVATIVE, MUTUAL_FUND, BOND, etc.)
 * @param assetClassName Human-readable asset class name
 * @param value Total value invested in this asset class
 * @param allocationPercent Percentage of total portfolio in this asset class
 * @param positionCount Number of positions in this asset class
 * @param symbols List of symbols/instruments in this asset class
 * @param avgReturn Average return for this asset class
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Asset Allocation Analysis)
 */
@Builder
public record AssetAllocation(
    String assetClass,
    String assetClassName,
    BigDecimal value,
    BigDecimal allocationPercent,
    Integer positionCount,
    List<String> symbols,
    BigDecimal avgReturn
) {
    
    /**
     * Check if this is the dominant asset class
     * 
     * @return true if allocation is more than 50%
     */
    public boolean isDominant() {
        return allocationPercent != null && allocationPercent.compareTo(BigDecimal.valueOf(50)) > 0;
    }
    
    /**
     * Check if allocation is within recommended diversification range
     * 
     * @return true if allocation is between 5% and 40%
     */
    public boolean isWellDiversified() {
        if (allocationPercent == null) {
            return false;
        }
        
        return allocationPercent.compareTo(BigDecimal.valueOf(5)) >= 0 && 
               allocationPercent.compareTo(BigDecimal.valueOf(40)) <= 0;
    }
    
    /**
     * Get risk level based on asset class
     * 
     * @return Risk level for this asset class
     */
    public RiskLevel getRiskLevel() {
        if (assetClass == null) {
            return RiskLevel.UNKNOWN;
        }
        
        return switch (assetClass.toUpperCase()) {
            case "EQUITY", "STOCK" -> RiskLevel.MEDIUM_HIGH;
            case "DERIVATIVE", "OPTIONS", "FUTURES" -> RiskLevel.HIGH;
            case "MUTUAL_FUND" -> RiskLevel.MEDIUM;
            case "BOND", "GOVERNMENT_BOND" -> RiskLevel.LOW;
            case "CORPORATE_BOND" -> RiskLevel.MEDIUM_LOW;
            case "COMMODITY" -> RiskLevel.HIGH;
            case "CRYPTO" -> RiskLevel.VERY_HIGH;
            case "CASH", "SAVINGS" -> RiskLevel.VERY_LOW;
            default -> RiskLevel.UNKNOWN;
        };
    }
    
    /**
     * Get formatted value
     * 
     * @return Formatted value string
     */
    public String getFormattedValue() {
        return formatCurrency(value);
    }
    
    /**
     * Get formatted percentage
     * 
     * @return Formatted percentage string
     */
    public String getFormattedPercentage() {
        if (allocationPercent == null) {
            return "0.0%";
        }
        
        return String.format("%.1f%%", allocationPercent);
    }
    
    /**
     * Get formatted average return
     * 
     * @return Formatted return string with sign
     */
    public String getFormattedAvgReturn() {
        if (avgReturn == null) {
            return "0.0%";
        }
        
        String sign = avgReturn.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, avgReturn);
    }
    
    /**
     * Get diversification recommendation
     * 
     * @return Recommendation string
     */
    public String getDiversificationAdvice() {
        if (allocationPercent == null) {
            return "Insufficient data for recommendation";
        }
        
        if (allocationPercent.compareTo(BigDecimal.valueOf(60)) > 0) {
            return "Consider reducing allocation - over-concentrated";
        } else if (allocationPercent.compareTo(BigDecimal.valueOf(40)) > 0) {
            return "High allocation - monitor closely";
        } else if (allocationPercent.compareTo(BigDecimal.valueOf(5)) < 0) {
            return "Low allocation - consider increasing if suitable";
        } else {
            return "Well diversified allocation";
        }
    }
    
    /**
     * Calculate allocation efficiency score (0.0 to 1.0)
     * 
     * @param benchmarkPercentage Expected or benchmark allocation
     * @return Efficiency score
     */
    public BigDecimal calculateEfficiencyScore(BigDecimal benchmarkPercentage) {
        if (allocationPercent == null || benchmarkPercentage == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal deviation = allocationPercent.subtract(benchmarkPercentage).abs();
        BigDecimal maxDeviation = benchmarkPercentage.max(BigDecimal.valueOf(100).subtract(benchmarkPercentage));
        
        if (maxDeviation.equals(BigDecimal.ZERO)) {
            return BigDecimal.ONE;
        }
        
        return BigDecimal.ONE.subtract(deviation.divide(maxDeviation, 4, java.math.RoundingMode.HALF_UP));
    }
    
    /**
     * Get top holdings in this asset class
     * 
     * @param limit Number of top holdings to return
     * @return List of top symbols
     */
    public List<String> getTopHoldings(int limit) {
        if (symbols == null) {
            return List.of();
        }
        
        return symbols.stream()
                     .limit(limit)
                     .toList();
    }
    
    /**
     * Format currency amount for display
     * 
     * @param amount Amount to format
     * @return Formatted currency string
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        
        return String.format("₹%,.2f", amount);
    }
    
    /**
     * Create summary for logging (safe, no sensitive data)
     * 
     * @return Safe summary string
     */
    public String toSafeSummary() {
        return String.format("AssetAllocation[class=%s, value=₹%.2f, percentage=%.1f%%, positions=%d]",
                           assetClass, value, allocationPercent, positionCount);
    }
    
    /**
     * Risk Level Enumeration for Asset Classes
     */
    public enum RiskLevel {
        VERY_LOW("Very Low", 1, "#28a745"),
        LOW("Low", 2, "#20c997"),
        MEDIUM_LOW("Medium-Low", 3, "#ffc107"),
        MEDIUM("Medium", 4, "#fd7e14"),
        MEDIUM_HIGH("Medium-High", 5, "#dc3545"),
        HIGH("High", 6, "#e83e8c"),
        VERY_HIGH("Very High", 7, "#6f42c1"),
        UNKNOWN("Unknown", 0, "#6c757d");
        
        private final String displayName;
        private final int level;
        private final String colorCode;
        
        RiskLevel(String displayName, int level, String colorCode) {
            this.displayName = displayName;
            this.level = level;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
        public String getColorCode() { return colorCode; }
        
        /**
         * Check if this risk level is higher than another
         * 
         * @param other Other risk level to compare
         * @return true if this level is higher risk
         */
        public boolean isHigherRiskThan(RiskLevel other) {
            return this.level > other.level;
        }
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}