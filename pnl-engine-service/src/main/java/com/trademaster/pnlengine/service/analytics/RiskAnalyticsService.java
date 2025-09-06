package com.trademaster.pnlengine.service.analytics;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.utils.RiskCalculations;
import com.trademaster.pnlengine.common.utils.StatisticalCalculations;
import com.trademaster.pnlengine.config.PnLEngineConfiguration;
import com.trademaster.pnlengine.service.analytics.PerformanceMetricsService.DailyReturn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Risk Analytics Calculation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for risk-adjusted returns and Value at Risk calculations
 * using functional composition and statistical analysis.
 * 
 * Single Responsibility: Risk metrics calculation and assessment
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class RiskAnalyticsService {
    
    private final PnLEngineConfiguration config;
    private final PerformanceMetricsService performanceMetricsService;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE RISK ANALYTICS METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate risk-adjusted returns (Sharpe, Sortino ratios)
     * Max 15 lines per method rule
     */
    public Result<RiskAdjustedMetrics, Exception> calculateRiskMetrics(List<DailyReturn> returns) {
        return Result.of(() -> {
            var avgReturn = performanceMetricsService.calculateAverageReturn(returns).getOrThrow();
            var volatility = performanceMetricsService.calculateVolatility(returns, avgReturn).getOrThrow();
            var excessReturn = RiskCalculations.calculateExcessReturn(avgReturn, 
                config.getAnalytics().getRiskFreeRate(), config.getAnalytics().getTradingDaysPerYear());
            
            var downsideDeviation = RiskCalculations.calculateDownsideDeviation(returns, BigDecimal.ZERO);
            var cumulativeReturns = StatisticalCalculations.calculateCumulativeReturns(returns);
            var maxDrawdown = RiskCalculations.calculateMaxDrawdown(cumulativeReturns);
            
            return new RiskAdjustedMetrics(
                RiskCalculations.calculateSharpeRatio(excessReturn, volatility),
                RiskCalculations.calculateSortinoRatio(excessReturn, downsideDeviation),
                StatisticalCalculations.annualizeVolatility(volatility, config.getAnalytics().getTradingDaysPerYear()),
                maxDrawdown
            );
        });
    }
    
    
    /**
     * Generate performance attribution analysis  
     */
    public Result<AttributionAnalysis, Exception> analyzePerformanceAttribution(
            List<DailyReturn> returns, Map<String, BigDecimal> benchmarkReturns) {
        
        return Result.of(() -> {
            var portfolioReturn = performanceMetricsService.calculateTotalReturn(returns).getOrThrow();
            var benchmarkReturn = RiskCalculations.calculateBenchmarkReturn(benchmarkReturns);
            var activeReturn = portfolioReturn.subtract(benchmarkReturn);
            
            // Simplified tracking error calculation using volatility difference
            var portfolioVolatility = performanceMetricsService.calculateVolatility(returns, 
                performanceMetricsService.calculateAverageReturn(returns).getOrThrow()).getOrThrow();
            var trackingError = RiskCalculations.calculateTrackingError(portfolioVolatility, BigDecimal.valueOf(0.12));
            var informationRatio = RiskCalculations.calculateInformationRatio(activeReturn, trackingError);
            
            return new AttributionAnalysis(
                portfolioReturn, benchmarkReturn, activeReturn,
                trackingError, informationRatio
            );
        });
    }
    
    // ============================================================================
    // VALIDATION METHODS
    // ============================================================================
    
    private Result<List<DailyReturn>, Exception> validateReturns(List<DailyReturn> returns) {
        return returns.size() >= 30 ?
            Result.success(returns) :
            Result.failure(new IllegalArgumentException("Insufficient data: minimum 30 returns required"));
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record RiskAdjustedMetrics(
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal annualizedVolatility,
        BigDecimal maxDrawdown
    ) {}
    
    public record AttributionAnalysis(
        BigDecimal portfolioReturn,
        BigDecimal benchmarkReturn,
        BigDecimal activeReturn,
        BigDecimal trackingError,
        BigDecimal informationRatio
    ) {}
}