package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Broker Portfolio Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents portfolio data from a single broker including all positions,
 * overall portfolio metrics, and broker-specific information.
 * 
 * Used as input for portfolio consolidation across multiple brokers.
 * Contains raw data from individual broker APIs before normalization.
 * 
 * Portfolio Data Structure:
 * - User identification and broker metadata
 * - Total portfolio value and investment amounts
 * - Individual position details for all holdings
 * - Performance metrics (P&L, day change)
 * - Data freshness and sync information
 * 
 * @param brokerId Unique broker identifier
 * @param brokerName Display name of the broker
 * @param userId User identifier who owns this portfolio
 * @param totalValue Current market value of portfolio
 * @param totalInvestment Total amount invested
 * @param dayPnl Day's profit/loss
 * @param totalPnl Total unrealized profit/loss
 * @param positions List of individual positions
 * @param lastSynced When data was last synced from broker
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Portfolio Data)
 */
@Builder
public record BrokerPortfolio(
    String brokerId,
    String brokerName,
    Long userId,
    BigDecimal totalValue,
    BigDecimal totalInvestment,
    BigDecimal dayPnl,
    BigDecimal totalPnl,
    List<BrokerPosition> positions,
    Instant lastSynced
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
     * Get number of positions
     * 
     * @return Position count
     */
    public int getPositionCount() {
        return positions != null ? positions.size() : 0;
    }
    
    /**
     * Check if portfolio is profitable
     * 
     * @return true if total P&L is positive
     */
    public boolean isProfitable() {
        return totalPnl != null && totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio gained today
     * 
     * @return true if day P&L is positive
     */
    public boolean isUpToday() {
        return dayPnl != null && dayPnl.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Calculate total P&L percentage
     * 
     * @return P&L percentage
     */
    public BigDecimal getPnLPercentage() {
        if (totalInvestment == null || totalInvestment.equals(BigDecimal.ZERO) || totalPnl == null) {
            return BigDecimal.ZERO;
        }
        
        return totalPnl.divide(totalInvestment, 4, java.math.RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculate day P&L percentage
     * 
     * @return Day P&L percentage
     */
    public BigDecimal getDayPnLPercentage() {
        if (totalValue == null || totalValue.equals(BigDecimal.ZERO) || dayPnl == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal previousValue = totalValue.subtract(dayPnl);
        if (previousValue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return dayPnl.divide(previousValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Find position by symbol
     * 
     * @param symbol Stock symbol
     * @return Optional position
     */
    public Optional<BrokerPosition> getPosition(String symbol) {
        if (positions == null || symbol == null) {
            return Optional.empty();
        }
        
        return positions.stream()
            .filter(pos -> symbol.equalsIgnoreCase(pos.symbol()))
            .findFirst();
    }
    
    /**
     * Get positions by exchange
     * 
     * @param exchange Exchange name
     * @return List of positions from the exchange
     */
    public List<BrokerPosition> getPositionsByExchange(String exchange) {
        if (positions == null || exchange == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> exchange.equalsIgnoreCase(pos.exchange()))
            .toList();
    }
    
    /**
     * Get top positions by value
     * 
     * @param count Number of top positions
     * @return List of top positions by current value
     */
    public List<BrokerPosition> getTopPositions(int count) {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.ltp() != null && pos.quantity() != null)
            .sorted((a, b) -> {
                BigDecimal valueA = a.ltp().multiply(BigDecimal.valueOf(a.quantity()));
                BigDecimal valueB = b.ltp().multiply(BigDecimal.valueOf(b.quantity()));
                return valueB.compareTo(valueA);
            })
            .limit(count)
            .toList();
    }
    
    /**
     * Get profitable positions
     * 
     * @return List of positions with positive P&L
     */
    public List<BrokerPosition> getProfitablePositions() {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.pnl() != null && pos.pnl().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }
    
    /**
     * Get losing positions
     * 
     * @return List of positions with negative P&L
     */
    public List<BrokerPosition> getLosingPositions() {
        if (positions == null) {
            return List.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.pnl() != null && pos.pnl().compareTo(BigDecimal.ZERO) < 0)
            .toList();
    }
    
    /**
     * Check if portfolio data is stale
     * 
     * @param maxAgeMinutes Maximum acceptable age in minutes
     * @return true if data is stale
     */
    public boolean isDataStale(long maxAgeMinutes) {
        if (lastSynced == null) {
            return true;
        }
        
        return Instant.now().isAfter(lastSynced.plusSeconds(maxAgeMinutes * 60));
    }
    
    /**
     * Get portfolio allocation by value
     * 
     * @return Map of symbol to allocation percentage
     */
    public java.util.Map<String, BigDecimal> getAllocationByValue() {
        if (positions == null || totalValue == null || totalValue.equals(BigDecimal.ZERO)) {
            return java.util.Map.of();
        }
        
        return positions.stream()
            .filter(pos -> pos.ltp() != null && pos.quantity() != null)
            .collect(java.util.stream.Collectors.toMap(
                BrokerPosition::symbol,
                pos -> {
                    BigDecimal positionValue = pos.ltp().multiply(BigDecimal.valueOf(pos.quantity()));
                    return positionValue.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                                     .multiply(BigDecimal.valueOf(100));
                }
            ));
    }
    
    /**
     * Get formatted total value
     * 
     * @return Formatted total value string
     */
    public String getFormattedTotalValue() {
        return formatCurrency(totalValue);
    }
    
    /**
     * Get formatted day P&L with sign
     * 
     * @return Formatted day P&L string
     */
    public String getFormattedDayPnL() {
        if (dayPnl == null) {
            return "₹0.00";
        }
        
        String sign = dayPnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(dayPnl);
    }
    
    /**
     * Get formatted total P&L with sign
     * 
     * @return Formatted total P&L string
     */
    public String getFormattedTotalPnL() {
        if (totalPnl == null) {
            return "₹0.00";
        }
        
        String sign = totalPnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatCurrency(totalPnl);
    }
    
    /**
     * Get portfolio statistics summary
     * 
     * @return Map of portfolio statistics
     */
    public java.util.Map<String, Object> getPortfolioStats() {
        return java.util.Map.of(
            "totalPositions", getPositionCount(),
            "profitablePositions", getProfitablePositions().size(),
            "losingPositions", getLosingPositions().size(),
            "pnlPercentage", getPnLPercentage(),
            "dayPnlPercentage", getDayPnLPercentage(),
            "dataAgeMinutes", lastSynced != null ? 
                java.time.Duration.between(lastSynced, Instant.now()).toMinutes() : -1L
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
        return String.format("BrokerPortfolio[broker=%s, positions=%d, value=₹%.2f, pnl=₹%.2f]",
                           brokerName, getPositionCount(), totalValue, totalPnl);
    }
    
    /**
     * Override toString to prevent accidental sensitive data logging
     */
    @Override
    public String toString() {
        return toSafeSummary();
    }
}