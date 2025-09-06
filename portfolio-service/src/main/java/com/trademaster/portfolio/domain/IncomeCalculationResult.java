package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Income Calculation Result for dividend and interest income tracking
 * 
 * @param portfolioId Portfolio identifier
 * @param fromDate Period start date
 * @param toDate Period end date
 * @param totalDividendIncome Total dividend income received
 * @param totalInterestIncome Total interest income received
 * @param totalIncomeReceived Total income received (dividends + interest)
 * @param projectedAnnualIncome Projected annual income based on current holdings
 * @param yieldOnCost Yield on original cost basis
 * @param currentYield Current yield based on market value
 * @param dividendDetails Detailed dividend information
 * @param interestDetails Detailed interest information
 * @param taxImplications Tax implications of income
 * @param calculationDate Date of calculation
 */
public record IncomeCalculationResult(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalDividendIncome,
    BigDecimal totalInterestIncome,
    BigDecimal totalIncomeReceived,
    BigDecimal projectedAnnualIncome,
    BigDecimal yieldOnCost,
    BigDecimal currentYield,
    List<DividendDetail> dividendDetails,
    List<InterestDetail> interestDetails,
    IncomeTaxImplications taxImplications,
    Instant calculationDate
) {
    
    public record DividendDetail(
        String symbol,
        Instant exDate,
        Instant payDate,
        BigDecimal dividendPerShare,
        Integer sharesHeld,
        BigDecimal totalDividend,
        String dividendType,
        Boolean qualified
    ) {}
    
    public record InterestDetail(
        String symbol,
        Instant paymentDate,
        BigDecimal interestRate,
        BigDecimal principal,
        BigDecimal interestAmount,
        String interestType
    ) {}
    
    public record IncomeTaxImplications(
        BigDecimal qualifiedDividends,
        BigDecimal nonQualifiedDividends,
        BigDecimal taxableInterest,
        BigDecimal taxExemptInterest,
        BigDecimal estimatedTaxLiability
    ) {}
    
    /**
     * Simple constructor for basic income calculation
     */
    public IncomeCalculationResult(
        BigDecimal totalIncome,
        BigDecimal totalDividends,
        BigDecimal totalInterest,
        Integer transactionCount
    ) {
        this(
            null, // portfolioId
            Instant.now(), // fromDate
            Instant.now(), // toDate
            totalDividends, // totalDividendIncome
            totalInterest, // totalInterestIncome
            totalIncome, // totalIncomeReceived
            BigDecimal.ZERO, // projectedAnnualIncome
            BigDecimal.ZERO, // yieldOnCost
            BigDecimal.ZERO, // currentYield
            List.of(), // dividendDetails
            List.of(), // interestDetails
            new IncomeTaxImplications(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            Instant.now() // calculationDate
        );
    }
}