package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trade Execution Request DTO
 * 
 * Data transfer object containing trade execution details for position updates.
 * Used when trade orders are executed and positions need to be updated.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record TradeExecutionRequest(
    Long orderId,
    String tradeId,
    String symbol,
    String exchange,
    TransactionType transactionType,
    Integer quantity,
    BigDecimal price,
    BigDecimal commission,
    BigDecimal tax,
    BigDecimal otherFees,
    Instant executedAt,
    String instrumentType
) {
    public TradeExecutionRequest {
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
        if (executedAt == null) {
            throw new IllegalArgumentException("Execution time cannot be null");
        }
        
        // Set default values for optional fields
        if (commission == null) commission = BigDecimal.ZERO;
        if (tax == null) tax = BigDecimal.ZERO;
        if (otherFees == null) otherFees = BigDecimal.ZERO;
        if (instrumentType == null) instrumentType = "EQUITY";
        if (exchange == null) exchange = "UNKNOWN";
    }
}