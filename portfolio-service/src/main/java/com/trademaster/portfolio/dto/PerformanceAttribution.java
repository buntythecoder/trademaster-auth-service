package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Performance Attribution DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record PerformanceAttribution(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalReturn,
    BigDecimal securitySelection,
    BigDecimal assetAllocation,
    BigDecimal timingEffect,
    BigDecimal interactionEffect,
    List<SectorAttribution> sectorBreakdown,
    Instant calculatedAt
) {}
