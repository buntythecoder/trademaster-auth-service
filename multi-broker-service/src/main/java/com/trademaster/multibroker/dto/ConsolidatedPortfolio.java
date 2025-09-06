package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Consolidated Portfolio Record  
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents aggregated portfolio data across all connected brokers for a user.
 * Contains consolidated positions, financial metrics, and broker breakdown.
 * 
 * Aggregation Logic:
 * - Same symbol positions merged across brokers with weighted average pricing
 * - Total values calculated using real-time market prices
 * - P&L calculated from actual buy prices vs current market prices
 * - Asset allocation computed from consolidated holdings
 * - Broker breakdown shows contribution from each connected broker
 * 
 * Performance Calculation:
 * - Unrealized P&L: (Current Value - Total Investment)
 * - Day Change: Sum of individual position day changes  
 * - Returns: P&L / Total Investment * 100
 * - Allocation: Position Value / Total Portfolio Value * 100
 * 
 * @param userId User identifier who owns this portfolio
 * @param totalValue Current market value of entire portfolio
 * @param totalCost Total amount invested (sum of all buy transactions)
 * @param unrealizedPnL Unrealized profit/loss (totalValue - totalCost)
 * @param unrealizedPnLPercent P&L percentage ((unrealizedPnL / totalCost) * 100)
 * @param dayChange Portfolio value change for current trading day
 * @param dayChangePercent Day change as percentage
 * @param positions List of consolidated positions across all brokers
 * @param brokerBreakdown Portfolio breakdown by each broker
 * @param assetAllocation Asset class allocation percentages
 * @param lastUpdated When portfolio data was last aggregated
 * @param dataFreshness How fresh the underlying broker data is
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker Portfolio Aggregation)
 */
@Builder
public record ConsolidatedPortfolio(
    String userId,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnLPercent,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    List<ConsolidatedPosition> positions,
    List<BrokerPortfolioBreakdown> brokerBreakdown,
    List<AssetAllocation> assetAllocation,
    Instant lastUpdated,
    DataFreshness dataFreshness
) {
    
    /**
     * Check if portfolio has any positions
     * 
     * @return true if positions exist
     */
    public boolean hasPositions() {
        return positions != null && !positions.isEmpty();
    }
    
    /**
     * Get number of unique stocks/instruments held
     * 
     * @return Count of unique positions
     */
    public long getPositionCount() {
        return positions != null ? positions.size() : 0L;
    }
    
    /**
     * Get number of brokers contributing to portfolio
     * 
     * @return Count of active broker connections
     */
    public long getBrokerCount() {
        return brokerBreakdown != null ? brokerBreakdown.size() : 0L;
    }
    
    /**
     * Check if portfolio is showing profit
     * 
     * @return true if unrealized P&L is positive
     */
    public boolean isProfitable() {
        return unrealizedPnL != null && 
               unrealizedPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio gained value today
     * 
     * @return true if day change is positive
     */
    public boolean isUpToday() {
        return dayChange != null && 
               dayChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get total invested amount formatted
     * 
     * @return Formatted investment amount
     */
    public String getFormattedTotalCost() {
        return formatCurrency(totalCost);
    }
    
    /**
     * Get current value formatted
     * 
     * @return Formatted current value
     */
    public String getFormattedTotalValue() {
        return formatCurrency(totalValue);
    }
    
    /**
     * Get P&L formatted with sign
     * 
     * @return Formatted P&L with + or - sign
     */
    public String getFormattedPnL() {
        if (unrealizedPnL == null) {
            return "₹0.00";
        }
        
        String sign = unrealizedPnL.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(unrealizedPnL);
    }
    
    /**
     * Get day change formatted with sign
     * 
     * @return Formatted day change with + or - sign
     */
    public String getFormattedDayChange() {
        if (dayChange == null) {
            return "₹0.00";
        }
        
        String sign = dayChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(dayChange);
    }
    
    /**
     * Get positions grouped by asset class
     * 
     * @return Map of asset class to positions
     */
    public java.util.Map<String, List<ConsolidatedPosition>> getPositionsByAssetClass() {
        if (positions == null) {
            return java.util.Map.of();
        }
        
        return positions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                pos -> determineAssetClass(pos.symbol())));
    }
    
    /**
     * Get top performing positions by P&L percentage
     * 
     * @param limit Number of top positions to return
     * @return List of best performing positions
     */
    public List<ConsolidatedPosition> getTopPerformers(int limit) {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.unrealizedPnLPercent() != null)
            .sorted((a, b) -> b.unrealizedPnLPercent().compareTo(a.unrealizedPnLPercent()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get worst performing positions by P&L percentage
     * 
     * @param limit Number of worst positions to return
     * @return List of worst performing positions
     */
    public List<ConsolidatedPosition> getWorstPerformers(int limit) {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.unrealizedPnLPercent() != null)
            .sorted((a, b) -> a.unrealizedPnLPercent().compareTo(b.unrealizedPnLPercent()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get largest positions by current value
     * 
     * @param limit Number of largest positions to return
     * @return List of largest positions by value
     */
    public List<ConsolidatedPosition> getLargestPositions(int limit) {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.currentValue() != null)
            .sorted((a, b) -> b.currentValue().compareTo(a.currentValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check if portfolio data is stale
     * 
     * @param maxAgeMinutes Maximum acceptable age in minutes
     * @return true if data is older than specified age
     */
    public boolean isDataStale(long maxAgeMinutes) {
        if (lastUpdated == null) {
            return true;
        }
        
        return Instant.now().isAfter(
            lastUpdated.plusSeconds(maxAgeMinutes * 60));
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
     * Determine asset class from symbol
     * 
     * @param symbol Stock symbol
     * @return Asset class category
     */
    private String determineAssetClass(String symbol) {
        if (symbol == null) {
            return "OTHER";
        }
        
        // Simple logic - can be enhanced with master data
        return switch (symbol.length()) {
            case 5, 6 -> symbol.endsWith("EQ") ? "EQUITY" : "DERIVATIVE";
            case 7, 8 -> "DERIVATIVE";
            default -> "EQUITY";
        };
    }
    
    /**
     * Data Freshness Indicator
     */
    public enum DataFreshness {
        REAL_TIME("Real-time", 0, 1),      // 0-1 minute
        FRESH("Fresh", 1, 5),              // 1-5 minutes  
        RECENT("Recent", 5, 15),           // 5-15 minutes
        STALE("Stale", 15, 60),            // 15-60 minutes
        OLD("Old", 60, Integer.MAX_VALUE); // > 1 hour
        
        private final String displayName;
        private final int minMinutes;
        private final int maxMinutes;
        
        DataFreshness(String displayName, int minMinutes, int maxMinutes) {
            this.displayName = displayName;
            this.minMinutes = minMinutes;
            this.maxMinutes = maxMinutes;
        }
        
        public String getDisplayName() { return displayName; }
        
        public static DataFreshness fromAge(long ageMinutes) {
            for (DataFreshness freshness : values()) {
                if (ageMinutes >= freshness.minMinutes && ageMinutes < freshness.maxMinutes) {
                    return freshness;
                }
            }
            return OLD;
        }
    }
}