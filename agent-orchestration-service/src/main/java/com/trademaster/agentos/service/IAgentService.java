package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ✅ INTERFACE SEGREGATION: Core Agent Management Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only core agent lifecycle operations
 * - Interface Segregation: Focused contract without unrelated methods
 * - Dependency Inversion: Abstractions for concrete implementations
 */
public interface IAgentService {
    
    /**
     * ✅ SRP: Agent registration - single responsibility
     */
    CompletableFuture<Agent> registerAgent(Agent agent);
    
    /**
     * ✅ SRP: Agent deregistration - single responsibility
     */
    Result<String, AgentError> deregisterAgent(Long agentId);
    
    /**
     * ✅ SRP: Find agent by ID - single responsibility
     */
    Optional<Agent> findById(Long agentId);
    
    /**
     * ✅ SRP: Find agent by name - single responsibility
     */
    Optional<Agent> findByName(String agentName);
    
    /**
     * ✅ SRP: Find agents by capability - single responsibility
     */
    List<Agent> findAgentsWithCapability(AgentCapability capability);
    
    /**
     * ✅ SRP: Find optimal agent for task - single responsibility
     */
    Result<Agent, AgentError> findOptimalAgentForTask(AgentType agentType, List<AgentCapability> requiredCapabilities);
    
    /**
     * ✅ SRP: Find all agents with pagination - single responsibility
     */
    Page<Agent> findAllAgents(Pageable pageable);
    
    /**
     * ✅ SRP: Find agents by type - single responsibility
     */
    List<Agent> findByType(AgentType agentType);
    
    /**
     * ✅ SRP: Find agents by status - single responsibility
     */
    List<Agent> findByStatus(AgentStatus status);
    
    /**
     * ✅ SRP: Find agents by user ID - single responsibility
     */
    List<Agent> findByUserId(Long userId);
    
    /**
     * ✅ SRP: Update agent status - single responsibility
     */
    void updateAgentStatus(Long agentId, AgentStatus status);
    
    /**
     * ✅ SRP: Find available agents by type - single responsibility
     */
    List<Agent> findAvailableAgentsByType(AgentType agentType);
    
    /**
     * ✅ SRP: Find top performing agents - single responsibility
     */
    List<Agent> findTopPerformingAgents(Long minTasksCompleted);
    
    /**
     * ✅ SRP: Get system health summary - single responsibility
     */
    IAgentHealthService.AgentHealthSummary getSystemHealthSummary();
    
    /**
     * ✅ SRP: Get agent statistics by type - single responsibility
     */
    List<Object[]> getAgentStatisticsByType();
    
    /**
     * ✅ SRP: Update performance metrics - single responsibility
     */
    void updatePerformanceMetrics(Long agentId, boolean taskSuccess, long responseTimeMs);
    
    /**
     * ✅ SRP: Increment agent load - single responsibility
     */
    Result<Agent, AgentError> incrementAgentLoad(Long agentId);
    
    /**
     * ✅ SRP: Decrement agent load - single responsibility
     */
    void decrementAgentLoad(Long agentId);
    
    /**
     * ✅ SRP: Perform health check - single responsibility
     */
    void performHealthCheck();
    
    /**
     * ✅ SRP: Process agent heartbeat - single responsibility
     */
    void processHeartbeat(Long agentId);
}