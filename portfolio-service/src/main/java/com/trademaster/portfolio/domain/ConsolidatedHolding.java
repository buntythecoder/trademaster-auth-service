package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated Holding Value Object
 * 
 * Represents aggregated holding across multiple brokers for same symbol.
 * Part of FE-015 Multi-Broker Portfolio Aggregation feature.
 * 
 * Features:
 * - Combined quantity across brokers
 * - Weighted average purchase price
 * - Consolidated P&L calculation
 * - Individual broker position breakdown
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-015)
 */
public record ConsolidatedHolding(
    String symbol,
    String companyName,
    BigDecimal totalQuantity,
    BigDecimal avgPrice,
    BigDecimal currentPrice,
    BigDecimal totalValue,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnLPercent,
    BigDecimal dayChange,
    List<BrokerPosition> brokerPositions
) {
    
    /**
     * Compact constructor with validation
     */
    public ConsolidatedHolding {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (brokerPositions == null) {
            throw new IllegalArgumentException("Broker positions cannot be null");
        }
        
        // Set defaults
        companyName = defaultIfNull(companyName, symbol);
        totalQuantity = defaultIfNull(totalQuantity, BigDecimal.ZERO);
        avgPrice = defaultIfNull(avgPrice, BigDecimal.ZERO);
        currentPrice = defaultIfNull(currentPrice, BigDecimal.ZERO);
        totalValue = defaultIfNull(totalValue, BigDecimal.ZERO);
        unrealizedPnL = defaultIfNull(unrealizedPnL, BigDecimal.ZERO);
        unrealizedPnLPercent = defaultIfNull(unrealizedPnLPercent, BigDecimal.ZERO);
        dayChange = defaultIfNull(dayChange, BigDecimal.ZERO);
    }
    
    /**
     * Check if holding is profitable
     */
    public boolean isProfitable() {
        return unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if holding is a major position (>5% of total value)
     */
    public boolean isMajorPosition(BigDecimal totalPortfolioValue) {
        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) <= 0) return false;
        
        BigDecimal percentage = totalValue.divide(totalPortfolioValue, 4, BigDecimal.ROUND_HALF_UP);
        return percentage.compareTo(new BigDecimal("0.05")) > 0;
    }
    
    /**
     * Get number of brokers holding this symbol
     */
    public int getBrokerCount() {
        return brokerPositions.size();
    }
    
    /**
     * Check if holding is diversified across multiple brokers
     */
    public boolean isDiversifiedAcrossBrokers() {
        return brokerPositions.size() > 1;
    }
    
    /**
     * Get largest broker position percentage
     */
    public BigDecimal getLargestBrokerPositionPercentage() {
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        
        return brokerPositions.stream()
            .map(position -> position.quantity().divide(totalQuantity, 4, BigDecimal.ROUND_HALF_UP))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate weighted average price from broker positions
     */
    public BigDecimal calculateWeightedAvgPrice() {
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        
        BigDecimal totalCost = brokerPositions.stream()
            .map(position -> position.quantity().multiply(position.avgPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalCost.divide(totalQuantity, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get performance category
     */
    public String getPerformanceCategory() {
        BigDecimal pnlPercent = unrealizedPnLPercent;
        
        if (pnlPercent.compareTo(new BigDecimal("20")) > 0) return "EXCELLENT";
        if (pnlPercent.compareTo(new BigDecimal("10")) > 0) return "GOOD";
        if (pnlPercent.compareTo(new BigDecimal("-10")) > 0) return "MODERATE";
        if (pnlPercent.compareTo(new BigDecimal("-20")) > 0) return "POOR";
        return "VERY_POOR";
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for ConsolidatedHolding
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ConsolidatedHolding
     */
    public static class Builder {
        private String symbol;
        private String companyName;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private BigDecimal avgPrice = BigDecimal.ZERO;
        private BigDecimal currentPrice = BigDecimal.ZERO;
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal unrealizedPnL = BigDecimal.ZERO;
        private BigDecimal unrealizedPnLPercent = BigDecimal.ZERO;
        private BigDecimal dayChange = BigDecimal.ZERO;
        private List<BrokerPosition> brokerPositions = List.of();
        
        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public Builder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }
        
        public Builder totalQuantity(BigDecimal totalQuantity) {
            this.totalQuantity = totalQuantity;
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
        
        public Builder totalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }
        
        public Builder unrealizedPnL(BigDecimal unrealizedPnL) {
            this.unrealizedPnL = unrealizedPnL;
            return this;
        }
        
        public Builder unrealizedPnLPercent(BigDecimal unrealizedPnLPercent) {
            this.unrealizedPnLPercent = unrealizedPnLPercent;
            return this;
        }
        
        public Builder dayChange(BigDecimal dayChange) {
            this.dayChange = dayChange;
            return this;
        }
        
        public Builder brokerPositions(List<BrokerPosition> brokerPositions) {
            this.brokerPositions = brokerPositions;
            return this;
        }
        
        public ConsolidatedHolding build() {
            return new ConsolidatedHolding(
                symbol, companyName, totalQuantity, avgPrice, currentPrice,
                totalValue, unrealizedPnL, unrealizedPnLPercent, dayChange,
                brokerPositions
            );
        }
    }
}