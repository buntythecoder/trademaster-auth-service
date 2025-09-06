package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PnL Trend Analysis for performance pattern identification
 * 
 * @param portfolioId Portfolio identifier
 * @param analysisFromDate Analysis period start date
 * @param analysisToDate Analysis period end date
 * @param periodicity Data periodicity (DAILY, WEEKLY, MONTHLY, QUARTERLY)
 * @param trendDirection Overall trend direction (UPWARD, DOWNWARD, SIDEWAYS)
 * @param trendStrength Trend strength (0.0 to 1.0)
 * @param trendConfidence Confidence in trend analysis (0.0 to 1.0)
 * @param periodicPnL PnL data points over time
 * @param movingAverages Moving averages for smoothing
 * @param volatilityMetrics Volatility measurements
 * @param seasonalityPattern Seasonal patterns if detected
 * @param anomalies Detected anomalies or outliers
 * @param forecastData Short-term forecast based on trend
 * @param keyInsights Key insights from trend analysis
 * @param analysisDate Date of analysis
 */
public record PnLTrendAnalysis(
    Long portfolioId,
    Instant analysisFromDate,
    Instant analysisToDate,
    Periodicity periodicity,
    TrendDirection trendDirection,
    BigDecimal trendStrength,
    BigDecimal trendConfidence,
    List<PnLDataPoint> periodicPnL,
    MovingAverageData movingAverages,
    VolatilityMetrics volatilityMetrics,
    SeasonalityPattern seasonalityPattern,
    List<PnLAnomaly> anomalies,
    ForecastData forecastData,
    List<String> keyInsights,
    Instant analysisDate
) {
    
    public enum Periodicity {
        DAILY, WEEKLY, MONTHLY, QUARTERLY
    }
    
    public enum TrendDirection {
        UPWARD, DOWNWARD, SIDEWAYS, VOLATILE
    }
    
    public record PnLDataPoint(
        Instant date,
        BigDecimal pnl,
        BigDecimal cumulativePnL,
        BigDecimal returnPercent
    ) {}
    
    public record MovingAverageData(
        List<BigDecimal> ma7,
        List<BigDecimal> ma30,
        List<BigDecimal> ma90
    ) {}
    
    public record VolatilityMetrics(
        BigDecimal standardDeviation,
        BigDecimal averageVolatility,
        BigDecimal maxVolatility,
        BigDecimal minVolatility
    ) {}
    
    public record SeasonalityPattern(
        Boolean seasonalityDetected,
        String pattern,
        BigDecimal seasonalityStrength,
        List<String> seasonalFactors
    ) {}
    
    public record PnLAnomaly(
        Instant date,
        BigDecimal value,
        String anomalyType,
        BigDecimal deviation,
        String possibleCause
    ) {}
    
    public record ForecastData(
        List<PnLDataPoint> forecasts,
        BigDecimal confidence,
        String forecastHorizon
    ) {}
}