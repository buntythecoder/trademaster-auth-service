package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Time Contribution Data
 * 
 * Represents performance contribution over time periods.
 * Used in attribution analysis.
 */
public record TimeContribution(
    Instant periodStart,
    Instant periodEnd,
    String periodLabel, // e.g., "Q1 2024", "Jan 2024"
    BigDecimal contribution, // Contribution to total return
    BigDecimal periodReturn, // Return for this period
    BigDecimal cumulativeReturn, // Cumulative return up to this period
    int tradingDays
) {}