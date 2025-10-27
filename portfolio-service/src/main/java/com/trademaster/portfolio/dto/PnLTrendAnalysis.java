package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * P&L Trend Analysis DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PnLTrendAnalysis(
    Long portfolioId,
    Integer periodDays,
    BigDecimal totalPnlChange,
    BigDecimal averageDailyPnl,
    BigDecimal volatility,
    BigDecimal bestDay,
    BigDecimal worstDay,
    Integer profitableDays,
    Integer losingDays,
    List<DailyPnLPoint> dailyPoints
) {}
