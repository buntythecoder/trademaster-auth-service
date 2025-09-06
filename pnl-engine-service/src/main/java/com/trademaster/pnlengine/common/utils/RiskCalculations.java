package com.trademaster.pnlengine.common.utils;

import com.trademaster.pnlengine.service.analytics.PerformanceMetricsService.DailyReturn;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Risk Calculations Utility
 * 
 * MANDATORY: Java 24 + Functional Programming + Immutable Patterns
 * 
 * Utility class providing risk-specific calculation functions.
 * All methods are pure functions without side effects.
 * 
 * Single Responsibility: Risk computation utilities
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
public final class RiskCalculations {
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    
    private RiskCalculations() {
        // Utility class - no instantiation
    }
    
    /**
     * Calculate Sharpe ratio
     */
    public static BigDecimal calculateSharpeRatio(BigDecimal excessReturn, BigDecimal volatility) {
        return volatility.compareTo(BigDecimal.ZERO) > 0 ?
            excessReturn.divide(volatility, PRECISION) : BigDecimal.ZERO;
    }
    
    /**
     * Calculate Sortino ratio
     */
    public static BigDecimal calculateSortinoRatio(BigDecimal excessReturn, BigDecimal downsideDeviation) {
        return downsideDeviation.compareTo(BigDecimal.ZERO) > 0 ?
            excessReturn.divide(downsideDeviation, PRECISION) : BigDecimal.ZERO;
    }
    
    /**
     * Calculate excess return over risk-free rate
     */
    public static BigDecimal calculateExcessReturn(BigDecimal avgReturn, BigDecimal riskFreeRate, int tradingDays) {
        return avgReturn.subtract(riskFreeRate.divide(BigDecimal.valueOf(tradingDays)));
    }
    
    /**
     * Calculate downside deviation from threshold
     */
    public static BigDecimal calculateDownsideDeviation(List<DailyReturn> returns, BigDecimal threshold) {
        var negativeDeviations = StatisticalCalculations.calculateNegativeSquaredDeviations(returns, threshold);
        
        if (negativeDeviations.isEmpty()) return BigDecimal.ZERO;
        
        var downVariance = StatisticalCalculations.calculateMean(negativeDeviations);
        return StatisticalCalculations.calculateStandardDeviation(downVariance);
    }
    
    /**
     * Calculate maximum drawdown from cumulative returns
     */
    public static BigDecimal calculateMaxDrawdown(List<BigDecimal> cumulativeReturns) {
        var runningMax = BigDecimal.ZERO;
        var maxDrawdown = BigDecimal.ZERO;
        
        for (var cumReturn : cumulativeReturns) {
            runningMax = runningMax.max(cumReturn);
            var drawdown = runningMax.subtract(cumReturn);
            maxDrawdown = maxDrawdown.max(drawdown);
        }
        
        return maxDrawdown;
    }
    
    /**
     * Calculate VaR index for given confidence level
     */
    public static int calculateVaRIndex(BigDecimal confidenceLevel, int returnCount) {
        return (int) Math.ceil((1 - confidenceLevel.doubleValue()) * returnCount) - 1;
    }
    
    /**
     * Calculate expected shortfall from sorted returns
     */
    public static BigDecimal calculateExpectedShortfall(List<BigDecimal> sortedReturns, int varIndex) {
        var tailReturns = sortedReturns.subList(0, Math.max(1, varIndex + 1));
        return StatisticalCalculations.calculateMean(tailReturns);
    }
    
    /**
     * Calculate information ratio
     */
    public static BigDecimal calculateInformationRatio(BigDecimal activeReturn, BigDecimal trackingError) {
        return trackingError.compareTo(BigDecimal.ZERO) > 0 ?
            activeReturn.divide(trackingError, PRECISION) : BigDecimal.ZERO;
    }
    
    /**
     * Calculate benchmark return from map of returns
     */
    public static BigDecimal calculateBenchmarkReturn(Map<String, BigDecimal> benchmarkReturns) {
        return benchmarkReturns.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Simplified tracking error calculation
     */
    public static BigDecimal calculateTrackingError(BigDecimal portfolioVolatility, BigDecimal benchmarkVolatility) {
        return portfolioVolatility.subtract(benchmarkVolatility).abs();
    }
}