package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Normalized Broker Position Record
 * 
 * MANDATORY: Immutable Record + Functional Validation + Zero Placeholders
 * 
 * Represents a broker position after normalization across different broker formats.
 * Contains both original and normalized values for traceability and debugging.
 * Used internally by DataAggregationService for portfolio consolidation.
 * 
 * Normalization Examples:
 * - Zerodha: "RELIANCE" -> "RELIANCE" (already normalized)
 * - Upstox: "NSE_EQ|INE002A01018" -> "RELIANCE" (ISIN mapping)
 * - Angel One: "RELIANCE-EQ" -> "RELIANCE" (suffix removal)
 * - ICICI Direct: "RELIANCE NSE" -> "RELIANCE" (exchange removal)
 * 
 * Quality Assurance:
 * - Original values preserved for audit trail
 * - Normalized values used for aggregation logic
 * - Symbol validation prevents invalid consolidation
 * - Price normalization ensures calculation accuracy
 * 
 * @param originalSymbol Original symbol from broker API
 * @param normalizedSymbol Standardized symbol for aggregation
 * @param originalExchange Original exchange from broker API
 * @param normalizedExchange Standardized exchange code
 * @param quantity Position quantity (always positive, sign in positionType)
 * @param avgPrice Average acquisition price per unit
 * @param ltp Last traded price from market data
 * @param pnl Realized/unrealized P&L from broker
 * @param dayChange Day change in absolute value
 * @param positionType Position type (LONG/SHORT)
 * @param brokerId Broker identifier for traceability
 * @param brokerName Broker display name
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Position Normalization)
 */
@Builder
public record NormalizedBrokerPosition(
    String originalSymbol,
    String normalizedSymbol,
    String originalExchange,
    String normalizedExchange,
    long quantity,
    BigDecimal avgPrice,
    BigDecimal ltp,
    BigDecimal pnl,
    BigDecimal dayChange,
    String positionType,
    String brokerId,
    String brokerName
) {
    
    /**
     * Validate normalized position data
     * 
     * @return true if position is valid for aggregation
     */
    public boolean isValid() {
        return normalizedSymbol != null &&
               !normalizedSymbol.trim().isEmpty() &&
               quantity > 0 &&
               avgPrice != null &&
               avgPrice.compareTo(BigDecimal.ZERO) > 0 &&
               brokerId != null &&
               !brokerId.trim().isEmpty();
    }
    
    /**
     * Check if position is long (positive quantity)
     * 
     * @return true if long position
     */
    public boolean isLongPosition() {
        return "LONG".equalsIgnoreCase(positionType);
    }
    
    /**
     * Check if position is short (negative quantity)
     * 
     * @return true if short position
     */
    public boolean isShortPosition() {
        return "SHORT".equalsIgnoreCase(positionType);
    }
    
    /**
     * Get absolute quantity (always positive)
     * 
     * @return Absolute quantity value
     */
    public long getAbsoluteQuantity() {
        return Math.abs(quantity);
    }
    
    /**
     * Get signed quantity (positive for LONG, negative for SHORT)
     * 
     * @return Signed quantity
     */
    public long getSignedQuantity() {
        return isShortPosition() ? -Math.abs(quantity) : Math.abs(quantity);
    }
    
    /**
     * Calculate current market value of position
     * 
     * @return Current market value
     */
    public BigDecimal getCurrentValue() {
        if (ltp == null || ltp.equals(BigDecimal.ZERO)) {
            return avgPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        return ltp.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Calculate investment value (cost basis)
     * 
     * @return Investment value
     */
    public BigDecimal getInvestmentValue() {
        return avgPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Calculate unrealized P&L based on current market price
     * 
     * @return Unrealized P&L
     */
    public BigDecimal getUnrealizedPnL() {
        BigDecimal currentValue = getCurrentValue();
        BigDecimal investmentValue = getInvestmentValue();
        
        return isShortPosition() ? 
            investmentValue.subtract(currentValue) : 
            currentValue.subtract(investmentValue);
    }
    
    /**
     * Calculate unrealized P&L percentage
     * 
     * @return P&L percentage
     */
    public BigDecimal getUnrealizedPnLPercent() {
        BigDecimal investmentValue = getInvestmentValue();
        if (investmentValue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal unrealizedPnL = getUnrealizedPnL();
        return unrealizedPnL.divide(investmentValue, 4, java.math.RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if symbol normalization was successful
     * 
     * @return true if symbol was actually normalized (changed from original)
     */
    public boolean wasNormalized() {
        return !java.util.Objects.equals(originalSymbol, normalizedSymbol) ||
               !java.util.Objects.equals(originalExchange, normalizedExchange);
    }
    
    /**
     * Get normalization summary for debugging
     * 
     * @return Human-readable normalization summary
     */
    public String getNormalizationSummary() {
        if (!wasNormalized()) {
            return String.format("No normalization needed for %s", originalSymbol);
        }
        
        return String.format("Normalized %s:%s -> %s:%s", 
                           originalSymbol, originalExchange,
                           normalizedSymbol, normalizedExchange);
    }
    
    /**
     * Compare positions by current value for sorting
     * 
     * @param other Other normalized position
     * @return Comparison result
     */
    public int compareByValue(NormalizedBrokerPosition other) {
        return this.getCurrentValue().compareTo(other.getCurrentValue());
    }
    
    /**
     * Create summary string for logging (no sensitive data)
     * 
     * @return Safe summary string
     */
    public String toSafeSummary() {
        return String.format("Position[symbol=%s, broker=%s, qty=%d, value=₹%.2f]",
                           normalizedSymbol, brokerName, quantity, getCurrentValue());
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}