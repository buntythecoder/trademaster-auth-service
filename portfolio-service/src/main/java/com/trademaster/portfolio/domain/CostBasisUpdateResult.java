package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cost Basis Update Result after trade execution
 * 
 * @param symbol Security symbol
 * @param previousQuantity Previous position quantity
 * @param tradeQuantity Trade quantity (positive for buy, negative for sell)
 * @param newQuantity New position quantity after trade
 * @param previousAverageCost Previous average cost per share
 * @param tradePrice Price of the executed trade
 * @param newAverageCost New average cost per share
 * @param totalCostBasis New total cost basis
 * @param realizedPnL Realized PnL from this trade (if any)
 * @param unrealizedPnL New unrealized PnL after trade
 * @param updateMethod Method used for cost basis calculation (FIFO, LIFO, Average)
 * @param updateDate Date of the update
 * @param lotDetails Details of lot tracking if applicable
 */
public record CostBasisUpdateResult(
    String symbol,
    Integer previousQuantity,
    Integer tradeQuantity,
    Integer newQuantity,
    BigDecimal previousAverageCost,
    BigDecimal tradePrice,
    BigDecimal newAverageCost,
    BigDecimal totalCostBasis,
    BigDecimal realizedPnL,
    BigDecimal unrealizedPnL,
    String updateMethod,
    Instant updateDate,
    LotTrackingDetails lotDetails
) {
    
    public record LotTrackingDetails(
        Integer lotsUpdated,
        BigDecimal totalRealizedGains,
        BigDecimal totalRealizedLosses,
        String taxLotMethod
    ) {}
}