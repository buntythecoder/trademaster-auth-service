package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.domain.PnLDomainTypes.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Broker P&L Calculation Engine Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance profit and loss calculation engine providing real-time
 * P&L computation across multiple brokers with comprehensive analytics,
 * tax optimization, and performance attribution.
 * 
 * Core Features:
 * - Real-time multi-broker P&L aggregation (<50ms response times)
 * - Multiple cost basis methods (FIFO, LIFO, Weighted Average, Specific ID)
 * - Advanced performance attribution analysis
 * - Tax-optimized realized P&L calculations with audit trails
 * - Risk-adjusted returns and volatility analysis
 * - WebSocket streaming for real-time P&L updates
 * - Comprehensive regulatory compliance reporting
 * 
 * Performance Targets:
 * - Portfolio P&L calculation: <50ms (cached) / <200ms (live)
 * - Position P&L calculation: <5ms per position
 * - Tax lot processing: <25ms per transaction
 * - Bulk calculations: <100ms for 1000+ positions
 * - Concurrent operations: 10,000+ with Virtual Threads
 * 
 * Integration Points:
 * - Multi-Broker Service: Real-time position and broker data
 * - Market Data Service: Live pricing feeds and market status
 * - Portfolio Service: Portfolio composition and holdings
 * - Notification Service: P&L alerts and threshold notifications
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PnLCalculationEngine {
    
    // ============================================================================
    // CORE P&L CALCULATION METHODS
    // ============================================================================
    
    /**
     * Calculate comprehensive multi-broker portfolio P&L
     * 
     * MANDATORY: <50ms response time with Redis caching
     * 
     * @param userId User identifier
     * @param portfolioId Portfolio identifier (optional for all portfolios)
     * @return Multi-broker portfolio P&L result
     */
    CompletableFuture<MultiBrokerPnLResult> calculateMultiBrokerPnL(String userId, Long portfolioId);
    
    /**
     * Calculate real-time P&L for specific broker
     * 
     * @param userId User identifier
     * @param brokerType Broker type (ZERODHA, UPSTOX, etc.)
     * @return Broker-specific P&L result
     */
    CompletableFuture<BrokerPnLResult> calculateBrokerPnL(String userId, BrokerType brokerType);
    
    /**
     * Calculate position-level P&L across all brokers
     * 
     * @param userId User identifier
     * @param symbol Symbol to calculate (optional for all positions)
     * @return Position P&L aggregated across brokers
     */
    CompletableFuture<List<PositionPnLResult>> calculatePositionPnL(String userId, String symbol);
    
    /**
     * Calculate unrealized P&L using current market prices
     * 
     * @param userId User identifier
     * @param brokerType Broker type (optional for all brokers)
     * @return Unrealized P&L calculation result
     */
    CompletableFuture<BigDecimal> calculateUnrealizedPnL(String userId, BrokerType brokerType);
    
    /**
     * Calculate realized P&L from completed trades
     * 
     * @param userId User identifier
     * @param fromDate Start date for calculation
     * @param toDate End date for calculation
     * @param brokerType Broker type (optional for all brokers)
     * @return Realized P&L calculation result
     */
    CompletableFuture<RealizedPnLResult> calculateRealizedPnL(String userId, Instant fromDate, 
                                                             Instant toDate, BrokerType brokerType);
    
    /**
     * Calculate day P&L using previous close prices
     * 
     * @param userId User identifier
     * @param tradingDate Trading date (optional for today)
     * @return Day P&L result across all brokers
     */
    CompletableFuture<DayPnLResult> calculateDayPnL(String userId, Instant tradingDate);
    
    // ============================================================================
    // ADVANCED ANALYTICS AND ATTRIBUTION
    // ============================================================================
    
    /**
     * Calculate comprehensive performance attribution
     * 
     * @param userId User identifier
     * @param fromDate Analysis start date
     * @param toDate Analysis end date
     * @param benchmarkSymbol Benchmark for comparison (e.g., NIFTY50)
     * @return Performance attribution analysis
     */
    CompletableFuture<PerformanceAttributionResult> calculatePerformanceAttribution(
            String userId, Instant fromDate, Instant toDate, String benchmarkSymbol);
    
    /**
     * Calculate risk-adjusted returns and metrics
     * 
     * @param userId User identifier
     * @param periodDays Analysis period in days
     * @return Risk metrics (Sharpe ratio, max drawdown, volatility, etc.)
     */
    CompletableFuture<RiskMetricsResult> calculateRiskMetrics(String userId, Integer periodDays);
    
    /**
     * Calculate portfolio correlation and beta analysis
     * 
     * @param userId User identifier
     * @param benchmarkSymbol Benchmark symbol for beta calculation
     * @param periodDays Analysis period in days
     * @return Correlation and beta analysis result
     */
    CompletableFuture<CorrelationAnalysisResult> calculateCorrelationAnalysis(
            String userId, String benchmarkSymbol, Integer periodDays);
    
    /**
     * Generate comprehensive P&L attribution report
     * 
     * @param userId User identifier
     * @param fromDate Report start date
     * @param toDate Report end date
     * @return Comprehensive P&L report with all analytics
     */
    CompletableFuture<ComprehensivePnLReport> generatePnLReport(String userId, 
                                                              Instant fromDate, Instant toDate);
    
    // ============================================================================
    // TAX OPTIMIZATION AND COMPLIANCE
    // ============================================================================
    
    /**
     * Calculate tax-optimized realized P&L using specified method
     * 
     * @param userId User identifier
     * @param symbol Symbol for calculation
     * @param sellQuantity Quantity to sell
     * @param currentPrice Current market price
     * @param costBasisMethod Cost basis method (FIFO, LIFO, etc.)
     * @return Tax-optimized P&L calculation with tax lots
     */
    CompletableFuture<TaxOptimizedPnLResult> calculateTaxOptimizedPnL(
            String userId, String symbol, Integer sellQuantity, 
            BigDecimal currentPrice, CostBasisMethod costBasisMethod);
    
    /**
     * Get tax lots for specific symbol across all brokers
     * 
     * @param userId User identifier
     * @param symbol Symbol to get tax lots for
     * @param costBasisMethod Cost basis method
     * @return Consolidated tax lots across brokers
     */
    CompletableFuture<List<TaxLotInfo>> getConsolidatedTaxLots(String userId, String symbol, 
                                                              CostBasisMethod costBasisMethod);
    
    /**
     * Calculate tax impact analysis for potential trades
     * 
     * @param userId User identifier
     * @param tradeScenarios List of potential trade scenarios
     * @return Tax impact analysis for each scenario
     */
    CompletableFuture<TaxImpactAnalysisResult> calculateTaxImpactAnalysis(
            String userId, List<TradeScenario> tradeScenarios);
    
    /**
     * Generate tax compliance report
     * 
     * @param userId User identifier
     * @param taxYear Tax year
     * @param jurisdiction Tax jurisdiction (INDIA, US, EU)
     * @return Tax compliance report
     */
    CompletableFuture<TaxComplianceReport> generateTaxReport(String userId, Integer taxYear, 
                                                            TaxJurisdiction jurisdiction);
    
    // ============================================================================
    // REAL-TIME STREAMING AND UPDATES
    // ============================================================================
    
    /**
     * Subscribe to real-time P&L updates via WebSocket
     * 
     * @param userId User identifier
     * @param subscriptionTypes Types of P&L updates to receive
     * @return Subscription confirmation
     */
    CompletableFuture<PnLSubscriptionResult> subscribeToRealtimePnL(String userId, 
                                                                   List<PnLUpdateType> subscriptionTypes);
    
    /**
     * Unsubscribe from real-time P&L updates
     * 
     * @param userId User identifier
     * @param subscriptionId Subscription identifier
     * @return Unsubscription confirmation
     */
    CompletableFuture<Void> unsubscribeFromRealtimePnL(String userId, String subscriptionId);
    
    /**
     * Get real-time P&L streaming status
     * 
     * @param userId User identifier
     * @return Current streaming status and active subscriptions
     */
    CompletableFuture<PnLStreamingStatus> getPnLStreamingStatus(String userId);
    
    // ============================================================================
    // BULK OPERATIONS AND BATCH PROCESSING
    // ============================================================================
    
    /**
     * Calculate P&L for multiple users in batch
     * 
     * MANDATORY: <100ms per 1000 calculations using Virtual Threads
     * 
     * @param userIds List of user identifiers
     * @return Batch P&L calculation results
     */
    CompletableFuture<List<MultiBrokerPnLResult>> calculateBatchPnL(List<String> userIds);
    
    /**
     * Recalculate all P&L data for user (maintenance operation)
     * 
     * @param userId User identifier
     * @param forceRefresh Force refresh of cached data
     * @return Recalculation result summary
     */
    CompletableFuture<PnLRecalculationResult> recalculateAllPnL(String userId, boolean forceRefresh);
    
    /**
     * Validate P&L calculation accuracy across brokers
     * 
     * @param userId User identifier
     * @return P&L validation results with discrepancy analysis
     */
    CompletableFuture<PnLValidationResult> validatePnLAccuracy(String userId);
    
    // ============================================================================
    // HISTORICAL ANALYSIS AND TRENDING
    // ============================================================================
    
    /**
     * Get historical P&L trends and analysis
     * 
     * @param userId User identifier
     * @param periodDays Historical period in days
     * @param granularity Data granularity (DAILY, WEEKLY, MONTHLY)
     * @return Historical P&L trend analysis
     */
    CompletableFuture<HistoricalPnLTrend> getHistoricalPnLTrend(String userId, Integer periodDays, 
                                                               DataGranularity granularity);
    
    /**
     * Calculate rolling performance metrics
     * 
     * @param userId User identifier
     * @param windowDays Rolling window size in days
     * @param periodDays Total analysis period in days
     * @return Rolling performance metrics
     */
    CompletableFuture<RollingPerformanceMetrics> calculateRollingPerformance(String userId, 
                                                                            Integer windowDays, Integer periodDays);
    
    /**
     * Get P&L benchmarking against market indices
     * 
     * @param userId User identifier
     * @param benchmarkSymbols List of benchmark symbols (NIFTY50, SENSEX, etc.)
     * @param fromDate Benchmarking start date
     * @param toDate Benchmarking end date
     * @return P&L benchmarking results
     */
    CompletableFuture<PnLBenchmarkingResult> benchmarkPnLPerformance(String userId, 
                                                                    List<String> benchmarkSymbols,
                                                                    Instant fromDate, Instant toDate);
    
    // ============================================================================
    // ALERTS AND NOTIFICATIONS
    // ============================================================================
    
    /**
     * Set P&L alert thresholds
     * 
     * @param userId User identifier
     * @param alertConfig P&L alert configuration
     * @return Alert configuration result
     */
    CompletableFuture<PnLAlertConfigResult> configurePnLAlerts(String userId, PnLAlertConfig alertConfig);
    
    /**
     * Get active P&L alerts for user
     * 
     * @param userId User identifier
     * @return List of active P&L alerts
     */
    CompletableFuture<List<PnLAlert>> getActivePnLAlerts(String userId);
    
    /**
     * Calculate P&L impact of market scenarios
     * 
     * @param userId User identifier
     * @param scenarios List of market scenarios to analyze
     * @return P&L impact analysis for each scenario
     */
    CompletableFuture<PnLScenarioAnalysis> analyzePnLScenarios(String userId, 
                                                              List<MarketScenario> scenarios);
}

// ============================================================================
// ENUMS AND SUPPORTING TYPES
// ============================================================================
// Enums are now defined in separate files in this package