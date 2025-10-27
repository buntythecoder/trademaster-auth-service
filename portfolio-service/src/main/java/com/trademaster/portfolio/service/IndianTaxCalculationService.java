package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.TaxCalculationRequest;
import com.trademaster.portfolio.dto.TaxReport;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Indian Tax Calculation Service Interface
 *
 * Provides comprehensive tax calculations for Indian markets:
 * - STCG (Short Term Capital Gains): 15% for equity held <1 year
 * - LTCG (Long Term Capital Gains): 10% for gains >₹1 lakh for equity held ≥1 year
 * - STT (Securities Transaction Tax): 0.025% for equity delivery, 0.1% for options
 *
 * Performance Targets:
 * - Tax calculation: <50ms
 * - Tax report generation: <200ms
 *
 * Rule #2: Interface Segregation - Focused interface for tax calculations
 * Rule #12: Virtual Threads - All async operations use CompletableFuture
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface IndianTaxCalculationService {

    /**
     * Calculate tax impact for a proposed trade
     *
     * @param request Tax calculation request
     * @return Tax impact including STCG/LTCG and STT
     */
    CompletableFuture<TaxImpact> calculateTaxImpact(TaxCalculationRequest request);

    /**
     * Generate comprehensive tax report for portfolio
     *
     * @param portfolioId Portfolio identifier
     * @param financialYear Financial year (e.g., "FY2024-25")
     * @return Complete tax report with STCG, LTCG, STT details
     */
    CompletableFuture<TaxReport> generateTaxReport(Long portfolioId, String financialYear);

    /**
     * Calculate tax on realized gains for a position sale
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Symbol being sold
     * @param quantity Quantity being sold
     * @param sellPrice Sell price per unit
     * @return Tax amount to be paid
     */
    CompletableFuture<BigDecimal> calculateRealizedGainsTax(
        Long portfolioId,
        String symbol,
        Integer quantity,
        BigDecimal sellPrice
    );

    /**
     * Tax impact result for a trade
     */
    record TaxImpact(
        String symbol,
        BigDecimal grossPnL,
        BigDecimal capitalGain,
        String taxCategory, // STCG or LTCG
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal sttAmount,
        BigDecimal totalTax,
        BigDecimal netPnL,
        String explanation
    ) {
        /**
         * Check if trade results in a loss
         * Rule #3: Functional predicate method
         */
        public boolean isLoss() {
            return grossPnL.compareTo(BigDecimal.ZERO) < 0;
        }

        /**
         * Check if this is LTCG
         * Rule #3: Functional predicate method
         */
        public boolean isLongTerm() {
            return "LTCG".equals(taxCategory);
        }
    }
}
