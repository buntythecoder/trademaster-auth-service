package com.trademaster.pnlengine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Notification Integration Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance notification service integration providing real-time
 * alerts, threshold notifications, and communication services for P&L
 * events and portfolio monitoring.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface NotificationIntegrationService {
    
    CompletableFuture<Void> sendPnLAlert(String userId, PnLAlertType alertType, 
                                        BigDecimal currentValue, BigDecimal thresholdValue);
    
    CompletableFuture<Void> sendPerformanceAlert(String userId, String message, 
                                                BigDecimal performanceMetric);
    
    CompletableFuture<Void> sendTaxOptimizationAlert(String userId, String symbol, 
                                                    BigDecimal potentialSavings);
    
    enum PnLAlertType {
        PORTFOLIO_THRESHOLD, POSITION_LOSS, DAY_PNL_LIMIT, MARGIN_CALL
    }
}