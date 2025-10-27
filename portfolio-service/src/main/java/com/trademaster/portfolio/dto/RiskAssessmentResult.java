package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Risk Assessment Result DTO
 *
 * Result of portfolio risk assessment with approval status, risk factors, and violations.
 * Used by PortfolioRiskService for trade risk assessment.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RiskAssessmentResult(
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
