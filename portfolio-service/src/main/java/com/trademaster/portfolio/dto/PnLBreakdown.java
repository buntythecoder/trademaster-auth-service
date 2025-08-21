package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * P&L Breakdown DTO
 * 
 * Comprehensive breakdown of profit and loss components over a time period.
 * Provides detailed analysis of P&L sources and attribution.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PnLBreakdown(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalPnl,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal dividendIncome,
    BigDecimal interestIncome,
    BigDecimal totalFees,
    BigDecimal totalCommissions,
    BigDecimal totalTaxes,
    BigDecimal netPnl,
    List<SecurityPnLBreakdown> securityBreakdown,
    List<SectorPnLBreakdown> sectorBreakdown,
    Instant calculatedAt
) {}

/**
 * Security P&L Breakdown DTO
 */
record SecurityPnLBreakdown(
    String symbol,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal dividends,
    BigDecimal fees,
    BigDecimal netPnl,
    BigDecimal contribution
) {}

/**
 * Sector P&L Breakdown DTO
 */
record SectorPnLBreakdown(
    String sector,
    BigDecimal totalPnl,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal contribution,
    Integer positionCount
) {}