package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Assessment Request DTO
 * 
 * Data transfer object for requesting risk assessment of proposed trades.
 * Contains all necessary information to evaluate trade risk impact.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RiskAssessmentRequest(
    String symbol,
    String exchange,
    TransactionType transactionType,
    Integer quantity,
    BigDecimal price,
    String instrumentType,
    String orderType,
    String timeInForce,
    BigDecimal estimatedCommission,
    boolean marginTrade,
    String riskOverride,
    String requestId,
    Instant requestTime
) {
    public RiskAssessmentRequest {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (quantity == null || quantity == 0) {
            throw new IllegalArgumentException("Quantity cannot be null or zero");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        // Set default values for optional fields
        if (exchange == null) exchange = "UNKNOWN";
        if (instrumentType == null) instrumentType = "EQUITY";
        if (orderType == null) orderType = "MARKET";
        if (timeInForce == null) timeInForce = "DAY";
        if (estimatedCommission == null) estimatedCommission = BigDecimal.ZERO;
        if (requestTime == null) requestTime = Instant.now();
        if (requestId == null) requestId = java.util.UUID.randomUUID().toString();
    }
}