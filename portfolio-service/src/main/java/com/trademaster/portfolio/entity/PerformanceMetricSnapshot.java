package com.trademaster.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Performance Metric Snapshot Entity
 *
 * Represents a point-in-time snapshot of portfolio performance metrics.
 * Used for time-series analysis, historical tracking, and performance visualization.
 *
 * Optimized for time-series queries with Java 24 Virtual Threads.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "performance_metrics", indexes = {
    @Index(name = "idx_performance_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_performance_snapshot_date", columnList = "snapshot_date"),
    @Index(name = "idx_performance_period_type", columnList = "period_type"),
    @Index(name = "idx_performance_portfolio_date", columnList = "portfolio_id, snapshot_date"),
    @Index(name = "idx_performance_portfolio_period_snapshot", columnList = "portfolio_id, period_type, snapshot_date", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "metricId")
@ToString(exclude = "portfolio")
public class PerformanceMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long metricId;

    @Column(name = "portfolio_id", nullable = false, insertable = false, updatable = false)
    private Long portfolioId;

    @Column(name = "snapshot_date", nullable = false)
    private Instant snapshotDate;

    @Column(name = "period_type", length = 20, nullable = false)
    @Builder.Default
    private String periodType = "DAILY";

    // Return metrics
    @Column(name = "total_return", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalReturn = BigDecimal.ZERO;

    @Column(name = "total_return_percent", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalReturnPercent = BigDecimal.ZERO;

    @Column(name = "annualized_return", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal annualizedReturn = BigDecimal.ZERO;

    // Risk metrics
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    @Column(name = "alpha", precision = 10, scale = 4)
    private BigDecimal alpha;

    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta;

    // Drawdown metrics
    @Column(name = "max_drawdown", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    @Column(name = "max_drawdown_percent", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal maxDrawdownPercent = BigDecimal.ZERO;

    @Column(name = "current_drawdown", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal currentDrawdown = BigDecimal.ZERO;

    @Column(name = "current_drawdown_percent", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal currentDrawdownPercent = BigDecimal.ZERO;

    // Win/Loss metrics
    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "avg_win", precision = 19, scale = 4)
    private BigDecimal avgWin;

    @Column(name = "avg_loss", precision = 19, scale = 4)
    private BigDecimal avgLoss;

    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    // Volatility metrics
    @Column(name = "volatility", precision = 10, scale = 4)
    private BigDecimal volatility;

    @Column(name = "downside_volatility", precision = 10, scale = 4)
    private BigDecimal downsideVolatility;

    @Column(name = "var_95", precision = 19, scale = 4)
    private BigDecimal var95;

    @Column(name = "var_99", precision = 19, scale = 4)
    private BigDecimal var99;

    // Portfolio values
    @Column(name = "portfolio_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal portfolioValue;

    @Column(name = "cash_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal cashBalance;

    @Column(name = "positions_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal positionsValue;

    // P&L metrics
    @Column(name = "realized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "day_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal dayPnl = BigDecimal.ZERO;

    // Position metrics
    @Column(name = "position_count", nullable = false)
    @Builder.Default
    private Integer positionCount = 0;

    @Column(name = "open_position_count", nullable = false)
    @Builder.Default
    private Integer openPositionCount = 0;

    @Column(name = "avg_position_size", precision = 19, scale = 4)
    private BigDecimal avgPositionSize;

    @Column(name = "largest_position_value", precision = 19, scale = 4)
    private BigDecimal largestPositionValue;

    // Trade metrics
    @Column(name = "trade_count", nullable = false)
    @Builder.Default
    private Integer tradeCount = 0;

    @Column(name = "winning_trades", nullable = false)
    @Builder.Default
    private Integer winningTrades = 0;

    @Column(name = "losing_trades", nullable = false)
    @Builder.Default
    private Integer losingTrades = 0;

    // Benchmark comparison
    @Column(name = "benchmark_symbol", length = 20)
    private String benchmarkSymbol;

    @Column(name = "benchmark_return", precision = 10, scale = 4)
    private BigDecimal benchmarkReturn;

    @Column(name = "relative_return", precision = 10, scale = 4)
    private BigDecimal relativeReturn;

    // Metadata
    @Column(name = "calculation_method", length = 50)
    @Builder.Default
    private String calculationMethod = "TIME_WEIGHTED";

    @Column(name = "calculation_timestamp", nullable = false)
    @Builder.Default
    private Instant calculationTimestamp = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // Business methods

    /**
     * Get total P&L (realized + unrealized)
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }

    /**
     * Check if portfolio is profitable
     */
    public boolean isProfitable() {
        return getTotalPnl().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get risk-adjusted return (return per unit of risk)
     */
    public BigDecimal getRiskAdjustedReturn() {
        if (volatility == null || volatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return annualizedReturn.divide(volatility, 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Check if outperforming benchmark
     */
    public boolean isOutperformingBenchmark() {
        return relativeReturn != null && relativeReturn.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get performance rating based on Sharpe ratio
     */
    public String getPerformanceRating() {
        if (sharpeRatio == null) return "NOT_RATED";

        if (sharpeRatio.compareTo(new BigDecimal("2.0")) > 0) return "EXCELLENT";
        if (sharpeRatio.compareTo(new BigDecimal("1.0")) > 0) return "GOOD";
        if (sharpeRatio.compareTo(new BigDecimal("0.5")) > 0) return "FAIR";
        if (sharpeRatio.compareTo(BigDecimal.ZERO) > 0) return "POOR";
        return "VERY_POOR";
    }

    /**
     * Get risk level based on volatility
     */
    public String getRiskLevel() {
        if (volatility == null) return "UNKNOWN";

        if (volatility.compareTo(new BigDecimal("30.0")) > 0) return "VERY_HIGH";
        if (volatility.compareTo(new BigDecimal("20.0")) > 0) return "HIGH";
        if (volatility.compareTo(new BigDecimal("15.0")) > 0) return "MODERATE";
        if (volatility.compareTo(new BigDecimal("10.0")) > 0) return "LOW";
        return "VERY_LOW";
    }

    /**
     * Check if snapshot is recent (within last hour)
     */
    public boolean isRecent() {
        return snapshotDate.isAfter(Instant.now().minusSeconds(3600));
    }

    /**
     * Check if this is a daily snapshot
     */
    public boolean isDailySnapshot() {
        return "DAILY".equalsIgnoreCase(periodType);
    }

    /**
     * Check if this is an intraday snapshot
     */
    public boolean isIntradaySnapshot() {
        return "INTRADAY".equalsIgnoreCase(periodType);
    }
}
