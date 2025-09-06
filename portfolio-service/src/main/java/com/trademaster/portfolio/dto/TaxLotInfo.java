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
    Boolean isLongTerm,
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
        // Note: Can't modify parameters in compact constructor
        // These calculations should be done in factory methods or builder
        if (holdingDays == null) {
            throw new IllegalArgumentException("Holding days must be calculated and provided");
        }
        if (isLongTerm == null) {
            throw new IllegalArgumentException("Long term status must be calculated and provided");
        }
        if (taxStatus == null) {
            throw new IllegalArgumentException("Tax status must be calculated and provided");
        }
    }
    
    /**
     * Factory method to create TaxLotInfo with calculated fields
     */
    public static TaxLotInfo create(
            Long lotId,
            String symbol,
            Instant purchaseDate,
            Integer originalQuantity,
            Integer remainingQuantity,
            BigDecimal costBasisPerShare,
            BigDecimal totalCostBasis,
            BigDecimal currentPrice,
            BigDecimal unrealizedPnl) {
        
        // Calculate holding days
        int holdingDays = (int) java.time.Duration.between(purchaseDate, Instant.now()).toDays();
        
        // Calculate long term status
        boolean isLongTerm = holdingDays >= 365;
        
        // Calculate tax status
        String taxStatus = isLongTerm ? "LONG_TERM" : "SHORT_TERM";
        
        return new TaxLotInfo(
            lotId,
            symbol,
            purchaseDate,
            originalQuantity,
            remainingQuantity,
            costBasisPerShare,
            totalCostBasis,
            currentPrice,
            unrealizedPnl,
            holdingDays,
            isLongTerm,
            taxStatus
        );
    }
}