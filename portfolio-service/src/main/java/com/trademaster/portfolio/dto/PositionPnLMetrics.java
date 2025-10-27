package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Position P&L Metrics DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PositionPnLMetrics(
    String symbol,
    BigDecimal marketValue,
    BigDecimal costBasis,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal totalPnl,
    BigDecimal totalReturn,
    BigDecimal dayPnl,
    Integer holdingDays,
    BigDecimal annualizedReturn
) {}
