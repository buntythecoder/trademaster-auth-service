package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Broker Position Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents an individual position from a broker's portfolio.
 * Contains raw position data before normalization for portfolio consolidation.
 * 
 * Position Data:
 * - Symbol identification and exchange information
 * - Quantity and pricing details
 * - Current market value and P&L calculations
 * - Position type (LONG/SHORT) and trading status
 * 
 * Data Flow:
 * 1. Fetched from broker APIs as raw position data
 * 2. Used in PositionNormalizationService for symbol standardization
 * 3. Aggregated in DataAggregationService for portfolio consolidation
 * 
 * @param symbol Stock symbol from broker
 * @param exchange Exchange code from broker
 * @param quantity Number of shares/units
 * @param avgPrice Average acquisition price
 * @param ltp Last traded price (current market price)
 * @param pnl Profit/loss for this position
 * @param positionType Position type (LONG/SHORT/COVER)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Position Data)
 */
@Builder
public record BrokerPosition(
    String symbol,
    String exchange,
    Integer quantity,
    BigDecimal avgPrice,
    BigDecimal ltp,
    BigDecimal pnl,
    String positionType
) {
    
    /**
     * Check if position is profitable
     * 
     * @return true if P&L is positive
     */
    public boolean isProfitable() {
        return pnl != null && pnl.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if position is a loss
     * 
     * @return true if P&L is negative
     */
    public boolean isLoss() {
        return pnl != null && pnl.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Check if position is long
     * 
     * @return true if position type is LONG
     */
    public boolean isLongPosition() {
        return "LONG".equalsIgnoreCase(positionType) || 
               "BUY".equalsIgnoreCase(positionType);
    }
    
    /**
     * Check if position is short
     * 
     * @return true if position type is SHORT
     */
    public boolean isShortPosition() {
        return "SHORT".equalsIgnoreCase(positionType) || 
               "SELL".equalsIgnoreCase(positionType);
    }
    
    /**
     * Calculate current market value
     * 
     * @return Current value (LTP * quantity)
     */
    public BigDecimal getCurrentValue() {
        if (ltp == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        
        return ltp.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Calculate cost basis
     * 
     * @return Cost basis (average price * quantity)
     */
    public BigDecimal getCostBasis() {
        if (avgPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        
        return avgPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Calculate P&L percentage
     * 
     * @return P&L percentage based on cost basis
     */
    public BigDecimal getPnLPercentage() {
        BigDecimal costBasis = getCostBasis();
        
        if (costBasis.equals(BigDecimal.ZERO) || pnl == null) {
            return BigDecimal.ZERO;
        }
        
        return pnl.divide(costBasis, 4, java.math.RoundingMode.HALF_UP)
                 .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculate price change percentage
     * 
     * @return Price change percentage (LTP vs average price)
     */
    public BigDecimal getPriceChangePercentage() {
        if (avgPrice == null || ltp == null || avgPrice.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal priceChange = ltp.subtract(avgPrice);
        return priceChange.divide(avgPrice, 4, java.math.RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if position is significant by value
     * 
     * @param threshold Minimum value threshold
     * @return true if position value exceeds threshold
     */
    public boolean isSignificantPosition(BigDecimal threshold) {
        return getCurrentValue().compareTo(threshold) > 0;
    }
    
    /**
     * Get position size category
     * 
     * @return Position size category
     */
    public PositionSize getPositionSize() {
        BigDecimal currentValue = getCurrentValue();
        
        if (currentValue.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            return PositionSize.LARGE;
        } else if (currentValue.compareTo(BigDecimal.valueOf(25000)) >= 0) {
            return PositionSize.MEDIUM;
        } else if (currentValue.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return PositionSize.SMALL;
        } else {
            return PositionSize.MICRO;
        }
    }
    
    /**
     * Get risk level based on P&L percentage
     * 
     * @return Risk level assessment
     */
    public RiskLevel getRiskLevel() {
        BigDecimal pnlPercent = getPnLPercentage().abs();
        
        if (pnlPercent.compareTo(BigDecimal.valueOf(25)) > 0) {
            return RiskLevel.HIGH;
        } else if (pnlPercent.compareTo(BigDecimal.valueOf(10)) > 0) {
            return RiskLevel.MEDIUM;
        } else if (pnlPercent.compareTo(BigDecimal.valueOf(5)) > 0) {
            return RiskLevel.LOW;
        } else {
            return RiskLevel.MINIMAL;
        }
    }
    
    /**
     * Get formatted current value
     * 
     * @return Formatted current value string
     */
    public String getFormattedCurrentValue() {
        return formatCurrency(getCurrentValue());
    }
    
    /**
     * Get formatted P&L with sign
     * 
     * @return Formatted P&L string
     */
    public String getFormattedPnL() {
        if (pnl == null) {
            return "₹0.00";
        }
        
        String sign = pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(pnl);
    }
    
    /**
     * Get formatted average price
     * 
     * @return Formatted average price string
     */
    public String getFormattedAvgPrice() {
        return formatCurrency(avgPrice);
    }
    
    /**
     * Get formatted LTP
     * 
     * @return Formatted LTP string
     */
    public String getFormattedLtp() {
        return formatCurrency(ltp);
    }
    
    /**
     * Validate position data integrity
     * 
     * @return true if position has valid data
     */
    public boolean isValidPosition() {
        return symbol != null && !symbol.trim().isEmpty() &&
               exchange != null && !exchange.trim().isEmpty() &&
               quantity != null && quantity > 0 &&
               avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0 &&
               ltp != null && ltp.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Create position summary
     * 
     * @return Position summary map
     */
    public java.util.Map<String, Object> getPositionSummary() {
        return java.util.Map.of(
            "symbol", symbol != null ? symbol : "",
            "exchange", exchange != null ? exchange : "",
            "quantity", quantity != null ? quantity : 0,
            "currentValue", getCurrentValue(),
            "costBasis", getCostBasis(),
            "pnlPercentage", getPnLPercentage(),
            "positionSize", getPositionSize().name(),
            "riskLevel", getRiskLevel().name(),
            "isValid", isValidPosition()
        );
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
        return String.format("BrokerPosition[symbol=%s, exchange=%s, qty=%d, value=₹%.2f, pnl=₹%.2f]",
                           symbol, exchange, quantity, getCurrentValue(), pnl);
    }
    
    /**
     * Position Size Enumeration
     */
    public enum PositionSize {
        MICRO("Micro", "< ₹5,000"),
        SMALL("Small", "₹5,000 - ₹25,000"),
        MEDIUM("Medium", "₹25,000 - ₹1,00,000"),
        LARGE("Large", "> ₹1,00,000");
        
        private final String displayName;
        private final String range;
        
        PositionSize(String displayName, String range) {
            this.displayName = displayName;
            this.range = range;
        }
        
        public String getDisplayName() { return displayName; }
        public String getRange() { return range; }
    }
    
    /**
     * Risk Level Enumeration
     */
    public enum RiskLevel {
        MINIMAL("Minimal", "< 5% P&L"),
        LOW("Low", "5% - 10% P&L"),
        MEDIUM("Medium", "10% - 25% P&L"),
        HIGH("High", "> 25% P&L");
        
        private final String displayName;
        private final String description;
        
        RiskLevel(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}