package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PnL Impact Analysis for proposed trades
 * 
 * @param portfolioId Portfolio identifier
 * @param symbol Security symbol
 * @param proposedQuantity Proposed trade quantity
 * @param proposedPrice Proposed trade price
 * @param currentPosition Current position in this security
 * @param currentMarketValue Current market value
 * @param projectedMarketValue Projected market value after trade
 * @param realizedPnLImpact Expected realized PnL from trade
 * @param unrealizedPnLImpact Impact on unrealized PnL
 * @param totalPnLImpact Total PnL impact
 * @param portfolioWeightChange Change in portfolio weight
 * @param riskImpact Risk impact metrics
 * @param newAverageCost New average cost if trade executed
 * @param breakEvenPrice Break-even price for new position
 * @param analysisDate Date of analysis
 */
public record PnLImpactAnalysis(
    Long portfolioId,
    String symbol,
    Integer proposedQuantity,
    BigDecimal proposedPrice,
    Integer currentPosition,
    BigDecimal currentMarketValue,
    BigDecimal projectedMarketValue,
    BigDecimal realizedPnLImpact,
    BigDecimal unrealizedPnLImpact,
    BigDecimal totalPnLImpact,
    BigDecimal portfolioWeightChange,
    RiskImpact riskImpact,
    BigDecimal newAverageCost,
    BigDecimal breakEvenPrice,
    Instant analysisDate
) {
    
    public record RiskImpact(
        BigDecimal betaChange,
        BigDecimal volatilityChange,
        BigDecimal concentrationRiskChange,
        BigDecimal sectorExposureChange
    ) {}
    
    /**
     * Factory method for new position impact analysis
     */
    public static PnLImpactAnalysis forNewPosition(
        Long portfolioId,
        String symbol,
        Integer quantity,
        BigDecimal price
    ) {
        return new PnLImpactAnalysis(
            portfolioId,
            symbol,
            quantity,
            price,
            0, // currentPosition
            BigDecimal.ZERO, // currentMarketValue
            price.multiply(BigDecimal.valueOf(Math.abs(quantity))), // projectedMarketValue
            BigDecimal.ZERO, // realizedPnLImpact
            BigDecimal.ZERO, // unrealizedPnLImpact
            BigDecimal.ZERO, // totalPnLImpact
            BigDecimal.ZERO, // portfolioWeightChange
            new RiskImpact(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            price, // newAverageCost
            price, // breakEvenPrice
            Instant.now() // analysisDate
        );
    }
}