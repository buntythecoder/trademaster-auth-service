package com.trademaster.trading.risk.impl;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.risk.RiskCheckEngine;
import com.trademaster.trading.risk.RiskCheckResult;
import com.trademaster.trading.risk.RiskMetrics;
import com.trademaster.trading.risk.RiskViolation;
import com.trademaster.trading.risk.RiskWarning;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Basic Risk Check Engine Implementation
 * 
 * Provides comprehensive pre-trade risk management using Java 24 Virtual Threads.
 * Performs concurrent risk checks for optimal performance:
 * - Buying power validation
 * - Position limit checks
 * - Daily trading limits
 * - Concentration risk analysis
 * - Real-time P&L monitoring
 * 
 * Performance Targets:
 * - Risk check completion: <10ms
 * - Concurrent validations: 10,000+ per second
 * - Memory usage: ~8KB per risk check (Virtual Thread)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@Slf4j
public class BasicRiskCheckEngine implements RiskCheckEngine {
    
    private final AsyncTaskExecutor riskManagementExecutor;
    
    // Risk Limits Configuration
    private static final BigDecimal MAX_SINGLE_ORDER_VALUE = new BigDecimal("1000000.00"); // 10 Lakh
    private static final BigDecimal MAX_DAILY_TRADING_VALUE = new BigDecimal("10000000.00"); // 1 Crore
    private static final int MAX_OPEN_ORDERS = 100;
    private static final BigDecimal MAX_POSITION_CONCENTRATION = new BigDecimal("0.20"); // 20% of portfolio
    
    public BasicRiskCheckEngine(@Qualifier("riskManagementExecutor") AsyncTaskExecutor riskManagementExecutor) {
        this.riskManagementExecutor = riskManagementExecutor;
    }
    
    @Override
    public RiskCheckResult performRiskCheck(OrderRequest orderRequest, Long userId) {
        long startTime = System.currentTimeMillis();
        
        RiskCheckResult result = RiskCheckResult.builder()
            .approved(true)
            .userId(userId)
            .orderValue(calculateOrderValue(orderRequest))
            .violations(new ArrayList<>())
            .warnings(new ArrayList<>())
            .build();
        
        try {
            // Perform concurrent risk checks using Virtual Threads
            CompletableFuture<Void> buyingPowerCheck = CompletableFuture.runAsync(() ->
                checkBuyingPower(orderRequest, userId, result), riskManagementExecutor);
            
            CompletableFuture<Void> positionLimitCheck = CompletableFuture.runAsync(() ->
                checkPositionLimits(orderRequest, userId, result), riskManagementExecutor);
            
            CompletableFuture<Void> dailyLimitCheck = CompletableFuture.runAsync(() ->
                checkDailyTradingLimits(orderRequest, userId, result), riskManagementExecutor);
            
            CompletableFuture<Void> concentrationCheck = CompletableFuture.runAsync(() ->
                checkConcentrationRisk(orderRequest, userId, result), riskManagementExecutor);
            
            CompletableFuture<Void> orderValueCheck = CompletableFuture.runAsync(() ->
                checkOrderValueLimits(orderRequest, result), riskManagementExecutor);
            
            // Wait for all risk checks to complete
            CompletableFuture.allOf(
                buyingPowerCheck, 
                positionLimitCheck, 
                dailyLimitCheck, 
                concentrationCheck,
                orderValueCheck
            ).join();
            
            // Determine final approval based on violations
            result.setApproved(result.getViolations().isEmpty());
            
        } catch (Exception e) {
            log.error("Risk check failed for user {} and order {}: {}", userId, orderRequest.getSymbol(), e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Risk check system error: " + e.getMessage());
            result.setApproved(false);
        }
        
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        log.info("Risk check completed for user {} - Order: {} {} @ {} - Approved: {} in {}ms", 
            userId, orderRequest.getQuantity(), orderRequest.getSymbol(), 
            orderRequest.getLimitPrice(), result.isApproved(), result.getProcessingTimeMs());
        
        return result;
    }
    
    @Override
    public RiskCheckResult performModificationRiskCheck(Order existingOrder, OrderRequest modificationRequest, Long userId) {
        // For modifications, check only the incremental risk
        RiskCheckResult result = RiskCheckResult.builder()
            .approved(true)
            .userId(userId)
            .orderValue(calculateOrderValue(modificationRequest))
            .violations(new ArrayList<>())
            .warnings(new ArrayList<>())
            .build();
        
        try {
            // Check if modification increases risk exposure
            BigDecimal currentOrderValue = calculateExistingOrderValue(existingOrder);
            BigDecimal newOrderValue = calculateOrderValue(modificationRequest);
            BigDecimal increaseInExposure = newOrderValue.subtract(currentOrderValue);
            
            if (increaseInExposure.compareTo(BigDecimal.ZERO) > 0) {
                // Increased exposure - perform full risk check on the increase
                OrderRequest increaseRequest = createIncrementalOrderRequest(modificationRequest, increaseInExposure);
                result = performRiskCheck(increaseRequest, userId);
            } else {
                // Reduced or same exposure - approve
                result.addWarning(RiskWarning.INFO, "Order modification reduces or maintains current exposure");
            }
            
        } catch (Exception e) {
            log.error("Risk check for modification failed: {}", e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Modification risk check error: " + e.getMessage());
            result.setApproved(false);
        }
        
        return result;
    }
    
    @Override
    public CompletableFuture<RiskMetrics> getRiskMetrics(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, these would come from the database
                return RiskMetrics.builder()
                    .userId(userId)
                    .totalExposure(new BigDecimal("500000.00"))
                    .availableBuyingPower(new BigDecimal("1500000.00"))
                    .dailyTradingVolume(new BigDecimal("2000000.00"))
                    .openOrdersCount(15)
                    .maxConcentration(new BigDecimal("0.15"))
                    .riskUtilization(new BigDecimal("0.33"))
                    .build();
                    
            } catch (Exception e) {
                log.error("Failed to fetch risk metrics for user {}: {}", userId, e.getMessage());
                throw new RuntimeException("Risk metrics calculation failed", e);
            }
        }, riskManagementExecutor);
    }
    
    @Override
    public CompletableFuture<Boolean> isApproachingRiskLimits(Long userId) {
        return getRiskMetrics(userId).thenApply(metrics -> {
            // Consider user approaching risk limits if:
            // 1. Risk utilization > 80%
            // 2. Daily trading volume > 80% of limit
            // 3. Open orders > 80 (80% of max 100)
            
            boolean approachingLimits = metrics.getRiskUtilization().compareTo(new BigDecimal("0.80")) > 0 ||
                                      metrics.getDailyTradingVolume().compareTo(MAX_DAILY_TRADING_VALUE.multiply(new BigDecimal("0.80"))) > 0 ||
                                      metrics.getOpenOrdersCount() > 80;
            
            if (approachingLimits) {
                log.warn("User {} is approaching risk limits - Risk: {}%, Daily Volume: {}, Open Orders: {}", 
                    userId, metrics.getRiskUtilization().multiply(new BigDecimal("100")), 
                    metrics.getDailyTradingVolume(), metrics.getOpenOrdersCount());
            }
            
            return approachingLimits;
        });
    }
    
    private void checkBuyingPower(OrderRequest orderRequest, Long userId, RiskCheckResult result) {
        try {
            BigDecimal orderValue = calculateOrderValue(orderRequest);
            BigDecimal availableBuyingPower = getAvailableBuyingPower(userId);
            
            if (orderValue.compareTo(availableBuyingPower) > 0) {
                result.addViolation(RiskViolation.INSUFFICIENT_BUYING_POWER, 
                    String.format("Order value %s exceeds available buying power %s", orderValue, availableBuyingPower));
            } else if (orderValue.compareTo(availableBuyingPower.multiply(new BigDecimal("0.90"))) > 0) {
                result.addWarning(RiskWarning.HIGH_BUYING_POWER_USAGE, 
                    "Order will use >90% of available buying power");
            }
        } catch (Exception e) {
            log.error("Buying power check failed for user {}: {}", userId, e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Buying power validation failed");
        }
    }
    
    private void checkPositionLimits(OrderRequest orderRequest, Long userId, RiskCheckResult result) {
        try {
            int currentOpenOrders = getCurrentOpenOrdersCount(userId);
            
            if (currentOpenOrders >= MAX_OPEN_ORDERS) {
                result.addViolation(RiskViolation.MAX_OPEN_ORDERS_EXCEEDED, 
                    String.format("User has %d open orders (max: %d)", currentOpenOrders, MAX_OPEN_ORDERS));
            } else if (currentOpenOrders >= MAX_OPEN_ORDERS * 0.8) {
                result.addWarning(RiskWarning.APPROACHING_POSITION_LIMIT, 
                    String.format("User has %d open orders (approaching max: %d)", currentOpenOrders, MAX_OPEN_ORDERS));
            }
        } catch (Exception e) {
            log.error("Position limit check failed for user {}: {}", userId, e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Position limit validation failed");
        }
    }
    
    private void checkDailyTradingLimits(OrderRequest orderRequest, Long userId, RiskCheckResult result) {
        try {
            BigDecimal dailyTradingVolume = getDailyTradingVolume(userId);
            BigDecimal orderValue = calculateOrderValue(orderRequest);
            BigDecimal newDailyVolume = dailyTradingVolume.add(orderValue);
            
            if (newDailyVolume.compareTo(MAX_DAILY_TRADING_VALUE) > 0) {
                result.addViolation(RiskViolation.DAILY_TRADING_LIMIT_EXCEEDED, 
                    String.format("Daily trading limit exceeded: %s + %s > %s", 
                        dailyTradingVolume, orderValue, MAX_DAILY_TRADING_VALUE));
            } else if (newDailyVolume.compareTo(MAX_DAILY_TRADING_VALUE.multiply(new BigDecimal("0.80"))) > 0) {
                result.addWarning(RiskWarning.APPROACHING_DAILY_LIMIT, 
                    "Approaching daily trading limit");
            }
        } catch (Exception e) {
            log.error("Daily trading limit check failed for user {}: {}", userId, e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Daily limit validation failed");
        }
    }
    
    private void checkConcentrationRisk(OrderRequest orderRequest, Long userId, RiskCheckResult result) {
        try {
            BigDecimal portfolioValue = getPortfolioValue(userId);
            BigDecimal currentSymbolExposure = getCurrentSymbolExposure(userId, orderRequest.getSymbol());
            BigDecimal orderValue = calculateOrderValue(orderRequest);
            BigDecimal newSymbolExposure = currentSymbolExposure.add(orderValue);
            BigDecimal concentrationRatio = newSymbolExposure.divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP);
            
            if (concentrationRatio.compareTo(MAX_POSITION_CONCENTRATION) > 0) {
                result.addViolation(RiskViolation.CONCENTRATION_RISK_EXCEEDED, 
                    String.format("Position concentration %.2f%% exceeds limit %.2f%% for symbol %s", 
                        concentrationRatio.multiply(new BigDecimal("100")), 
                        MAX_POSITION_CONCENTRATION.multiply(new BigDecimal("100")), 
                        orderRequest.getSymbol()));
            } else if (concentrationRatio.compareTo(MAX_POSITION_CONCENTRATION.multiply(new BigDecimal("0.80"))) > 0) {
                result.addWarning(RiskWarning.HIGH_CONCENTRATION_RISK, 
                    String.format("High concentration risk for %s: %.2f%%", 
                        orderRequest.getSymbol(), concentrationRatio.multiply(new BigDecimal("100"))));
            }
        } catch (Exception e) {
            log.error("Concentration risk check failed for user {}: {}", userId, e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Concentration risk validation failed");
        }
    }
    
    private void checkOrderValueLimits(OrderRequest orderRequest, RiskCheckResult result) {
        try {
            BigDecimal orderValue = calculateOrderValue(orderRequest);
            
            if (orderValue.compareTo(MAX_SINGLE_ORDER_VALUE) > 0) {
                result.addViolation(RiskViolation.ORDER_VALUE_EXCEEDED, 
                    String.format("Order value %s exceeds maximum single order limit %s", 
                        orderValue, MAX_SINGLE_ORDER_VALUE));
            } else if (orderValue.compareTo(MAX_SINGLE_ORDER_VALUE.multiply(new BigDecimal("0.80"))) > 0) {
                result.addWarning(RiskWarning.LARGE_ORDER_VALUE, 
                    "Large order value detected");
            }
        } catch (Exception e) {
            log.error("Order value limit check failed: {}", e.getMessage());
            result.addViolation(RiskViolation.SYSTEM_ERROR, "Order value validation failed");
        }
    }
    
    private BigDecimal calculateOrderValue(OrderRequest orderRequest) {
        if (orderRequest.getLimitPrice() != null) {
            return orderRequest.getLimitPrice().multiply(new BigDecimal(orderRequest.getQuantity()));
        } else {
            // For market orders, estimate using current market price (would fetch from market data service)
            BigDecimal estimatedPrice = getEstimatedMarketPrice(orderRequest.getSymbol());
            return estimatedPrice.multiply(new BigDecimal(orderRequest.getQuantity()));
        }
    }
    
    private BigDecimal calculateExistingOrderValue(Order existingOrder) {
        BigDecimal price = existingOrder.getLimitPrice() != null ? 
            existingOrder.getLimitPrice() : existingOrder.getStopPrice();
        return price.multiply(new BigDecimal(existingOrder.getQuantity()));
    }
    
    private OrderRequest createIncrementalOrderRequest(OrderRequest modificationRequest, BigDecimal increaseInExposure) {
        // Create a dummy order request representing only the increased exposure
        return OrderRequest.builder()
            .symbol(modificationRequest.getSymbol())
            .exchange(modificationRequest.getExchange())
            .orderType(modificationRequest.getOrderType())
            .side(modificationRequest.getSide())
            .quantity(1) // Minimal quantity
            .limitPrice(increaseInExposure) // Use increased exposure as price
            .timeInForce(modificationRequest.getTimeInForce())
            .build();
    }
    
    // Mock data methods - in real implementation, these would call repositories/services
    private BigDecimal getAvailableBuyingPower(Long userId) {
        // Mock: In real implementation, fetch from user's account
        return new BigDecimal("2000000.00"); // 20 Lakh
    }
    
    private int getCurrentOpenOrdersCount(Long userId) {
        // Mock: In real implementation, count from order repository
        return 15;
    }
    
    private BigDecimal getDailyTradingVolume(Long userId) {
        // Mock: In real implementation, sum today's order values
        return new BigDecimal("1500000.00"); // 15 Lakh
    }
    
    private BigDecimal getPortfolioValue(Long userId) {
        // Mock: In real implementation, calculate from positions
        return new BigDecimal("5000000.00"); // 50 Lakh
    }
    
    private BigDecimal getCurrentSymbolExposure(Long userId, String symbol) {
        // Mock: In real implementation, sum positions for symbol
        return new BigDecimal("500000.00"); // 5 Lakh
    }
    
    private BigDecimal getEstimatedMarketPrice(String symbol) {
        // Mock: In real implementation, fetch from market data service
        return new BigDecimal("100.00"); // Mock price
    }
}