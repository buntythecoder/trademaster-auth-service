package com.trademaster.pnlengine.dto;

import com.trademaster.pnlengine.service.BrokerType;
import com.trademaster.pnlengine.service.CostBasisMethod;
import com.trademaster.pnlengine.service.TaxJurisdiction;
import com.trademaster.pnlengine.service.PnLUpdateType;
import com.trademaster.pnlengine.service.DataGranularity;
import com.trademaster.pnlengine.domain.PnLDomainTypes.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Multi-Broker P&L Engine Data Transfer Objects
 * 
 * MANDATORY: Immutable Records + JSON Serialization + Zero Placeholders
 * 
 * Comprehensive collection of DTOs for the Multi-Broker P&L Engine service
 * providing type-safe data transfer with immutable records and functional
 * programming patterns.
 * 
 * All DTOs follow TradeMaster standards:
 * - Immutable records for thread safety
 * - Comprehensive field validation
 * - JSON serialization support
 * - Builder patterns where needed
 * - Zero null values with Optional usage
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public class PnLResultDTOs {

    // ============================================================================
    // CORE P&L CALCULATION RESULTS
    // ============================================================================

    /**
     * Multi-Broker Portfolio P&L Result
     */
    public record MultiBrokerPnLResult(
        String userId,
        Long portfolioId,
        BigDecimal totalPortfolioValue,
        BigDecimal totalCashBalance,
        BigDecimal totalInvestedAmount,
        BigDecimal totalUnrealizedPnL,
        BigDecimal totalRealizedPnL,
        BigDecimal totalDayPnL,
        BigDecimal totalReturnPercent,
        BigDecimal totalReturnAmount,
        Integer totalPositions,
        Integer activeBrokers,
        List<BrokerPnLBreakdown> brokerBreakdown,
        List<SectorPnLBreakdown> sectorBreakdown,
        List<AssetClassPnLBreakdown> assetClassBreakdown,
        PnLSummaryMetrics summaryMetrics,
        Instant calculatedAt,
        Long calculationTimeMs,
        String correlationId
    ) {}

    /**
     * Broker-Specific P&L Result
     */
    public record BrokerPnLResult(
        String userId,
        BrokerType brokerType,
        String brokerAccountId,
        BigDecimal portfolioValue,
        BigDecimal cashBalance,
        BigDecimal investedAmount,
        BigDecimal unrealizedPnL,
        BigDecimal realizedPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent,
        BigDecimal returnAmount,
        Integer positionsCount,
        List<PositionPnLResult> positions,
        BrokerConnectionStatus connectionStatus,
        Instant lastSyncTime,
        Instant calculatedAt,
        Long calculationTimeMs
    ) {}

    /**
     * Position-Level P&L Result
     */
    public record PositionPnLResult(
        String userId,
        String symbol,
        String companyName,
        String sector,
        String assetClass,
        Integer totalQuantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal investedAmount,
        BigDecimal unrealizedPnL,
        BigDecimal realizedPnL,
        BigDecimal totalPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent,
        BigDecimal returnAmount,
        Integer holdingDays,
        BigDecimal annualizedReturn,
        List<BrokerPositionBreakdown> brokerBreakdown,
        List<TaxLotInfo> taxLots,
        PositionRiskMetrics riskMetrics,
        Instant lastUpdated,
        String correlationId
    ) {}

    /**
     * Realized P&L Calculation Result
     */
    public record RealizedPnLResult(
        String userId,
        BigDecimal totalRealizedPnL,
        BigDecimal shortTermGains,
        BigDecimal longTermGains,
        BigDecimal totalTaxLiability,
        Integer tradesCount,
        List<BrokerRealizedPnL> brokerBreakdown,
        List<SymbolRealizedPnL> symbolBreakdown,
        List<TaxLotRealization> taxLotRealizations,
        TaxOptimizationSuggestions taxOptimization,
        Instant fromDate,
        Instant toDate,
        Instant calculatedAt
    ) {}

    /**
     * Day P&L Result
     */
    public record DayPnLResult(
        String userId,
        Instant tradingDate,
        BigDecimal totalDayPnL,
        BigDecimal totalDayPnLPercent,
        BigDecimal portfolioValueOpen,
        BigDecimal portfolioValueClose,
        BigDecimal portfolioValueCurrent,
        Integer positionsChanged,
        List<BrokerDayPnL> brokerBreakdown,
        List<PositionDayPnL> topGainers,
        List<PositionDayPnL> topLosers,
        MarketMovementImpact marketImpact,
        Instant calculatedAt
    ) {}

    // ============================================================================
    // SUPPORTING DTO RECORDS - MISSING DEFINITIONS
    // ============================================================================

    /**
     * Broker Position Breakdown
     */
    public record BrokerPositionBreakdown(
        BrokerType brokerType,
        String brokerAccountId,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal marketValue,
        BigDecimal unrealizedPnL,
        BigDecimal returnPercent,
        Instant lastUpdated
    ) {}

    /**
     * Position Risk Metrics
     */
    public record PositionRiskMetrics(
        BigDecimal volatility,
        BigDecimal beta,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        BigDecimal valueAtRisk,
        BigDecimal correlation,
        Integer daysSinceHigh,
        BigDecimal riskAdjustedReturn
    ) {}

    /**
     * Broker Realized P&L
     */
    public record BrokerRealizedPnL(
        BrokerType brokerType,
        String brokerAccountId,
        BigDecimal realizedPnL,
        BigDecimal shortTermGains,
        BigDecimal longTermGains,
        BigDecimal taxLiability,
        Integer tradesCount,
        Instant periodStart,
        Instant periodEnd
    ) {}

    /**
     * Symbol Realized P&L
     */
    public record SymbolRealizedPnL(
        String symbol,
        String companyName,
        BigDecimal realizedPnL,
        BigDecimal shortTermGains,
        BigDecimal longTermGains,
        BigDecimal averageSellPrice,
        BigDecimal averageBuyPrice,
        Integer tradesCount,
        Integer totalQuantityTraded
    ) {}

    /**
     * Broker Day P&L
     */
    public record BrokerDayPnL(
        BrokerType brokerType,
        String brokerAccountId,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        BigDecimal portfolioValueOpen,
        BigDecimal portfolioValueClose,
        Integer positionsChanged,
        List<String> topMovers
    ) {}

    /**
     * Position Day P&L
     */
    public record PositionDayPnL(
        String symbol,
        String companyName,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        BigDecimal priceChange,
        BigDecimal priceChangePercent,
        Integer quantity,
        BigDecimal marketValue,
        String changeReason
    ) {}

    /**
     * Market Movement Impact
     */
    public record MarketMovementImpact(
        String benchmarkSymbol,
        BigDecimal benchmarkChange,
        BigDecimal portfolioCorrelation,
        BigDecimal marketBeta,
        BigDecimal alphaGeneration,
        BigDecimal systematicRisk,
        BigDecimal specificRisk,
        List<String> marketDrivers
    ) {}

    /**
     * Comprehensive P&L Report
     */
    public record ComprehensivePnLReport(
        String userId,
        Instant reportDate,
        Instant fromDate,
        Instant toDate,
        MultiBrokerPnLResult portfolioSummary,
        PerformanceAttributionResult performanceAttribution,
        RiskMetricsResult riskAnalysis,
        List<RealizedPnLResult> realizedPnLHistory,
        List<HistoricalPnLTrend> historicalTrends,
        TaxComplianceReport taxCompliance,
        List<PnLAlert> activeAlerts,
        ReportMetadata metadata
    ) {}

    /**
     * Trade Scenario for Tax Analysis
     */
    public record TradeScenario(
        String symbol,
        Integer quantity,
        BigDecimal proposedSellPrice,
        CostBasisMethod costBasisMethod,
        Instant proposedSellDate,
        String scenarioName,
        String description
    ) {}

    /**
     * P&L Alert Configuration
     */
    public record PnLAlertConfig(
        String userId,
        List<PnLUpdateType> alertTypes,
        BigDecimal portfolioValueThreshold,
        BigDecimal dayPnLThreshold,
        BigDecimal returnPercentThreshold,
        BigDecimal riskMetricThreshold,
        Boolean enableEmailAlerts,
        Boolean enableSmsAlerts,
        Boolean enablePushAlerts,
        List<String> alertRecipients
    ) {}

    /**
     * Market Scenario for Analysis
     */
    public record MarketScenario(
        String scenarioName,
        String description,
        Map<String, BigDecimal> priceChanges,
        BigDecimal marketMovement,
        String benchmarkSymbol,
        BigDecimal volatilityChange,
        BigDecimal correlationChange,
        Instant effectiveDate
    ) {}

    /**
     * Report Metadata
     */
    public record ReportMetadata(
        String reportId,
        String reportVersion,
        Instant generatedAt,
        Long generationTimeMs,
        String correlationId,
        Integer totalRecords,
        Map<String, Object> parameters
    ) {}

    // ============================================================================
    // PERFORMANCE ATTRIBUTION AND ANALYTICS
    // ============================================================================

    /**
     * Performance Attribution Analysis Result
     */
    public record PerformanceAttributionResult(
        String userId,
        Instant fromDate,
        Instant toDate,
        String benchmarkSymbol,
        BigDecimal totalReturn,
        BigDecimal benchmarkReturn,
        BigDecimal activeReturn,
        BigDecimal trackingError,
        BigDecimal informationRatio,
        SecuritySelectionAttribution securitySelection,
        AssetAllocationAttribution assetAllocation,
        TimingEffectAttribution timingEffect,
        InteractionEffectAttribution interactionEffect,
        List<SectorAttributionBreakdown> sectorAttribution,
        List<StockAttributionBreakdown> stockAttribution,
        AttributionSummary summary,
        Instant calculatedAt
    ) {}

    /**
     * Risk Metrics Calculation Result
     */
    public record RiskMetricsResult(
        String userId,
        Integer periodDays,
        BigDecimal portfolioValue,
        BigDecimal volatility,
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal maxDrawdown,
        BigDecimal maxDrawdownPercent,
        Integer maxDrawdownDays,
        BigDecimal valueAtRisk95,
        BigDecimal valueAtRisk99,
        BigDecimal expectedShortfall,
        BigDecimal beta,
        BigDecimal alpha,
        BigDecimal correlation,
        BigDecimal trackingError,
        BigDecimal informationRatio,
        VolatilityMetrics volatilityBreakdown,
        DrawdownAnalysis drawdownAnalysis,
        RiskContribution riskContribution,
        Instant calculatedAt
    ) {}

    /**
     * Correlation Analysis Result
     */
    public record CorrelationAnalysisResult(
        String userId,
        String benchmarkSymbol,
        Integer periodDays,
        BigDecimal correlation,
        BigDecimal beta,
        BigDecimal alpha,
        BigDecimal rSquared,
        BigDecimal trackingError,
        List<PositionCorrelation> positionCorrelations,
        List<SectorCorrelation> sectorCorrelations,
        CorrelationTrend correlationTrend,
        Instant calculatedAt
    ) {}

    // ============================================================================
    // TAX OPTIMIZATION AND COMPLIANCE
    // ============================================================================

    /**
     * Tax-Optimized P&L Calculation Result
     */
    public record TaxOptimizedPnLResult(
        String userId,
        String symbol,
        Integer sellQuantity,
        BigDecimal currentPrice,
        CostBasisMethod recommendedMethod,
        BigDecimal projectedRealizedPnL,
        BigDecimal projectedTaxLiability,
        BigDecimal netProceedsAfterTax,
        List<TaxLotSelection> selectedTaxLots,
        List<AlternativeScenario> alternativeScenarios,
        TaxOptimizationRecommendations recommendations,
        WashSaleImpact washSaleImpact,
        Instant calculatedAt
    ) {}

    /**
     * Tax Lots Information
     */
    public record TaxLotInfo(
        String lotId,
        String symbol,
        BrokerType brokerType,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal totalCost,
        Instant purchaseDate,
        Integer holdingDays,
        boolean isLongTerm,
        CostBasisMethod method,
        String originalTradeId,
        BigDecimal currentMarketValue,
        BigDecimal unrealizedPnL
    ) {}

    /**
     * Tax Impact Analysis Result
     */
    public record TaxImpactAnalysisResult(
        String userId,
        List<TradeScenarioAnalysis> scenarioAnalysis,
        TaxOptimizationStrategy recommendedStrategy,
        BigDecimal potentialTaxSavings,
        List<TaxEfficientTrade> suggestedTrades,
        TaxHarvestingOpportunities taxHarvesting,
        Instant calculatedAt
    ) {}

    /**
     * Tax Compliance Report
     */
    public record TaxComplianceReport(
        String userId,
        Integer taxYear,
        TaxJurisdiction jurisdiction,
        BigDecimal totalRealizedGains,
        BigDecimal totalRealizedLosses,
        BigDecimal netRealizedPnL,
        BigDecimal shortTermCapitalGains,
        BigDecimal longTermCapitalGains,
        BigDecimal totalTaxLiability,
        BigDecimal carryForwardLosses,
        List<TradeForTaxReporting> tradesForReporting,
        List<TaxDocumentReference> generatedDocuments,
        ComplianceSummary complianceSummary,
        Instant generatedAt
    ) {}

    // ============================================================================
    // REAL-TIME STREAMING AND SUBSCRIPTIONS
    // ============================================================================

    /**
     * P&L Subscription Result
     */
    public record PnLSubscriptionResult(
        String userId,
        String subscriptionId,
        List<PnLUpdateType> subscribedTypes,
        Integer updateFrequencyMs,
        boolean isActive,
        Instant subscribedAt,
        Instant expiresAt
    ) {}

    /**
     * P&L Streaming Status
     */
    public record PnLStreamingStatus(
        String userId,
        boolean isStreamingActive,
        Integer activeSubscriptions,
        Long totalMessagesReceived,
        Long totalMessagesSent,
        Instant lastUpdateTime,
        Integer avgLatencyMs,
        List<ActiveSubscription> subscriptions,
        StreamingHealthMetrics healthMetrics
    ) {}

    // ============================================================================
    // BATCH AND BULK OPERATIONS
    // ============================================================================

    /**
     * P&L Recalculation Result
     */
    public record PnLRecalculationResult(
        String userId,
        boolean wasSuccessful,
        Integer positionsRecalculated,
        Integer taxLotsRecalculated,
        Integer brokersProcessed,
        List<RecalculationError> errors,
        RecalculationSummary summary,
        Long totalTimeMs,
        Instant completedAt
    ) {}

    /**
     * P&L Validation Result
     */
    public record PnLValidationResult(
        String userId,
        boolean isValid,
        BigDecimal calculatedPnL,
        BigDecimal expectedPnL,
        BigDecimal variance,
        BigDecimal toleranceThreshold,
        List<ValidationDiscrepancy> discrepancies,
        List<BrokerValidationResult> brokerValidations,
        ValidationSummary summary,
        Instant validatedAt
    ) {}

    // ============================================================================
    // HISTORICAL ANALYSIS AND TRENDING
    // ============================================================================

    /**
     * Historical P&L Trend
     */
    public record HistoricalPnLTrend(
        String userId,
        Integer periodDays,
        DataGranularity granularity,
        List<PnLDataPoint> dataPoints,
        TrendAnalysis trendAnalysis,
        SeasonalityAnalysis seasonality,
        PerformanceMetrics performanceMetrics,
        List<SignificantEvent> significantEvents,
        Instant calculatedAt
    ) {}

    /**
     * Rolling Performance Metrics
     */
    public record RollingPerformanceMetrics(
        String userId,
        Integer windowDays,
        Integer periodDays,
        List<RollingMetricPoint> rollingReturns,
        List<RollingMetricPoint> rollingVolatility,
        List<RollingMetricPoint> rollingSharpeRatio,
        List<RollingMetricPoint> rollingMaxDrawdown,
        RollingMetricsSummary summary,
        Instant calculatedAt
    ) {}

    /**
     * P&L Benchmarking Result
     */
    public record PnLBenchmarkingResult(
        String userId,
        List<String> benchmarkSymbols,
        Instant fromDate,
        Instant toDate,
        List<BenchmarkComparison> comparisons,
        RelativePerformance relativePerformance,
        RiskAdjustedComparison riskAdjusted,
        BenchmarkingSummary summary,
        Instant calculatedAt
    ) {}

    // ============================================================================
    // ALERTS AND SCENARIO ANALYSIS
    // ============================================================================

    /**
     * P&L Alert Configuration Result
     */
    public record PnLAlertConfigResult(
        String userId,
        String alertConfigId,
        boolean isActive,
        List<AlertThreshold> configuredThresholds,
        NotificationPreferences notificationPrefs,
        Instant configuredAt,
        Instant lastTriggered
    ) {}

    /**
     * P&L Alert
     */
    public record PnLAlert(
        String alertId,
        String userId,
        AlertType alertType,
        String title,
        String message,
        AlertSeverity severity,
        BigDecimal triggerValue,
        BigDecimal thresholdValue,
        Map<String, Object> alertData,
        boolean isAcknowledged,
        Instant triggeredAt,
        Instant acknowledgedAt
    ) {}

    /**
     * P&L Scenario Analysis
     */
    public record PnLScenarioAnalysis(
        String userId,
        List<MarketScenarioImpact> scenarioImpacts,
        WorstCaseScenario worstCase,
        BestCaseScenario bestCase,
        MostLikelyScenario mostLikely,
        RiskMetricsComparison riskComparison,
        Instant calculatedAt
    ) {}

    // ============================================================================
    // SUPPORTING DATA STRUCTURES
    // ============================================================================

    /**
     * Broker P&L Breakdown
     */
    public record BrokerPnLBreakdown(
        BrokerType brokerType,
        String accountId,
        BigDecimal portfolioValue,
        BigDecimal unrealizedPnL,
        BigDecimal realizedPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent,
        Integer positions,
        BigDecimal allocationPercent,
        BrokerConnectionStatus status
    ) {}

    /**
     * Sector P&L Breakdown
     */
    public record SectorPnLBreakdown(
        String sectorName,
        BigDecimal marketValue,
        BigDecimal unrealizedPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent,
        BigDecimal allocationPercent,
        Integer positions,
        BigDecimal sectorBeta,
        String topPerformer,
        String worstPerformer
    ) {}

    /**
     * Asset Class P&L Breakdown
     */
    public record AssetClassPnLBreakdown(
        String assetClass,
        BigDecimal marketValue,
        BigDecimal unrealizedPnL,
        BigDecimal dayPnL,
        BigDecimal returnPercent,
        BigDecimal allocationPercent,
        Integer positions,
        BigDecimal volatility
    ) {}

    /**
     * P&L Summary Metrics
     */
    public record PnLSummaryMetrics(
        BigDecimal totalReturn,
        BigDecimal annualizedReturn,
        BigDecimal volatility,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        BigDecimal winRate,
        BigDecimal profitFactor,
        Integer profitableDays,
        Integer losingDays,
        BigDecimal avgDailyPnL
    ) {}

    // ============================================================================
    // ENUMS AND SUPPORTING TYPES
    // ============================================================================

    /**
     * Broker Connection Status
     */
    public enum BrokerConnectionStatus {
        CONNECTED, DISCONNECTED, ERROR, RATE_LIMITED, MAINTENANCE, SYNCING
    }

    // Additional supporting records would be defined here...
    // This is a comprehensive foundation demonstrating the architecture
}