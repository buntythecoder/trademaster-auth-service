package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Sector Attribution DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record SectorAttribution(
    String sector,
    BigDecimal allocation,
    BigDecimal securityReturn,
    BigDecimal benchmarkReturn,
    BigDecimal activeReturn,
    BigDecimal contribution
) {}
