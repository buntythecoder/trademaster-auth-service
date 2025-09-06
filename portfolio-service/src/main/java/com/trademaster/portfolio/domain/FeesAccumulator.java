package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Functional accumulator for fees calculation
 * 
 * Immutable record that accumulates different types of fees and commissions
 * following functional programming principles.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Functional Programming)
 */
public record FeesAccumulator(
    BigDecimal totalCommissions,
    BigDecimal totalTax,
    BigDecimal totalBrokerageFees,
    BigDecimal totalExchangeFees,
    BigDecimal totalOtherFees,
    Integer transactionCount
) {
    
    /**
     * Create new empty accumulator
     */
    public FeesAccumulator() {
        this(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0
        );
    }
    
    /**
     * Add fees from transaction
     */
    public FeesAccumulator add(
        BigDecimal commission,
        BigDecimal tax,
        BigDecimal brokerageFee,
        BigDecimal exchangeFee,
        BigDecimal otherFees
    ) {
        return new FeesAccumulator(
            this.totalCommissions.add(commission != null ? commission : BigDecimal.ZERO),
            this.totalTax.add(tax != null ? tax : BigDecimal.ZERO),
            this.totalBrokerageFees.add(brokerageFee != null ? brokerageFee : BigDecimal.ZERO),
            this.totalExchangeFees.add(exchangeFee != null ? exchangeFee : BigDecimal.ZERO),
            this.totalOtherFees.add(otherFees != null ? otherFees : BigDecimal.ZERO),
            this.transactionCount + 1
        );
    }
    
    /**
     * Combine two accumulators
     */
    public static FeesAccumulator combine(FeesAccumulator a, FeesAccumulator b) {
        return new FeesAccumulator(
            a.totalCommissions.add(b.totalCommissions),
            a.totalTax.add(b.totalTax),
            a.totalBrokerageFees.add(b.totalBrokerageFees),
            a.totalExchangeFees.add(b.totalExchangeFees),
            a.totalOtherFees.add(b.totalOtherFees),
            a.transactionCount + b.transactionCount
        );
    }
    
    /**
     * Calculate total fees across all categories
     */
    public BigDecimal getTotalFees() {
        return totalCommissions
            .add(totalTax)
            .add(totalBrokerageFees)
            .add(totalExchangeFees)
            .add(totalOtherFees);
    }
    
    /**
     * Calculate average fees per transaction
     */
    public BigDecimal getAverageFees() {
        return transactionCount > 0 ?
            getTotalFees().divide(BigDecimal.valueOf(transactionCount), 2, java.math.RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
    }
    
    /**
     * Convert to result record
     */
    public FeesCalculationResult toResult() {
        return new FeesCalculationResult(
            this.totalCommissions,
            this.totalTax,
            this.totalBrokerageFees,
            this.totalExchangeFees,
            this.totalOtherFees,
            getTotalFees(),
            getAverageFees(),
            this.transactionCount
        );
    }
}