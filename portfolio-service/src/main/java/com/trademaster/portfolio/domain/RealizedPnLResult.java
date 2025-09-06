package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Realized PnL calculation result for specific positions
 * 
 * @param portfolioId Portfolio identifier
 * @param symbol Security symbol
 * @param totalRealizedPnL Total realized profit/loss
 * @param realizedGains Total realized gains
 * @param realizedLosses Total realized losses
 * @param shortTermGains Short-term capital gains
 * @param longTermGains Long-term capital gains
 * @param taxImplications Tax-related information
 * @param transactions List of realized transactions
 * @param calculationDate Date of calculation
 * @param averageBuyPrice Average buy price
 * @param averageSellPrice Average sell price
 */
public record RealizedPnLResult(
    Long portfolioId,
    String symbol,
    BigDecimal totalRealizedPnL,
    BigDecimal realizedGains,
    BigDecimal realizedLosses,
    BigDecimal shortTermGains,
    BigDecimal longTermGains,
    TaxImplications taxImplications,
    List<RealizedTransaction> transactions,
    Instant calculationDate,
    BigDecimal averageBuyPrice,
    BigDecimal averageSellPrice
) {
    
    public record TaxImplications(
        BigDecimal shortTermTaxLiability,
        BigDecimal longTermTaxLiability,
        BigDecimal totalTaxLiability,
        String taxYear
    ) {}
    
    public record RealizedTransaction(
        Instant transactionDate,
        String transactionType,
        Integer quantity,
        BigDecimal price,
        BigDecimal pnl,
        Integer daysHeld,
        String gainType
    ) {}
}