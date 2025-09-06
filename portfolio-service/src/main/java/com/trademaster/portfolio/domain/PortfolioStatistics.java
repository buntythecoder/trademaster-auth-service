package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Statistics for comprehensive portfolio overview
 * 
 * @param portfolioId Portfolio identifier
 * @param totalPortfolios Total number of portfolios managed
 * @param totalValue Total value across all portfolios
 * @param totalCash Total cash positions
 * @param totalEquity Total equity positions
 * @param totalFixedIncome Total fixed income positions
 * @param totalAlternatives Total alternative investments
 * @param averagePortfolioSize Average portfolio size
 * @param largestPortfolio Largest portfolio value
 * @param smallestPortfolio Smallest portfolio value
 * @param totalRealizedPnL Total realized PnL across all portfolios
 * @param totalUnrealizedPnL Total unrealized PnL across all portfolios
 * @param averageReturn Average return across all portfolios
 * @param bestPerformingPortfolio Best performing portfolio ID
 * @param worstPerformingPortfolio Worst performing portfolio ID
 * @param totalPositions Total number of positions
 * @param averagePositionsPerPortfolio Average positions per portfolio
 * @param mostHeldSymbol Most commonly held symbol
 * @param averageDiversification Average diversification score
 * @param totalDividendIncome Total dividend income
 * @param totalInterestIncome Total interest income
 * @param lastUpdated Last update timestamp
 * @param calculationDate Date of statistics calculation
 */
public record PortfolioStatistics(
    Long portfolioId,
    Integer totalPortfolios,
    BigDecimal totalValue,
    BigDecimal totalCash,
    BigDecimal totalEquity,
    BigDecimal totalFixedIncome,
    BigDecimal totalAlternatives,
    BigDecimal averagePortfolioSize,
    BigDecimal largestPortfolio,
    BigDecimal smallestPortfolio,
    BigDecimal totalRealizedPnL,
    BigDecimal totalUnrealizedPnL,
    BigDecimal averageReturn,
    Long bestPerformingPortfolio,
    Long worstPerformingPortfolio,
    Integer totalPositions,
    Integer averagePositionsPerPortfolio,
    String mostHeldSymbol,
    BigDecimal averageDiversification,
    BigDecimal totalDividendIncome,
    BigDecimal totalInterestIncome,
    Instant lastUpdated,
    Instant calculationDate
) {}