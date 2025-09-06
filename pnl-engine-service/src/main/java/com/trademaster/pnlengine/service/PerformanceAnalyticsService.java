package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Performance Analytics Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PerformanceAnalyticsService {
    
    CompletableFuture<PerformanceAttributionResult> calculateAttribution(
        String userId, Instant fromDate, Instant toDate, String benchmark);
    
    CompletableFuture<RiskMetricsResult> calculateRiskMetrics(String userId, Integer periodDays);
    
    CompletableFuture<CorrelationAnalysisResult> calculateCorrelation(
        String userId, String benchmark, Integer periodDays);
}