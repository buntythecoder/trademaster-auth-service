package com.trademaster.trading.service;

import com.trademaster.trading.dto.AdvancedOrderRequest;
import com.trademaster.trading.dto.OrderAnalytics;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderExecutionStrategy;
import com.trademaster.trading.model.OrderValidationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced Order Management Service
 * 
 * Provides sophisticated order management capabilities including:
 * - Advanced order types (Iceberg, Hidden, Bracket, OCO)
 * - Smart execution algorithms (TWAP, VWAP, Implementation Shortfall)
 * - Multi-leg order strategies
 * - Advanced validation and risk controls
 * - Order performance analytics and monitoring
 * 
 * Built for high-frequency trading with Virtual Threads for unlimited scalability.
 * 
 * Performance Targets:
 * - Order validation: <5ms
 * - Execution algorithm selection: <10ms  
 * - Multi-leg coordination: <15ms
 * - Analytics calculation: <20ms
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Java 24 + Virtual Threads)
 */
public interface AdvancedOrderManagementService {
    
    /**
     * Place advanced order with sophisticated execution strategy
     * 
     * @param request Advanced order request with execution parameters
     * @param userId User placing the order
     * @return CompletableFuture<Order> containing placed order with execution plan
     */
    CompletableFuture<Order> placeAdvancedOrder(AdvancedOrderRequest request, Long userId);
    
    /**
     * Create iceberg order that hides large quantity by showing small slices
     * 
     * @param symbol Trading symbol
     * @param totalQuantity Total order quantity
     * @param displayQuantity Quantity to display in order book
     * @param limitPrice Limit price for execution
     * @param userId User ID
     * @return CompletableFuture<Order> iceberg order with hidden quantity
     */
    CompletableFuture<Order> createIcebergOrder(String symbol, Integer totalQuantity, 
                                               Integer displayQuantity, BigDecimal limitPrice, Long userId);
    
    /**
     * Create bracket order with profit target and stop loss
     * 
     * @param symbol Trading symbol
     * @param quantity Order quantity
     * @param entryPrice Entry limit price
     * @param profitTarget Take profit price
     * @param stopLoss Stop loss price
     * @param userId User ID
     * @return CompletableFuture<List<Order>> containing entry, profit, and stop orders
     */
    CompletableFuture<List<Order>> createBracketOrder(String symbol, Integer quantity,
                                                     BigDecimal entryPrice, BigDecimal profitTarget,
                                                     BigDecimal stopLoss, Long userId);
    
    /**
     * Create OCO (One-Cancels-Other) order pair
     * 
     * @param symbol Trading symbol
     * @param quantity Order quantity
     * @param firstPrice First order price
     * @param secondPrice Second order price
     * @param userId User ID
     * @return CompletableFuture<List<Order>> OCO order pair
     */
    CompletableFuture<List<Order>> createOCOOrder(String symbol, Integer quantity,
                                                 BigDecimal firstPrice, BigDecimal secondPrice, Long userId);
    
    /**
     * Execute TWAP (Time-Weighted Average Price) strategy
     * 
     * @param symbol Trading symbol
     * @param totalQuantity Total quantity to execute
     * @param duration Execution duration in minutes
     * @param userId User ID
     * @return CompletableFuture<List<Order>> TWAP child orders
     */
    CompletableFuture<List<Order>> executeTWAPStrategy(String symbol, Integer totalQuantity,
                                                      Integer duration, Long userId);
    
    /**
     * Execute VWAP (Volume-Weighted Average Price) strategy
     * 
     * @param symbol Trading symbol
     * @param totalQuantity Total quantity to execute
     * @param participationRate Market participation rate (0.1 = 10%)
     * @param userId User ID
     * @return CompletableFuture<List<Order>> VWAP child orders
     */
    CompletableFuture<List<Order>> executeVWAPStrategy(String symbol, Integer totalQuantity,
                                                      BigDecimal participationRate, Long userId);
    
    /**
     * Execute Implementation Shortfall strategy
     * 
     * @param symbol Trading symbol
     * @param totalQuantity Total quantity to execute
     * @param riskAversion Risk aversion parameter (0.0-1.0)
     * @param userId User ID
     * @return CompletableFuture<List<Order>> IS strategy child orders
     */
    CompletableFuture<List<Order>> executeImplementationShortfallStrategy(String symbol, Integer totalQuantity,
                                                                         BigDecimal riskAversion, Long userId);
    
    /**
     * Validate order with advanced business rules and risk checks
     * 
     * @param order Order to validate
     * @param userId User ID for context
     * @return OrderValidationResult comprehensive validation result
     */
    OrderValidationResult validateAdvancedOrder(Order order, Long userId);
    
    /**
     * Calculate optimal execution strategy based on order characteristics
     * 
     * @param symbol Trading symbol
     * @param quantity Order quantity
     * @param urgency Execution urgency (0.0-1.0)
     * @param marketConditions Current market conditions
     * @return OrderExecutionStrategy optimal execution strategy
     */
    OrderExecutionStrategy calculateOptimalStrategy(String symbol, Integer quantity,
                                                   BigDecimal urgency, String marketConditions);
    
    /**
     * Monitor order execution performance and adjust strategy
     * 
     * @param parentOrderId Parent order ID
     * @return CompletableFuture<Void> monitoring completion
     */
    CompletableFuture<Void> monitorOrderExecution(String parentOrderId);
    
    /**
     * Get comprehensive order analytics and performance metrics
     * 
     * @param userId User ID
     * @param fromTime Start time for analytics
     * @param toTime End time for analytics
     * @return CompletableFuture<OrderAnalytics> detailed analytics
     */
    CompletableFuture<OrderAnalytics> getOrderAnalytics(Long userId, Instant fromTime, Instant toTime);
    
    /**
     * Cancel all related orders (for bracket, OCO, iceberg strategies)
     * 
     * @param parentOrderId Parent order ID
     * @param userId User ID for authorization
     * @return CompletableFuture<List<Order>> cancelled orders
     */
    CompletableFuture<List<Order>> cancelRelatedOrders(String parentOrderId, Long userId);
    
    /**
     * Get execution quality report for order
     * 
     * @param orderId Order ID
     * @param userId User ID for authorization
     * @return CompletableFuture<OrderAnalytics> execution quality metrics
     */
    CompletableFuture<OrderAnalytics> getExecutionQualityReport(String orderId, Long userId);
    
    /**
     * Auto-adjust order parameters based on market conditions
     * 
     * @param orderId Order ID to adjust
     * @param userId User ID for authorization
     * @return CompletableFuture<Order> adjusted order
     */
    CompletableFuture<Order> autoAdjustOrder(String orderId, Long userId);
    
    /**
     * Get smart order routing recommendation
     * 
     * @param symbol Trading symbol
     * @param quantity Order quantity
     * @param side Order side
     * @return CompletableFuture<String> recommended execution venue
     */
    CompletableFuture<String> getSmartOrderRouting(String symbol, Integer quantity, String side);
}