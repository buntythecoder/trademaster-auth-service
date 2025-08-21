package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Alert DTO
 * 
 * Data transfer object representing risk alerts and warnings for portfolios.
 * Used for real-time risk monitoring and alerting systems.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RiskAlert(
    String alertId,
    Long portfolioId,
    String alertType,
    String severity,
    String title,
    String description,
    BigDecimal currentValue,
    BigDecimal thresholdValue,
    BigDecimal deviationPercent,
    String symbol,
    String sector,
    Instant alertTime,
    Instant expirationTime,
    boolean acknowledged,
    String recommendedAction
) {
    public RiskAlert {
        if (alertId == null || alertId.trim().isEmpty()) {
            throw new IllegalArgumentException("Alert ID cannot be null or empty");
        }
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (alertType == null || alertType.trim().isEmpty()) {
            throw new IllegalArgumentException("Alert type cannot be null or empty");
        }
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity cannot be null or empty");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (alertTime == null) {
            alertTime = Instant.now();
        }
    }
}