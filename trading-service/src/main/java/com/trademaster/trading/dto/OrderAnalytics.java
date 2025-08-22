package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Order Analytics DTO
 * 
 * Comprehensive analytics and performance metrics for order execution:
 * - Execution quality metrics (implementation shortfall, price improvement)
 * - Performance benchmarking against TWAP, VWAP, arrival price
 * - Risk-adjusted returns and Sharpe ratios
 * - Market impact analysis and cost breakdown
 * - Venue performance comparison and routing efficiency
 * - Strategy effectiveness and recommendation scoring
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalytics {
    
    /**
     * Basic Order Information
     */
    private String orderId;
    private Long userId;
    private String symbol;
    private Integer quantity;
    private String strategy;
    private Instant periodStart;
    private Instant periodEnd;
    private Integer totalOrders;
    
    /**
     * Execution Quality Metrics
     */
    private BigDecimal implementationShortfall; // Basis points
    private BigDecimal priceImprovement; // Basis points (positive = better than benchmark)
    private BigDecimal executionSpeed; // Percentage of target speed achieved
    private BigDecimal fillRatio; // Percentage of order quantity filled
    private BigDecimal overallQualityScore; // 0.0-1.0 composite score
    
    /**
     * Performance Benchmarks
     */
    private BigDecimal arrivalPriceBenchmark; // Performance vs. arrival price
    private BigDecimal twapBenchmark; // Performance vs. TWAP
    private BigDecimal vwapBenchmark; // Performance vs. VWAP
    private BigDecimal closePriceBenchmark; // Performance vs. close price
    private BigDecimal midPointBenchmark; // Performance vs. mid-point
    
    /**
     * Cost Analysis (all in basis points)
     */
    private BigDecimal totalTradingCost; // Total cost of execution
    private BigDecimal marketImpactCost; // Temporary + permanent market impact
    private BigDecimal timingCost; // Cost of timing execution
    private BigDecimal spreadCost; // Bid-ask spread cost
    private BigDecimal commissionCost; // Brokerage and exchange fees
    private BigDecimal opportunityCost; // Cost of unfilled quantity
    
    /**
     * Market Impact Analysis
     */
    private BigDecimal temporaryImpact; // Immediate price impact (basis points)
    private BigDecimal permanentImpact; // Lasting price impact (basis points)
    private BigDecimal impactDecayRate; // Rate of temporary impact decay
    private BigDecimal liquidityConsumption; // Percentage of available liquidity consumed
    private BigDecimal volatilityDuringExecution; // Market volatility during execution
    
    /**
     * Execution Timing Analysis
     */
    private Instant orderPlacedAt;
    private Instant firstFillAt;
    private Instant lastFillAt;
    private Duration totalExecutionTime;
    private Duration timeToFirstFill;
    private Duration averageTimeBetweenFills;
    private Integer numberOfFills;
    
    /**
     * Volume and Participation Analysis
     */
    private BigDecimal marketVolumeParticipation; // Percentage of market volume
    private BigDecimal averageFillSize;
    private BigDecimal largestFillSize;
    private BigDecimal smallestFillSize;
    private BigDecimal volumeWeightedAveragePrice; // Actual VWAP achieved
    
    /**
     * Strategy Performance Metrics
     */
    private BigDecimal strategyEffectiveness; // 0.0-1.0 score
    private BigDecimal adaptabilityScore; // How well strategy adapted to conditions
    private BigDecimal consistencyScore; // Performance consistency across executions
    private BigDecimal riskAdjustedReturn; // Return adjusted for execution risk
    private BigDecimal informationRatio; // Excess return per unit of risk
    
    /**
     * Risk Metrics
     */
    private BigDecimal executionRisk; // Standard deviation of execution performance
    private BigDecimal maxDrawdown; // Maximum adverse price movement during execution
    private BigDecimal valueAtRisk; // VaR of execution at 95% confidence
    private BigDecimal expectedShortfall; // Expected loss beyond VaR
    private BigDecimal riskAdjustedScore; // Overall score adjusted for risk
    
    /**
     * Venue Performance Analysis
     */
    private Map<String, VenuePerformance> venueBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenuePerformance {
        private String venueName;
        private Integer fillCount;
        private Integer totalQuantity;
        private BigDecimal averageFillPrice;
        private BigDecimal totalFillValue;
        private Duration averageFillTime;
        private BigDecimal priceImprovement;
        private BigDecimal marketShare; // Percentage of total execution
    }
    
    /**
     * Market Conditions During Execution
     */
    private BigDecimal averageSpread; // Average bid-ask spread
    private BigDecimal averageOrderBookDepth;
    private BigDecimal marketTrend; // Positive = uptrend, negative = downtrend
    private BigDecimal relativePriceVolatility;
    private String marketRegime; // BULL, BEAR, SIDEWAYS, VOLATILE
    
    /**
     * Slippage Analysis
     */
    private BigDecimal totalSlippage; // Total slippage in basis points
    private BigDecimal positiveSlippage; // Favorable price movements
    private BigDecimal negativeSlippage; // Adverse price movements
    private BigDecimal slippageStandardDeviation;
    private List<SlippageBreakdown> slippageByFill;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlippageBreakdown {
        private Integer fillSequence;
        private Instant fillTime;
        private Integer fillQuantity;
        private BigDecimal fillPrice;
        private BigDecimal benchmarkPrice;
        private BigDecimal slippage; // Basis points
        private String slippageCategory; // POSITIVE, NEUTRAL, NEGATIVE
    }
    
    /**
     * Advanced Analytics
     */
    private BigDecimal alphaGeneration; // Excess return generation
    private BigDecimal betaToMarket; // Correlation with market movement
    private BigDecimal trackingError; // Deviation from benchmark
    private BigDecimal informationCoefficient; // Quality of execution decisions
    private BigDecimal executionAlpha; // Alpha attributed to execution skill
    
    /**
     * Execution Analytics
     */
    private ExecutionAnalytics executionAnalytics;
    private PerformanceAnalytics performanceAnalytics;
    private RiskAnalytics riskAnalytics;
    private CostAnalytics costAnalytics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionAnalytics {
        private BigDecimal averageExecutionTime;
        private BigDecimal executionSuccessRate;
        private BigDecimal partialFillRate;
        private BigDecimal cancelationRate;
        private BigDecimal rejectRate;
        private Map<String, Integer> statusDistribution;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAnalytics {
        private BigDecimal averageReturn;
        private BigDecimal volatility;
        private BigDecimal sharpeRatio;
        private BigDecimal maxDrawdown;
        private BigDecimal winRate;
        private BigDecimal averageWin;
        private BigDecimal averageLoss;
        private BigDecimal profitFactor;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalytics {
        private BigDecimal portfolioVaR;
        private BigDecimal expectedShortfall;
        private BigDecimal beta;
        private BigDecimal correlationToMarket;
        private BigDecimal concentrationRisk;
        private Map<String, BigDecimal> riskFactorExposure;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostAnalytics {
        private BigDecimal totalCost;
        private BigDecimal explicitCosts;
        private BigDecimal implicitCosts;
        private BigDecimal averageCostPerShare;
        private BigDecimal costAsPercentageOfValue;
        private Map<String, BigDecimal> costBreakdown;
    }
    
    /**
     * Recommendations and Insights
     */
    private List<String> performanceInsights;
    private List<String> improvementRecommendations;
    private List<String> riskWarnings;
    private String optimalStrategyRecommendation;
    private BigDecimal confidenceInRecommendation;
    
    /**
     * Metadata
     */
    private Instant generatedAt;
    private String analyticsVersion;
    private Boolean isRealTime;
    private String errorMessage;
    
    /**
     * Static factory methods
     */
    public static OrderAnalytics empty() {
        return OrderAnalytics.builder()
            .totalOrders(0)
            .overallQualityScore(BigDecimal.ZERO)
            .generatedAt(Instant.now())
            .build();
    }
    
    public static OrderAnalytics error(String errorMessage) {
        return OrderAnalytics.builder()
            .errorMessage(errorMessage)
            .generatedAt(Instant.now())
            .build();
    }
    
    /**
     * Calculate composite execution score
     */
    public BigDecimal calculateCompositeScore() {
        if (overallQualityScore != null && strategyEffectiveness != null && 
            riskAdjustedScore != null && executionSpeed != null) {
            
            return overallQualityScore.multiply(new BigDecimal("0.3"))
                .add(strategyEffectiveness.multiply(new BigDecimal("0.25")))
                .add(riskAdjustedScore.multiply(new BigDecimal("0.25")))
                .add(executionSpeed.multiply(new BigDecimal("0.2")));
        }
        
        return overallQualityScore != null ? overallQualityScore : BigDecimal.ZERO;
    }
    
    /**
     * Determine performance category
     */
    public String getPerformanceCategory() {
        BigDecimal score = calculateCompositeScore();
        
        if (score.compareTo(new BigDecimal("0.9")) >= 0) {
            return "EXCELLENT";
        } else if (score.compareTo(new BigDecimal("0.8")) >= 0) {
            return "VERY_GOOD";
        } else if (score.compareTo(new BigDecimal("0.7")) >= 0) {
            return "GOOD";
        } else if (score.compareTo(new BigDecimal("0.6")) >= 0) {
            return "SATISFACTORY";
        } else if (score.compareTo(new BigDecimal("0.5")) >= 0) {
            return "BELOW_AVERAGE";
        } else {
            return "POOR";
        }
    }
    
    /**
     * Check if order exceeded performance thresholds
     */
    public boolean isPerformanceAcceptable() {
        BigDecimal minAcceptableScore = new BigDecimal("0.6");
        return calculateCompositeScore().compareTo(minAcceptableScore) >= 0;
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Object> getSummaryStats() {
        return Map.of(
            "compositeScore", calculateCompositeScore(),
            "performanceCategory", getPerformanceCategory(),
            "isAcceptable", isPerformanceAcceptable(),
            "totalCost", totalTradingCost != null ? totalTradingCost : BigDecimal.ZERO,
            "executionTime", totalExecutionTime != null ? totalExecutionTime.toMinutes() : 0,
            "fillRatio", fillRatio != null ? fillRatio : BigDecimal.ZERO
        );
    }
}