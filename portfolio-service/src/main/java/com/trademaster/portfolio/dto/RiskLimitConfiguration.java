package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Risk Limit Configuration DTO
 * 
 * Data transfer object containing risk limit configuration for portfolios.
 * Defines thresholds and limits for various risk metrics and controls.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RiskLimitConfiguration(
    Long portfolioId,
    BigDecimal maxSinglePositionPercent,
    BigDecimal maxSectorConcentrationPercent,
    BigDecimal maxLeverageRatio,
    BigDecimal dailyLossLimit,
    BigDecimal maxDrawdownPercent,
    BigDecimal var95Limit,
    BigDecimal var99Limit,
    Integer maxDayTrades,
    BigDecimal marginCallThreshold,
    BigDecimal marginMaintenanceRatio,
    Map<String, BigDecimal> sectorLimits,
    Map<String, BigDecimal> instrumentTypeLimits,
    Map<String, BigDecimal> exchangeLimits,
    boolean autoLiquidationEnabled,
    boolean alertsEnabled,
    String riskFramework,
    Instant effectiveDate,
    Instant lastModified,
    Long modifiedBy
) {
    public RiskLimitConfiguration {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        
        // Set default values for required fields
        if (maxSinglePositionPercent == null) {
            maxSinglePositionPercent = new BigDecimal("20.00"); // 20% default
        }
        if (maxSectorConcentrationPercent == null) {
            maxSectorConcentrationPercent = new BigDecimal("30.00"); // 30% default
        }
        if (maxLeverageRatio == null) {
            maxLeverageRatio = new BigDecimal("2.00"); // 2:1 default
        }
        if (maxDrawdownPercent == null) {
            maxDrawdownPercent = new BigDecimal("25.00"); // 25% default
        }
        if (maxDayTrades == null) {
            maxDayTrades = 3; // PDT rule default
        }
        if (marginCallThreshold == null) {
            marginCallThreshold = new BigDecimal("30.00"); // 30% default
        }
        if (marginMaintenanceRatio == null) {
            marginMaintenanceRatio = new BigDecimal("25.00"); // 25% default
        }
        if (riskFramework == null) {
            riskFramework = "STANDARD";
        }
        if (effectiveDate == null) {
            effectiveDate = Instant.now();
        }
        if (lastModified == null) {
            lastModified = Instant.now();
        }
    }
}