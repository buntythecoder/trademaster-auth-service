package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

import java.util.Map;

/**
 * ✅ INTERFACE SEGREGATION: Agent State Management Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only state management operations
 * - Interface Segregation: Separated from core agent operations
 * - Dependency Inversion: Abstractions for state management
 */
public interface IAgentStateService {
    
    /**
     * ✅ SRP: Start agent - single responsibility
     */
    Result<Agent, AgentError> startAgent(Long agentId);
    
    /**
     * ✅ SRP: Stop agent - single responsibility
     */
    Result<Agent, AgentError> stopAgent(Long agentId);
    
    /**
     * ✅ SRP: Enter maintenance mode - single responsibility
     */
    Result<Agent, AgentError> enterMaintenanceMode(Long agentId);
    
    /**
     * ✅ SRP: Exit maintenance mode - single responsibility
     */
    Result<Agent, AgentError> exitMaintenanceMode(Long agentId);
    
    /**
     * ✅ SRP: Handle agent failure - single responsibility
     */
    Result<Agent, AgentError> handleAgentFailure(Long agentId, String reason);
    
    /**
     * ✅ SRP: Recover agent - single responsibility
     */
    Result<Agent, AgentError> recoverAgent(Long agentId);
    
    /**
     * ✅ SRP: Check if agent can accept tasks - single responsibility
     */
    Result<Boolean, AgentError> canAgentAcceptTasks(Long agentId);
    
    /**
     * ✅ SRP: Get agent state info - single responsibility
     */
    Result<Map<String, Object>, AgentError> getAgentStateInfo(Long agentId);
}