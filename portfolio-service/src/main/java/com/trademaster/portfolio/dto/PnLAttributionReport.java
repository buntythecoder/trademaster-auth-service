package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * P&L Attribution Report DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PnLAttributionReport(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalPnl,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal income,
    BigDecimal fees,
    List<PositionPnLMetrics> positionBreakdown,
    List<SectorAttribution> sectorBreakdown,
    PerformanceAttribution attribution,
    Instant generatedAt
) {}
