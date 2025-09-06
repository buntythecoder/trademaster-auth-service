package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broker Portfolio Breakdown Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents portfolio breakdown from a single broker showing contribution
 * to the consolidated portfolio. Used for transparency and analysis of
 * broker-wise portfolio distribution.
 * 
 * Contains aggregated portfolio metrics from one broker including total value,
 * position count, performance metrics, and contribution percentages to the
 * overall consolidated portfolio.
 * 
 * @param brokerId Unique broker identifier
 * @param brokerName Display name of the broker
 * @param totalValue Total portfolio value at this broker
 * @param totalInvestment Total investment amount at this broker
 * @param unrealizedPnL Profit/loss for positions at this broker
 * @param positionCount Number of positions held at this broker
 * @param allocationPercent Percentage contribution to total portfolio
 * @param lastSynced When broker data was last synchronized
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Portfolio Analysis)
 */
@Builder
public record BrokerPortfolioBreakdown(
    String brokerId,
    String brokerName,
    BigDecimal totalValue,
    BigDecimal totalInvestment,
    BigDecimal unrealizedPnL,
    Integer positionCount,
    BigDecimal allocationPercent,
    Instant lastSynced
) {
    
    /**
     * Check if broker portfolio is profitable
     * 
     * @return true if unrealized P&L is positive
     */
    public boolean isProfitable() {
        return unrealizedPnL != null && unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Calculate P&L percentage for this broker
     * 
     * @return P&L percentage
     */
    public BigDecimal calculatePnLPercent() {
        if (totalInvestment == null || totalInvestment.equals(BigDecimal.ZERO) || unrealizedPnL == null) {
            return BigDecimal.ZERO;
        }
        
        return unrealizedPnL.divide(totalInvestment, 4, java.math.RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get formatted total value
     * 
     * @return Formatted total value string
     */
    public String getFormattedTotalValue() {
        return formatCurrency(totalValue);
    }
    
    /**
     * Get formatted P&L with sign
     * 
     * @return Formatted P&L string
     */
    public String getFormattedPnL() {
        if (unrealizedPnL == null) {
            return "₹0.00";
        }
        
        String sign = unrealizedPnL.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(unrealizedPnL);
    }
    
    /**
     * Check if broker data is stale
     * 
     * @param maxAgeMinutes Maximum acceptable age
     * @return true if data is stale
     */
    public boolean isDataStale(long maxAgeMinutes) {
        if (lastSynced == null) {
            return true;
        }
        
        return Instant.now().isAfter(lastSynced.plusSeconds(maxAgeMinutes * 60));
    }
    
    /**
     * Get broker health status based on data freshness
     * 
     * @return Health status
     */
    public BrokerHealthStatus getHealthStatus() {
        if (lastSynced == null) {
            return BrokerHealthStatus.UNKNOWN;
        }
        
        long minutesSinceSync = java.time.Duration.between(lastSynced, Instant.now()).toMinutes();
        
        return switch ((int) Math.min(minutesSinceSync / 5, 4)) {
            case 0 -> BrokerHealthStatus.HEALTHY;     // 0-5 minutes
            case 1 -> BrokerHealthStatus.DEGRADED;    // 5-10 minutes
            case 2 -> BrokerHealthStatus.WARNING;     // 10-15 minutes
            case 3 -> BrokerHealthStatus.CRITICAL;    // 15-20 minutes
            default -> BrokerHealthStatus.OFFLINE;    // >20 minutes
        };
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
        return String.format("BrokerBreakdown[broker=%s, positions=%d, value=₹%.2f, pnl=₹%.2f, contribution=%.1f%%]",
                           brokerName, positionCount, totalValue, unrealizedPnL, allocationPercent);
    }
    
    /**
     * Broker Health Status Enumeration
     */
    public enum BrokerHealthStatus {
        HEALTHY("Healthy", "#28a745"),
        DEGRADED("Degraded", "#ffc107"),
        WARNING("Warning", "#fd7e14"),
        CRITICAL("Critical", "#dc3545"),
        OFFLINE("Offline", "#6c757d"),
        UNKNOWN("Unknown", "#adb5bd");
        
        private final String displayName;
        private final String colorCode;
        
        BrokerHealthStatus(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}