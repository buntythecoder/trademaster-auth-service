package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Position-level PnL metrics for detailed analysis
 * 
 * @param symbol Security symbol
 * @param quantity Current position quantity
 * @param currentPrice Current market price
 * @param averageCost Average cost basis
 * @param marketValue Current market value
 * @param costBasis Total cost basis
 * @param unrealizedPnL Unrealized profit/loss
 * @param unrealizedPnLPercent Unrealized PnL percentage
 * @param realizedPnL Realized profit/loss to date
 * @param totalPnL Total PnL (realized + unrealized)
 * @param dayChange Day over day change
 * @param dayChangePercent Day change percentage
 * @param beta Beta coefficient vs market
 * @param contribution Contribution to portfolio return
 * @param weightInPortfolio Weight as percentage of portfolio
 * @param lastUpdated Last update timestamp
 */
public record PositionPnLMetrics(
    String symbol,
    Integer quantity,
    BigDecimal currentPrice,
    BigDecimal averageCost,
    BigDecimal marketValue,
    BigDecimal costBasis,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnLPercent,
    BigDecimal realizedPnL,
    BigDecimal totalPnL,
    BigDecimal dayChange,
    BigDecimal dayChangePercent,
    BigDecimal beta,
    BigDecimal contribution,
    BigDecimal weightInPortfolio,
    Instant lastUpdated
) {}