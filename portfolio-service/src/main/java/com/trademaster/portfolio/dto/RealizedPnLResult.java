package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.CostBasisMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Realized P&L Result DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record RealizedPnLResult(
    BigDecimal realizedPnl,
    BigDecimal averageCostBasis,
    BigDecimal netProceeds,
    Integer sharesTraded,
    CostBasisMethod methodUsed,
    List<TaxLotInfo> taxLotsUsed,
    Instant calculationTime
) {}
