package com.trademaster.agentos.service;

import com.trademaster.agentos.service.MCPProtocolService.*;

import java.util.concurrent.CompletableFuture;

/**
 * ✅ INTERFACE SEGREGATION: Trade Extension Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only trade-specific MCP operations
 * - Interface Segregation: Separated from core MCP operations
 * - Dependency Inversion: Abstractions for trade extensions
 */
public interface ITradeExtensionService {
    
    /**
     * ✅ SRP: Execute trade order - single responsibility
     */
    CompletableFuture<MCPExecutionResult> executeTradeOrder(MCPTradeOrder order);
    
    /**
     * ✅ SRP: Analyze market data - single responsibility
     */
    CompletableFuture<MCPMarketInsights> analyzeMarketData(MCPAnalysisRequest request);
}