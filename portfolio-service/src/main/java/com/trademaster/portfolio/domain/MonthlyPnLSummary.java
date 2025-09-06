package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Monthly PnL Summary for reporting and analysis
 * 
 * @param portfolioId Portfolio identifier
 * @param yearMonth Year and month for this summary
 * @param openingValue Portfolio value at start of month
 * @param closingValue Portfolio value at end of month
 * @param monthlyReturn Monthly return percentage
 * @param realizedPnL Realized PnL for the month
 * @param unrealizedPnL Unrealized PnL at month end
 * @param totalPnL Total PnL for the month
 * @param netDeposits Net deposits/withdrawals during month
 * @param tradingDays Number of trading days in month
 * @param averageDailyPnL Average daily PnL
 * @param bestDay Best performing day
 * @param worstDay Worst performing day
 * @param winningDays Number of positive PnL days
 * @param losingDays Number of negative PnL days
 * @param topPerformers Top performing positions
 * @param topLosers Worst performing positions
 * @param monthlyBeta Monthly beta vs benchmark
 * @param sharpeRatio Monthly Sharpe ratio
 */
public record MonthlyPnLSummary(
    Long portfolioId,
    YearMonth yearMonth,
    BigDecimal openingValue,
    BigDecimal closingValue,
    BigDecimal monthlyReturn,
    BigDecimal realizedPnL,
    BigDecimal unrealizedPnL,
    BigDecimal totalPnL,
    BigDecimal netDeposits,
    Integer tradingDays,
    BigDecimal averageDailyPnL,
    DailyPerformance bestDay,
    DailyPerformance worstDay,
    Integer winningDays,
    Integer losingDays,
    List<PositionPerformance> topPerformers,
    List<PositionPerformance> topLosers,
    BigDecimal monthlyBeta,
    BigDecimal sharpeRatio
) {
    
    public record DailyPerformance(
        String date,
        BigDecimal pnl,
        BigDecimal returnPercent
    ) {}
    
    public record PositionPerformance(
        String symbol,
        BigDecimal pnl,
        BigDecimal returnPercent,
        BigDecimal contribution
    ) {}
}