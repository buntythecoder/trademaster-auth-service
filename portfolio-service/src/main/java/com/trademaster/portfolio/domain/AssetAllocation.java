package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Asset Allocation Value Object
 * 
 * Represents portfolio allocation by asset category.
 * Part of FE-015 Multi-Broker Portfolio Aggregation feature.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-015)
 */
public record AssetAllocation(
    String category, // 'Large Cap', 'Mid Cap', 'Small Cap', 'Cash', 'Bonds', 'International'
    BigDecimal value,
    BigDecimal percentage,
    BigDecimal change24h
) {
    
    /**
     * Predefined asset categories
     */
    public enum Category {
        LARGE_CAP("Large Cap"),
        MID_CAP("Mid Cap"), 
        SMALL_CAP("Small Cap"),
        CASH("Cash"),
        BONDS("Bonds"),
        INTERNATIONAL("International"),
        SECTOR_ETF("Sector ETF"),
        CRYPTOCURRENCY("Cryptocurrency"),
        COMMODITIES("Commodities"),
        REAL_ESTATE("Real Estate");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Compact constructor with validation
     */
    public AssetAllocation {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be null or blank");
        }
        
        // Set defaults
        value = defaultIfNull(value, BigDecimal.ZERO);
        percentage = defaultIfNull(percentage, BigDecimal.ZERO);
        change24h = defaultIfNull(change24h, BigDecimal.ZERO);
        
        // Validate percentage is between 0-100
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || 
            percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100: " + percentage);
        }
    }
    
    /**
     * Check if this is a high-growth category (>10% gain in 24h)
     */
    public boolean isHighGrowth() {
        return change24h.compareTo(new BigDecimal("10.0")) > 0;
    }
    
    /**
     * Check if this is a declining category (<-5% in 24h)
     */
    public boolean isDeclining() {
        return change24h.compareTo(new BigDecimal("-5.0")) < 0;
    }
    
    /**
     * Check if this is a major allocation (>20% of portfolio)
     */
    public boolean isMajorAllocation() {
        return percentage.compareTo(new BigDecimal("20.0")) > 0;
    }
    
    /**
     * Get risk level based on category
     */
    public String getRiskLevel() {
        return switch (category.toLowerCase()) {
            case "cash", "bonds" -> "LOW";
            case "large cap" -> "MEDIUM";
            case "mid cap", "sector etf", "international" -> "MEDIUM_HIGH";
            case "small cap", "real estate", "commodities" -> "HIGH";
            case "cryptocurrency" -> "VERY_HIGH";
            default -> "MEDIUM";
        };
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for AssetAllocation
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AssetAllocation
     */
    public static class Builder {
        private String category;
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
        private BigDecimal change24h = BigDecimal.ZERO;
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder category(Category category) {
            this.category = category.getDisplayName();
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
        
        public Builder change24h(BigDecimal change24h) {
            this.change24h = change24h;
            return this;
        }
        
        public AssetAllocation build() {
            return new AssetAllocation(category, value, percentage, change24h);
        }
    }
}