package com.trademaster.trading.service;

import com.trademaster.trading.entity.Order;

import java.math.BigDecimal;

/**
 * Portfolio Service
 * 
 * Service interface for portfolio and position management using Java 24 Virtual Threads.
 * Manages real-time position tracking, P&L calculations, and portfolio analytics.
 * 
 * Key Features:
 * - Real-time position tracking (pending and filled)
 * - Intraday and overnight P&L calculations
 * - Portfolio risk metrics (VaR, exposure, concentration)
 * - Position reconciliation with broker statements
 * - Cash and margin balance management
 * 
 * Performance Targets:
 * - Position updates: <5ms (cached calculations)
 * - P&L calculations: <10ms (real-time market data)
 * - Portfolio queries: <15ms (aggregated views)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PortfolioService {
    
    /**
     * Update pending position when order is placed
     * 
     * @param userId The user ID
     * @param order The pending order
     */
    void updatePendingPosition(Long userId, Order order);
    
    /**
     * Remove pending position when order is cancelled
     * 
     * @param userId The user ID
     * @param order The cancelled order
     */
    void removePendingPosition(Long userId, Order order);
    
    /**
     * Update filled position when order is executed
     * 
     * @param userId The user ID
     * @param order The filled order
     * @param fillQuantity The fill quantity
     * @param fillPrice The fill price
     */
    void updateFilledPosition(Long userId, Order order, Integer fillQuantity, BigDecimal fillPrice);
}