package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tax Lot Information DTO
 * 
 * Data transfer object containing tax lot information for cost basis calculations.
 * Used for FIFO, LIFO, and tax optimization strategies.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TaxLotInfo(
    Long lotId,
    String symbol,
    Instant purchaseDate,
    Integer originalQuantity,
    Integer remainingQuantity,
    BigDecimal costBasisPerShare,
    BigDecimal totalCostBasis,
    BigDecimal currentPrice,
    BigDecimal unrealizedPnl,
    Integer holdingDays,
    boolean isLongTerm,
    String taxStatus
) {
    public TaxLotInfo {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Purchase date cannot be null");
        }
        if (originalQuantity == null || originalQuantity <= 0) {
            throw new IllegalArgumentException("Original quantity must be positive");
        }
        if (remainingQuantity == null || remainingQuantity < 0) {
            throw new IllegalArgumentException("Remaining quantity cannot be negative");
        }
        if (costBasisPerShare == null || costBasisPerShare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost basis per share cannot be negative");
        }
        
        // Calculate derived fields
        if (holdingDays == null) {
            holdingDays = (int) java.time.Duration.between(purchaseDate, Instant.now()).toDays();
        }
        if (isLongTerm == null) {
            isLongTerm = holdingDays >= 365;
        }
        if (taxStatus == null) {
            taxStatus = isLongTerm ? "LONG_TERM" : "SHORT_TERM";
        }
    }
}