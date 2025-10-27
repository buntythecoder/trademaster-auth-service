package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Symbol Trading Statistics DTO
 *
 * Immutable record representing trading statistics for a specific symbol.
 * Used by TransactionService for symbol-level trading analysis.
 *
 * Rule #9: Immutable Records for DTOs
 * Rule #3: Functional Programming with validation
 *
 * @param symbol Stock symbol
 * @param totalTrades Total number of trades (buy + sell)
 * @param sharesBought Total shares bought
 * @param sharesSold Total shares sold
 * @param buyVolume Total buy volume in currency
 * @param sellVolume Total sell volume in currency
 * @param realizedPnL Realized profit/loss
 * @param averagePrice Average trade price
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record SymbolTradingStats(
    String symbol,
    Integer totalTrades,
    Integer sharesBought,
    Integer sharesSold,
    BigDecimal buyVolume,
    BigDecimal sellVolume,
    BigDecimal realizedPnL,
    BigDecimal averagePrice
) {
    /**
     * Compact constructor with validation
     */
    public SymbolTradingStats {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (totalTrades == null || totalTrades < 0) {
            throw new IllegalArgumentException("Total trades cannot be negative");
        }
        if (sharesBought == null) sharesBought = 0;
        if (sharesSold == null) sharesSold = 0;
        if (buyVolume == null) buyVolume = BigDecimal.ZERO;
        if (sellVolume == null) sellVolume = BigDecimal.ZERO;
        if (realizedPnL == null) realizedPnL = BigDecimal.ZERO;
        if (averagePrice == null) averagePrice = BigDecimal.ZERO;
    }

    /**
     * Calculate net position (shares bought - shares sold)
     *
     * @return Net position
     */
    public Integer netPosition() {
        return sharesBought - sharesSold;
    }

    /**
     * Calculate total trading volume
     *
     * @return Total buy + sell volume
     */
    public BigDecimal totalVolume() {
        return buyVolume.add(sellVolume);
    }
}
