package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * P&L Validation Result DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PnLValidationResult(
    boolean isValid,
    BigDecimal calculatedPnl,
    BigDecimal expectedPnl,
    BigDecimal variance,
    BigDecimal toleranceThreshold,
    List<String> discrepancies,
    Instant validationTime
) {}
