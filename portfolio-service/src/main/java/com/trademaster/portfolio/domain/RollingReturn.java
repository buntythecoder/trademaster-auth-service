package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rolling Return data for performance analysis
 * 
 * @param date The date for this rolling return calculation
 * @param period The period in days (e.g., 30, 90, 365)
 * @param returnValue The rolling return value as a percentage
 * @param annualizedReturn The annualized return for this period
 * @param volatility The volatility during this period
 */
public record RollingReturn(
    LocalDate date,
    Integer period,
    BigDecimal returnValue,
    BigDecimal annualizedReturn,
    BigDecimal volatility
) {}