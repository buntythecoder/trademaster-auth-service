package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Rolling Performance Data
 * 
 * Represents performance metrics calculated on a rolling basis.
 */
public record RollingPerformanceData(
    Instant date,
    String window, // e.g., "30D", "90D", "1Y"
    BigDecimal rollingReturn,
    BigDecimal rollingVolatility,
    BigDecimal rollingSharpeRatio,
    BigDecimal rollingMaxDrawdown,
    BigDecimal rollingBeta,
    BigDecimal rollingAlpha
) {}