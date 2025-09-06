package com.trademaster.pnlengine.service.calculation;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Performance Analytics and Metrics Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for calculating advanced performance metrics and analytics
 * using functional composition and statistical computations.
 * 
 * Single Responsibility: Performance metrics calculation and risk analytics
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PerformanceAnalyticsService {
    
    @Value("${pnl.analytics.risk-free-rate:0.06}")
    private BigDecimal riskFreeRate;
    
    @Value("${pnl.analytics.trading-days-per-year:252}")
    private int tradingDaysPerYear;
    
    @Value("${pnl.analytics.confidence-level:0.95}")
    private BigDecimal confidenceLevel;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE ANALYTICS METHODS (MAX 10 METHODS RULE)  
    // ============================================================================
    
    /**
     * Calculate comprehensive performance analytics
     * Max 15 lines per method rule
     */
    public Result<PerformanceMetrics, Exception> calculatePerformanceMetrics(
            String userId, List<DailyReturn> returns) {
        
        return Validation.USER_ID.apply(userId)
            .flatMap(validUserId -> validateReturns(returns))
            .map(this::computePerformanceStatistics)
            .flatMap(this::validateMetrics);
    }
    
    /**
     * Calculate risk-adjusted returns (Sharpe, Sortino ratios)
     */
    public Result<RiskAdjustedMetrics, Exception> calculateRiskMetrics(List<DailyReturn> returns) {
        return Result.of(() -> {
            var avgReturn = calculateAverageReturn(returns);
            var volatility = calculateVolatility(returns, avgReturn);
            var downsideDeviation = calculateDownsideDeviation(returns, BigDecimal.ZERO);
            
            return new RiskAdjustedMetrics(
                calculateSharpeRatio(avgReturn, volatility),
                calculateSortinoRatio(avgReturn, downsideDeviation),
                volatility.multiply(BigDecimal.valueOf(Math.sqrt(tradingDaysPerYear))),
                calculateMaxDrawdown(returns)
            );
        });
    }
    
    /**
     * Calculate Value at Risk (VaR) metrics
     */
    public Result<VaRMetrics, Exception> calculateVaR(List<DailyReturn> returns, BigDecimal portfolioValue) {
        return validateReturns(returns)
            .map(validReturns -> {
                var sortedReturns = validReturns.stream()
                    .map(DailyReturn::returnPercent)
                    .sorted()
                    .toList();
                
                var varIndex = (int) Math.ceil((1 - confidenceLevel.doubleValue()) * sortedReturns.size()) - 1;
                var historicalVaR = sortedReturns.get(Math.max(0, varIndex));
                var expectedShortfall = calculateExpectedShortfall(sortedReturns, varIndex);
                
                return new VaRMetrics(
                    historicalVaR.multiply(portfolioValue).abs(),
                    expectedShortfall.multiply(portfolioValue).abs(),
                    confidenceLevel.multiply(HUNDRED)
                );
            });
    }
    
    /**
     * Generate performance attribution analysis  
     */
    public Result<AttributionAnalysis, Exception> analyzePerformanceAttribution(
            List<DailyReturn> returns, Map<String, BigDecimal> benchmarkReturns) {
        
        return Result.of(() -> {
            var portfolioReturn = calculateTotalReturn(returns);
            var benchmarkReturn = benchmarkReturns.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            var activeReturn = portfolioReturn.subtract(benchmarkReturn);
            var trackingError = calculateTrackingError(returns, benchmarkReturns);
            var informationRatio = calculateInformationRatio(activeReturn, trackingError);
            
            return new AttributionAnalysis(
                portfolioReturn, benchmarkReturn, activeReturn,
                trackingError, informationRatio
            );
        });
    }
    
    // ============================================================================
    // STATISTICAL CALCULATION METHODS
    // ============================================================================
    
    private PerformanceMetrics computePerformanceStatistics(List<DailyReturn> returns) {
        var totalReturn = calculateTotalReturn(returns);
        var avgReturn = calculateAverageReturn(returns);
        var volatility = calculateVolatility(returns, avgReturn);
        var maxDrawdown = calculateMaxDrawdown(returns);
        
        var annualizedReturn = avgReturn.multiply(BigDecimal.valueOf(tradingDaysPerYear));
        var annualizedVolatility = volatility.multiply(BigDecimal.valueOf(Math.sqrt(tradingDaysPerYear)));
        
        return new PerformanceMetrics(
            totalReturn, annualizedReturn, annualizedVolatility, maxDrawdown,
            calculateWinRate(returns), returns.size()
        );
    }
    
    private BigDecimal calculateTotalReturn(List<DailyReturn> returns) {
        return returns.stream()
            .map(ret -> BigDecimal.ONE.add(ret.returnPercent().divide(HUNDRED)))
            .reduce(BigDecimal.ONE, BigDecimal::multiply)
            .subtract(BigDecimal.ONE)
            .multiply(HUNDRED);
    }
    
    private BigDecimal calculateAverageReturn(List<DailyReturn> returns) {
        return returns.stream()
            .map(DailyReturn::returnPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), PRECISION);
    }
    
    private BigDecimal calculateVolatility(List<DailyReturn> returns, BigDecimal avgReturn) {
        var variance = returns.stream()
            .map(ret -> ret.returnPercent().subtract(avgReturn))
            .map(diff -> diff.multiply(diff))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size() - 1), PRECISION);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
    
    // ============================================================================
    // RISK METRICS CALCULATION
    // ============================================================================
    
    private BigDecimal calculateSharpeRatio(BigDecimal avgReturn, BigDecimal volatility) {
        var excessReturn = avgReturn.subtract(riskFreeRate.divide(BigDecimal.valueOf(tradingDaysPerYear)));
        return volatility.compareTo(BigDecimal.ZERO) > 0 ?
            excessReturn.divide(volatility, PRECISION) : BigDecimal.ZERO;
    }
    
    private BigDecimal calculateDownsideDeviation(List<DailyReturn> returns, BigDecimal threshold) {
        var negativeReturns = returns.stream()
            .map(DailyReturn::returnPercent)
            .filter(ret -> ret.compareTo(threshold) < 0)
            .map(ret -> ret.subtract(threshold))
            .map(diff -> diff.multiply(diff))
            .toList();
        
        if (negativeReturns.isEmpty()) return BigDecimal.ZERO;
        
        var downVariance = negativeReturns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(negativeReturns.size()), PRECISION);
        
        return BigDecimal.valueOf(Math.sqrt(downVariance.doubleValue()));
    }
    
    private BigDecimal calculateMaxDrawdown(List<DailyReturn> returns) {
        var cumulativeReturns = calculateCumulativeReturns(returns);
        var runningMax = BigDecimal.ZERO;
        var maxDrawdown = BigDecimal.ZERO;
        
        for (var cumReturn : cumulativeReturns) {
            runningMax = runningMax.max(cumReturn);
            var drawdown = runningMax.subtract(cumReturn);
            maxDrawdown = maxDrawdown.max(drawdown);
        }
        
        return maxDrawdown;
    }
    
    // ============================================================================
    // VALIDATION AND HELPER METHODS
    // ============================================================================
    
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
    
    private List<BigDecimal> calculateCumulativeReturns(List<DailyReturn> returns) {
        return IntStream.range(0, returns.size())
            .mapToObj(i -> returns.subList(0, i + 1))
            .map(this::calculateTotalReturn)
            .toList();
    }
    
    private BigDecimal calculateWinRate(List<DailyReturn> returns) {
        var positiveReturns = returns.stream()
            .map(DailyReturn::returnPercent)
            .filter(ret -> ret.compareTo(BigDecimal.ZERO) > 0)
            .count();
        
        return BigDecimal.valueOf(positiveReturns)
            .divide(BigDecimal.valueOf(returns.size()), PRECISION)
            .multiply(HUNDRED);
    }
    
    // Additional helper methods for complex calculations
    private BigDecimal calculateExpectedShortfall(List<BigDecimal> sortedReturns, int varIndex) {
        return sortedReturns.subList(0, Math.max(1, varIndex + 1)).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(1, varIndex + 1)), PRECISION);
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
    
    public record RiskAdjustedMetrics(
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal annualizedVolatility,
        BigDecimal maxDrawdown
    ) {}
    
    public record VaRMetrics(
        BigDecimal valueAtRisk,
        BigDecimal expectedShortfall,
        BigDecimal confidenceLevel
    ) {}
}