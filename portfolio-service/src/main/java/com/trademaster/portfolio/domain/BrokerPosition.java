package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broker Position Value Object
 * 
 * Represents individual position at a specific broker.
 * Part of consolidated holding breakdown.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-015)
 */
public record BrokerPosition(
    String brokerId,
    String brokerName,
    String symbol,
    BigDecimal quantity,
    BigDecimal avgPrice,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    BigDecimal unrealizedPnL,
    Instant lastUpdated
) {
    
    /**
     * Compact constructor with validation
     */
    public BrokerPosition {
        if (brokerId == null || brokerId.isBlank()) {
            throw new IllegalArgumentException("Broker ID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        
        // Set defaults
        brokerName = defaultIfNull(brokerName, brokerId);
        quantity = defaultIfNull(quantity, BigDecimal.ZERO);
        avgPrice = defaultIfNull(avgPrice, BigDecimal.ZERO);
        currentPrice = defaultIfNull(currentPrice, BigDecimal.ZERO);
        marketValue = defaultIfNull(marketValue, quantity.multiply(currentPrice));
        unrealizedPnL = defaultIfNull(unrealizedPnL, marketValue.subtract(quantity.multiply(avgPrice)));
        lastUpdated = defaultIfNull(lastUpdated, Instant.now());
    }
    
    /**
     * Calculate P&L percentage
     */
    public BigDecimal getPnLPercentage() {
        if (avgPrice.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        
        return currentPrice.subtract(avgPrice)
            .divide(avgPrice, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if position data is stale (>2 minutes old)
     */
    public boolean isStale() {
        return lastUpdated.isBefore(Instant.now().minusSeconds(120)); // 2 minutes
    }
    
    /**
     * Get position size category
     */
    public String getPositionSize() {
        BigDecimal value = marketValue.abs();
        
        if (value.compareTo(new BigDecimal("100000")) > 0) return "LARGE";  // >1L
        if (value.compareTo(new BigDecimal("50000")) > 0) return "MEDIUM";  // >50K
        if (value.compareTo(new BigDecimal("10000")) > 0) return "SMALL";   // >10K
        return "MICRO";  // <=10K
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for BrokerPosition
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for BrokerPosition
     */
    public static class Builder {
        private String brokerId;
        private String brokerName;
        private String symbol;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal avgPrice = BigDecimal.ZERO;
        private BigDecimal currentPrice = BigDecimal.ZERO;
        private BigDecimal marketValue;
        private BigDecimal unrealizedPnL;
        private Instant lastUpdated;
        
        public Builder brokerId(String brokerId) {
            this.brokerId = brokerId;
            return this;
        }
        
        public Builder brokerName(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }
        
        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public Builder avgPrice(BigDecimal avgPrice) {
            this.avgPrice = avgPrice;
            return this;
        }
        
        public Builder currentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }
        
        public Builder marketValue(BigDecimal marketValue) {
            this.marketValue = marketValue;
            return this;
        }
        
        public Builder unrealizedPnL(BigDecimal unrealizedPnL) {
            this.unrealizedPnL = unrealizedPnL;
            return this;
        }
        
        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        public BrokerPosition build() {
            return new BrokerPosition(
                brokerId, brokerName, symbol, quantity, avgPrice,
                currentPrice, marketValue, unrealizedPnL, lastUpdated
            );
        }
    }
}