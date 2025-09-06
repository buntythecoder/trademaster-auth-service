package com.trademaster.pnlengine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * P&L Audit Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Comprehensive audit logging service for P&L calculations providing
 * regulatory compliance, security monitoring, and operational transparency
 * for all financial calculations and transactions.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PnLAuditService {
    
    CompletableFuture<Void> logPnLCalculation(String userId, String calculationType, 
                                            BigDecimal result, long calculationTimeMs, String correlationId);
    
    CompletableFuture<Void> logTaxCalculation(String userId, String symbol, 
                                            BigDecimal taxLiability, String method, String correlationId);
    
    CompletableFuture<Void> logPerformanceAttribution(String userId, String benchmark, 
                                                     BigDecimal activeReturn, String correlationId);
    
    CompletableFuture<Void> logSecurityEvent(String userId, String eventType, 
                                           String details, String correlationId);
}