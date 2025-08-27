package com.trademaster.agentos.repository;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.domain.entity.AgentCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Agent Repository
 * 
 * Data access layer for Agent entities with specialized query methods
 * for agent selection, health monitoring, and load balancing.
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * Find agent by unique name
     */
    Optional<Agent> findByAgentName(String agentName);

    /**
     * Find all agents by type
     */
    List<Agent> findByAgentType(AgentType agentType);

    /**
     * Find all agents by status
     */
    List<Agent> findByStatus(AgentStatus status);

    /**
     * Find all agents by user ID
     */
    List<Agent> findByUserId(Long userId);

    /**
     * Find all active agents by type
     */
    List<Agent> findByAgentTypeAndStatus(AgentType agentType, AgentStatus status);

    /**
     * Find available agents that can accept new tasks
     */
    @Query("SELECT a FROM Agent a WHERE a.status = 'ACTIVE' AND a.currentLoad < a.maxConcurrentTasks")
    List<Agent> findAvailableAgents();

    /**
     * Find available agents by type that can accept new tasks
     */
    @Query("SELECT a FROM Agent a WHERE a.agentType = :agentType AND a.status = 'ACTIVE' AND a.currentLoad < a.maxConcurrentTasks")
    List<Agent> findAvailableAgentsByType(@Param("agentType") AgentType agentType);

    /**
     * Find agents with specific capability
     */
    @Query("SELECT a FROM Agent a JOIN a.capabilities c WHERE c = :capability AND a.status = 'ACTIVE'")
    List<Agent> findAgentsWithCapability(@Param("capability") AgentCapability capability);

    /**
     * Find agents with multiple capabilities
     */
    @Query("""
        SELECT a FROM Agent a 
        WHERE a.status = 'ACTIVE' 
        AND SIZE(a.capabilities) >= :requiredCapabilityCount
        AND (
            SELECT COUNT(DISTINCT c) 
            FROM Agent a2 JOIN a2.capabilities c 
            WHERE a2.agentId = a.agentId AND c IN :requiredCapabilities
        ) >= :requiredCapabilityCount
        """)
    List<Agent> findAgentsWithCapabilities(
        @Param("requiredCapabilities") List<AgentCapability> requiredCapabilities,
        @Param("requiredCapabilityCount") long requiredCapabilityCount
    );

    /**
     * Find agents ordered by performance (success rate and response time)
     */
    @Query("SELECT a FROM Agent a WHERE a.status = 'ACTIVE' ORDER BY a.successRate DESC, a.averageResponseTime ASC")
    List<Agent> findAgentsOrderedByPerformance();

    /**
     * Find agents with lowest current load
     */
    @Query("SELECT a FROM Agent a WHERE a.status = 'ACTIVE' ORDER BY a.currentLoad ASC, a.successRate DESC")
    List<Agent> findAgentsOrderedByLoad();

    /**
     * Find agents that haven't sent heartbeat recently (potentially unhealthy)
     */
    @Query("SELECT a FROM Agent a WHERE a.lastHeartbeat < :threshold AND a.status IN ('ACTIVE', 'BUSY')")
    List<Agent> findAgentsWithStaleHeartbeat(@Param("threshold") Instant threshold);

    /**
     * Find overloaded agents (current load >= max concurrent tasks)
     */
    @Query("SELECT a FROM Agent a WHERE a.currentLoad >= a.maxConcurrentTasks AND a.status = 'ACTIVE'")
    List<Agent> findOverloadedAgents();

    /**
     * Find underutilized agents (low load relative to capacity)
     */
    @Query("SELECT a FROM Agent a WHERE a.status = 'ACTIVE' AND (a.currentLoad * 1.0 / a.maxConcurrentTasks) < :utilizationThreshold")
    List<Agent> findUnderutilizedAgents(@Param("utilizationThreshold") double utilizationThreshold);

    /**
     * Get agent statistics by type
     */
    @Query("""
        SELECT a.agentType as agentType,
               COUNT(a) as totalCount,
               COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) as activeCount,
               COUNT(CASE WHEN a.status = 'BUSY' THEN 1 END) as busyCount,
               COUNT(CASE WHEN a.status = 'ERROR' THEN 1 END) as errorCount,
               AVG(a.successRate) as avgSuccessRate,
               AVG(a.averageResponseTime) as avgResponseTime,
               SUM(a.totalTasksCompleted) as totalTasksCompleted
        FROM Agent a 
        GROUP BY a.agentType
        """)
    List<Object[]> getAgentStatisticsByType();

    /**
     * Get system-wide agent statistics
     */
    @Query("""
        SELECT COUNT(a) as totalAgents,
               COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) as activeAgents,
               COUNT(CASE WHEN a.status = 'BUSY' THEN 1 END) as busyAgents,
               COUNT(CASE WHEN a.status = 'ERROR' THEN 1 END) as errorAgents,
               AVG(a.currentLoad) as avgLoad,
               AVG(a.successRate) as avgSuccessRate,
               SUM(a.totalTasksCompleted) as totalTasksCompleted
        FROM Agent a
        """)
    Object[] getSystemAgentStatistics();

    /**
     * Count agents by status
     */
    long countByStatus(AgentStatus status);

    /**
     * Count agents by type and status
     */
    long countByAgentTypeAndStatus(AgentType agentType, AgentStatus status);

    /**
     * Check if agent name exists
     */
    boolean existsByAgentName(String agentName);

    /**
     * Find top performing agents by success rate
     */
    @Query("SELECT a FROM Agent a WHERE a.totalTasksCompleted > :minTasksCompleted ORDER BY a.successRate DESC")
    List<Agent> findTopPerformingAgents(@Param("minTasksCompleted") Long minTasksCompleted);

    /**
     * Find agents by user with status filter
     */
    List<Agent> findByUserIdAndStatus(Long userId, AgentStatus status);

    /**
     * Find agents created within time range
     */
    @Query("SELECT a FROM Agent a WHERE a.createdAt BETWEEN :startTime AND :endTime")
    List<Agent> findAgentsCreatedBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Update agent heartbeat timestamp
     */
    @Query("UPDATE Agent a SET a.lastHeartbeat = :heartbeatTime, a.updatedAt = :heartbeatTime WHERE a.agentId = :agentId")
    void updateAgentHeartbeat(@Param("agentId") Long agentId, @Param("heartbeatTime") Instant heartbeatTime);

    /**
     * Update agent status
     */
    @Query("UPDATE Agent a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.agentId = :agentId")
    void updateAgentStatus(@Param("agentId") Long agentId, @Param("status") AgentStatus status);

    /**
     * Increment agent load
     */
    @Query("UPDATE Agent a SET a.currentLoad = a.currentLoad + 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.agentId = :agentId")
    void incrementAgentLoad(@Param("agentId") Long agentId);

    /**
     * Decrement agent load
     */
    @Query("UPDATE Agent a SET a.currentLoad = CASE WHEN a.currentLoad > 0 THEN a.currentLoad - 1 ELSE 0 END, a.updatedAt = CURRENT_TIMESTAMP WHERE a.agentId = :agentId")
    void decrementAgentLoad(@Param("agentId") Long agentId);

    /**
     * Custom query to find optimal agent for task assignment
     * Considers agent type, capabilities, current load, and performance
     */
    @Query("""
        SELECT a FROM Agent a 
        WHERE a.agentType = :agentType 
        AND a.status = 'ACTIVE' 
        AND a.currentLoad < a.maxConcurrentTasks
        AND (
            :requiredCapabilities IS NULL OR 
            SIZE(a.capabilities) = 0 OR
            (
                SELECT COUNT(DISTINCT c) 
                FROM Agent a2 JOIN a2.capabilities c 
                WHERE a2.agentId = a.agentId AND c IN :requiredCapabilities
            ) = SIZE(:requiredCapabilities)
        )
        ORDER BY 
            (a.currentLoad * 1.0 / a.maxConcurrentTasks) ASC,
            a.successRate DESC,
            a.averageResponseTime ASC,
            a.totalTasksCompleted DESC
        """)
    List<Agent> findOptimalAgentForTask(
        @Param("agentType") AgentType agentType, 
        @Param("requiredCapabilities") List<AgentCapability> requiredCapabilities
    );
}