package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Holding Contribution Data
 * 
 * Represents an individual holding's contribution to portfolio performance.
 * Used in attribution analysis.
 */
public record HoldingContribution(
    String symbol,
    String name,
    String sector,
    BigDecimal weight, // Portfolio weight percentage
    BigDecimal returnContribution, // Contribution to total return
    BigDecimal individualReturn, // Holding's individual return
    BigDecimal benchmarkWeight, // Weight in benchmark
    BigDecimal overUnderWeight, // Over/under weight vs benchmark
    BigDecimal attributionValue // Attribution value
) {}