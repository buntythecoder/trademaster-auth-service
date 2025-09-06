package com.trademaster.portfolio.service;

import com.trademaster.portfolio.domain.Portfolio;
import com.trademaster.portfolio.domain.PortfolioData;
import com.trademaster.portfolio.functional.Result;
import com.trademaster.portfolio.functional.PortfolioErrors;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Risk Management Service for Portfolio Operations
 * 
 * Provides risk assessment and monitoring capabilities for portfolios.
 * Implements functional programming patterns with CompletableFuture for async operations.
 */
public interface RiskManagementService {
    
    /**
     * Assess portfolio risk based on positions and market conditions
     * 
     * @param portfolioData Portfolio data to assess
     * @return Risk assessment result
     */
    CompletableFuture<RiskAssessmentResult> assessPortfolioRisk(PortfolioData portfolioData);
    
    /**
     * Calculate Value at Risk (VaR) for portfolio
     * 
     * @param portfolioData Portfolio data
     * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
     * @param timeHorizon Time horizon in days
     * @return VaR calculation result
     */
    CompletableFuture<BigDecimal> calculateVaR(PortfolioData portfolioData, double confidenceLevel, int timeHorizon);
    
    /**
     * Monitor portfolio for risk limit violations
     * 
     * @param portfolio Portfolio to monitor
     * @return Risk monitoring result
     */
    CompletableFuture<RiskMonitoringResult> monitorRiskLimits(Portfolio portfolio);
    
    /**
     * Calculate portfolio beta relative to market benchmark
     * 
     * @param portfolioData Portfolio data
     * @param benchmarkSymbol Benchmark symbol (e.g., "NIFTY50")
     * @return Portfolio beta
     */
    CompletableFuture<BigDecimal> calculatePortfolioBeta(PortfolioData portfolioData, String benchmarkSymbol);
    
    /**
     * Calculate comprehensive portfolio risk metrics
     * 
     * @param userId User ID
     * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
     * @return Risk metrics result
     */
    Result<RiskMetrics, PortfolioErrors> calculatePortfolioRisk(Long userId, Double confidenceLevel);
    
    /**
     * Calculate Value at Risk with specific parameters
     * 
     * @param userId User ID
     * @param confidenceLevel Confidence level
     * @param horizon Time horizon in days
     * @return VaR metrics result
     */
    Result<VarMetrics, PortfolioErrors> calculateValueAtRisk(Long userId, Double confidenceLevel, Integer horizon);
    
    /**
     * Analyze concentration risk in portfolio
     * 
     * @param userId User ID
     * @return Concentration risk analysis
     */
    Result<ConcentrationRisk, PortfolioErrors> analyzeConcentrationRisk(Long userId);
    
    /**
     * Risk Assessment Result
     */
    record RiskAssessmentResult(
        String portfolioId,
        String riskLevel, // LOW, MEDIUM, HIGH, CRITICAL
        BigDecimal riskScore, // 0.0 to 1.0
        BigDecimal valueAtRisk,
        BigDecimal expectedShortfall,
        java.time.Instant assessmentTimestamp,
        java.util.List<String> riskFactors
    ) {}
    
    /**
     * Risk Monitoring Result
     */
    record RiskMonitoringResult(
        String portfolioId,
        boolean hasViolations,
        java.util.List<RiskViolation> violations,
        java.time.Instant monitoringTimestamp
    ) {}
    
    /**
     * Risk Violation Details
     */
    record RiskViolation(
        String violationType, // CONCENTRATION, VAR, DRAWDOWN, etc.
        String description,
        BigDecimal currentValue,
        BigDecimal limitValue,
        String severity // WARNING, CRITICAL
    ) {}
    
    /**
     * Risk Metrics
     */
    record RiskMetrics(
        BigDecimal riskScore,
        BigDecimal valueAtRisk,
        BigDecimal concentrationRisk,
        BigDecimal volatility,
        java.time.Instant calculatedAt
    ) {}
    
    /**
     * VaR Metrics
     */
    record VarMetrics(
        BigDecimal valueAtRisk,
        BigDecimal expectedShortfall,
        BigDecimal maxDrawdown,
        java.time.Instant calculatedAt
    ) {}
    
    /**
     * Concentration Risk
     */
    record ConcentrationRisk(
        BigDecimal maxSingleHolding,
        BigDecimal topHoldingsWeight,
        boolean isDiversified,
        java.time.Instant calculatedAt
    ) {}
}