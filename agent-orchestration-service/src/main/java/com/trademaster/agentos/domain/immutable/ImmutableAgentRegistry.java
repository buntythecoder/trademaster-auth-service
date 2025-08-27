package com.trademaster.agentos.domain.immutable;

import com.trademaster.agentos.domain.dto.AgentDto;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * ✅ IMMUTABLE DATA STRUCTURE: Agent Registry
 * 
 * Immutable collection for agent registry with persistent data structures.
 * All modifications return new instances, ensuring thread safety and history preservation.
 */
public final class ImmutableAgentRegistry {
    
    private final Map<Long, AgentDto> agents;
    private final Map<AgentType, Set<Long>> agentsByType;
    private final Map<AgentStatus, Set<Long>> agentsByStatus;
    private final Instant lastModified;
    private final int version;
    
    /**
     * ✅ PRIVATE CONSTRUCTOR: Enforce immutability
     */
    private ImmutableAgentRegistry(
        Map<Long, AgentDto> agents,
        Map<AgentType, Set<Long>> agentsByType,
        Map<AgentStatus, Set<Long>> agentsByStatus,
        Instant lastModified,
        int version
    ) {
        // ✅ DEFENSIVE COPYING: Deep copy all mutable structures
        this.agents = Map.copyOf(agents);
        this.agentsByType = agentsByType.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> Set.copyOf(entry.getValue())
            ));
        this.agentsByStatus = agentsByStatus.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> Set.copyOf(entry.getValue())
            ));
        this.lastModified = lastModified;
        this.version = version;
    }
    
    /**
     * ✅ FACTORY METHOD: Create empty registry
     */
    public static ImmutableAgentRegistry empty() {
        return new ImmutableAgentRegistry(
            Map.of(),
            Map.of(),
            Map.of(),
            Instant.now(),
            0
        );
    }
    
    /**
     * ✅ FACTORY METHOD: Create from collection
     */
    public static ImmutableAgentRegistry of(Collection<AgentDto> agents) {
        Map<Long, AgentDto> agentMap = agents.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                AgentDto::agentId,
                agent -> agent
            ));
            
        Map<AgentType, Set<Long>> byType = new HashMap<>();
        Map<AgentStatus, Set<Long>> byStatus = new HashMap<>();
        
        agents.forEach(agent -> {
            byType.computeIfAbsent(agent.agentType(), k -> new HashSet<>()).add(agent.agentId());
            byStatus.computeIfAbsent(agent.status(), k -> new HashSet<>()).add(agent.agentId());
        });
        
        return new ImmutableAgentRegistry(
            agentMap,
            byType,
            byStatus,
            Instant.now(),
            1
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Add agent (returns new instance)
     */
    public ImmutableAgentRegistry withAgent(AgentDto agent) {
        if (agents.containsKey(agent.agentId())) {
            return this; // No change needed
        }
        
        Map<Long, AgentDto> newAgents = new HashMap<>(agents);
        newAgents.put(agent.agentId(), agent);
        
        Map<AgentType, Set<Long>> newByType = new HashMap<>(agentsByType);
        newByType.computeIfAbsent(agent.agentType(), k -> new HashSet<>()).add(agent.agentId());
        
        Map<AgentStatus, Set<Long>> newByStatus = new HashMap<>(agentsByStatus);
        newByStatus.computeIfAbsent(agent.status(), k -> new HashSet<>()).add(agent.agentId());
        
        return new ImmutableAgentRegistry(
            newAgents,
            newByType,
            newByStatus,
            Instant.now(),
            version + 1
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Remove agent (returns new instance)
     */
    public ImmutableAgentRegistry withoutAgent(Long agentId) {
        if (!agents.containsKey(agentId)) {
            return this; // No change needed
        }
        
        AgentDto removedAgent = agents.get(agentId);
        
        Map<Long, AgentDto> newAgents = new HashMap<>(agents);
        newAgents.remove(agentId);
        
        Map<AgentType, Set<Long>> newByType = new HashMap<>(agentsByType);
        Set<Long> typeSet = newByType.get(removedAgent.agentType());
        if (typeSet != null) {
            Set<Long> newTypeSet = new HashSet<>(typeSet);
            newTypeSet.remove(agentId);
            if (newTypeSet.isEmpty()) {
                newByType.remove(removedAgent.agentType());
            } else {
                newByType.put(removedAgent.agentType(), newTypeSet);
            }
        }
        
        Map<AgentStatus, Set<Long>> newByStatus = new HashMap<>(agentsByStatus);
        Set<Long> statusSet = newByStatus.get(removedAgent.status());
        if (statusSet != null) {
            Set<Long> newStatusSet = new HashSet<>(statusSet);
            newStatusSet.remove(agentId);
            if (newStatusSet.isEmpty()) {
                newByStatus.remove(removedAgent.status());
            } else {
                newByStatus.put(removedAgent.status(), newStatusSet);
            }
        }
        
        return new ImmutableAgentRegistry(
            newAgents,
            newByType,
            newByStatus,
            Instant.now(),
            version + 1
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Update agent status (returns new instance)
     */
    public ImmutableAgentRegistry withAgentStatus(Long agentId, AgentStatus newStatus) {
        AgentDto existingAgent = agents.get(agentId);
        if (existingAgent == null || existingAgent.status() == newStatus) {
            return this; // No change needed
        }
        
        // Create updated agent DTO
        AgentDto updatedAgent = AgentDto.forResponse(
            existingAgent.agentId(),
            existingAgent.agentName(),
            existingAgent.agentType(),
            newStatus,
            existingAgent.currentLoad(),
            existingAgent.maxConcurrentTasks(),
            existingAgent.successRate()
        );
        
        return withoutAgent(agentId).withAgent(updatedAgent);
    }
    
    /**
     * ✅ QUERY OPERATIONS: Immutable access methods
     */
    
    public Optional<AgentDto> getAgent(Long agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }
    
    public List<AgentDto> getAgentsByType(AgentType type) {
        Set<Long> agentIds = agentsByType.getOrDefault(type, Set.of());
        return agentIds.stream()
            .map(agents::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public List<AgentDto> getAgentsByStatus(AgentStatus status) {
        Set<Long> agentIds = agentsByStatus.getOrDefault(status, Set.of());
        return agentIds.stream()
            .map(agents::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public List<AgentDto> getAvailableAgents() {
        return getAllAgents().stream()
            .filter(AgentDto::isAvailable)
            .sorted((a1, a2) -> Double.compare(a1.loadPercentage(), a2.loadPercentage()))
            .toList();
    }
    
    public List<AgentDto> getAllAgents() {
        return List.copyOf(agents.values());
    }
    
    public Stream<AgentDto> streamAgents() {
        return agents.values().stream();
    }
    
    public List<AgentDto> findAgents(Predicate<AgentDto> predicate) {
        return agents.values().stream()
            .filter(predicate)
            .toList();
    }
    
    /**
     * ✅ METADATA ACCESS: Registry information
     */
    
    public int size() {
        return agents.size();
    }
    
    public boolean isEmpty() {
        return agents.isEmpty();
    }
    
    public Instant getLastModified() {
        return lastModified;
    }
    
    public int getVersion() {
        return version;
    }
    
    public Map<AgentType, Integer> getAgentCountsByType() {
        return agentsByType.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }
    
    public Map<AgentStatus, Integer> getAgentCountsByStatus() {
        return agentsByStatus.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }
    
    /**
     * ✅ BULK OPERATIONS: Efficient batch modifications
     */
    
    public ImmutableAgentRegistry withAllAgents(Collection<AgentDto> newAgents) {
        if (newAgents.isEmpty()) {
            return this;
        }
        
        ImmutableAgentRegistry result = this;
        for (AgentDto agent : newAgents) {
            result = result.withAgent(agent);
        }
        return result;
    }
    
    public ImmutableAgentRegistry withoutAllAgents(Collection<Long> agentIds) {
        if (agentIds.isEmpty()) {
            return this;
        }
        
        ImmutableAgentRegistry result = this;
        for (Long agentId : agentIds) {
            result = result.withoutAgent(agentId);
        }
        return result;
    }
    
    /**
     * ✅ TRANSFORMATION: Apply function to all agents
     */
    public ImmutableAgentRegistry transform(java.util.function.Function<AgentDto, AgentDto> transformer) {
        List<AgentDto> transformedAgents = agents.values().stream()
            .map(transformer)
            .toList();
        
        return ImmutableAgentRegistry.of(transformedAgents);
    }
    
    /**
     * ✅ EQUALITY & HASHING: Based on content, not identity
     */
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImmutableAgentRegistry other)) return false;
        
        return Objects.equals(agents, other.agents) &&
               Objects.equals(agentsByType, other.agentsByType) &&
               Objects.equals(agentsByStatus, other.agentsByStatus) &&
               version == other.version;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agents, agentsByType, agentsByStatus, version);
    }
    
    @Override
    public String toString() {
        return "ImmutableAgentRegistry{" +
               "size=" + agents.size() +
               ", version=" + version +
               ", lastModified=" + lastModified +
               '}';
    }
}