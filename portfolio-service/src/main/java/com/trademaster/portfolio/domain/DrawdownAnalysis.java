package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Drawdown Analysis for portfolio performance
 * 
 * @param maxDrawdown Maximum drawdown percentage
 * @param maxDrawdownPeriod Period of maximum drawdown
 * @param currentDrawdown Current drawdown from recent peak
 * @param drawdownDuration Days in current drawdown
 * @param recoveryTime Expected recovery time in days
 * @param drawdownPeriods List of all drawdown periods
 */
public record DrawdownAnalysis(
    BigDecimal maxDrawdown,
    DrawdownPeriod maxDrawdownPeriod,
    BigDecimal currentDrawdown,
    Integer drawdownDuration,
    Integer recoveryTime,
    List<DrawdownPeriod> drawdownPeriods
) {
    
    public record DrawdownPeriod(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate recoveryDate,
        BigDecimal peakValue,
        BigDecimal troughValue,
        BigDecimal drawdownPercent,
        Integer durationDays
    ) {}
}