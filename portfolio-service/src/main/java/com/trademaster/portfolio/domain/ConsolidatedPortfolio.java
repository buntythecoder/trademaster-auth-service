package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Consolidated Portfolio Value Object (FE-015 Implementation)
 * 
 * Multi-broker portfolio aggregation with real-time consolidation.
 * Follows Epic 3 requirements for portfolio aggregation across brokers.
 * 
 * Features:
 * - Real-time portfolio consolidation across multiple brokers
 * - Position-level aggregation for same symbols
 * - Historical portfolio tracking
 * - Weighted average price calculations
 * - Asset allocation breakdown
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-015)
 */
public record ConsolidatedPortfolio(
    Long userId,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnLPercent,
    BigDecimal realizedPnL,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    Instant lastUpdated,
    
    List<BrokerPortfolioBreakdown> brokerBreakdown,
    List<AssetAllocation> assetAllocation,
    List<ConsolidatedHolding> topHoldings
) {
    
    /**
     * Compact constructor with validation
     */
    public ConsolidatedPortfolio {
        // Validate required fields
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        if (brokerBreakdown == null) throw new IllegalArgumentException("Broker breakdown cannot be null");
        if (assetAllocation == null) throw new IllegalArgumentException("Asset allocation cannot be null");
        if (topHoldings == null) throw new IllegalArgumentException("Top holdings cannot be null");
        
        // Set defaults
        totalValue = defaultIfNull(totalValue, BigDecimal.ZERO);
        totalCost = defaultIfNull(totalCost, BigDecimal.ZERO);
        unrealizedPnL = defaultIfNull(unrealizedPnL, BigDecimal.ZERO);
        unrealizedPnLPercent = defaultIfNull(unrealizedPnLPercent, BigDecimal.ZERO);
        realizedPnL = defaultIfNull(realizedPnL, BigDecimal.ZERO);
        dayChange = defaultIfNull(dayChange, BigDecimal.ZERO);
        dayChangePercent = defaultIfNull(dayChangePercent, BigDecimal.ZERO);
        lastUpdated = defaultIfNull(lastUpdated, Instant.now());
    }
    
    /**
     * Builder for ConsolidatedPortfolio construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Calculate total return percentage
     */
    public BigDecimal calculateTotalReturnPercent() {
        return switch (totalCost.compareTo(BigDecimal.ZERO)) {
            case 0, -1 -> BigDecimal.ZERO;
            default -> unrealizedPnL.add(realizedPnL)
                .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        };
    }
    
    /**
     * Get broker count
     */
    public int getBrokerCount() {
        return brokerBreakdown.size();
    }
    
    /**
     * Get total holdings count
     */
    public int getTotalHoldingsCount() {
        return brokerBreakdown.stream()
            .mapToInt(BrokerPortfolioBreakdown::holdingsCount)
            .sum();
    }
    
    /**
     * Check if portfolio is diversified (no single holding >20%)
     */
    public boolean isDiversified() {
        BigDecimal maxHoldingPercent = topHoldings.stream()
            .map(holding -> holding.totalValue().divide(totalValue, 4, BigDecimal.ROUND_HALF_UP))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        return maxHoldingPercent.compareTo(new BigDecimal("0.20")) <= 0;
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Builder for ConsolidatedPortfolio
     */
    public static class Builder {
        private Long userId;
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal unrealizedPnL = BigDecimal.ZERO;
        private BigDecimal unrealizedPnLPercent = BigDecimal.ZERO;
        private BigDecimal realizedPnL = BigDecimal.ZERO;
        private BigDecimal dayChange = BigDecimal.ZERO;
        private BigDecimal dayChangePercent = BigDecimal.ZERO;
        private Instant lastUpdated;
        private List<BrokerPortfolioBreakdown> brokerBreakdown = List.of();
        private List<AssetAllocation> assetAllocation = List.of();
        private List<ConsolidatedHolding> topHoldings = List.of();
        
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder totalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }
        
        public Builder totalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
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
        
        public Builder realizedPnL(BigDecimal realizedPnL) {
            this.realizedPnL = realizedPnL;
            return this;
        }
        
        public Builder dayChange(BigDecimal dayChange) {
            this.dayChange = dayChange;
            return this;
        }
        
        public Builder dayChangePercent(BigDecimal dayChangePercent) {
            this.dayChangePercent = dayChangePercent;
            return this;
        }
        
        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        public Builder brokerBreakdown(List<BrokerPortfolioBreakdown> brokerBreakdown) {
            this.brokerBreakdown = brokerBreakdown;
            return this;
        }
        
        public Builder assetAllocation(List<AssetAllocation> assetAllocation) {
            this.assetAllocation = assetAllocation;
            return this;
        }
        
        public Builder topHoldings(List<ConsolidatedHolding> topHoldings) {
            this.topHoldings = topHoldings;
            return this;
        }
        
        public ConsolidatedPortfolio build() {
            return new ConsolidatedPortfolio(
                userId, totalValue, totalCost, unrealizedPnL, unrealizedPnLPercent,
                realizedPnL, dayChange, dayChangePercent, lastUpdated,
                brokerBreakdown, assetAllocation, topHoldings
            );
        }
    }
}