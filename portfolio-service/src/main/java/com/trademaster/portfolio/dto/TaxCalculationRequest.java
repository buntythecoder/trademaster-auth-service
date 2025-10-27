package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tax Calculation Request DTO
 *
 * Request for calculating tax impact of a trade or position sale.
 * Supports Indian tax regulations (STCG, LTCG, STT).
 *
 * Rule #9: Immutable Records for Data Transfer Objects
 * Rule #3: Functional Programming - Immutable data structures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TaxCalculationRequest(
    String symbol,
    Integer quantity,
    BigDecimal sellPrice,
    BigDecimal purchasePrice,
    Instant purchaseDate,
    String assetType, // EQUITY, DEBT, COMMODITY
    String transactionType // DELIVERY, INTRADAY, OPTIONS, FUTURES
) {

    /**
     * Compact constructor with validation
     * Rule #9: Validation in record compact constructors
     */
    public TaxCalculationRequest {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (sellPrice == null || sellPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sell price cannot be negative");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Purchase date cannot be null");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("Asset type cannot be null");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
    }

    /**
     * Calculate holding period in days
     * Rule #3: Functional calculation method
     */
    public int getHoldingDays() {
        return (int) java.time.Duration.between(purchaseDate, Instant.now()).toDays();
    }

    /**
     * Check if this is long-term holding (≥365 days for equity)
     * Rule #3: Functional predicate method
     */
    public boolean isLongTerm() {
        return getHoldingDays() >= 365;
    }

    /**
     * Calculate gross profit/loss
     * Rule #3: Functional calculation method
     */
    public BigDecimal calculateGrossPnL() {
        return sellPrice.subtract(purchasePrice).multiply(BigDecimal.valueOf(quantity));
    }
}
