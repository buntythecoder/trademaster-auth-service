package com.trademaster.pnlengine.common.utils;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.service.analytics.PerformanceMetricsService.DailyReturn;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Statistical Calculations Utility
 * 
 * MANDATORY: Java 24 + Functional Programming + Immutable Patterns
 * 
 * Utility class providing common statistical functions used across analytics services.
 * All methods are pure functions without side effects.
 * 
 * Single Responsibility: Statistical computation utilities
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
public final class StatisticalCalculations {
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    private StatisticalCalculations() {
        // Utility class - no instantiation
    }
    
    /**
     * Calculate variance from returns and mean
     */
    public static BigDecimal calculateVariance(List<DailyReturn> returns, BigDecimal mean) {
        return returns.stream()
            .map(ret -> ret.returnPercent().subtract(mean))
            .map(diff -> diff.multiply(diff))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size() - 1), PRECISION);
    }
    
    /**
     * Calculate standard deviation from variance
     */
    public static BigDecimal calculateStandardDeviation(BigDecimal variance) {
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
    
    /**
     * Calculate cumulative returns from daily returns
     */
    public static List<BigDecimal> calculateCumulativeReturns(List<DailyReturn> returns) {
        return IntStream.range(0, returns.size())
            .mapToObj(i -> returns.subList(0, i + 1))
            .map(StatisticalCalculations::calculateCompoundReturn)
            .toList();
    }
    
    /**
     * Calculate compound return from list of daily returns
     */
    public static BigDecimal calculateCompoundReturn(List<DailyReturn> returns) {
        return returns.stream()
            .map(ret -> BigDecimal.ONE.add(ret.returnPercent().divide(HUNDRED)))
            .reduce(BigDecimal.ONE, BigDecimal::multiply)
            .subtract(BigDecimal.ONE)
            .multiply(HUNDRED);
    }
    
    /**
     * Calculate sum of BigDecimal values
     */
    public static BigDecimal calculateSum(List<BigDecimal> values) {
        return values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate mean of BigDecimal values
     */
    public static BigDecimal calculateMean(List<BigDecimal> values) {
        return calculateSum(values)
            .divide(BigDecimal.valueOf(values.size()), PRECISION);
    }
    
    /**
     * Calculate maximum value in list
     */
    public static BigDecimal calculateMax(List<BigDecimal> values) {
        return values.stream()
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Count positive values in list
     */
    public static long countPositiveValues(List<BigDecimal> values) {
        return values.stream()
            .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
            .count();
    }
    
    /**
     * Filter negative values and apply transformation
     */
    public static List<BigDecimal> calculateNegativeSquaredDeviations(List<DailyReturn> returns, BigDecimal threshold) {
        return returns.stream()
            .map(DailyReturn::returnPercent)
            .filter(ret -> ret.compareTo(threshold) < 0)
            .map(ret -> ret.subtract(threshold))
            .map(diff -> diff.multiply(diff))
            .toList();
    }
    
    /**
     * Calculate annualized value
     */
    public static BigDecimal annualize(BigDecimal value, int tradingDaysPerYear) {
        return value.multiply(BigDecimal.valueOf(tradingDaysPerYear));
    }
    
    /**
     * Calculate annualized volatility
     */
    public static BigDecimal annualizeVolatility(BigDecimal volatility, int tradingDaysPerYear) {
        return volatility.multiply(BigDecimal.valueOf(Math.sqrt(tradingDaysPerYear)));
    }
}