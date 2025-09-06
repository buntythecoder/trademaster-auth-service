package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Comprehensive PnL Attribution Report
 * 
 * @param reportId Unique report identifier
 * @param portfolioId Portfolio identifier
 * @param fromDate Report period start date
 * @param toDate Report period end date
 * @param executiveSummary Executive summary of performance
 * @param performanceMetrics Key performance metrics
 * @param attributionAnalysis Attribution analysis by various factors
 * @param riskMetrics Risk-adjusted performance metrics
 * @param benchmarkComparison Benchmark comparison data
 * @param topContributors Top contributing positions
 * @param topDetractors Top detracting positions
 * @param sectorBreakdown Performance by sector
 * @param recommendations Actionable recommendations
 * @param generatedDate Report generation date
 * @param generatedBy Report generator (system/user)
 */
public record PnLAttributionReport(
    String reportId,
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    ExecutiveSummary executiveSummary,
    PerformanceMetrics performanceMetrics,
    AttributionAnalysis attributionAnalysis,
    RiskMetrics riskMetrics,
    BenchmarkComparison benchmarkComparison,
    List<PositionContribution> topContributors,
    List<PositionContribution> topDetractors,
    List<SectorBreakdown> sectorBreakdown,
    List<String> recommendations,
    Instant generatedDate,
    String generatedBy
) {
    
    public record ExecutiveSummary(
        BigDecimal totalReturn,
        BigDecimal totalReturnPercent,
        BigDecimal benchmarkReturn,
        BigDecimal activeReturn,
        BigDecimal sharpeRatio,
        String performanceRating
    ) {}
    
    public record PerformanceMetrics(
        BigDecimal realizedPnL,
        BigDecimal unrealizedPnL,
        BigDecimal totalPnL,
        BigDecimal averageDailyReturn,
        BigDecimal volatility,
        BigDecimal maxDrawdown,
        BigDecimal informationRatio
    ) {}
    
    public record AttributionAnalysis(
        BigDecimal allocationEffect,
        BigDecimal selectionEffect,
        BigDecimal interactionEffect,
        BigDecimal currencyEffect,
        BigDecimal timingEffect
    ) {}
    
    public record RiskMetrics(
        BigDecimal beta,
        BigDecimal alpha,
        BigDecimal trackingError,
        BigDecimal var95,
        BigDecimal cvar95
    ) {}
    
    public record BenchmarkComparison(
        String benchmarkName,
        BigDecimal benchmarkReturn,
        BigDecimal outperformance,
        BigDecimal correlation,
        Integer outperformanceDays,
        Integer underperformanceDays
    ) {}
    
    public record PositionContribution(
        String symbol,
        BigDecimal weight,
        BigDecimal return_,
        BigDecimal contribution,
        String sector
    ) {}
    
    public record SectorBreakdown(
        String sector,
        BigDecimal weight,
        BigDecimal return_,
        BigDecimal contribution,
        BigDecimal activeWeight,
        BigDecimal allocationEffect,
        BigDecimal selectionEffect
    ) {}
}