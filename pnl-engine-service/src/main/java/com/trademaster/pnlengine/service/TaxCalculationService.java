package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tax Calculation Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface TaxCalculationService {
    
    CompletableFuture<TaxOptimizedPnLResult> calculateOptimizedTaxLiability(
        String userId, String symbol, Integer quantity, CostBasisMethod method);
    
    CompletableFuture<List<TaxLotInfo>> getTaxLots(String userId, String symbol, CostBasisMethod method);
    
    CompletableFuture<TaxComplianceReport> generateTaxReport(String userId, Integer taxYear, TaxJurisdiction jurisdiction);
}