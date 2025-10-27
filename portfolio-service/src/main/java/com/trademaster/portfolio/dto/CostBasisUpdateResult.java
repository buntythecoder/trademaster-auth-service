package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.CostBasisMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cost Basis Update Result DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record CostBasisUpdateResult(
    BigDecimal newAverageCost,
    BigDecimal newTotalCost,
    Integer newQuantity,
    BigDecimal realizedPnl,
    CostBasisMethod methodUsed,
    List<TaxLotInfo> updatedTaxLots
) {}
