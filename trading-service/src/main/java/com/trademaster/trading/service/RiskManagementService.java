package com.trademaster.trading.service;

import com.trademaster.trading.entity.Order;

/**
 * Risk Management Service
 * 
 * Service interface for pre-trade and post-trade risk management using Java 24 Virtual Threads.
 * Implements real-time risk checks, position limits, and compliance validation.
 * 
 * Key Risk Controls:
 * - Buying power validation (real-time account balance checks)
 * - Position limit enforcement (concentration risk management)
 * - Daily trading limits (velocity controls and day trading rules)
 * - Margin requirements (leverage and collateral management)
 * - Real-time portfolio exposure monitoring
 * 
 * Performance Targets:
 * - Risk validation: <10ms (cached portfolio data)
 * - Position calculations: <5ms (in-memory aggregation)
 * - Margin checks: <15ms (real-time market data integration)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface RiskManagementService {
    
    /**
     * Validate user has sufficient buying power for the order
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @throws RiskCheckException if insufficient buying power
     */
    void validateBuyingPower(Long userId, Order order);
    
    /**
     * Validate order doesn't exceed position limits
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @throws RiskCheckException if position limits exceeded
     */
    void validatePositionLimits(Long userId, Order order);
    
    /**
     * Validate order doesn't exceed daily trading limits
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @throws RiskCheckException if daily limits exceeded
     */
    void validateDailyLimits(Long userId, Order order);
}