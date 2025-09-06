package com.trademaster.pnlengine.service.analytics;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.utils.RiskCalculations;
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

/**
 * Value at Risk Calculation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for VaR calculations and attribution analysis
 * using functional composition and statistical analysis.
 * 
 * Single Responsibility: VaR metrics and attribution analysis
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class VaRCalculationService {
    
    private final PnLEngineConfiguration config;
    private final PerformanceMetricsService performanceMetricsService;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE VAR CALCULATION METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate Value at Risk (VaR) metrics
     * Max 15 lines per method rule
     */
    public Result<VaRMetrics, Exception> calculateVaR(List<DailyReturn> returns, BigDecimal portfolioValue) {
        return validateReturns(returns)
            .map(validReturns -> {
                var sortedReturns = validReturns.stream()
                    .map(DailyReturn::returnPercent)
                    .sorted()
                    .toList();
                
                var confidenceLevel = config.getAnalytics().getConfidenceLevel();
                var varIndex = RiskCalculations.calculateVaRIndex(confidenceLevel, sortedReturns.size());
                var historicalVaR = sortedReturns.get(Math.max(0, varIndex));
                var expectedShortfall = RiskCalculations.calculateExpectedShortfall(sortedReturns, varIndex);
                
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
            var portfolioReturn = performanceMetricsService.calculateTotalReturn(returns).getOrThrow();
            var benchmarkReturn = RiskCalculations.calculateBenchmarkReturn(benchmarkReturns);
            var activeReturn = portfolioReturn.subtract(benchmarkReturn);
            var trackingError = RiskCalculations.calculateTrackingError(BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.12));
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
    
    public record VaRMetrics(
        BigDecimal valueAtRisk,
        BigDecimal expectedShortfall,
        BigDecimal confidenceLevel
    ) {}
    
    public record AttributionAnalysis(
        BigDecimal portfolioReturn,
        BigDecimal benchmarkReturn,
        BigDecimal activeReturn,
        BigDecimal trackingError,
        BigDecimal informationRatio
    ) {}
}