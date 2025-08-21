package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.PortfolioStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Summary DTO
 * 
 * Comprehensive summary of portfolio with key metrics and performance data.
 * Used for dashboard displays and quick portfolio overviews.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PortfolioSummary(
    Long portfolioId,
    String portfolioName,
    PortfolioStatus status,
    BigDecimal totalValue,
    BigDecimal cashBalance,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal dayPnl,
    Integer totalPositions,
    Integer profitablePositions,
    Integer losingPositions,
    BigDecimal largestPosition,
    Instant lastValuationAt,
    Instant summaryGeneratedAt
) {}