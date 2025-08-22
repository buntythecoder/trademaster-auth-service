package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Position Analytics DTO
 * 
 * Advanced position analytics and insights with:
 * - Historical performance analysis
 * - Risk-return profiling
 * - Trading pattern analysis
 * - Benchmark comparisons
 * - Optimization recommendations
 * - Predictive analytics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionAnalytics {
    
    /**
     * Analytics Metadata
     */
    private Long userId;
    private String symbol;
    private String analyticsId;
    private Instant generatedAt;
    private LocalDate analysisStartDate;
    private LocalDate analysisEndDate;
    private Integer lookbackDays;
    private String analysisType; // COMPREHENSIVE, RISK, PERFORMANCE, TRADING
    
    /**
     * Performance Analysis
     */
    private PerformanceAnalysis performanceAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAnalysis {
        private BigDecimal totalReturn; // Total return over period
        private BigDecimal annualizedReturn; // Annualized return
        private BigDecimal compoundReturn; // Compound annual growth rate
        private BigDecimal volatility; // Return volatility
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal sortinoRatio; // Downside risk-adjusted return
        private BigDecimal calmarRatio; // Return/max drawdown ratio
        private BigDecimal informationRatio; // Excess return/tracking error
        private BigDecimal maxDrawdown; // Maximum drawdown
        private BigDecimal currentDrawdown; // Current drawdown from peak
        private Integer drawdownDays; // Days in current drawdown
        private BigDecimal recoveryFactor; // Recovery from max drawdown
        private String performanceGrade; // A, B, C, D, F
        private List<PerformancePeriod> periodicReturns; // Monthly/quarterly returns
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformancePeriod {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String period; // DAILY, WEEKLY, MONTHLY, QUARTERLY
        private BigDecimal returnValue;
        private BigDecimal cumulativeReturn;
        private BigDecimal volatility;
        private String performance; // OUTPERFORM, UNDERPERFORM, INLINE
    }
    
    /**
     * Risk Analysis
     */
    private RiskAnalysis riskAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysis {
        private BigDecimal valueAtRisk95; // 95% VaR
        private BigDecimal valueAtRisk99; // 99% VaR
        private BigDecimal expectedShortfall; // Conditional VaR
        private BigDecimal beta; // Market beta
        private BigDecimal alpha; // Market alpha
        private BigDecimal correlationToMarket; // Market correlation
        private BigDecimal trackingError; // Tracking error
        private BigDecimal upCaptureRatio; // Up market capture
        private BigDecimal downCaptureRatio; // Down market capture
        private BigDecimal tailRisk; // Tail risk measure
        private BigDecimal skewness; // Return skewness
        private BigDecimal kurtosis; // Return kurtosis
        private String riskProfile; // CONSERVATIVE, MODERATE, AGGRESSIVE
        private List<RiskScenario> stressTests; // Stress test results
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskScenario {
        private String scenarioName;
        private BigDecimal priceShock; // Price shock percentage
        private BigDecimal pnlImpact; // P&L impact
        private BigDecimal probabilityOfLoss;
        private String severity; // LOW, MEDIUM, HIGH, EXTREME
    }
    
    /**
     * Trading Pattern Analysis
     */
    private TradingPatternAnalysis tradingPatterns;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingPatternAnalysis {
        private Integer totalTrades; // Total number of trades
        private BigDecimal averageTradeSize; // Average position size
        private BigDecimal averageHoldingPeriod; // Average holding days
        private BigDecimal turnoverRate; // Position turnover rate
        private Integer winnignTrades; // Number of winning trades
        private Integer losingTrades; // Number of losing trades
        private BigDecimal winRate; // Win rate percentage
        private BigDecimal averageWin; // Average winning trade
        private BigDecimal averageLoss; // Average losing trade
        private BigDecimal profitFactor; // Gross profit/gross loss
        private BigDecimal largestWin; // Largest winning trade
        private BigDecimal largestLoss; // Largest losing trade
        private String tradingStyle; // SCALPER, SWING, POSITION, BUY_HOLD
        private Map<String, Integer> tradingByTimeOfDay; // Trading frequency by hour
        private Map<String, Integer> tradingByDayOfWeek; // Trading by day of week
        private List<TradingStreak> streaks; // Winning/losing streaks
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingStreak {
        private String streakType; // WINNING, LOSING
        private Integer length; // Streak length
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal cumulativePnL;
    }
    
    /**
     * Benchmark Comparison
     */
    private BenchmarkComparison benchmarkComparison;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkComparison {
        private String benchmarkSymbol; // Benchmark symbol (e.g., NIFTY50)
        private BigDecimal positionReturn; // Position return
        private BigDecimal benchmarkReturn; // Benchmark return
        private BigDecimal activeReturn; // Active return (position - benchmark)
        private BigDecimal outperformance; // Outperformance amount
        private BigDecimal informationRatio; // Information ratio
        private BigDecimal trackingError; // Tracking error
        private Integer outperformanceDays; // Days of outperformance
        private Integer underperformanceDays; // Days of underperformance
        private BigDecimal hitRate; // Hit rate vs benchmark
        private String relativePerformance; // OUTPERFORM, UNDERPERFORM, INLINE
        private List<BenchmarkPeriod> periodicComparison; // Period-by-period comparison
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkPeriod {
        private LocalDate period;
        private BigDecimal positionReturn;
        private BigDecimal benchmarkReturn;
        private BigDecimal activeReturn;
        private String performance; // OUTPERFORM, UNDERPERFORM, INLINE
    }
    
    /**
     * Cost Analysis
     */
    private CostAnalysis costAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostAnalysis {
        private BigDecimal totalTradingCosts; // Total trading costs
        private BigDecimal commissionCosts; // Commission costs
        private BigDecimal spreadCosts; // Bid-ask spread costs
        private BigDecimal marketImpactCosts; // Market impact costs
        private BigDecimal slippageCosts; // Slippage costs
        private BigDecimal opportunityCosts; // Opportunity costs
        private BigDecimal costAsPercentOfPnL; // Cost as % of P&L
        private BigDecimal costDrag; // Cost drag on returns
        private BigDecimal averageCostPerTrade; // Average cost per trade
        private BigDecimal costEfficiencyRatio; // Cost efficiency
        private String costProfile; // LOW_COST, AVERAGE, HIGH_COST
        private Map<String, BigDecimal> costBreakdown; // Cost breakdown by type
    }
    
    /**
     * Liquidity Analysis
     */
    private LiquidityAnalysis liquidityAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityAnalysis {
        private BigDecimal averageVolume; // Average trading volume
        private BigDecimal volumeRank; // Volume percentile rank
        private BigDecimal averageSpread; // Average bid-ask spread
        private BigDecimal spreadVolatility; // Spread volatility
        private Integer liquidationTime; // Estimated liquidation time (days)
        private BigDecimal liquidationCost; // Estimated liquidation cost
        private String liquidityRating; // EXCELLENT, GOOD, FAIR, POOR
        private BigDecimal marketImpact; // Estimated market impact
        private List<LiquidityEvent> liquidityEvents; // Liquidity stress events
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityEvent {
        private LocalDate eventDate;
        private String eventType; // VOLUME_SPIKE, SPREAD_WIDENING, HALT
        private BigDecimal impactMagnitude;
        private String description;
    }
    
    /**
     * Correlation Analysis
     */
    private CorrelationAnalysis correlationAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrelationAnalysis {
        private BigDecimal marketCorrelation; // Correlation to market index
        private BigDecimal sectorCorrelation; // Correlation to sector
        private Map<String, BigDecimal> peerCorrelations; // Correlation to peer symbols
        private BigDecimal averageCorrelation; // Average correlation
        private BigDecimal maxCorrelation; // Maximum correlation
        private BigDecimal minCorrelation; // Minimum correlation
        private String diversificationBenefit; // HIGH, MEDIUM, LOW
        private List<CorrelationPeriod> rollingCorrelations; // Rolling correlation analysis
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrelationPeriod {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal correlation;
        private String correlationRegime; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Seasonality Analysis
     */
    private SeasonalityAnalysis seasonalityAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalityAnalysis {
        private Map<String, BigDecimal> monthlyReturns; // Average returns by month
        private Map<String, BigDecimal> quarterlyReturns; // Average returns by quarter
        private Map<String, BigDecimal> dayOfWeekReturns; // Average returns by day
        private String bestMonth; // Best performing month
        private String worstMonth; // Worst performing month
        private String bestQuarter; // Best performing quarter
        private String bestDayOfWeek; // Best performing day
        private BigDecimal seasonalityStrength; // Seasonality strength score
        private List<SeasonalPattern> patterns; // Identified seasonal patterns
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalPattern {
        private String patternType; // MONTHLY, QUARTERLY, EARNINGS, HOLIDAY
        private String period; // Specific period
        private BigDecimal averageReturn;
        private BigDecimal probability;
        private String confidence; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Optimization Recommendations
     */
    private List<OptimizationRecommendation> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationRecommendation {
        private String category; // RISK, RETURN, COST, TAX, TIMING
        private String priority; // HIGH, MEDIUM, LOW
        private String recommendation;
        private String rationale;
        private BigDecimal potentialImpact; // Estimated impact
        private String implementationComplexity; // LOW, MEDIUM, HIGH
        private List<String> actionItems; // Specific actions
        private String timeframe; // Implementation timeframe
    }
    
    /**
     * Predictive Analytics
     */
    private PredictiveAnalytics predictiveAnalytics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictiveAnalytics {
        private BigDecimal predictedReturn; // Predicted return (next period)
        private BigDecimal confidenceInterval; // Confidence interval
        private BigDecimal probabilityOfGain; // Probability of positive return
        private BigDecimal probabilityOfLoss; // Probability of negative return
        private BigDecimal expectedValue; // Expected value
        private String trendDirection; // BULLISH, BEARISH, NEUTRAL
        private BigDecimal trendStrength; // Trend strength score
        private List<String> riskFactors; // Identified risk factors
        private List<String> catalysts; // Potential catalysts
        private String modelAccuracy; // Historical model accuracy
    }
    
    /**
     * Advanced Metrics
     */
    private AdvancedMetrics advancedMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvancedMetrics {
        private BigDecimal omegaRatio; // Omega ratio
        private BigDecimal gainLossRatio; // Gain to loss ratio
        private BigDecimal payoffRatio; // Average win / average loss
        private BigDecimal expectancy; // Mathematical expectancy
        private BigDecimal kellyPercentage; // Kelly criterion percentage
        private BigDecimal ulcerIndex; // Ulcer index
        private BigDecimal painIndex; // Pain index
        private BigDecimal lakingRatio; // LAK ratio
        private BigDecimal modifiedSharpe; // Modified Sharpe ratio
        private BigDecimal adjustedAlpha; // Risk-adjusted alpha
        private String efficiencyRating; // EXCELLENT, GOOD, AVERAGE, POOR
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Get overall analytics grade
     */
    public String getOverallGrade() {
        if (performanceAnalysis != null && performanceAnalysis.getPerformanceGrade() != null) {
            return performanceAnalysis.getPerformanceGrade();
        }
        return "N/A";
    }
    
    /**
     * Check if position outperformed benchmark
     */
    public boolean outperformedBenchmark() {
        return benchmarkComparison != null && 
               benchmarkComparison.getActiveReturn() != null &&
               benchmarkComparison.getActiveReturn().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get risk-adjusted performance score
     */
    public BigDecimal getRiskAdjustedScore() {
        if (performanceAnalysis == null || performanceAnalysis.getSharpeRatio() == null) {
            return BigDecimal.ZERO;
        }
        return performanceAnalysis.getSharpeRatio();
    }
    
    /**
     * Get top recommendation by priority
     */
    public OptimizationRecommendation getTopRecommendation() {
        if (recommendations == null || recommendations.isEmpty()) {
            return null;
        }
        
        return recommendations.stream()
            .filter(rec -> "HIGH".equals(rec.getPriority()))
            .findFirst()
            .orElse(recommendations.get(0));
    }
    
    /**
     * Calculate overall efficiency score
     */
    public BigDecimal getEfficiencyScore() {
        // Composite score based on multiple factors
        BigDecimal score = BigDecimal.ZERO;
        int factors = 0;
        
        if (performanceAnalysis != null && performanceAnalysis.getSharpeRatio() != null) {
            score = score.add(performanceAnalysis.getSharpeRatio().multiply(BigDecimal.valueOf(25)));
            factors++;
        }
        
        if (costAnalysis != null && costAnalysis.getCostEfficiencyRatio() != null) {
            score = score.add(costAnalysis.getCostEfficiencyRatio().multiply(BigDecimal.valueOf(25)));
            factors++;
        }
        
        if (tradingPatterns != null && tradingPatterns.getWinRate() != null) {
            score = score.add(tradingPatterns.getWinRate().multiply(BigDecimal.valueOf(0.25)));
            factors++;
        }
        
        if (riskAnalysis != null && riskAnalysis.getAlpha() != null) {
            score = score.add(riskAnalysis.getAlpha().multiply(BigDecimal.valueOf(25)));
            factors++;
        }
        
        return factors > 0 ? score.divide(BigDecimal.valueOf(factors), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    
    /**
     * Get analytics summary
     */
    public Map<String, Object> getAnalyticsSummary() {
        return Map.of(
            "symbol", symbol != null ? symbol : "N/A",
            "analysisType", analysisType != null ? analysisType : "COMPREHENSIVE",
            "overallGrade", getOverallGrade(),
            "totalReturn", performanceAnalysis != null && performanceAnalysis.getTotalReturn() != null ? 
                         performanceAnalysis.getTotalReturn() : BigDecimal.ZERO,
            "sharpeRatio", performanceAnalysis != null && performanceAnalysis.getSharpeRatio() != null ? 
                         performanceAnalysis.getSharpeRatio() : BigDecimal.ZERO,
            "maxDrawdown", performanceAnalysis != null && performanceAnalysis.getMaxDrawdown() != null ? 
                         performanceAnalysis.getMaxDrawdown() : BigDecimal.ZERO,
            "winRate", tradingPatterns != null && tradingPatterns.getWinRate() != null ? 
                      tradingPatterns.getWinRate() : BigDecimal.ZERO,
            "outperformedBenchmark", outperformedBenchmark(),
            "efficiencyScore", getEfficiencyScore(),
            "lookbackDays", lookbackDays != null ? lookbackDays : 0,
            "generatedAt", generatedAt != null ? generatedAt : Instant.EPOCH
        );
    }
    
    /**
     * Static factory method
     */
    public static PositionAnalytics comprehensive(Long userId, String symbol, Integer lookbackDays) {
        return PositionAnalytics.builder()
            .userId(userId)
            .symbol(symbol)
            .analyticsId("PA_" + symbol + "_" + System.currentTimeMillis())
            .generatedAt(Instant.now())
            .lookbackDays(lookbackDays)
            .analysisType("COMPREHENSIVE")
            .analysisStartDate(LocalDate.now().minusDays(lookbackDays))
            .analysisEndDate(LocalDate.now())
            .build();
    }
}