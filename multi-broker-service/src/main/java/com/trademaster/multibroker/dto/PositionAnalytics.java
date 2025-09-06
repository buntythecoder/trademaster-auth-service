package com.trademaster.multibroker.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * Position Analytics DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Comprehensive analytics for position portfolio with key metrics
 * and performance indicators.
 */
@Builder
@Jacksonized
public record PositionAnalytics(
    int totalPositions,
    BigDecimal totalInvestment,
    BigDecimal totalCurrentValue,
    BigDecimal totalPnl,
    BigDecimal pnlPercentage,
    String largestPosition,
    String mostProfitable
) {
    
    public PositionAnalytics {
        if (totalInvestment == null) {
            totalInvestment = BigDecimal.ZERO;
        }
        if (totalCurrentValue == null) {
            totalCurrentValue = BigDecimal.ZERO;
        }
        if (totalPnl == null) {
            totalPnl = BigDecimal.ZERO;
        }
        if (pnlPercentage == null) {
            pnlPercentage = BigDecimal.ZERO;
        }
    }
    
    /**
     * Check if portfolio is profitable overall
     */
    public boolean isOverallProfitable() {
        return totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get portfolio diversity (positions per rupee invested)
     */
    public BigDecimal getPortfolioDiversity() {
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalPositions)
                        .divide(totalInvestment.divide(BigDecimal.valueOf(100000), 2, java.math.RoundingMode.HALF_UP), 
                               2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get risk level based on P&L percentage
     */
    public String getRiskLevel() {
        BigDecimal absPnl = pnlPercentage.abs();
        
        if (absPnl.compareTo(BigDecimal.valueOf(5)) <= 0) {
            return "LOW";
        } else if (absPnl.compareTo(BigDecimal.valueOf(15)) <= 0) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}