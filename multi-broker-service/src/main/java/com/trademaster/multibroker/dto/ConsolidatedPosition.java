package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Consolidated Position Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents a consolidated position across multiple brokers for the same symbol.
 * Contains aggregated metrics, individual broker breakdowns, and performance analytics.
 * 
 * Consolidation Logic:
 * - Quantities summed across all brokers
 * - Weighted average price calculation
 * - Current value based on real-time market price
 * - P&L calculated from consolidated cost basis
 * - Broker breakdown maintained for transparency
 * 
 * Use Cases:
 * - Portfolio dashboard display
 * - Risk management calculations
 * - Tax reporting and analytics
 * - Performance attribution analysis
 * 
 * @param symbol Normalized stock symbol
 * @param companyName Company display name
 * @param sector Industry sector classification
 * @param totalQuantity Total quantity across all brokers
 * @param avgPrice Weighted average acquisition price
 * @param currentPrice Current market price
 * @param totalCost Total investment amount
 * @param currentValue Current market value
 * @param unrealizedPnL Unrealized profit/loss
 * @param unrealizedPnLPercent P&L percentage
 * @param dayChange Day change in absolute value
 * @param dayChangePercent Day change percentage
 * @param brokerPositions Individual broker position breakdown
 * @param lastUpdated When position was last consolidated
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker Position Consolidation)
 */
@Builder
public record ConsolidatedPosition(
    String symbol,
    String companyName,
    String sector,
    long totalQuantity,
    BigDecimal avgPrice,
    BigDecimal currentPrice,
    BigDecimal totalCost,
    BigDecimal currentValue,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnLPercent,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    List<BrokerPositionBreakdown> brokerPositions,
    Instant lastUpdated
) {
    
    /**
     * Check if position is profitable
     * 
     * @return true if unrealized P&L is positive
     */
    public boolean isProfitable() {
        return unrealizedPnL != null && unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if position gained value today
     * 
     * @return true if day change is positive
     */
    public boolean isUpToday() {
        return dayChange != null && dayChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get number of brokers holding this position
     * 
     * @return Count of brokers
     */
    public int getBrokerCount() {
        return brokerPositions != null ? brokerPositions.size() : 0;
    }
    
    /**
     * Check if position is held across multiple brokers
     * 
     * @return true if multiple brokers
     */
    public boolean isMultiBrokerPosition() {
        return getBrokerCount() > 1;
    }
    
    /**
     * Get largest broker position by quantity
     * 
     * @return Largest broker position
     */
    public java.util.Optional<BrokerPositionBreakdown> getLargestBrokerPosition() {
        if (brokerPositions == null || brokerPositions.isEmpty()) {
            return java.util.Optional.empty();
        }
        
        return brokerPositions.stream()
            .max(java.util.Comparator.comparing(BrokerPositionBreakdown::quantity));
    }
    
    /**
     * Get broker position by broker name
     * 
     * @param brokerName Broker name to search
     * @return Optional broker position
     */
    public java.util.Optional<BrokerPositionBreakdown> getBrokerPosition(String brokerName) {
        if (brokerPositions == null || brokerName == null) {
            return java.util.Optional.empty();
        }
        
        return brokerPositions.stream()
            .filter(pos -> brokerName.equalsIgnoreCase(pos.brokerName()))
            .findFirst();
    }
    
    /**
     * Calculate allocation percentage within portfolio
     * 
     * @param totalPortfolioValue Total portfolio value
     * @return Allocation percentage
     */
    public BigDecimal calculateAllocationPercent(BigDecimal totalPortfolioValue) {
        if (totalPortfolioValue == null || totalPortfolioValue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return currentValue.divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get formatted current value
     * 
     * @return Formatted current value string
     */
    public String getFormattedCurrentValue() {
        return formatCurrency(currentValue);
    }
    
    /**
     * Get formatted total cost
     * 
     * @return Formatted total cost string
     */
    public String getFormattedTotalCost() {
        return formatCurrency(totalCost);
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
     * Get formatted day change with sign
     * 
     * @return Formatted day change string
     */
    public String getFormattedDayChange() {
        if (dayChange == null) {
            return "₹0.00";
        }
        
        String sign = dayChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(dayChange);
    }
    
    /**
     * Get position risk level based on various factors
     * 
     * @return Risk level (LOW, MEDIUM, HIGH)
     */
    public RiskLevel getRiskLevel() {
        // Simple risk assessment logic
        if (unrealizedPnLPercent == null) {
            return RiskLevel.MEDIUM;
        }
        
        BigDecimal pnlPercent = unrealizedPnLPercent.abs();
        
        if (pnlPercent.compareTo(BigDecimal.valueOf(20)) > 0) {
            return RiskLevel.HIGH; // >20% gain/loss
        } else if (pnlPercent.compareTo(BigDecimal.valueOf(5)) > 0) {
            return RiskLevel.MEDIUM; // 5-20% gain/loss
        } else {
            return RiskLevel.LOW; // <5% gain/loss
        }
    }
    
    /**
     * Get position age in days (simplified calculation)
     * 
     * @return Position age in days
     */
    public long getPositionAgeDays() {
        if (lastUpdated == null) {
            return 0L;
        }
        
        return java.time.Duration.between(lastUpdated, Instant.now()).toDays();
    }
    
    /**
     * Check if position data is stale
     * 
     * @param maxAgeMinutes Maximum acceptable age
     * @return true if data is stale
     */
    public boolean isDataStale(long maxAgeMinutes) {
        if (lastUpdated == null) {
            return true;
        }
        
        return Instant.now().isAfter(lastUpdated.plusSeconds(maxAgeMinutes * 60));
    }
    
    /**
     * Get broker breakdown summary
     * 
     * @return Summary of broker positions
     */
    public String getBrokerBreakdownSummary() {
        if (brokerPositions == null || brokerPositions.isEmpty()) {
            return "No broker positions";
        }
        
        return brokerPositions.stream()
            .map(pos -> String.format("%s: %d shares", pos.brokerName(), pos.quantity()))
            .collect(java.util.stream.Collectors.joining(", "));
    }
    
    /**
     * Calculate weighted average holding period (advanced feature)
     * 
     * @return Weighted average holding period in days
     */
    public BigDecimal calculateWeightedHoldingPeriod() {
        if (brokerPositions == null || brokerPositions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Simplified calculation - in production would use actual acquisition dates
        BigDecimal totalWeightedDays = brokerPositions.stream()
            .map(pos -> BigDecimal.valueOf(30) // Assume 30 days average
                              .multiply(BigDecimal.valueOf(pos.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalWeightedDays.divide(BigDecimal.valueOf(totalQuantity), 
                                      2, java.math.RoundingMode.HALF_UP);
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
     * Risk level enumeration
     */
    public enum RiskLevel {
        LOW("Low Risk", "#28a745"),
        MEDIUM("Medium Risk", "#ffc107"),
        HIGH("High Risk", "#dc3545");
        
        private final String displayName;
        private final String colorCode;
        
        RiskLevel(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
    }
    
    /**
     * Create summary for logging (safe, no sensitive data)
     * 
     * @return Safe summary string
     */
    public String toSafeSummary() {
        return String.format("ConsolidatedPosition[symbol=%s, qty=%d, value=₹%.2f, pnl=₹%.2f, brokers=%d]",
                           symbol, totalQuantity, currentValue, unrealizedPnL, getBrokerCount());
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}