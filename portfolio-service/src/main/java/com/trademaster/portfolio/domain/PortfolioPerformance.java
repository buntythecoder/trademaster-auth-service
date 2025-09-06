package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio Performance metrics for comprehensive analysis
 * 
 * @param portfolioId Portfolio identifier
 * @param fromDate Performance period start date
 * @param toDate Performance period end date
 * @param totalReturn Total return for the period
 * @param totalReturnPercent Total return as percentage
 * @param annualizedReturn Annualized return
 * @param volatility Portfolio volatility (standard deviation)
 * @param sharpeRatio Sharpe ratio (risk-adjusted return)
 * @param sortinoRatio Sortino ratio (downside deviation adjusted)
 * @param informationRatio Information ratio vs benchmark
 * @param alpha Alpha vs benchmark
 * @param beta Beta vs benchmark
 * @param maxDrawdown Maximum drawdown during period
 * @param trackingError Tracking error vs benchmark
 * @param upCaptureRatio Up-market capture ratio
 * @param downCaptureRatio Down-market capture ratio
 * @param winRate Percentage of positive return periods
 * @param averageWin Average positive return
 * @param averageLoss Average negative return
 * @param profitFactor Profit factor (gross profit / gross loss)
 * @param calmarRatio Calmar ratio (annual return / max drawdown)
 * @param treynorRatio Treynor ratio (return per unit of systematic risk)
 * @param benchmarkReturn Benchmark return for comparison
 * @param rollingPerformance Rolling performance data
 * @param calculationDate Date of calculation
 */
public record PortfolioPerformance(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalReturn,
    BigDecimal totalReturnPercent,
    BigDecimal annualizedReturn,
    BigDecimal volatility,
    BigDecimal sharpeRatio,
    BigDecimal sortinoRatio,
    BigDecimal informationRatio,
    BigDecimal alpha,
    BigDecimal beta,
    BigDecimal maxDrawdown,
    BigDecimal trackingError,
    BigDecimal upCaptureRatio,
    BigDecimal downCaptureRatio,
    BigDecimal winRate,
    BigDecimal averageWin,
    BigDecimal averageLoss,
    BigDecimal profitFactor,
    BigDecimal calmarRatio,
    BigDecimal treynorRatio,
    BigDecimal benchmarkReturn,
    List<RollingPerformanceData> rollingPerformance,
    Instant calculationDate
) {}