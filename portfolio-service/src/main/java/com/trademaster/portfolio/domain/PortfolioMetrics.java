package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Metrics domain object for performance and risk analytics
 * 
 * @param portfolioId Portfolio identifier
 * @param totalValue Total portfolio value
 * @param totalCost Total cost basis
 * @param totalPnL Total profit/loss
 * @param totalPnLPercent Total PnL percentage
 * @param dayChange Day over day change
 * @param dayChangePercent Day change percentage
 * @param realizedPnL Total realized PnL
 * @param unrealizedPnL Total unrealized PnL
 * @param totalReturn Total return since inception
 * @param totalReturnPercent Total return percentage
 * @param annualizedReturn Annualized return
 * @param sharpeRatio Sharpe ratio
 * @param beta Beta vs benchmark
 * @param alpha Alpha vs benchmark
 * @param volatility Portfolio volatility
 * @param maxDrawdown Maximum drawdown
 * @param positionCount Number of positions
 * @param diversificationScore Diversification score
 * @param concentrationRisk Concentration risk level
 * @param valueAtRisk Value at Risk (95% confidence)
 * @param lastUpdated Last update timestamp
 * @param calculationDate Date of metrics calculation
 */
public record PortfolioMetrics(
    Long portfolioId,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal totalPnL,
    BigDecimal totalPnLPercent,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    BigDecimal realizedPnL,
    BigDecimal unrealizedPnL,
    BigDecimal totalReturn,
    BigDecimal totalReturnPercent,
    BigDecimal annualizedReturn,
    BigDecimal sharpeRatio,
    BigDecimal beta,
    BigDecimal alpha,
    BigDecimal volatility,
    BigDecimal maxDrawdown,
    Integer positionCount,
    BigDecimal diversificationScore,
    BigDecimal concentrationRisk,
    BigDecimal valueAtRisk,
    Instant lastUpdated,
    Instant calculationDate
) {}