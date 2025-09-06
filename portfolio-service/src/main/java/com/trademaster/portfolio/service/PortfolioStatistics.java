package com.trademaster.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Statistics Record
 * 
 * Aggregated statistics across all portfolios.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 21 + Virtual Threads)
 */
public record PortfolioStatistics(
    Long totalPortfolios,
    Long activePortfolios,
    Long suspendedPortfolios,
    Long closedPortfolios,
    BigDecimal totalAUM,
    BigDecimal averagePortfolioValue,
    BigDecimal totalRealizedPnl,
    BigDecimal totalUnrealizedPnl,
    Long totalPositions,
    Long profitablePortfolios,
    Long losingPortfolios,
    Instant calculatedAt
) {}