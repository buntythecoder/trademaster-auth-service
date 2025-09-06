package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Period Return Data
 * 
 * Represents return data for a specific time period.
 * Used in performance metrics calculation.
 */
public record PeriodReturn(
    String period, // e.g., "1M", "3M", "1Y"
    Instant startDate,
    Instant endDate,
    BigDecimal returnPercentage,
    BigDecimal absoluteReturn,
    BigDecimal benchmarkReturn,
    BigDecimal excessReturn
) {}