package com.trademaster.userprofile.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RiskProfile(
    @NotNull(message = "Risk level is required")
    RiskLevel riskLevel,
    
    @NotNull(message = "Risk tolerance score is required")
    @Min(value = 1, message = "Risk tolerance score must be between 1 and 10")
    @Max(value = 10, message = "Risk tolerance score must be between 1 and 10")
    Integer riskToleranceScore,
    
    @NotNull(message = "Investment horizon is required")
    InvestmentHorizon investmentHorizon,
    
    BigDecimal maxInvestmentAmount,
    
    BigDecimal maxLossPerTrade,
    
    Integer maxPositionsHeld,
    
    Boolean leverageAllowed,
    
    String riskQuestionnaire,
    
    String investmentGoals
) {
    
    public boolean allowsHighRiskInstruments() {
        return (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.VERY_HIGH) && riskToleranceScore >= 7;
    }
    
    public boolean isConservativeInvestor() {
        return riskLevel == RiskLevel.LOW && 
               investmentHorizon == InvestmentHorizon.LONG_TERM;
    }
    
    public BigDecimal getRecommendedPositionSize(BigDecimal portfolioValue) {
        if (portfolioValue == null) return BigDecimal.ZERO;
        
        return switch (riskLevel) {
            case LOW -> portfolioValue.multiply(BigDecimal.valueOf(0.02)); // 2%
            case MODERATE, MEDIUM -> portfolioValue.multiply(BigDecimal.valueOf(0.05)); // 5%
            case HIGH -> portfolioValue.multiply(BigDecimal.valueOf(0.10)); // 10%
            case VERY_HIGH -> portfolioValue.multiply(BigDecimal.valueOf(0.15)); // 15%
        };
    }
}