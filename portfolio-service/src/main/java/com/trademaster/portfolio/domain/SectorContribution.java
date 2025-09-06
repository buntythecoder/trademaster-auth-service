package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Sector Contribution Data
 * 
 * Represents a sector's contribution to portfolio performance.
 * Used in attribution analysis.
 */
public record SectorContribution(
    String sectorName,
    String sectorCode,
    BigDecimal weight, // Portfolio weight percentage
    BigDecimal returnContribution, // Contribution to total return
    BigDecimal allocationEffect, // Asset allocation effect
    BigDecimal selectionEffect, // Stock selection effect
    BigDecimal totalEffect, // Total contribution effect
    int numberOfHoldings
) {}