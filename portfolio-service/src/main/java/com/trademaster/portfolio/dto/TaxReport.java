package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Tax Report DTO
 *
 * Comprehensive tax report for portfolio transactions with Indian tax calculations.
 * Includes STCG, LTCG, STT, and total tax liability.
 *
 * Rule #9: Immutable Records for Data Transfer Objects
 * Rule #3: Functional Programming - Immutable data structures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TaxReport(
    Long portfolioId,
    String financialYear,
    BigDecimal totalRealizedGains,
    BigDecimal totalRealizedLosses,
    BigDecimal netRealizedPnL,
    BigDecimal shortTermCapitalGains,
    BigDecimal longTermCapitalGains,
    BigDecimal stcgTax,
    BigDecimal ltcgTax,
    BigDecimal sttPaid,
    BigDecimal totalTaxLiability,
    BigDecimal netProfitAfterTax,
    List<TransactionTaxDetail> transactions,
    Instant generatedAt
) {

    /**
     * Transaction tax detail for individual trades
     */
    public record TransactionTaxDetail(
        String symbol,
        Instant transactionDate,
        Integer quantity,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        BigDecimal grossPnL,
        int holdingDays,
        boolean isLongTerm,
        String taxCategory, // STCG or LTCG
        BigDecimal capitalGain,
        BigDecimal taxAmount,
        BigDecimal sttAmount,
        BigDecimal netPnL
    ) {
        /**
         * Calculate total tax paid on this transaction
         * Rule #3: Functional calculation method
         */
        public BigDecimal getTotalTax() {
            return taxAmount.add(sttAmount);
        }
    }

    /**
     * Factory method for empty tax report
     * Rule #4: Factory Pattern with functional construction
     */
    public static TaxReport empty(Long portfolioId, String financialYear) {
        return new TaxReport(
            portfolioId,
            financialYear,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            Instant.now()
        );
    }

    /**
     * Calculate effective tax rate
     * Rule #3: Functional calculation method
     */
    public BigDecimal getEffectiveTaxRate() {
        return netRealizedPnL.compareTo(BigDecimal.ZERO) > 0
            ? totalTaxLiability.divide(netRealizedPnL, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
    }

    /**
     * Get LTCG exemption benefit (first ₹1 lakh)
     * Rule #3: Functional calculation method
     */
    public BigDecimal getLtcgExemptionBenefit() {
        BigDecimal exemptionLimit = BigDecimal.valueOf(100000);
        return longTermCapitalGains.min(exemptionLimit);
    }

    /**
     * Get taxable LTCG (after ₹1 lakh exemption)
     * Rule #3: Functional calculation method
     */
    public BigDecimal getTaxableLongTermGains() {
        BigDecimal exemptionLimit = BigDecimal.valueOf(100000);
        return longTermCapitalGains.subtract(exemptionLimit).max(BigDecimal.ZERO);
    }
}
