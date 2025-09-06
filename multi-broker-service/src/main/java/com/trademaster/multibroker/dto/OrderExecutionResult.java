package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order Execution Result DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Represents the result of order execution with comprehensive
 * execution details including fills, status, and broker information.
 */
@Builder
@Jacksonized
public record OrderExecutionResult(
    String orderId,
    String symbol,
    int quantity,
    BigDecimal executionPrice,
    BigDecimal totalValue,
    BrokerType broker,
    String status,
    String message,
    Instant executionTime,
    boolean success
) {
    
    public OrderExecutionResult {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (executionTime == null) {
            throw new IllegalArgumentException("Execution time cannot be null");
        }
    }
    
    /**
     * Create successful execution result
     */
    public static OrderExecutionResult success(String orderId, String symbol, int quantity, 
                                             BigDecimal executionPrice, BigDecimal totalValue,
                                             BrokerType broker, String status, Instant executionTime) {
        return OrderExecutionResult.builder()
            .orderId(orderId)
            .symbol(symbol)
            .quantity(quantity)
            .executionPrice(executionPrice)
            .totalValue(totalValue)
            .broker(broker)
            .status(status)
            .message("Order executed successfully")
            .executionTime(executionTime)
            .success(true)
            .build();
    }
    
    /**
     * Create pending execution result
     */
    public static OrderExecutionResult pending(String orderId, String symbol, int quantity,
                                             BigDecimal price, BrokerType broker, String status,
                                             Instant executionTime) {
        return OrderExecutionResult.builder()
            .orderId(orderId)
            .symbol(symbol)
            .quantity(quantity)
            .executionPrice(price)
            .totalValue(price != null ? price.multiply(BigDecimal.valueOf(quantity)) : null)
            .broker(broker)
            .status(status)
            .message("Order placed and pending execution")
            .executionTime(executionTime)
            .success(true)
            .build();
    }
    
    /**
     * Create failure execution result
     */
    public static OrderExecutionResult failure(String symbol, BrokerType broker, 
                                             String errorMessage, Instant executionTime) {
        return OrderExecutionResult.builder()
            .orderId(null)
            .symbol(symbol)
            .quantity(0)
            .executionPrice(null)
            .totalValue(null)
            .broker(broker)
            .status("FAILED")
            .message(errorMessage)
            .executionTime(executionTime)
            .success(false)
            .build();
    }
    
    /**
     * Check if order was executed
     */
    public boolean isExecuted() {
        return success && "EXECUTED".equals(status);
    }
    
    /**
     * Check if order is pending
     */
    public boolean isPending() {
        return success && "PENDING".equals(status);
    }
    
    /**
     * Check if order failed
     */
    public boolean isFailed() {
        return !success;
    }
}