package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broker Portfolio Breakdown Value Object
 * 
 * Represents individual broker contribution to consolidated portfolio.
 * Part of FE-015 Multi-Broker Portfolio Aggregation feature.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-015)
 */
public record BrokerPortfolioBreakdown(
    String brokerId,
    String brokerName,
    BigDecimal value,
    BigDecimal percentage,
    BigDecimal dayChange,
    Integer holdingsCount,
    Instant lastSynced
) {
    
    /**
     * Compact constructor with validation
     */
    public BrokerPortfolioBreakdown {
        if (brokerId == null || brokerId.isBlank()) {
            throw new IllegalArgumentException("Broker ID cannot be null or blank");
        }
        if (brokerName == null || brokerName.isBlank()) {
            throw new IllegalArgumentException("Broker name cannot be null or blank");
        }
        
        // Set defaults
        value = defaultIfNull(value, BigDecimal.ZERO);
        percentage = defaultIfNull(percentage, BigDecimal.ZERO);
        dayChange = defaultIfNull(dayChange, BigDecimal.ZERO);
        holdingsCount = defaultIfNull(holdingsCount, 0);
        lastSynced = defaultIfNull(lastSynced, Instant.now());
    }
    
    /**
     * Check if broker data is stale (>5 minutes old)
     */
    public boolean isStale() {
        return lastSynced.isBefore(Instant.now().minusSeconds(300)); // 5 minutes
    }
    
    /**
     * Check if broker has significant holdings (>5% of total)
     */
    public boolean hasSignificantHoldings() {
        return percentage.compareTo(new BigDecimal("5.0")) > 0;
    }
    
    /**
     * Get sync status description
     */
    public String getSyncStatus() {
        return switch (isStale()) {
            case true -> "STALE";
            case false -> "CURRENT";
        };
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for BrokerPortfolioBreakdown
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for BrokerPortfolioBreakdown
     */
    public static class Builder {
        private String brokerId;
        private String brokerName;
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
        private BigDecimal dayChange = BigDecimal.ZERO;
        private Integer holdingsCount = 0;
        private Instant lastSynced;
        
        public Builder brokerId(String brokerId) {
            this.brokerId = brokerId;
            return this;
        }
        
        public Builder brokerName(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }
        
        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }
        
        public Builder percentage(BigDecimal percentage) {
            this.percentage = percentage;
            return this;
        }
        
        public Builder dayChange(BigDecimal dayChange) {
            this.dayChange = dayChange;
            return this;
        }
        
        public Builder holdingsCount(Integer holdingsCount) {
            this.holdingsCount = holdingsCount;
            return this;
        }
        
        public Builder lastSynced(Instant lastSynced) {
            this.lastSynced = lastSynced;
            return this;
        }
        
        public BrokerPortfolioBreakdown build() {
            return new BrokerPortfolioBreakdown(
                brokerId, brokerName, value, percentage, 
                dayChange, holdingsCount, lastSynced
            );
        }
    }
}