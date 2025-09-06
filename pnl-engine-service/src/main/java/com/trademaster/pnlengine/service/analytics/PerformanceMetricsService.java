package com.trademaster.pnlengine.service.analytics;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import com.trademaster.pnlengine.common.utils.StatisticalCalculations;
import com.trademaster.pnlengine.config.PnLEngineConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Performance Metrics Calculation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for calculating core performance statistics
 * using functional composition and statistical computations.
 * 
 * Single Responsibility: Core performance metrics calculation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PerformanceMetricsService {
    
    private final PnLEngineConfiguration config;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE METRICS METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate comprehensive performance metrics
     * Max 15 lines per method rule
     */
    public Result<PerformanceMetrics, Exception> calculatePerformanceMetrics(
            String userId, List<DailyReturn> returns) {
        
        return Validation.USER_ID.apply(userId)
            .flatMap(validUserId -> validateReturns(returns))
            .map(this::computePerformanceStatistics)
            .flatMap(this::validateMetrics);
    }
    
    
    // ============================================================================
    // INTERNAL CALCULATION METHODS
    // ============================================================================
    
    private PerformanceMetrics computePerformanceStatistics(List<DailyReturn> returns) {
        // Calculate metrics directly using utility functions
        var totalReturn = StatisticalCalculations.calculateCompoundReturn(returns);
        var avgReturn = StatisticalCalculations.calculateMean(returns.stream().map(DailyReturn::returnPercent).toList());
        var variance = StatisticalCalculations.calculateVariance(returns, avgReturn);
        var volatility = StatisticalCalculations.calculateStandardDeviation(variance);
        
        // Calculate win rate directly
        var returnValues = returns.stream().map(DailyReturn::returnPercent).toList();
        var positiveCount = StatisticalCalculations.countPositiveValues(returnValues);
        var winRate = BigDecimal.valueOf(positiveCount)
            .divide(BigDecimal.valueOf(returns.size()), PRECISION)
            .multiply(HUNDRED);
        
        var tradingDays = config.getAnalytics().getTradingDaysPerYear();
        var annualizedReturn = StatisticalCalculations.annualize(avgReturn, tradingDays);
        var annualizedVolatility = StatisticalCalculations.annualizeVolatility(volatility, tradingDays);
        
        return new PerformanceMetrics(
            totalReturn, annualizedReturn, annualizedVolatility, BigDecimal.ZERO,
            winRate, returns.size()
        );
    }
    
    private Result<List<DailyReturn>, Exception> validateReturns(List<DailyReturn> returns) {
        return returns.size() >= 30 ?
            Result.success(returns) :
            Result.failure(new IllegalArgumentException("Insufficient data: minimum 30 returns required"));
    }
    
    private Result<PerformanceMetrics, Exception> validateMetrics(PerformanceMetrics metrics) {
        return metrics.totalReturn().abs().compareTo(BigDecimal.valueOf(1000)) <= 0 ?
            Result.success(metrics) :
            Result.failure(new IllegalStateException("Invalid performance metrics calculated"));
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record DailyReturn(LocalDate date, BigDecimal returnPercent) {}
    
    public record PerformanceMetrics(
        BigDecimal totalReturn,
        BigDecimal annualizedReturn, 
        BigDecimal volatility,
        BigDecimal maxDrawdown,
        BigDecimal winRate,
        Integer observationCount
    ) {}
}