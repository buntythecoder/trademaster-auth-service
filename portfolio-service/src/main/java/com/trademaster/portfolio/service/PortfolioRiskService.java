package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.RiskAlert;
import com.trademaster.portfolio.dto.RiskLimitConfiguration;
import com.trademaster.portfolio.dto.RiskAssessmentRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Risk Management Service Interface
 * 
 * Comprehensive risk management and monitoring service using Java 24 Virtual Threads.
 * Provides real-time risk assessment, limit monitoring, and proactive risk alerts.
 * 
 * Key Features:
 * - Real-time risk limit monitoring with automatic alerts
 * - Position-level and portfolio-level risk assessment
 * - Concentration risk analysis and early warning system
 * - Market risk metrics (VaR, CVaR, Beta, Volatility)
 * - Margin requirement calculations and monitoring
 * - Regulatory compliance monitoring and reporting
 * - Stress testing and scenario analysis
 * 
 * Performance Targets:
 * - Risk assessment: <25ms
 * - Limit validation: <10ms
 * - Alert generation: <5ms
 * - Risk metrics calculation: <50ms
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PortfolioRiskService {
    
    /**
     * Validate portfolio operation against risk limits
     * 
     * @param portfolioId The portfolio ID
     * @param operation The operation being performed
     * @return true if operation is within risk limits
     */
    boolean validatePortfolioOperation(Long portfolioId, String operation);
    
    /**
     * Assess risk for proposed trade
     * 
     * @param portfolioId The portfolio ID
     * @param request Risk assessment request
     * @return Risk assessment result
     */
    RiskAssessmentResult assessTradeRisk(Long portfolioId, RiskAssessmentRequest request);
    
    /**
     * Assess risk for proposed trade asynchronously
     * 
     * @param portfolioId The portfolio ID
     * @param request Risk assessment request
     * @return CompletableFuture with risk assessment result
     */
    CompletableFuture<RiskAssessmentResult> assessTradeRiskAsync(Long portfolioId, RiskAssessmentRequest request);
    
    /**
     * Calculate current portfolio risk metrics
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio risk metrics
     */
    PortfolioRiskMetrics calculateRiskMetrics(Long portfolioId);
    
    /**
     * Monitor risk limits and generate alerts
     * 
     * @param portfolioId The portfolio ID
     * @return List of active risk alerts
     */
    List<RiskAlert> monitorRiskLimits(Long portfolioId);
    
    /**
     * Get current risk configuration for portfolio
     * 
     * @param portfolioId The portfolio ID
     * @return Risk limit configuration
     */
    RiskLimitConfiguration getRiskConfiguration(Long portfolioId);
    
    /**
     * Update risk configuration for portfolio
     * 
     * @param portfolioId The portfolio ID
     * @param configuration New risk configuration
     * @param adminUserId Admin user making the change
     * @return Updated risk configuration
     */
    RiskLimitConfiguration updateRiskConfiguration(Long portfolioId, RiskLimitConfiguration configuration, Long adminUserId);
    
    /**
     * Calculate concentration risk for portfolio
     * 
     * @param portfolioId The portfolio ID
     * @return Concentration risk analysis
     */
    ConcentrationRiskAssessment calculateConcentrationRisk(Long portfolioId);
    
    /**
     * Calculate margin requirements
     * 
     * @param portfolioId The portfolio ID
     * @return Margin requirement calculation
     */
    MarginRequirement calculateMarginRequirement(Long portfolioId);
    
    /**
     * Monitor margin utilization and generate alerts
     * 
     * @param portfolioId The portfolio ID
     * @return Margin monitoring result
     */
    MarginMonitoringResult monitorMarginUtilization(Long portfolioId);
    
    /**
     * Calculate position-level risk metrics
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @return Position risk metrics
     */
    PositionRiskMetrics calculatePositionRisk(Long portfolioId, String symbol);
    
    /**
     * Validate buying power for trade
     * 
     * @param portfolioId The portfolio ID
     * @param tradeValue Trade value
     * @param tradeType Trade type (BUY, SELL, SHORT_SELL, BUY_TO_COVER)
     * @return Buying power validation result
     */
    BuyingPowerValidation validateBuyingPower(Long portfolioId, BigDecimal tradeValue, String tradeType);
    
    /**
     * Calculate portfolio leverage
     * 
     * @param portfolioId The portfolio ID
     * @return Current leverage ratio
     */
    BigDecimal calculateLeverage(Long portfolioId);
    
    /**
     * Check if portfolio exceeds maximum leverage
     * 
     * @param portfolioId The portfolio ID
     * @return true if leverage exceeds limits
     */
    boolean isLeverageExceeded(Long portfolioId);
    
    /**
     * Calculate daily trading limits utilization
     * 
     * @param portfolioId The portfolio ID
     * @return Daily limits status
     */
    DailyLimitsStatus getDailyLimitsStatus(Long portfolioId);
    
    /**
     * Validate position size limits
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param proposedQuantity Proposed position quantity
     * @return Position size validation result
     */
    PositionSizeValidation validatePositionSize(Long portfolioId, String symbol, Integer proposedQuantity);
    
    /**
     * Calculate sector concentration limits
     * 
     * @param portfolioId The portfolio ID
     * @return Sector concentration analysis
     */
    SectorConcentrationAnalysis analyzeSectorConcentration(Long portfolioId);
    
    /**
     * Generate risk compliance report
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Risk compliance report
     */
    CompletableFuture<RiskComplianceReport> generateComplianceReport(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Calculate correlation risk with market indices
     * 
     * @param portfolioId The portfolio ID
     * @param marketIndices List of market indices to check correlation
     * @return Correlation risk assessment
     */
    CorrelationRiskAssessment calculateCorrelationRisk(Long portfolioId, List<String> marketIndices);
    
    /**
     * Monitor overnight position risk
     * 
     * @param portfolioId The portfolio ID
     * @return Overnight risk assessment
     */
    OvernightRiskAssessment assessOvernightRisk(Long portfolioId);
    
    /**
     * Calculate portfolio Greeks (for options positions)
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio Greeks calculation
     */
    PortfolioGreeks calculatePortfolioGreeks(Long portfolioId);
    
    /**
     * Validate maximum drawdown limits
     * 
     * @param portfolioId The portfolio ID
     * @return Drawdown validation result
     */
    DrawdownValidation validateDrawdownLimits(Long portfolioId);
    
    /**
     * Calculate liquidity risk for positions
     * 
     * @param portfolioId The portfolio ID
     * @return Liquidity risk assessment
     */
    LiquidityRiskAssessment calculateLiquidityRisk(Long portfolioId);
    
    /**
     * Generate real-time risk dashboard data
     * 
     * @param portfolioId The portfolio ID
     * @return Risk dashboard data
     */
    RiskDashboardData generateRiskDashboard(Long portfolioId);
    
    /**
     * Simulate portfolio risk under stress scenarios
     * 
     * @param portfolioId The portfolio ID
     * @param stressScenarios List of stress scenarios
     * @return Stress test risk results
     */
    CompletableFuture<StressTestRiskResult> simulateStressScenarios(Long portfolioId, List<StressScenario> stressScenarios);
    
    /**
     * Calculate time-weighted risk metrics
     * 
     * @param portfolioId The portfolio ID
     * @param timeHorizonDays Time horizon in days
     * @return Time-weighted risk metrics
     */
    TimeWeightedRiskMetrics calculateTimeWeightedRisk(Long portfolioId, Integer timeHorizonDays);
    
    /**
     * Monitor intraday risk limits
     * 
     * @param portfolioId The portfolio ID
     * @return Intraday risk monitoring result
     */
    IntradayRiskMonitoring monitorIntradayRisk(Long portfolioId);
    
    /**
     * Calculate regulatory capital requirements
     * 
     * @param portfolioId The portfolio ID
     * @return Regulatory capital requirement
     */
    RegulatoryCapitalRequirement calculateRegulatoryCapital(Long portfolioId);
}

/**
 * Risk Assessment Result DTO
 */
record RiskAssessmentResult(
    boolean approved,
    String riskLevel,
    BigDecimal riskScore,
    List<String> riskFactors,
    List<String> warnings,
    List<String> violations,
    BigDecimal requiredMargin,
    BigDecimal impactOnPortfolio,
    Instant assessmentTime
) {}

/**
 * Portfolio Risk Metrics DTO
 */
record PortfolioRiskMetrics(
    Long portfolioId,
    BigDecimal valueAtRisk95,
    BigDecimal valueAtRisk99,
    BigDecimal conditionalVaR,
    BigDecimal portfolioBeta,
    BigDecimal portfolioVolatility,
    BigDecimal sharpeRatio,
    BigDecimal maxDrawdown,
    BigDecimal currentLeverage,
    BigDecimal concentrationRisk,
    String riskRating,
    Instant calculatedAt
) {}

/**
 * Concentration Risk Assessment DTO
 */
record ConcentrationRiskAssessment(
    Long portfolioId,
    BigDecimal maxSinglePositionPercent,
    BigDecimal top5PositionsPercent,
    BigDecimal top10PositionsPercent,
    Integer positionsAboveThreshold,
    String concentrationLevel,
    List<ConcentrationViolation> violations,
    List<String> recommendations,
    Instant assessmentTime
) {}

/**
 * Concentration Violation DTO
 */
record ConcentrationViolation(
    String violationType,
    String identifier,
    BigDecimal currentPercent,
    BigDecimal limitPercent,
    String severity
) {}

/**
 * Margin Requirement DTO
 */
record MarginRequirement(
    Long portfolioId,
    BigDecimal totalMarginRequired,
    BigDecimal initialMarginRequired,
    BigDecimal maintenanceMarginRequired,
    BigDecimal availableMargin,
    BigDecimal marginUtilization,
    List<PositionMarginDetail> positionMargins,
    boolean marginCallRequired,
    Instant calculatedAt
) {}

/**
 * Position Margin Detail DTO
 */
record PositionMarginDetail(
    String symbol,
    BigDecimal positionValue,
    BigDecimal marginRequirement,
    BigDecimal marginRate,
    String positionType
) {}

/**
 * Margin Monitoring Result DTO
 */
record MarginMonitoringResult(
    Long portfolioId,
    BigDecimal currentMarginUtilization,
    BigDecimal marginThreshold,
    boolean marginCallTriggered,
    BigDecimal marginCallAmount,
    List<String> marginAlerts,
    BigDecimal timeToMarginCall,
    Instant monitoringTime
) {}

/**
 * Position Risk Metrics DTO
 */
record PositionRiskMetrics(
    String symbol,
    BigDecimal positionValue,
    BigDecimal positionPercent,
    BigDecimal positionBeta,
    BigDecimal positionVolatility,
    BigDecimal positionVaR,
    BigDecimal marginRequirement,
    String riskLevel,
    List<String> riskWarnings
) {}

/**
 * Buying Power Validation DTO
 */
record BuyingPowerValidation(
    boolean sufficient,
    BigDecimal availableBuyingPower,
    BigDecimal requiredBuyingPower,
    BigDecimal marginRequirement,
    String validationStatus,
    List<String> restrictions
) {}

/**
 * Daily Limits Status DTO
 */
record DailyLimitsStatus(
    Long portfolioId,
    Integer dayTradesUsed,
    Integer dayTradesLimit,
    BigDecimal dailyLossLimit,
    BigDecimal currentDailyLoss,
    BigDecimal dailyVolumeLimit,
    BigDecimal currentDailyVolume,
    List<String> limitWarnings,
    boolean anyLimitExceeded
) {}

/**
 * Position Size Validation DTO
 */
record PositionSizeValidation(
    boolean valid,
    Integer maxAllowedQuantity,
    Integer proposedQuantity,
    BigDecimal maxPositionValue,
    BigDecimal maxConcentrationPercent,
    String validationMessage
) {}

/**
 * Sector Concentration Analysis DTO
 */
record SectorConcentrationAnalysis(
    Long portfolioId,
    List<SectorConcentration> sectorBreakdown,
    String mostConcentratedSector,
    BigDecimal maxSectorConcentration,
    List<String> concentrationWarnings,
    Instant analysisTime
) {}

/**
 * Sector Concentration DTO
 */
record SectorConcentration(
    String sector,
    BigDecimal allocationPercent,
    BigDecimal limitPercent,
    boolean exceedsLimit,
    String riskLevel
) {}

/**
 * Risk Compliance Report DTO
 */
record RiskComplianceReport(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    List<RiskViolation> violations,
    List<ComplianceMetric> metrics,
    String overallComplianceStatus,
    List<String> recommendations,
    Instant reportGeneratedAt
) {}

/**
 * Risk Violation DTO
 */
record RiskViolation(
    Instant violationTime,
    String violationType,
    String description,
    String severity,
    BigDecimal violationAmount,
    String resolution
) {}

/**
 * Compliance Metric DTO
 */
record ComplianceMetric(
    String metricName,
    BigDecimal currentValue,
    BigDecimal limitValue,
    String status,
    BigDecimal utilizationPercent
) {}

/**
 * Correlation Risk Assessment DTO
 */
record CorrelationRiskAssessment(
    Long portfolioId,
    List<IndexCorrelationRisk> correlations,
    BigDecimal averageCorrelation,
    String correlationRiskLevel,
    List<String> riskMitigation,
    Instant assessmentTime
) {}

/**
 * Index Correlation Risk DTO
 */
record IndexCorrelationRisk(
    String indexSymbol,
    BigDecimal correlation,
    String riskLevel,
    String description
) {}

/**
 * Overnight Risk Assessment DTO
 */
record OvernightRiskAssessment(
    Long portfolioId,
    BigDecimal overnightVaR,
    BigDecimal overnightExposure,
    List<String> overnightRisks,
    List<String> recommendations,
    boolean requiresAction,
    Instant assessmentTime
) {}

/**
 * Portfolio Greeks DTO
 */
record PortfolioGreeks(
    Long portfolioId,
    BigDecimal delta,
    BigDecimal gamma,
    BigDecimal theta,
    BigDecimal vega,
    BigDecimal rho,
    List<PositionGreeks> positionGreeks,
    Instant calculatedAt
) {}

/**
 * Position Greeks DTO
 */
record PositionGreeks(
    String symbol,
    BigDecimal delta,
    BigDecimal gamma,
    BigDecimal theta,
    BigDecimal vega,
    BigDecimal rho
) {}

/**
 * Drawdown Validation DTO
 */
record DrawdownValidation(
    boolean valid,
    BigDecimal currentDrawdown,
    BigDecimal maxAllowedDrawdown,
    BigDecimal peakPortfolioValue,
    BigDecimal currentPortfolioValue,
    String status
) {}

/**
 * Liquidity Risk Assessment DTO
 */
record LiquidityRiskAssessment(
    Long portfolioId,
    BigDecimal liquidityScore,
    List<PositionLiquidity> positionLiquidity,
    BigDecimal averageDailyVolume,
    Integer liquidationDays,
    String liquidityRating,
    Instant assessmentTime
) {}

/**
 * Position Liquidity DTO
 */
record PositionLiquidity(
    String symbol,
    BigDecimal positionValue,
    BigDecimal averageDailyVolume,
    Integer daysToLiquidate,
    String liquidityRating
) {}

/**
 * Risk Dashboard Data DTO
 */
record RiskDashboardData(
    Long portfolioId,
    PortfolioRiskMetrics overallRisk,
    List<RiskAlert> activeAlerts,
    ConcentrationRiskAssessment concentration,
    MarginMonitoringResult margin,
    DailyLimitsStatus dailyLimits,
    List<String> recommendations,
    Instant lastUpdated
) {}

/**
 * Stress Test Risk Result DTO
 */
record StressTestRiskResult(
    Long portfolioId,
    List<StressScenarioRiskResult> scenarioResults,
    BigDecimal worstCaseVaR,
    BigDecimal expectedLoss,
    String overallRiskRating,
    List<String> criticalRisks,
    Instant testDate
) {}

/**
 * Stress Scenario Risk Result DTO
 */
record StressScenarioRiskResult(
    String scenarioName,
    BigDecimal portfolioLoss,
    BigDecimal lossPercent,
    BigDecimal stressedVaR,
    String riskLevel
) {}

/**
 * Time Weighted Risk Metrics DTO
 */
record TimeWeightedRiskMetrics(
    Long portfolioId,
    Integer timeHorizonDays,
    BigDecimal timeWeightedVaR,
    BigDecimal timeWeightedVolatility,
    BigDecimal timeDecayFactor,
    BigDecimal adjustedRiskScore,
    Instant calculatedAt
) {}

/**
 * Intraday Risk Monitoring DTO
 */
record IntradayRiskMonitoring(
    Long portfolioId,
    List<IntradayRiskMetric> hourlyMetrics,
    BigDecimal peakIntradayRisk,
    String currentRiskStatus,
    List<String> intradayAlerts,
    Instant lastUpdate
) {}

/**
 * Intraday Risk Metric DTO
 */
record IntradayRiskMetric(
    Instant timestamp,
    BigDecimal intradayVaR,
    BigDecimal realizationRatio,
    String riskLevel
) {}

/**
 * Regulatory Capital Requirement DTO
 */
record RegulatoryCapitalRequirement(
    Long portfolioId,
    BigDecimal riskWeightedAssets,
    BigDecimal requiredCapital,
    BigDecimal availableCapital,
    BigDecimal capitalRatio,
    String complianceStatus,
    List<String> regulatoryWarnings,
    Instant calculatedAt
) {}