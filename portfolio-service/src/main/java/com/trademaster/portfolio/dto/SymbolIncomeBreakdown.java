package com.trademaster.portfolio.dto;

import java.math.BigDecimal;

/**
 * Symbol Income Breakdown DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record SymbolIncomeBreakdown(
    String symbol,
    BigDecimal dividends,
    BigDecimal interest,
    Integer payments,
    BigDecimal yield
) {}
