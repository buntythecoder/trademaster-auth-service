package com.trademaster.agentos.domain.dto;

import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;

import java.time.Instant;
import java.util.List;

/**
 * ✅ RECORD: Immutable Agent Data Transfer Object
 * 
 * Records provide immutable data structures with automatic equals/hashCode/toString.
 * Used for API responses and inter-service communication.
 */
public record AgentDto(
    Long agentId,
    String agentName,
    AgentType agentType,
    String description,
    AgentStatus status,
    int currentLoad,
    int maxConcurrentTasks,
    double successRate,
    long averageResponseTime,
    long totalTasksCompleted,
    Instant lastHeartbeat,
    Long userId,
    Instant createdAt,
    Instant updatedAt,
    List<String> capabilities
) {
    
    /**
     * ✅ VALIDATION: Compact constructor with validation
     */
    public AgentDto {
        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException("Agent ID must be positive");
        }
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("Agent name cannot be blank");
        }
        if (agentType == null) {
            throw new IllegalArgumentException("Agent type cannot be null");
        }
        if (currentLoad < 0) {
            throw new IllegalArgumentException("Current load cannot be negative");
        }
        if (maxConcurrentTasks <= 0) {
            throw new IllegalArgumentException("Max concurrent tasks must be positive");
        }
        if (successRate < 0.0 || successRate > 1.0) {
            throw new IllegalArgumentException("Success rate must be between 0.0 and 1.0");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        // Defensive copying of mutable collections
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
    
    /**
     * ✅ DERIVED PROPERTY: Load percentage calculation
     */
    public double loadPercentage() {
        return maxConcurrentTasks == 0 ? 0.0 : (double) currentLoad / maxConcurrentTasks * 100.0;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if agent is available
     */
    public boolean isAvailable() {
        return status == AgentStatus.ACTIVE || status == AgentStatus.OVERLOADED;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if agent is overloaded
     */
    public boolean isOverloaded() {
        return loadPercentage() > 80.0;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if agent is performing well
     */
    public boolean isPerformingWell() {
        return successRate > 0.9 && averageResponseTime < 1000;
    }
    
    /**
     * ✅ FACTORY METHOD: Create from entity
     */
    public static AgentDto fromEntity(com.trademaster.agentos.domain.entity.Agent agent, List<String> capabilities) {
        return new AgentDto(
            agent.getAgentId(),
            agent.getAgentName(),
            agent.getAgentType(),
            agent.getDescription(),
            agent.getStatus(),
            agent.getCurrentLoad(),
            agent.getMaxConcurrentTasks(),
            agent.getSuccessRate(),
            agent.getAverageResponseTime(),
            agent.getTotalTasksCompleted(),
            agent.getLastHeartbeat(),
            agent.getUserId(),
            agent.getCreatedAt(),
            agent.getUpdatedAt(),
            capabilities
        );
    }
    
    /**
     * ✅ FACTORY METHOD: Create for API response
     */
    public static AgentDto forResponse(
        Long agentId,
        String agentName,
        AgentType agentType,
        AgentStatus status,
        int currentLoad,
        int maxConcurrentTasks,
        double successRate
    ) {
        return new AgentDto(
            agentId,
            agentName,
            agentType,
            null, // description
            status,
            currentLoad,
            maxConcurrentTasks,
            successRate,
            0L, // averageResponseTime
            0L, // totalTasksCompleted
            Instant.now(), // lastHeartbeat
            1L, // userId (default)
            Instant.now(), // createdAt
            Instant.now(), // updatedAt
            List.of() // capabilities
        );
    }
}