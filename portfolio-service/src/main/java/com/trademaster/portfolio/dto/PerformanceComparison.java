package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Performance Comparison DTO
 * 
 * Data transfer object containing portfolio performance comparison against benchmarks.
 * Provides detailed analysis of relative performance and attribution.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PerformanceComparison(
    Long portfolioId,
    String benchmarkSymbol,
    String benchmarkName,
    Instant fromDate,
    Instant toDate,
    BigDecimal portfolioReturn,
    BigDecimal benchmarkReturn,
    BigDecimal excessReturn,
    BigDecimal portfolioVolatility,
    BigDecimal benchmarkVolatility,
    BigDecimal portfolioSharpe,
    BigDecimal benchmarkSharpe,
    BigDecimal portfolioBeta,
    BigDecimal portfolioAlpha,
    BigDecimal trackingError,
    BigDecimal informationRatio,
    BigDecimal correlation,
    BigDecimal maxDrawdown,
    BigDecimal benchmarkMaxDrawdown,
    List<PeriodComparison> periodBreakdown,
    List<String> outperformancePeriods,
    List<String> underperformancePeriods,
    String overallRating,
    Instant comparisonDate
) {}

/**
 * Period Comparison DTO
 */
record PeriodComparison(
    String period,
    Instant periodStart,
    Instant periodEnd,
    BigDecimal portfolioReturn,
    BigDecimal benchmarkReturn,
    BigDecimal excessReturn,
    boolean outperformed
) {}