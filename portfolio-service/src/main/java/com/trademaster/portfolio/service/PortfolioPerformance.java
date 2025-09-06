package com.trademaster.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Performance Record
 * 
 * Performance analysis data for a specific time period.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 21 + Virtual Threads)
 */
public record PortfolioPerformance(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal startingValue,
    BigDecimal endingValue,
    BigDecimal totalReturn,
    BigDecimal annualizedReturn,
    BigDecimal volatility,
    BigDecimal sharpeRatio,
    BigDecimal maxDrawdown,
    BigDecimal averageDailyReturn,
    Integer tradingDays,
    BigDecimal totalFees,
    BigDecimal totalDividends,
    Instant calculatedAt
) {}