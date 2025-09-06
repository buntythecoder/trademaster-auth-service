package com.trademaster.multibroker.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * Order Request DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Represents an order request for routing to brokers with comprehensive
 * order details including symbol, quantity, pricing, and execution parameters.
 */
@Builder
@Jacksonized
public record OrderRequest(
    String symbol,
    int quantity,
    OrderType orderType,
    OrderSide side,
    BigDecimal price,      // For limit orders
    BigDecimal stopPrice,  // For stop loss orders
    BigDecimal targetPrice, // For bracket orders
    String userId,
    String clientId,
    OrderValidity validity,
    String tag // Optional order tag
) {
    
    public OrderRequest {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("Order type cannot be null");
        }
        if (side == null) {
            throw new IllegalArgumentException("Order side cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Validate order type specific requirements
        validateOrderTypeRequirements(orderType, price, stopPrice);
    }
    
    private static void validateOrderTypeRequirements(OrderType orderType, BigDecimal price, BigDecimal stopPrice) {
        switch (orderType) {
            case LIMIT -> {
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Limit orders require a valid price");
                }
            }
            case STOP_LOSS -> {
                if (stopPrice == null || stopPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Stop loss orders require a valid stop price");
                }
            }
            case BRACKET -> {
                if (price == null || stopPrice == null) {
                    throw new IllegalArgumentException("Bracket orders require both price and stop price");
                }
            }
            case MARKET -> {
                // No additional validation for market orders
            }
        }
    }
    
    /**
     * Create market order
     */
    public static OrderRequest createMarketOrder(String symbol, int quantity, OrderSide side, String userId) {
        return OrderRequest.builder()
            .symbol(symbol)
            .quantity(quantity)
            .orderType(OrderType.MARKET)
            .side(side)
            .userId(userId)
            .validity(OrderValidity.DAY)
            .build();
    }
    
    /**
     * Create limit order
     */
    public static OrderRequest createLimitOrder(String symbol, int quantity, OrderSide side, 
                                              BigDecimal price, String userId) {
        return OrderRequest.builder()
            .symbol(symbol)
            .quantity(quantity)
            .orderType(OrderType.LIMIT)
            .side(side)
            .price(price)
            .userId(userId)
            .validity(OrderValidity.DAY)
            .build();
    }
    
    /**
     * Create stop loss order
     */
    public static OrderRequest createStopLossOrder(String symbol, int quantity, OrderSide side, 
                                                 BigDecimal stopPrice, String userId) {
        return OrderRequest.builder()
            .symbol(symbol)
            .quantity(quantity)
            .orderType(OrderType.STOP_LOSS)
            .side(side)
            .stopPrice(stopPrice)
            .userId(userId)
            .validity(OrderValidity.DAY)
            .build();
    }
}