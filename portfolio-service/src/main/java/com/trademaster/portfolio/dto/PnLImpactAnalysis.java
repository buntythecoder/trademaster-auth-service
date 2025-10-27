package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * P&L Impact Analysis DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PnLImpactAnalysis(
    BigDecimal currentUnrealizedPnl,
    BigDecimal projectedRealizedPnl,
    BigDecimal projectedUnrealizedPnl,
    BigDecimal netPnlImpact,
    BigDecimal taxImpact,
    BigDecimal breakEvenPrice,
    String riskAssessment
) {}
