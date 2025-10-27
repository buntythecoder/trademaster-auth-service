package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Daily P&L Point DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record DailyPnLPoint(
    Instant date,
    BigDecimal dailyPnl,
    BigDecimal cumulativePnl,
    BigDecimal portfolioValue
) {}
