package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio Valuation Result with comprehensive metrics
 * 
 * @param portfolioId Portfolio identifier
 * @param valuationDate Date of valuation
 * @param totalValue Total portfolio value
 * @param totalCost Total cost basis
 * @param unrealizedPnL Unrealized profit/loss
 * @param realizedPnL Realized profit/loss
 * @param totalPnL Total profit/loss
 * @param dayChange Day over day change
 * @param dayChangePercent Day change percentage
 * @param totalReturn Total return since inception
 * @param totalReturnPercent Total return percentage
 * @param positionValuations Individual position valuations
 * @param calculationTimeMs Time taken for calculation
 * @param lastUpdated Last update timestamp
 */
public record PortfolioValuationResult(
    Long portfolioId,
    Instant valuationDate,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal unrealizedPnL,
    BigDecimal realizedPnL,
    BigDecimal totalPnL,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    BigDecimal totalReturn,
    BigDecimal totalReturnPercent,
    List<PositionValuation> positionValuations,
    Long calculationTimeMs,
    Instant lastUpdated
) {
    
    public record PositionValuation(
        String symbol,
        Integer quantity,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal unrealizedPnL,
        BigDecimal unrealizedPnLPercent
    ) {}
}