package com.trademaster.pnlengine.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * P&L Domain Types and Supporting Records
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Comprehensive collection of domain types and supporting records for
 * multi-broker P&L calculations, tax optimization, and performance analytics.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public class PnLDomainTypes {
    
    // ============================================================================
    // PERFORMANCE ATTRIBUTION RECORDS
    // ============================================================================
    
    /**
     * Security Selection Attribution Analysis
     */
    public record SecuritySelectionAttribution(
        BigDecimal totalContribution,
        BigDecimal averageContribution,
        BigDecimal bestPerformer,
        BigDecimal worstPerformer,
        Integer securitiesAnalyzed,
        String attributionMethod
    ) {}
    
    /**
     * Asset Allocation Attribution Analysis
     */
    public record AssetAllocationAttribution(
        BigDecimal sectorAllocation,
        BigDecimal assetClassAllocation,
        BigDecimal geographicAllocation,
        BigDecimal totalAllocationEffect,
        String allocationStrategy
    ) {}
    
    /**
     * Timing Effect Attribution
     */
    public record TimingEffectAttribution(
        BigDecimal entryTimingEffect,
        BigDecimal exitTimingEffect,
        BigDecimal totalTimingEffect,
        Integer tradingDecisions,
        BigDecimal timingSkill
    ) {}
    
    /**
     * Interaction Effect Attribution
     */
    public record InteractionEffectAttribution(
        BigDecimal selectionInteraction,
        BigDecimal allocationInteraction,
        BigDecimal timingInteraction,
        BigDecimal totalInteractionEffect
    ) {}
    
    /**
     * Sector Attribution Breakdown
     */
    public record SectorAttributionBreakdown(
        String sectorName,
        BigDecimal selectionEffect,
        BigDecimal allocationEffect,
        BigDecimal interactionEffect,
        BigDecimal totalContribution,
        BigDecimal benchmarkWeight,
        BigDecimal portfolioWeight
    ) {}
    
    /**
     * Stock Attribution Breakdown
     */
    public record StockAttributionBreakdown(
        String symbol,
        String companyName,
        BigDecimal securitySelection,
        BigDecimal allocationEffect,
        BigDecimal totalContribution,
        BigDecimal portfolioWeight,
        BigDecimal benchmarkWeight
    ) {}
    
    /**
     * Attribution Summary
     */
    public record AttributionSummary(
        BigDecimal totalActiveReturn,
        BigDecimal securitySelectionReturn,
        BigDecimal assetAllocationReturn,
        BigDecimal timingReturn,
        BigDecimal interactionReturn,
        String topContributor,
        String bottomContributor
    ) {}
    
    // ============================================================================
    // RISK ANALYSIS RECORDS
    // ============================================================================
    
    /**
     * Comprehensive Volatility Metrics
     */
    public record VolatilityMetrics(
        BigDecimal annualizedVolatility,
        BigDecimal downSideVolatility,
        BigDecimal upSideVolatility,
        BigDecimal rollingVolatility,
        Integer observationPeriod,
        String calculationMethod
    ) {}
    
    /**
     * Drawdown Analysis
     */
    public record DrawdownAnalysis(
        BigDecimal maxDrawdown,
        BigDecimal maxDrawdownPercent,
        Integer maxDrawdownDuration,
        Instant maxDrawdownStart,
        Instant maxDrawdownEnd,
        Instant recoveryDate,
        Integer currentDrawdownDays
    ) {}
    
    /**
     * Risk Contribution Analysis
     */
    public record RiskContribution(
        String contributorType,
        String contributorName,
        BigDecimal volatilityContribution,
        BigDecimal varContribution,
        BigDecimal correlationContribution,
        BigDecimal percentageOfTotalRisk
    ) {}
    
    // ============================================================================
    // CORRELATION ANALYSIS RECORDS
    // ============================================================================
    
    /**
     * Position Correlation Analysis
     */
    public record PositionCorrelation(
        String symbol,
        String companyName,
        BigDecimal correlationToBenchmark,
        BigDecimal correlationToPortfolio,
        BigDecimal beta,
        BigDecimal specificRisk,
        BigDecimal systematicRisk
    ) {}
    
    /**
     * Sector Correlation Analysis
     */
    public record SectorCorrelation(
        String sectorName,
        BigDecimal correlationToBenchmark,
        BigDecimal sectorBeta,
        BigDecimal sectorWeight,
        BigDecimal riskContribution,
        Integer positionsInSector
    ) {}
    
    /**
     * Correlation Trend Analysis
     */
    public record CorrelationTrend(
        String trendDirection,
        BigDecimal currentCorrelation,
        BigDecimal averageCorrelation,
        BigDecimal trendSlope,
        Integer periodDays,
        List<PnLDataPoint> correlationHistory
    ) {}
    
    // ============================================================================
    // TAX OPTIMIZATION RECORDS
    // ============================================================================
    
    /**
     * Tax Lot Selection for Optimization
     */
    public record TaxLotSelection(
        String lotId,
        String symbol,
        Integer quantity,
        BigDecimal purchasePrice,
        Instant purchaseDate,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnL,
        boolean isLongTerm,
        BigDecimal taxImpact,
        String selectionReason
    ) {}
    
    /**
     * Alternative Tax Scenario
     */
    public record AlternativeScenario(
        String scenarioName,
        String costBasisMethod,
        BigDecimal projectedRealizedPnL,
        BigDecimal projectedTaxLiability,
        BigDecimal afterTaxProceeds,
        List<TaxLotSelection> selectedLots,
        String recommendation
    ) {}
    
    /**
     * Tax Optimization Recommendations
     */
    public record TaxOptimizationRecommendations(
        String recommendedStrategy,
        String recommendedCostBasisMethod,
        BigDecimal projectedTaxSavings,
        List<String> actionItems,
        String washSaleWarning,
        Instant recommendedExecutionDate
    ) {}
    
    /**
     * Wash Sale Impact Analysis
     */
    public record WashSaleImpact(
        BigDecimal disallowedLoss,
        List<String> affectedSymbols,
        List<Instant> restrictedPurchaseDates,
        Integer daysUntilClear,
        String washSaleRule,
        BigDecimal adjustedCostBasis
    ) {}
    
    /**
     * Tax Lot Realization Record
     */
    public record TaxLotRealization(
        String lotId,
        String symbol,
        Integer quantityRealized,
        BigDecimal realizedPnL,
        BigDecimal shortTermGain,
        BigDecimal longTermGain,
        Instant realizationDate,
        BigDecimal taxImpact
    ) {}
    
    /**
     * Tax Optimization Suggestions
     */
    public record TaxOptimizationSuggestions(
        String suggestionType,
        String description,
        BigDecimal potentialSavings,
        String actionRequired,
        Instant suggestedTiming,
        String riskLevel
    ) {}
    
    /**
     * Trade Scenario Analysis
     */
    public record TradeScenarioAnalysis(
        String scenarioId,
        String scenarioDescription,
        List<TradeImpact> tradeImpacts,
        BigDecimal totalTaxImpact,
        BigDecimal afterTaxReturn,
        String recommendation,
        Integer confidenceLevel
    ) {}
    
    public record TradeImpact(
        String symbol,
        Integer quantity,
        BigDecimal priceImpact,
        BigDecimal taxImpact,
        BigDecimal netProceedsChange
    ) {}
    
    /**
     * Tax Optimization Strategy
     */
    public record TaxOptimizationStrategy(
        String strategyName,
        String description,
        List<String> optimizationTechniques,
        BigDecimal expectedTaxSavings,
        String riskAssessment,
        List<String> prerequisites
    ) {}
    
    /**
     * Tax Efficient Trade
     */
    public record TaxEfficientTrade(
        String tradeId,
        String symbol,
        String tradeType,
        Integer quantity,
        BigDecimal targetPrice,
        String taxRationale,
        Instant optimalExecutionDate
    ) {}
    
    /**
     * Tax Harvesting Opportunities
     */
    public record TaxHarvestingOpportunities(
        String opportunityType,
        List<String> candidateSymbols,
        BigDecimal potentialLossHarvest,
        BigDecimal estimatedTaxBenefit,
        String washSaleConsiderations,
        Instant expirationDate
    ) {}
    
    /**
     * Trade for Tax Reporting
     */
    public record TradeForTaxReporting(
        String tradeId,
        String symbol,
        String tradeType,
        Integer quantity,
        BigDecimal executionPrice,
        Instant executionDate,
        BigDecimal realizedPnL,
        String taxCategory,
        boolean isWashSale
    ) {}
    
    /**
     * Tax Document Reference
     */
    public record TaxDocumentReference(
        String documentId,
        String documentType,
        Integer taxYear,
        String fileName,
        String generatedBy,
        Instant generatedDate,
        String downloadUrl
    ) {}
    
    /**
     * Compliance Summary
     */
    public record ComplianceSummary(
        String jurisdiction,
        Integer taxYear,
        boolean isCompliant,
        List<String> complianceChecks,
        List<String> violations,
        String overallStatus
    ) {}
    
    // ============================================================================
    // REAL-TIME STREAMING RECORDS
    // ============================================================================
    
    /**
     * Active Subscription
     */
    public record ActiveSubscription(
        String subscriptionId,
        String userId,
        List<String> subscriptionTypes,
        boolean isActive,
        Instant subscribedAt,
        Instant lastUpdateSent,
        Integer messagesSent
    ) {}
    
    /**
     * Streaming Health Metrics
     */
    public record StreamingHealthMetrics(
        String status,
        Integer activeConnections,
        Long totalMessagesSent,
        Long totalMessagesReceived,
        Integer avgLatencyMs,
        Integer errorCount,
        Instant lastHealthCheck
    ) {}
    
    // ============================================================================
    // BATCH PROCESSING RECORDS
    // ============================================================================
    
    /**
     * Recalculation Error
     */
    public record RecalculationError(
        String errorType,
        String errorMessage,
        String affectedEntity,
        String errorContext,
        Instant errorTime,
        String suggestedResolution
    ) {}
    
    /**
     * Recalculation Summary
     */
    public record RecalculationSummary(
        Integer totalEntitiesProcessed,
        Integer successfulCalculations,
        Integer failedCalculations,
        Long totalProcessingTime,
        List<String> processedEntityTypes,
        String overallStatus
    ) {}
    
    /**
     * Validation Discrepancy
     */
    public record ValidationDiscrepancy(
        String discrepancyType,
        String description,
        BigDecimal expectedValue,
        BigDecimal actualValue,
        BigDecimal variance,
        String severity,
        String suggestedAction
    ) {}
    
    /**
     * Broker Validation Result
     */
    public record BrokerValidationResult(
        String brokerType,
        boolean isValid,
        List<ValidationDiscrepancy> discrepancies,
        BigDecimal totalVariance,
        String validationStatus,
        Instant validationTime
    ) {}
    
    /**
     * Validation Summary
     */
    public record ValidationSummary(
        boolean overallValid,
        Integer totalValidations,
        Integer passedValidations,
        Integer failedValidations,
        List<String> criticalIssues,
        String recommendedAction
    ) {}
    
    // ============================================================================
    // HISTORICAL ANALYSIS RECORDS
    // ============================================================================
    
    /**
     * P&L Data Point for Historical Analysis
     */
    public record PnLDataPoint(
        Instant timestamp,
        BigDecimal portfolioValue,
        BigDecimal unrealizedPnL,
        BigDecimal realizedPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent
    ) {}
    
    /**
     * Trend Analysis
     */
    public record TrendAnalysis(
        String trendDirection,
        BigDecimal trendSlope,
        BigDecimal r2Value,
        Integer periodDays,
        String trendStrength,
        List<String> trendFactors
    ) {}
    
    /**
     * Seasonality Analysis
     */
    public record SeasonalityAnalysis(
        String seasonalPattern,
        List<MonthlyPattern> monthlyPatterns,
        List<DailyPattern> dailyPatterns,
        BigDecimal seasonalityStrength,
        String recommendedTiming
    ) {}
    
    public record MonthlyPattern(String month, BigDecimal averageReturn, BigDecimal volatility) {}
    public record DailyPattern(String dayOfWeek, BigDecimal averageReturn, BigDecimal volatility) {}
    
    /**
     * Performance Metrics
     */
    public record PerformanceMetrics(
        BigDecimal totalReturn,
        BigDecimal annualizedReturn,
        BigDecimal volatility,
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal maxDrawdown,
        BigDecimal winRate,
        Integer tradingDays
    ) {}
    
    /**
     * Significant Event
     */
    public record SignificantEvent(
        String eventType,
        String eventDescription,
        Instant eventTimestamp,
        BigDecimal portfolioImpact,
        String impactCategory,
        List<String> affectedSymbols
    ) {}
    
    /**
     * Rolling Metric Point
     */
    public record RollingMetricPoint(
        Instant timestamp,
        BigDecimal metricValue,
        String metricType,
        Integer rollingWindowDays,
        BigDecimal confidenceInterval
    ) {}
    
    /**
     * Rolling Metrics Summary
     */
    public record RollingMetricsSummary(
        String metricType,
        Integer windowDays,
        BigDecimal currentValue,
        BigDecimal averageValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        String trend
    ) {}
    
    // ============================================================================
    // BENCHMARKING RECORDS
    // ============================================================================
    
    /**
     * Benchmark Comparison
     */
    public record BenchmarkComparison(
        String benchmarkSymbol,
        String benchmarkName,
        BigDecimal benchmarkReturn,
        BigDecimal portfolioReturn,
        BigDecimal activeReturn,
        BigDecimal trackingError,
        BigDecimal informationRatio
    ) {}
    
    /**
     * Relative Performance
     */
    public record RelativePerformance(
        BigDecimal outperformance,
        BigDecimal relativeVolatility,
        BigDecimal relativeSharpeRatio,
        Integer outperformingPeriods,
        Integer totalPeriods,
        String performanceCategory
    ) {}
    
    /**
     * Risk Adjusted Comparison
     */
    public record RiskAdjustedComparison(
        BigDecimal riskAdjustedReturn,
        BigDecimal benchmarkRiskAdjustedReturn,
        BigDecimal alpha,
        BigDecimal beta,
        BigDecimal treynorRatio,
        String riskAdjustmentMethod
    ) {}
    
    /**
     * Benchmarking Summary
     */
    public record BenchmarkingSummary(
        Integer benchmarksCompared,
        String bestPerformingBenchmark,
        String closestMatchBenchmark,
        BigDecimal averageOutperformance,
        String overallRanking,
        List<String> insights
    ) {}
    
    // ============================================================================
    // ALERT AND NOTIFICATION RECORDS
    // ============================================================================
    
    /**
     * Alert Threshold
     */
    public record AlertThreshold(
        String thresholdType,
        BigDecimal thresholdValue,
        String comparisonOperator,
        String alertSeverity,
        boolean isEnabled,
        String notificationMethod
    ) {}
    
    /**
     * Notification Preferences
     */
    public record NotificationPreferences(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean pushEnabled,
        boolean webSocketEnabled,
        List<String> notificationTypes,
        String timeZone,
        List<String> quietHours
    ) {}
    
    // ============================================================================
    // SCENARIO ANALYSIS RECORDS
    // ============================================================================
    
    /**
     * Market Scenario Impact
     */
    public record MarketScenarioImpact(
        String scenarioName,
        String scenarioDescription,
        BigDecimal portfolioImpact,
        BigDecimal probabilityEstimate,
        List<SymbolImpact> symbolImpacts,
        String riskLevel
    ) {}
    
    public record SymbolImpact(String symbol, BigDecimal priceChange, BigDecimal portfolioImpact) {}
    
    /**
     * Worst Case Scenario
     */
    public record WorstCaseScenario(
        BigDecimal worstCaseReturn,
        BigDecimal worstCasePortfolioValue,
        String scenarioDescription,
        BigDecimal probabilityEstimate,
        List<String> keyRiskFactors
    ) {}
    
    /**
     * Best Case Scenario
     */
    public record BestCaseScenario(
        BigDecimal bestCaseReturn,
        BigDecimal bestCasePortfolioValue,
        String scenarioDescription,
        BigDecimal probabilityEstimate,
        List<String> keySuccessFactors
    ) {}
    
    /**
     * Most Likely Scenario
     */
    public record MostLikelyScenario(
        BigDecimal expectedReturn,
        BigDecimal expectedPortfolioValue,
        String scenarioDescription,
        BigDecimal confidenceLevel,
        String baselineAssumptions
    ) {}
    
    /**
     * Risk Metrics Comparison
     */
    public record RiskMetricsComparison(
        String comparisonType,
        String baselineMetrics,
        String scenarioMetrics,
        BigDecimal riskIncrease,
        List<String> changedMetrics,
        String riskAssessment
    ) {}
    
    // Alert enums
    public enum AlertType {
        PORTFOLIO_VALUE, POSITION_PNL, DAY_PNL, DRAWDOWN, VOLATILITY, 
        CONCENTRATION, MARGIN_UTILIZATION, TAX_THRESHOLD
    }
    
    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}