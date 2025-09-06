package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import java.util.concurrent.CompletableFuture;

/**
 * P&L Validation Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PnLValidationService {
    
    CompletableFuture<PnLValidationResult> validateCalculationAccuracy(String userId);
    
    CompletableFuture<Boolean> validateBrokerConsistency(String userId, BrokerType brokerType);
}