package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Fees Calculation Result DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record FeesCalculationResult(
    BigDecimal totalCommissions,
    BigDecimal totalTaxes,
    BigDecimal totalOtherFees,
    BigDecimal totalFees,
    Integer transactions,
    BigDecimal averageFeePerTrade,
    BigDecimal feesAsPercentOfVolume
) {}
