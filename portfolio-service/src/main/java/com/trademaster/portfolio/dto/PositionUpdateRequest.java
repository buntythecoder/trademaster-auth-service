package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Position Update Request DTO
 * 
 * Data transfer object for position update requests.
 * Used for manual position adjustments and corrections.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PositionUpdateRequest(
    String symbol,
    Integer newQuantity,
    BigDecimal newAverageCost,
    BigDecimal newCurrentPrice,
    String updateReason,
    Long adminUserId,
    Instant updateTime
) {
    public PositionUpdateRequest {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (newQuantity == null) {
            throw new IllegalArgumentException("New quantity cannot be null");
        }
        if (updateReason == null || updateReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Update reason cannot be null or empty");
        }
        if (adminUserId == null) {
            throw new IllegalArgumentException("Admin user ID cannot be null");
        }
        if (updateTime == null) {
            updateTime = Instant.now();
        }
    }
}