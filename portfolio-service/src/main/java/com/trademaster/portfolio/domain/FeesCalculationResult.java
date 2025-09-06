package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Fees Calculation Result for portfolio fee tracking and analysis
 * 
 * @param portfolioId Portfolio identifier
 * @param fromDate Period start date
 * @param toDate Period end date
 * @param totalFees Total fees paid during period
 * @param tradingFees Trading/brokerage fees
 * @param managementFees Portfolio management fees
 * @param custodyFees Asset custody fees
 * @param performanceFees Performance-based fees
 * @param otherFees Other miscellaneous fees
 * @param feesByBroker Fees breakdown by broker
 * @param feesByAssetClass Fees breakdown by asset class
 * @param averageFeeRate Average fee rate as percentage
 * @param feeImpactOnReturns Impact of fees on returns
 * @param calculationDate Date of calculation
 */
public record FeesCalculationResult(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalFees,
    BigDecimal tradingFees,
    BigDecimal managementFees,
    BigDecimal custodyFees,
    BigDecimal performanceFees,
    BigDecimal otherFees,
    List<BrokerFeeBreakdown> feesByBroker,
    List<AssetClassFeeBreakdown> feesByAssetClass,
    BigDecimal averageFeeRate,
    BigDecimal feeImpactOnReturns,
    Instant calculationDate
) {
    
    public record BrokerFeeBreakdown(
        String brokerName,
        BigDecimal totalFees,
        BigDecimal tradingFees,
        BigDecimal otherFees,
        BigDecimal feeRate
    ) {}
    
    public record AssetClassFeeBreakdown(
        String assetClass,
        BigDecimal totalFees,
        BigDecimal averageFeeRate,
        Integer transactionCount
    ) {}
    
    /**
     * Simple constructor for basic fees calculation
     */
    public FeesCalculationResult(
        BigDecimal totalCommissions,
        BigDecimal totalTax,
        BigDecimal totalBrokerageFees,
        BigDecimal totalExchangeFees,
        BigDecimal totalOtherFees,
        BigDecimal totalFees,
        BigDecimal averageFees,
        Integer transactionCount
    ) {
        this(
            null, // portfolioId
            Instant.now(), // fromDate
            Instant.now(), // toDate
            totalFees, // totalFees
            totalCommissions.add(totalBrokerageFees), // tradingFees
            BigDecimal.ZERO, // managementFees
            BigDecimal.ZERO, // custodyFees
            BigDecimal.ZERO, // performanceFees
            totalOtherFees, // otherFees
            List.of(), // feesByBroker
            List.of(), // feesByAssetClass
            averageFees, // averageFeeRate
            BigDecimal.ZERO, // feeImpactOnReturns
            Instant.now() // calculationDate
        );
    }
}