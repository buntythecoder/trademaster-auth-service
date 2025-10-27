package com.trademaster.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Limit Entity
 *
 * Represents risk management limits and thresholds for a portfolio.
 * Defines maximum exposure, concentration, leverage, and loss limits.
 *
 * Optimized for high-frequency risk checks with Java 24 Virtual Threads.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "risk_limits", indexes = {
    @Index(name = "idx_risk_limit_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_risk_limit_effective_date", columnList = "effective_date"),
    @Index(name = "idx_risk_limit_risk_framework", columnList = "risk_framework"),
    @Index(name = "idx_risk_limit_portfolio_active", columnList = "portfolio_id, effective_date", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "riskLimitId")
@ToString(exclude = "portfolio")
public class RiskLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_limit_id")
    private Long riskLimitId;

    @Column(name = "portfolio_id", nullable = false, insertable = false, updatable = false)
    private Long portfolioId;

    @Column(name = "max_single_position_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal maxSinglePositionPercent = new BigDecimal("20.00");

    @Column(name = "max_sector_concentration_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal maxSectorConcentrationPercent = new BigDecimal("30.00");

    @Column(name = "max_leverage_ratio", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal maxLeverageRatio = new BigDecimal("2.00");

    @Column(name = "daily_loss_limit", precision = 19, scale = 4)
    private BigDecimal dailyLossLimit;

    @Column(name = "max_drawdown_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal maxDrawdownPercent = new BigDecimal("25.00");

    @Column(name = "var_95_limit", precision = 19, scale = 4)
    private BigDecimal var95Limit;

    @Column(name = "var_99_limit", precision = 19, scale = 4)
    private BigDecimal var99Limit;

    @Column(name = "max_day_trades", nullable = false)
    @Builder.Default
    private Integer maxDayTrades = 3;

    @Column(name = "margin_call_threshold", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal marginCallThreshold = new BigDecimal("30.00");

    @Column(name = "margin_maintenance_ratio", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal marginMaintenanceRatio = new BigDecimal("25.00");

    @Column(name = "sector_limits", columnDefinition = "jsonb")
    private String sectorLimits;

    @Column(name = "instrument_type_limits", columnDefinition = "jsonb")
    private String instrumentTypeLimits;

    @Column(name = "exchange_limits", columnDefinition = "jsonb")
    private String exchangeLimits;

    @Column(name = "auto_liquidation_enabled", nullable = false)
    @Builder.Default
    private Boolean autoLiquidationEnabled = false;

    @Column(name = "alerts_enabled", nullable = false)
    @Builder.Default
    private Boolean alertsEnabled = true;

    @Column(name = "risk_framework", length = 50, nullable = false)
    @Builder.Default
    private String riskFramework = "STANDARD";

    @Column(name = "effective_date", nullable = false)
    @Builder.Default
    private Instant effectiveDate = Instant.now();

    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // Business methods

    /**
     * Check if position size exceeds single position limit
     */
    public boolean exceedsSinglePositionLimit(BigDecimal positionValue, BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal positionPercent = positionValue
            .divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

        return positionPercent.compareTo(maxSinglePositionPercent) > 0;
    }

    /**
     * Check if sector concentration exceeds limit
     */
    public boolean exceedsSectorConcentrationLimit(BigDecimal sectorValue, BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal sectorPercent = sectorValue
            .divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

        return sectorPercent.compareTo(maxSectorConcentrationPercent) > 0;
    }

    /**
     * Check if leverage exceeds maximum
     */
    public boolean exceedsLeverageLimit(BigDecimal totalExposure, BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal currentLeverage = totalExposure.divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP);
        return currentLeverage.compareTo(maxLeverageRatio) > 0;
    }

    /**
     * Check if daily loss exceeds limit
     */
    public boolean exceedsDailyLossLimit(BigDecimal dailyLoss) {
        if (dailyLossLimit == null) {
            return false;
        }

        return dailyLoss.abs().compareTo(dailyLossLimit) > 0;
    }

    /**
     * Check if drawdown exceeds maximum
     */
    public boolean exceedsDrawdownLimit(BigDecimal currentValue, BigDecimal peakValue) {
        if (peakValue.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal drawdownPercent = peakValue.subtract(currentValue)
            .divide(peakValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

        return drawdownPercent.compareTo(maxDrawdownPercent) > 0;
    }

    /**
     * Check if day trades exceed Pattern Day Trader rule limit
     */
    public boolean exceedsDayTradeLimit(Integer currentDayTrades) {
        return currentDayTrades >= maxDayTrades;
    }

    /**
     * Check if margin call threshold is breached
     */
    public boolean triggersMarginCall(BigDecimal equity, BigDecimal marginLoan) {
        if (marginLoan.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal marginRatio = equity
            .divide(marginLoan, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

        return marginRatio.compareTo(marginCallThreshold) < 0;
    }

    /**
     * Check if margin maintenance requirement is met
     */
    public boolean meetsMaintenanceRequirement(BigDecimal equity, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        BigDecimal equityPercent = equity
            .divide(totalValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

        return equityPercent.compareTo(marginMaintenanceRatio) >= 0;
    }

    /**
     * Check if any risk limit is breached
     */
    public boolean hasAnyLimitBreach(
            BigDecimal largestPositionValue,
            BigDecimal largestSectorValue,
            BigDecimal totalExposure,
            BigDecimal portfolioValue,
            BigDecimal dailyLoss,
            BigDecimal peakValue,
            Integer dayTradesCount,
            BigDecimal equity,
            BigDecimal marginLoan) {

        return exceedsSinglePositionLimit(largestPositionValue, portfolioValue)
            || exceedsSectorConcentrationLimit(largestSectorValue, portfolioValue)
            || exceedsLeverageLimit(totalExposure, portfolioValue)
            || exceedsDailyLossLimit(dailyLoss)
            || exceedsDrawdownLimit(portfolioValue, peakValue)
            || exceedsDayTradeLimit(dayTradesCount)
            || triggersMarginCall(equity, marginLoan);
    }

    /**
     * Get risk framework type
     */
    public String getRiskFrameworkType() {
        return riskFramework;
    }

    /**
     * Check if this is a conservative risk framework
     */
    public boolean isConservative() {
        return "CONSERVATIVE".equalsIgnoreCase(riskFramework);
    }

    /**
     * Check if this is an aggressive risk framework
     */
    public boolean isAggressive() {
        return "AGGRESSIVE".equalsIgnoreCase(riskFramework);
    }

    /**
     * Check if auto-liquidation is enabled
     */
    public boolean shouldAutoLiquidate() {
        return Boolean.TRUE.equals(autoLiquidationEnabled);
    }

    /**
     * Check if risk alerts are enabled
     */
    public boolean shouldSendAlerts() {
        return Boolean.TRUE.equals(alertsEnabled);
    }

    // ==================== CONVENIENCE ALIAS METHODS ====================
    // Rule #18: Provide alternative method names for common usage patterns

    /**
     * Alias for getMaxSinglePositionPercent() - for service layer compatibility
     */
    public BigDecimal getMaxPositionSize() {
        return maxSinglePositionPercent;
    }

    /**
     * Alias for getDailyLossLimit() - for service layer compatibility
     */
    public BigDecimal getMaxDailyLoss() {
        return dailyLossLimit;
    }

    /**
     * Alias for getMaxLeverageRatio() - for service layer compatibility
     */
    public BigDecimal getMaxLeverage() {
        return maxLeverageRatio;
    }

    /**
     * Alias for getMaxSectorConcentrationPercent() - for service layer compatibility
     */
    public BigDecimal getMaxSectorConcentration() {
        return maxSectorConcentrationPercent;
    }

    /**
     * Get stop loss percentage - derived from max drawdown
     * Rule #18: Default stop loss as percentage of max drawdown
     */
    public BigDecimal getStopLossPercent() {
        return maxDrawdownPercent;
    }
}
