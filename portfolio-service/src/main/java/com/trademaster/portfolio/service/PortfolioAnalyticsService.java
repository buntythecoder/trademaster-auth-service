package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.PerformanceAttribution;
import com.trademaster.portfolio.dto.PerformanceComparison;
import com.trademaster.portfolio.dto.PortfolioOptimizationSuggestion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Analytics Service Interface
 * 
 * Advanced analytics and reporting service using Java 24 Virtual Threads.
 * Provides comprehensive portfolio performance analysis, benchmarking, and optimization insights.
 * 
 * Key Features:
 * - Portfolio performance metrics and attribution analysis
 * - Risk-adjusted returns and volatility measurements
 * - Benchmark comparison and relative performance tracking
 * - Portfolio optimization suggestions and rebalancing recommendations
 * - Advanced statistical analysis and trend identification
 * - Real-time analytics with sub-100ms response times
 * 
 * Performance Targets:
 * - Metrics calculation: <50ms
 * - Performance analysis: <100ms
 * - Benchmark comparison: <75ms
 * - Optimization analysis: <200ms
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PortfolioAnalyticsService {
    
    /**
     * Calculate comprehensive portfolio metrics
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio metrics
     */
    com.trademaster.portfolio.service.PortfolioMetrics calculatePortfolioMetrics(Long portfolioId);
    
    /**
     * Calculate portfolio performance over time period
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Portfolio performance data
     */
    PortfolioPerformance calculatePortfolioPerformance(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Calculate portfolio statistics summary
     * 
     * @return Portfolio statistics across all portfolios
     */
    PortfolioStatistics calculatePortfolioStatistics();
    
    /**
     * Compare portfolio performance against benchmark
     * 
     * @param portfolioId The portfolio ID
     * @param benchmarkSymbol Benchmark symbol (e.g., "SPY", "VTI")
     * @param fromDate Start date
     * @param toDate End date
     * @return Performance comparison result
     */
    CompletableFuture<PerformanceComparison> compareWithBenchmark(
        Long portfolioId, String benchmarkSymbol, Instant fromDate, Instant toDate);
    
    /**
     * Analyze portfolio correlation with market indices
     * 
     * @param portfolioId The portfolio ID
     * @param indices List of index symbols to compare against
     * @param periodDays Analysis period in days
     * @return Correlation analysis result
     */
    CorrelationAnalysis analyzeCorrelation(Long portfolioId, List<String> indices, Integer periodDays);
    
    /**
     * Calculate portfolio beta against market
     * 
     * @param portfolioId The portfolio ID
     * @param marketSymbol Market index symbol
     * @param periodDays Analysis period in days
     * @return Beta coefficient
     */
    BigDecimal calculatePortfolioBeta(Long portfolioId, String marketSymbol, Integer periodDays);
    
    /**
     * Analyze portfolio diversification
     * 
     * @param portfolioId The portfolio ID
     * @return Diversification analysis result
     */
    DiversificationAnalysis analyzeDiversification(Long portfolioId);
    
    /**
     * Generate portfolio optimization suggestions
     * 
     * @param portfolioId The portfolio ID
     * @param optimizationObjective Optimization objective (RETURN, RISK, SHARPE)
     * @return Portfolio optimization suggestions
     */
    CompletableFuture<List<PortfolioOptimizationSuggestion>> generateOptimizationSuggestions(
        Long portfolioId, String optimizationObjective);
    
    /**
     * Calculate value at risk (VaR)
     * 
     * @param portfolioId The portfolio ID
     * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
     * @param holdingPeriodDays Holding period in days
     * @return Value at Risk amount
     */
    BigDecimal calculateValueAtRisk(Long portfolioId, BigDecimal confidenceLevel, Integer holdingPeriodDays);
    
    /**
     * Calculate conditional value at risk (CVaR/Expected Shortfall)
     * 
     * @param portfolioId The portfolio ID
     * @param confidenceLevel Confidence level
     * @param holdingPeriodDays Holding period in days
     * @return Conditional Value at Risk amount
     */
    BigDecimal calculateConditionalVaR(Long portfolioId, BigDecimal confidenceLevel, Integer holdingPeriodDays);
    
    /**
     * Analyze portfolio performance attribution
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Performance attribution breakdown
     */
    PerformanceAttribution analyzePerformanceAttribution(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Calculate tracking error against benchmark
     * 
     * @param portfolioId The portfolio ID
     * @param benchmarkSymbol Benchmark symbol
     * @param periodDays Analysis period in days
     * @return Tracking error (standard deviation of excess returns)
     */
    BigDecimal calculateTrackingError(Long portfolioId, String benchmarkSymbol, Integer periodDays);
    
    /**
     * Calculate information ratio
     * 
     * @param portfolioId The portfolio ID
     * @param benchmarkSymbol Benchmark symbol
     * @param periodDays Analysis period in days
     * @return Information ratio
     */
    BigDecimal calculateInformationRatio(Long portfolioId, String benchmarkSymbol, Integer periodDays);
    
    /**
     * Analyze sector allocation and performance
     * 
     * @param portfolioId The portfolio ID
     * @return Sector analysis result
     */
    SectorAnalysis analyzeSectorAllocation(Long portfolioId);
    
    /**
     * Calculate portfolio turnover rate
     * 
     * @param portfolioId The portfolio ID
     * @param periodDays Analysis period in days
     * @return Turnover rate percentage
     */
    BigDecimal calculateTurnoverRate(Long portfolioId, Integer periodDays);
    
    /**
     * Analyze trading patterns and frequency
     * 
     * @param portfolioId The portfolio ID
     * @param periodDays Analysis period in days
     * @return Trading pattern analysis
     */
    TradingPatternAnalysis analyzeTradingPatterns(Long portfolioId, Integer periodDays);
    
    /**
     * Calculate cost of trading (fees as % of performance)
     * 
     * @param portfolioId The portfolio ID
     * @param periodDays Analysis period in days
     * @return Trading cost analysis
     */
    TradingCostAnalysis calculateTradingCost(Long portfolioId, Integer periodDays);
    
    /**
     * Generate portfolio stress test scenarios
     * 
     * @param portfolioId The portfolio ID
     * @param stressScenarios List of stress scenarios to test
     * @return Stress test results
     */
    CompletableFuture<StressTestResult> runStressTest(Long portfolioId, List<StressScenario> stressScenarios);
    
    /**
     * Calculate portfolio efficiency frontier
     * 
     * @param portfolioId The portfolio ID
     * @param targetReturns List of target return levels
     * @return Efficiency frontier points
     */
    CompletableFuture<List<EfficientFrontierPoint>> calculateEfficientFrontier(
        Long portfolioId, List<BigDecimal> targetReturns);
    
    /**
     * Analyze portfolio concentration risk
     * 
     * @param portfolioId The portfolio ID
     * @return Concentration risk analysis
     */
    ConcentrationRiskAnalysis analyzeConcentrationRisk(Long portfolioId);
    
    /**
     * Calculate rolling performance metrics
     * 
     * @param portfolioId The portfolio ID
     * @param rollingWindowDays Rolling window size in days
     * @param periodDays Total analysis period in days
     * @return Rolling performance data
     */
    List<RollingPerformancePoint> calculateRollingPerformance(
        Long portfolioId, Integer rollingWindowDays, Integer periodDays);
    
    /**
     * Generate custom analytics report
     * 
     * @param portfolioId The portfolio ID
     * @param reportConfig Report configuration
     * @return Custom analytics report
     */
    CompletableFuture<CustomAnalyticsReport> generateCustomReport(Long portfolioId, ReportConfiguration reportConfig);
}

/**
 * Correlation Analysis DTO
 */
record CorrelationAnalysis(
    Long portfolioId,
    List<IndexCorrelation> correlations,
    BigDecimal averageCorrelation,
    BigDecimal maxCorrelation,
    BigDecimal minCorrelation,
    Instant analysisDate
) {}

/**
 * Index Correlation DTO
 */
record IndexCorrelation(
    String indexSymbol,
    String indexName,
    BigDecimal correlation,
    BigDecimal pValue,
    String significance
) {}

/**
 * Diversification Analysis DTO
 */
record DiversificationAnalysis(
    Long portfolioId,
    Integer totalPositions,
    Integer uniqueSectors,
    Integer uniqueExchanges,
    BigDecimal herfindahlIndex,
    BigDecimal effectivePositions,
    String diversificationLevel,
    List<ConcentrationWarning> concentrationWarnings,
    Instant analysisDate
) {}

/**
 * Concentration Warning DTO
 */
record ConcentrationWarning(
    String type,
    String identifier,
    BigDecimal concentration,
    BigDecimal threshold,
    String severity
) {}

/**
 * Sector Analysis DTO
 */
record SectorAnalysis(
    Long portfolioId,
    List<SectorAllocation> sectorBreakdown,
    String mostAllocatedSector,
    String bestPerformingSector,
    String worstPerformingSector,
    BigDecimal sectorDiversification,
    Instant analysisDate
) {}

/**
 * Sector Allocation DTO
 */
record SectorAllocation(
    String sector,
    Integer positionCount,
    BigDecimal allocation,
    BigDecimal value,
    BigDecimal performance,
    BigDecimal benchmarkAllocation,
    BigDecimal activeAllocation
) {}

/**
 * Trading Pattern Analysis DTO
 */
record TradingPatternAnalysis(
    Long portfolioId,
    Integer totalTrades,
    BigDecimal averageTradeSize,
    BigDecimal averageHoldingPeriod,
    Integer dayTrades,
    Integer swingTrades,
    Integer longTermHolds,
    BigDecimal tradingFrequency,
    String tradingStyle,
    Instant analysisDate
) {}

/**
 * Trading Cost Analysis DTO
 */
record TradingCostAnalysis(
    Long portfolioId,
    BigDecimal totalFees,
    BigDecimal feesAsPercentOfValue,
    BigDecimal feesAsPercentOfReturns,
    BigDecimal averageFeePerTrade,
    BigDecimal implicitCosts,
    BigDecimal totalTradingCost,
    Instant analysisDate
) {}

/**
 * Stress Test Result DTO
 */
record StressTestResult(
    Long portfolioId,
    List<ScenarioResult> scenarioResults,
    BigDecimal worstCaseScenario,
    BigDecimal bestCaseScenario,
    BigDecimal expectedLoss,
    String riskRating,
    Instant testDate
) {}

/**
 * Scenario Result DTO
 */
record ScenarioResult(
    String scenarioName,
    BigDecimal portfolioImpact,
    BigDecimal percentageImpact,
    List<PositionImpact> positionImpacts
) {}

/**
 * Position Impact DTO
 */
record PositionImpact(
    String symbol,
    BigDecimal currentValue,
    BigDecimal stressedValue,
    BigDecimal impact,
    BigDecimal percentageImpact
) {}

/**
 * Stress Scenario DTO
 */
record StressScenario(
    String name,
    String description,
    List<MarketShock> shocks
) {}

/**
 * Market Shock DTO
 */
record MarketShock(
    String assetClass,
    BigDecimal priceChange,
    BigDecimal volatilityMultiplier
) {}

/**
 * Efficient Frontier Point DTO
 */
record EfficientFrontierPoint(
    BigDecimal expectedReturn,
    BigDecimal volatility,
    BigDecimal sharpeRatio,
    List<AssetWeight> optimalWeights
) {}

/**
 * Asset Weight DTO
 */
record AssetWeight(
    String symbol,
    BigDecimal weight,
    BigDecimal expectedReturn,
    BigDecimal volatility
) {}

/**
 * Concentration Risk Analysis DTO
 */
record ConcentrationRiskAnalysis(
    Long portfolioId,
    BigDecimal maxPositionConcentration,
    BigDecimal top5Concentration,
    BigDecimal top10Concentration,
    Integer positionsAbove5Percent,
    Integer positionsAbove10Percent,
    String concentrationRisk,
    List<ConcentrationWarning> warnings,
    Instant analysisDate
) {}

/**
 * Rolling Performance Point DTO
 */
record RollingPerformancePoint(
    Instant date,
    BigDecimal rollingReturn,
    BigDecimal rollingVolatility,
    BigDecimal rollingSharpe,
    BigDecimal rollingMaxDrawdown
) {}

/**
 * Custom Analytics Report DTO
 */
record CustomAnalyticsReport(
    Long portfolioId,
    String reportTitle,
    Instant fromDate,
    Instant toDate,
    List<AnalyticsSection> sections,
    Instant generatedAt
) {}

/**
 * Analytics Section DTO
 */
record AnalyticsSection(
    String sectionName,
    String sectionType,
    Object sectionData,
    List<String> insights
) {}

/**
 * Report Configuration DTO
 */
record ReportConfiguration(
    String reportType,
    List<String> includedSections,
    List<String> benchmarks,
    Integer analysisWindowDays,
    Map<String, Object> customParameters
) {}