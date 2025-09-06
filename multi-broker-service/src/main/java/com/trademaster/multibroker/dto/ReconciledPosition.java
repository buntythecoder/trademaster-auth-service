package com.trademaster.multibroker.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Reconciled Position DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Represents a position that has been reconciled across multiple brokers
 * with aggregated quantities, prices, and P&L calculations.
 */
@Builder
@Jacksonized
public record ReconciledPosition(
    String symbol,
    long totalQuantity,
    BigDecimal weightedAveragePrice,
    BigDecimal currentPrice,
    BigDecimal totalPnl,
    List<NormalizedBrokerPosition> brokerPositions,
    Instant reconciliationTime
) {
    
    public ReconciledPosition {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (weightedAveragePrice == null) {
            throw new IllegalArgumentException("Weighted average price cannot be null");
        }
        if (currentPrice == null) {
            throw new IllegalArgumentException("Current price cannot be null");
        }
        if (totalPnl == null) {
            throw new IllegalArgumentException("Total P&L cannot be null");
        }
        if (reconciliationTime == null) {
            throw new IllegalArgumentException("Reconciliation time cannot be null");
        }
        if (brokerPositions == null) {
            brokerPositions = List.of();
        }
    }
    
    /**
     * Get current market value
     */
    public BigDecimal getCurrentValue() {
        return currentPrice.multiply(BigDecimal.valueOf(totalQuantity));
    }
    
    /**
     * Get investment value
     */
    public BigDecimal getInvestmentValue() {
        return weightedAveragePrice.multiply(BigDecimal.valueOf(totalQuantity));
    }
    
    /**
     * Get P&L percentage
     */
    public BigDecimal getPnlPercentage() {
        BigDecimal investment = getInvestmentValue();
        if (investment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPnl.divide(investment, 4, java.math.RoundingMode.HALF_UP)
                     .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get number of brokers holding this position
     */
    public int getBrokerCount() {
        return brokerPositions.size();
    }
    
    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }
}