package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ INTERFACE SEGREGATION: Agent Health Management Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only health monitoring operations
 * - Interface Segregation: Separated from core agent operations
 * - Dependency Inversion: Abstractions for health management
 */
public interface IAgentHealthService {
    
    /**
     * ✅ SRP: Process agent heartbeat - single responsibility
     */
    Result<String, AgentError> processHeartbeat(Long agentId);
    
    /**
     * ✅ SRP: Update agent status - single responsibility
     */
    void updateAgentStatus(Long agentId, AgentStatus status);
    
    /**
     * ✅ SRP: Perform health check - single responsibility
     */
    Result<String, AgentError> performHealthCheck();
    
    /**
     * ✅ SRP: Get system health summary - single responsibility
     */
    AgentHealthSummary getSystemHealthSummary();
    
    /**
     * ✅ SRP: Start monitoring an agent - single responsibility
     */
    CompletableFuture<Result<String, AgentError>> startMonitoring(Long agentId);
    
    /**
     * ✅ SRP: Stop monitoring an agent - single responsibility
     */
    void stopMonitoring(Long agentId);
    
    /**
     * ✅ IMMUTABLE: Health summary record
     */
    record AgentHealthSummary(
        Long totalAgents,
        Long activeAgents,
        Long busyAgents,
        Long errorAgents,
        Double averageLoad,
        Double averageSuccessRate,
        Long totalTasksCompleted
    ) {}
}