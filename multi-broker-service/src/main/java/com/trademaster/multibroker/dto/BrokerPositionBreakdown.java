package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broker Position Breakdown Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Individual broker position breakdown within a consolidated position.
 * Used to show position distribution across multiple brokers for transparency
 * and risk management purposes.
 * 
 * Contains position details from a single broker including quantity,
 * average price, current value, and P&L for the specific broker.
 * 
 * @param brokerId Unique broker identifier
 * @param brokerName Display name of the broker
 * @param quantity Number of shares/units held at this broker
 * @param avgPrice Average acquisition price at this broker
 * @param currentValue Current market value at this broker
 * @param unrealizedPnL Profit/loss for this broker position
 * @param percentOfTotal Percentage of total position held at this broker
 * @param lastSynced When position data was last synchronized
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Position Analysis)
 */
@Builder
public record BrokerPositionBreakdown(
    String brokerId,
    String brokerName,
    Long quantity,
    BigDecimal avgPrice,
    BigDecimal currentValue,
    BigDecimal unrealizedPnL,
    BigDecimal percentOfTotal,
    Instant lastSynced
) {
    
    /**
     * Check if broker position is profitable
     * 
     * @return true if unrealized P&L is positive
     */
    public boolean isProfitable() {
        return unrealizedPnL != null && unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted position value
     * 
     * @return Formatted current value string
     */
    public String getFormattedValue() {
        return currentValue != null ? String.format("₹%,.2f", currentValue) : "₹0.00";
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
        return sign + String.format("₹%,.2f", unrealizedPnL);
    }
    
    /**
     * Calculate cost basis for this broker position
     * 
     * @return Total cost (quantity * avgPrice)
     */
    public BigDecimal calculateCostBasis() {
        if (quantity == null || avgPrice == null) {
            return BigDecimal.ZERO;
        }
        
        return avgPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Create summary for logging (safe, no sensitive data)
     * 
     * @return Safe summary string
     */
    public String toSafeSummary() {
        return String.format("BrokerPosition[broker=%s, qty=%d, value=₹%.2f, pnl=₹%.2f]",
                           brokerName, quantity, currentValue, unrealizedPnL);
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}