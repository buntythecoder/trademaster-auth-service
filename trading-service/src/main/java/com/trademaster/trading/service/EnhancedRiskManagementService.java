package com.trademaster.trading.service;

import com.trademaster.trading.dto.RiskAssessment;
import com.trademaster.trading.dto.RiskLimits;
import com.trademaster.trading.dto.RiskMetrics;
import com.trademaster.trading.dto.StressTestResult;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.entity.Position;
import com.trademaster.trading.entity.RiskLimit;
import com.trademaster.trading.model.RiskViolation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Risk Management Service
 * 
 * Comprehensive risk management with real-time monitoring and advanced analytics:
 * - Multi-layered risk controls (pre-trade, intraday, overnight)
 * - Dynamic risk limits with market condition adaptation
 * - Real-time portfolio VaR and stress testing
 * - Regulatory compliance monitoring (SEBI, RBI guidelines)
 * - Machine learning-based risk prediction
 * - Cross-asset correlation analysis
 * - Liquidity risk assessment
 * - Credit and counterparty risk monitoring
 * 
 * Built with Java 24 Virtual Threads and Structured Concurrency for:
 * - Parallel risk validation across multiple models
 * - Real-time portfolio exposure calculations
 * - Concurrent stress testing scenarios
 * - High-frequency risk monitoring (microsecond latency)
 * 
 * Performance Targets:
 * - Pre-trade risk checks: <5ms (cached with Redis)
 * - Real-time VaR calculation: <15ms (parallel computation)
 * - Stress testing: <50ms (concurrent scenarios)
 * - Portfolio exposure updates: <8ms (in-memory aggregation)
 * - Risk limit monitoring: <3ms (event-driven)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + ML Risk Models)
 */
public interface EnhancedRiskManagementService extends RiskManagementService {
    
    // ========== Advanced Pre-Trade Risk Validation ==========
    
    /**
     * Comprehensive pre-trade risk assessment
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return CompletableFuture<RiskAssessment> comprehensive risk analysis
     */
    CompletableFuture<RiskAssessment> assessPreTradeRisk(Long userId, Order order);
    
    /**
     * Validate order against all risk limits
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return CompletableFuture<List<RiskViolation>> list of risk violations (empty if none)
     */
    CompletableFuture<List<RiskViolation>> validateAllRiskLimits(Long userId, Order order);
    
    /**
     * Calculate margin requirements for order
     * 
     * @param userId The user ID
     * @param order The order to analyze
     * @return CompletableFuture<BigDecimal> margin requirement amount
     */
    CompletableFuture<BigDecimal> calculateMarginRequirement(Long userId, Order order);
    
    /**
     * Advanced order impact analysis
     * 
     * @param userId The user ID
     * @param order The order to analyze
     * @return CompletableFuture<Map<String, BigDecimal>> impact analysis metrics
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeOrderImpact(Long userId, Order order);
    
    // ========== Portfolio Risk Monitoring ==========
    
    /**
     * Calculate real-time portfolio Value at Risk (VaR)
     * 
     * @param userId The user ID
     * @param confidenceLevel Confidence level (0.95, 0.99)
     * @param timeHorizon Time horizon in days
     * @return CompletableFuture<BigDecimal> VaR amount
     */
    CompletableFuture<BigDecimal> calculateVaR(Long userId, BigDecimal confidenceLevel, Integer timeHorizon);
    
    /**
     * Calculate Expected Shortfall (Conditional VaR)
     * 
     * @param userId The user ID
     * @param confidenceLevel Confidence level (0.95, 0.99)
     * @param timeHorizon Time horizon in days
     * @return CompletableFuture<BigDecimal> Expected Shortfall amount
     */
    CompletableFuture<BigDecimal> calculateExpectedShortfall(Long userId, BigDecimal confidenceLevel, Integer timeHorizon);
    
    /**
     * Perform comprehensive stress testing
     * 
     * @param userId The user ID
     * @param scenarios List of stress test scenarios
     * @return CompletableFuture<List<StressTestResult>> stress test results
     */
    CompletableFuture<List<StressTestResult>> performStressTesting(Long userId, List<String> scenarios);
    
    /**
     * Calculate portfolio Greeks (Delta, Gamma, Theta, Vega)
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> Greeks by risk factor
     */
    CompletableFuture<Map<String, BigDecimal>> calculatePortfolioGreeks(Long userId);
    
    /**
     * Analyze portfolio concentration risk
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> concentration metrics
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeConcentrationRisk(Long userId);
    
    /**
     * Calculate real-time portfolio exposure by sector/industry
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Map<String, BigDecimal>>> exposure breakdown
     */
    CompletableFuture<Map<String, Map<String, BigDecimal>>> calculatePortfolioExposure(Long userId);
    
    // ========== Risk Limits Management ==========
    
    /**
     * Set risk limits for user
     * 
     * @param userId The user ID
     * @param riskLimits Risk limits configuration
     * @return CompletableFuture<Void> completion indicator
     */
    CompletableFuture<Void> setRiskLimits(Long userId, RiskLimits riskLimits);
    
    /**
     * Get current risk limits for user
     * 
     * @param userId The user ID
     * @return CompletableFuture<RiskLimits> current risk limits
     */
    CompletableFuture<RiskLimits> getRiskLimits(Long userId);
    
    /**
     * Update specific risk limit
     * 
     * @param userId The user ID
     * @param limitType Type of limit to update
     * @param newValue New limit value
     * @return CompletableFuture<Void> completion indicator
     */
    CompletableFuture<Void> updateRiskLimit(Long userId, String limitType, BigDecimal newValue);
    
    /**
     * Get risk limit utilization
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> limit utilization percentages
     */
    CompletableFuture<Map<String, BigDecimal>> getRiskLimitUtilization(Long userId);
    
    /**
     * Check if any risk limits are breached
     * 
     * @param userId The user ID
     * @return CompletableFuture<List<RiskViolation>> active risk violations
     */
    CompletableFuture<List<RiskViolation>> checkRiskLimitBreaches(Long userId);
    
    // ========== Liquidity Risk Management ==========
    
    /**
     * Assess liquidity risk for portfolio
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Object>> liquidity risk metrics
     */
    CompletableFuture<Map<String, Object>> assessLiquidityRisk(Long userId);
    
    /**
     * Calculate time to liquidate portfolio
     * 
     * @param userId The user ID
     * @param marketParticipationRate Maximum market participation rate
     * @return CompletableFuture<Map<String, Integer>> liquidation time by position
     */
    CompletableFuture<Map<String, Integer>> calculateLiquidationTime(Long userId, BigDecimal marketParticipationRate);
    
    /**
     * Estimate liquidation cost for emergency scenarios
     * 
     * @param userId The user ID
     * @param urgencyLevel Liquidation urgency (1-10, 10 = immediate)
     * @return CompletableFuture<BigDecimal> estimated liquidation cost
     */
    CompletableFuture<BigDecimal> estimateLiquidationCost(Long userId, Integer urgencyLevel);
    
    // ========== Credit and Counterparty Risk ==========
    
    /**
     * Assess counterparty risk exposure
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> counterparty exposure
     */
    CompletableFuture<Map<String, BigDecimal>> assessCounterpartyRisk(Long userId);
    
    /**
     * Monitor credit utilization and limits
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Object>> credit metrics
     */
    CompletableFuture<Map<String, Object>> monitorCreditUtilization(Long userId);
    
    // ========== Market Risk Analytics ==========
    
    /**
     * Calculate portfolio beta and correlation to market
     * 
     * @param userId The user ID
     * @param benchmarkSymbol Benchmark for comparison
     * @param lookBackDays Historical period for calculation
     * @return CompletableFuture<Map<String, BigDecimal>> beta and correlation metrics
     */
    CompletableFuture<Map<String, BigDecimal>> calculateMarketRiskMetrics(Long userId, String benchmarkSymbol, Integer lookBackDays);
    
    /**
     * Analyze interest rate sensitivity
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> interest rate risk metrics
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeInterestRateSensitivity(Long userId);
    
    /**
     * Calculate currency exposure and risk
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> currency exposure by currency
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeCurrencyRisk(Long userId);
    
    // ========== Regulatory Compliance ==========
    
    /**
     * Check SEBI compliance requirements
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return CompletableFuture<List<String>> compliance issues (empty if compliant)
     */
    CompletableFuture<List<String>> checkSEBICompliance(Long userId, Order order);
    
    /**
     * Validate margin trading regulations
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return CompletableFuture<Boolean> true if compliant
     */
    CompletableFuture<Boolean> validateMarginTradingRules(Long userId, Order order);
    
    /**
     * Check circuit breaker and price band limits
     * 
     * @param symbol Trading symbol
     * @param orderPrice Order price
     * @return CompletableFuture<Boolean> true if within limits
     */
    CompletableFuture<Boolean> checkCircuitBreakerLimits(String symbol, BigDecimal orderPrice);
    
    // ========== Real-time Risk Monitoring ==========
    
    /**
     * Start real-time risk monitoring for user
     * 
     * @param userId The user ID
     * @param monitoringParameters Monitoring configuration
     */
    void startRiskMonitoring(Long userId, Map<String, Object> monitoringParameters);
    
    /**
     * Stop real-time risk monitoring
     * 
     * @param userId The user ID
     */
    void stopRiskMonitoring(Long userId);
    
    /**
     * Get current risk dashboard metrics
     * 
     * @param userId The user ID
     * @return CompletableFuture<RiskMetrics> comprehensive risk metrics
     */
    CompletableFuture<RiskMetrics> getRiskDashboard(Long userId);
    
    /**
     * Generate risk alert when thresholds are breached
     * 
     * @param userId The user ID
     * @param violation Risk violation details
     * @param alertLevel Alert severity level
     */
    void generateRiskAlert(Long userId, RiskViolation violation, String alertLevel);
    
    // ========== Historical Risk Analysis ==========
    
    /**
     * Analyze historical risk-adjusted performance
     * 
     * @param userId The user ID
     * @param fromDate Start date
     * @param toDate End date
     * @return CompletableFuture<Map<String, BigDecimal>> historical risk metrics
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeHistoricalRisk(Long userId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * Calculate maximum historical drawdown periods
     * 
     * @param userId The user ID
     * @param lookBackDays Historical period
     * @return CompletableFuture<Map<String, Object>> drawdown analysis
     */
    CompletableFuture<Map<String, Object>> analyzeHistoricalDrawdowns(Long userId, Integer lookBackDays);
    
    // ========== Machine Learning Risk Models ==========
    
    /**
     * Machine learning-based risk prediction
     * 
     * @param userId The user ID
     * @param predictionHorizonHours Prediction time horizon
     * @return CompletableFuture<Map<String, BigDecimal>> predicted risk metrics
     */
    CompletableFuture<Map<String, BigDecimal>> predictRiskMetrics(Long userId, Integer predictionHorizonHours);
    
    /**
     * Anomaly detection for unusual trading patterns
     * 
     * @param userId The user ID
     * @param lookBackDays Historical comparison period
     * @return CompletableFuture<List<String>> detected anomalies
     */
    CompletableFuture<List<String>> detectTradingAnomalies(Long userId, Integer lookBackDays);
    
    /**
     * Dynamic risk model calibration based on market regime
     * 
     * @param marketRegime Current market regime (BULL, BEAR, VOLATILE, STABLE)
     * @return CompletableFuture<Void> calibration completion
     */
    CompletableFuture<Void> calibrateRiskModelsForRegime(String marketRegime);
    
    // ========== Risk Model Calibration ==========
    
    /**
     * Calibrate risk models with latest market data
     * 
     * @return CompletableFuture<Void> calibration completion
     */
    CompletableFuture<Void> calibrateRiskModels();
    
    /**
     * Update correlation matrices and covariance estimates
     * 
     * @param symbols List of symbols to update
     * @return CompletableFuture<Void> update completion
     */
    CompletableFuture<Void> updateCorrelationMatrices(List<String> symbols);
    
    /**
     * Validate risk model accuracy with backtesting
     * 
     * @param modelType Type of risk model to validate
     * @param backtestPeriodDays Backtesting period
     * @return CompletableFuture<Map<String, BigDecimal>> model accuracy metrics
     */
    CompletableFuture<Map<String, BigDecimal>> validateRiskModelAccuracy(String modelType, Integer backtestPeriodDays);
    
    // ========== Emergency Risk Procedures ==========
    
    /**
     * Initiate emergency risk shutdown
     * 
     * @param userId The user ID
     * @param reason Reason for shutdown
     * @return CompletableFuture<List<Order>> emergency orders placed
     */
    CompletableFuture<List<Order>> initiateEmergencyRiskShutdown(Long userId, String reason);
    
    /**
     * Calculate optimal hedging strategy for risk reduction
     * 
     * @param userId The user ID
     * @param targetRiskReduction Desired risk reduction percentage
     * @return CompletableFuture<List<Order>> recommended hedging orders
     */
    CompletableFuture<List<Order>> calculateOptimalHedgingStrategy(Long userId, BigDecimal targetRiskReduction);
    
    /**
     * Execute portfolio risk rebalancing
     * 
     * @param userId The user ID
     * @param targetRiskProfile Desired risk profile
     * @return CompletableFuture<List<Order>> rebalancing orders
     */
    CompletableFuture<List<Order>> executeRiskRebalancing(Long userId, Map<String, BigDecimal> targetRiskProfile);
    
    // ========== Risk Reporting ==========
    
    /**
     * Generate comprehensive risk report
     * 
     * @param userId The user ID
     * @param reportType Type of risk report
     * @param period Report period
     * @return CompletableFuture<byte[]> PDF risk report
     */
    CompletableFuture<byte[]> generateRiskReport(Long userId, String reportType, String period);
    
    /**
     * Export risk data for regulatory reporting
     * 
     * @param userId The user ID
     * @param format Export format (CSV, XML, JSON)
     * @param regulatoryFramework Target regulatory framework
     * @return CompletableFuture<byte[]> exported risk data
     */
    CompletableFuture<byte[]> exportRegulatoryRiskData(Long userId, String format, String regulatoryFramework);
    
    /**
     * Generate daily risk summary report
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Object>> daily risk summary
     */
    CompletableFuture<Map<String, Object>> generateDailyRiskSummary(Long userId);
}