package com.trademaster.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Metrics Record
 * 
 * Comprehensive metrics for portfolio performance analysis.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 21 + Virtual Threads)
 */
public record PortfolioMetrics(
    Long portfolioId,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal totalPnl,
    BigDecimal totalReturn,
    BigDecimal dayPnl,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    Integer totalPositions,
    Integer profitablePositions,
    Integer losingPositions,
    BigDecimal largestPosition,
    BigDecimal concentrationRisk,
    Instant lastValuationAt,
    Instant calculatedAt
) {}