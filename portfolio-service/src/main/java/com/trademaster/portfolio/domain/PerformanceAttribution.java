package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Performance Attribution analysis for portfolio returns
 * 
 * @param portfolioId Portfolio identifier
 * @param fromDate Analysis start date
 * @param toDate Analysis end date
 * @param totalReturn Total portfolio return for period
 * @param benchmarkReturn Benchmark return for period
 * @param activeReturn Active return (portfolio - benchmark)
 * @param sectorAttribution Attribution by sector
 * @param securityAttribution Attribution by security
 * @param allocationEffect Asset allocation effect
 * @param selectionEffect Security selection effect
 * @param interactionEffect Allocation-selection interaction
 * @param currencyEffect Currency exposure effect
 * @param calculationDate Date of calculation
 */
public record PerformanceAttribution(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalReturn,
    BigDecimal benchmarkReturn,
    BigDecimal activeReturn,
    List<SectorAttribution> sectorAttribution,
    List<SecurityAttribution> securityAttribution,
    BigDecimal allocationEffect,
    BigDecimal selectionEffect,
    BigDecimal interactionEffect,
    BigDecimal currencyEffect,
    Instant calculationDate
) {
    
    public record SectorAttribution(
        String sectorName,
        BigDecimal portfolioWeight,
        BigDecimal benchmarkWeight,
        BigDecimal portfolioReturn,
        BigDecimal benchmarkReturn,
        BigDecimal allocationContribution,
        BigDecimal selectionContribution,
        BigDecimal totalContribution
    ) {}
    
    public record SecurityAttribution(
        String symbol,
        BigDecimal weight,
        BigDecimal return_,
        BigDecimal benchmarkReturn,
        BigDecimal contribution,
        String sector
    ) {}
}