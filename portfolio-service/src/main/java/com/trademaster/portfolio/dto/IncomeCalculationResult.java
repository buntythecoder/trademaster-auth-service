package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Income Calculation Result DTO
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public record IncomeCalculationResult(
    BigDecimal totalDividends,
    BigDecimal totalInterest,
    BigDecimal totalIncome,
    Integer dividendPayments,
    Integer interestPayments,
    List<SymbolIncomeBreakdown> symbolBreakdown
) {}
