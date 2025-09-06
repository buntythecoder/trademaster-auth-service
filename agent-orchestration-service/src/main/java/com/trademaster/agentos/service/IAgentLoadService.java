package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

/**
 * ✅ INTERFACE SEGREGATION: Agent Load Management Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only load management operations
 * - Interface Segregation: Separated from core agent operations
 * - Dependency Inversion: Abstractions for load management
 */
public interface IAgentLoadService {
    
    /**
     * ✅ SRP: Increment agent load - single responsibility
     */
    Result<String, AgentError> incrementAgentLoad(Long agentId);
    
    /**
     * ✅ SRP: Decrement agent load - single responsibility
     */
    Result<String, AgentError> decrementAgentLoad(Long agentId);
    
    /**
     * ✅ SRP: Update performance metrics - single responsibility
     */
    Result<Agent, AgentError> updatePerformanceMetrics(Long agentId, boolean taskSuccess, long responseTimeMs);
}