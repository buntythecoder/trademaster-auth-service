package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Order Response DTO
 * 
 * Data Transfer Object for order information responses.
 * Optimized for efficient serialization and minimal bandwidth usage.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class OrderResponse {
    
    /**
     * Internal order ID
     */
    private Long id;
    
    /**
     * External order identifier
     */
    private String orderId;
    
    /**
     * User ID who placed the order
     */
    private Long userId;
    
    /**
     * Trading symbol
     */
    private String symbol;
    
    /**
     * Exchange
     */
    private String exchange;
    
    /**
     * Order type
     */
    private OrderType orderType;
    
    /**
     * Order side
     */
    private OrderSide side;
    
    /**
     * Order quantity
     */
    private Integer quantity;
    
    /**
     * Limit price (if applicable)
     */
    private BigDecimal limitPrice;
    
    /**
     * Stop price (if applicable)
     */
    private BigDecimal stopPrice;
    
    /**
     * Time in force
     */
    private TimeInForce timeInForce;
    
    /**
     * Expiry date (for GTD orders)
     */
    private LocalDate expiryDate;
    
    /**
     * Current order status
     */
    private OrderStatus status;
    
    /**
     * Broker order ID
     */
    private String brokerOrderId;
    
    /**
     * Broker name
     */
    private String brokerName;
    
    /**
     * Quantity filled
     */
    private Integer filledQuantity;
    
    /**
     * Remaining quantity to be filled
     */
    private Integer remainingQuantity;
    
    /**
     * Average fill price
     */
    private BigDecimal averagePrice;
    
    /**
     * Fill percentage
     */
    private Double fillPercentage;
    
    /**
     * Total order value
     */
    private BigDecimal orderValue;
    
    /**
     * Executed value
     */
    private BigDecimal executedValue;
    
    /**
     * Rejection reason (if applicable)
     */
    private String rejectionReason;
    
    /**
     * Order creation timestamp
     */
    private Instant createdAt;
    
    /**
     * Last modification timestamp
     */
    private Instant updatedAt;
    
    /**
     * Order submission timestamp
     */
    private Instant submittedAt;
    
    /**
     * Order execution timestamp
     */
    private Instant executedAt;
    
    /**
     * Order summary for quick display
     */
    public String getOrderSummary() {
        return String.format("%s %d %s @ %s", 
                           side.getDisplayName(),
                           quantity,
                           symbol,
                           orderType == OrderType.MARKET ? "MARKET" : 
                           (limitPrice != null ? limitPrice.toString() : "N/A"));
    }
    
    /**
     * Check if order is in terminal state
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
    
    /**
     * Check if order is actively trading
     */
    public boolean isActive() {
        return status.isActive();
    }
    
    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return status.isCancellable();
    }
    
    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return status.isModifiable();
    }
}