package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.observer.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ✅ STATE PATTERN: Functional Agent State Manager
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Agent state management only
 * - Open/Closed: Extensible via new state implementations
 * - Interface Segregation: Focused state management interface
 * - Dependency Inversion: Uses state abstractions
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - No if-else statements (uses Map-based state routing)
 * - Function-based state transitions
 * - Result monad for error handling
 * - Immutable state transition definitions
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentStateManager {
    
    private final AgentEventPublisher eventPublisher;
    
    /**
     * ✅ STATE PATTERN: State transition map (no if-else statements)
     * Maps current state + target state to transition functions
     */
    private final Map<StateTransitionKey, Function<Agent, Result<Agent, AgentError>>> stateTransitions = Map.of(
        new StateTransitionKey(AgentStatus.INITIALIZING, AgentStatus.IDLE), this::transitionToIdle,
        new StateTransitionKey(AgentStatus.IDLE, AgentStatus.ACTIVE), this::transitionToActive,
        new StateTransitionKey(AgentStatus.ACTIVE, AgentStatus.IDLE), this::transitionToIdle,
        new StateTransitionKey(AgentStatus.ACTIVE, AgentStatus.OVERLOADED), this::transitionToOverloaded,
        new StateTransitionKey(AgentStatus.OVERLOADED, AgentStatus.ACTIVE), this::transitionToActive,
        new StateTransitionKey(AgentStatus.IDLE, AgentStatus.MAINTENANCE), this::transitionToMaintenance,
        new StateTransitionKey(AgentStatus.MAINTENANCE, AgentStatus.IDLE), this::transitionToIdle,
        new StateTransitionKey(AgentStatus.ACTIVE, AgentStatus.FAILED), this::transitionToFailed,
        new StateTransitionKey(AgentStatus.FAILED, AgentStatus.IDLE), this::transitionToIdle,
        new StateTransitionKey(AgentStatus.IDLE, AgentStatus.SHUTDOWN), this::transitionToShutdown
    );
    
    /**
     * ✅ STATE PATTERN: State validation predicates (no if-else)
     */
    private final Map<AgentStatus, Predicate<Agent>> stateValidators = Map.of(
        AgentStatus.IDLE, agent -> agent.getCurrentLoad() == 0,
        AgentStatus.ACTIVE, agent -> agent.getCurrentLoad() > 0 && agent.getCurrentLoad() < agent.getMaxConcurrentTasks(),
        AgentStatus.OVERLOADED, agent -> agent.getCurrentLoad() >= agent.getMaxConcurrentTasks(),
        AgentStatus.FAILED, agent -> agent.hasRecentError(),
        AgentStatus.MAINTENANCE, agent -> true, // Always valid for maintenance
        AgentStatus.SHUTDOWN, agent -> true  // Terminal state
    );
    
    /**
     * State transition key for functional routing
     */
    private record StateTransitionKey(AgentStatus fromState, AgentStatus toState) {}
    
    /**
     * ✅ STATE PATTERN: Main state transition method
     * Cognitive Complexity: 3 (map lookup + validation + event publishing)
     */
    public CompletableFuture<Result<Agent, AgentError>> transitionState(
            Agent agent, AgentStatus targetState, String reason) {
        
        log.info("Agent state transition requested", Map.of(
            "agentId", agent.getAgentId(),
            "agentName", agent.getAgentName(),
            "currentState", agent.getStatus(),
            "targetState", targetState,
            "reason", reason
        ));
        
        return CompletableFuture.supplyAsync(() -> {
            AgentStatus previousState = agent.getStatus();
            
            return validateStateTransition(agent, targetState)
                .flatMap(validatedAgent -> executeStateTransition(validatedAgent, targetState))
                .map(transitionedAgent -> {
                    publishStateChangeEvent(transitionedAgent, previousState, reason);
                    return transitionedAgent;
                });
        });
    }
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Validate state transition
     * Cognitive Complexity: 2
     */
    private Result<Agent, AgentError> validateStateTransition(Agent agent, AgentStatus targetState) {
        StateTransitionKey transitionKey = new StateTransitionKey(agent.getStatus(), targetState);
        
        return stateTransitions.containsKey(transitionKey) ?
            Result.success(agent) :
            Result.failure(new AgentError.InvalidStateTransition(
                agent.getAgentId(), agent.getStatus(), targetState));
    }
    
    /**
     * ✅ STATE PATTERN: Execute state transition using functional mapping
     * Cognitive Complexity: 1
     */
    private Result<Agent, AgentError> executeStateTransition(Agent agent, AgentStatus targetState) {
        StateTransitionKey transitionKey = new StateTransitionKey(agent.getStatus(), targetState);
        
        return stateTransitions
            .getOrDefault(transitionKey, this::invalidTransition)
            .apply(agent);
    }
    
    /**
     * ✅ STATE TRANSITION IMPLEMENTATIONS: Specific state transition functions
     */
    
    private Result<Agent, AgentError> transitionToIdle(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.IDLE)
            .currentLoad(0)
            .updatedAt(Instant.now())
            .build();
        
        return stateValidators.get(AgentStatus.IDLE).test(updatedAgent) ?
            Result.success(updatedAgent) :
            Result.failure(new AgentError.StateValidationFailed(
                agent.getAgentId(), AgentStatus.IDLE, "Agent has active load"));
    }
    
    private Result<Agent, AgentError> transitionToActive(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.ACTIVE)
            .updatedAt(Instant.now())
            .build();
        
        return stateValidators.get(AgentStatus.ACTIVE).test(updatedAgent) ?
            Result.success(updatedAgent) :
            Result.failure(new AgentError.StateValidationFailed(
                agent.getAgentId(), AgentStatus.ACTIVE, "Agent load not within active range"));
    }
    
    private Result<Agent, AgentError> transitionToOverloaded(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.OVERLOADED)
            .updatedAt(Instant.now())
            .build();
        
        return stateValidators.get(AgentStatus.OVERLOADED).test(updatedAgent) ?
            Result.success(updatedAgent) :
            Result.failure(new AgentError.StateValidationFailed(
                agent.getAgentId(), AgentStatus.OVERLOADED, "Agent not at maximum capacity"));
    }
    
    private Result<Agent, AgentError> transitionToMaintenance(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.MAINTENANCE)
            .updatedAt(Instant.now())
            .build();
        
        return Result.success(updatedAgent);
    }
    
    private Result<Agent, AgentError> transitionToFailed(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.FAILED)
            .updatedAt(Instant.now())
            .build();
        
        return Result.success(updatedAgent);
    }
    
    private Result<Agent, AgentError> transitionToShutdown(Agent agent) {
        Agent updatedAgent = agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .currentLoad(0)
            .updatedAt(Instant.now())
            .build();
        
        return Result.success(updatedAgent);
    }
    
    private Result<Agent, AgentError> invalidTransition(Agent agent) {
        return Result.failure(new AgentError.InvalidStateTransition(
            agent.getAgentId(), agent.getStatus(), null));
    }
    
    /**
     * ✅ FUNCTIONAL AUTOMATION: Auto state management based on conditions
     * Cognitive Complexity: 4 (multiple state checks)
     */
    public CompletableFuture<Result<Agent, AgentError>> autoManageState(Agent agent) {
        return CompletableFuture.supplyAsync(() -> {
            AgentStatus recommendedState = determineOptimalState(agent);
            
            return (recommendedState != agent.getStatus()) ?
                transitionState(agent, recommendedState, "Auto state management").join() :
                Result.success(agent);
        });
    }
    
    /**
     * ✅ FUNCTIONAL STATE DETERMINATION: Determine optimal state without if-else
     * Uses functional composition for state logic
     */
    private AgentStatus determineOptimalState(Agent agent) {
        Map<Predicate<Agent>, AgentStatus> stateConditions = Map.of(
            this::isAgentFailed, AgentStatus.FAILED,
            this::isAgentOverloaded, AgentStatus.OVERLOADED,
            this::isAgentActive, AgentStatus.ACTIVE,
            this::isAgentIdle, AgentStatus.IDLE
        );
        
        return stateConditions.entrySet().stream()
            .filter(entry -> entry.getKey().test(agent))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(agent.getStatus());
    }
    
    /**
     * ✅ FUNCTIONAL PREDICATES: State condition checks
     */
    
    private boolean isAgentFailed(Agent agent) {
        return agent.hasRecentError() || 
               (agent.getLastHeartbeat() != null && 
                agent.getLastHeartbeat().isBefore(Instant.now().minusSeconds(300)));
    }
    
    private boolean isAgentOverloaded(Agent agent) {
        return agent.getCurrentLoad() >= agent.getMaxConcurrentTasks();
    }
    
    private boolean isAgentActive(Agent agent) {
        return agent.getCurrentLoad() > 0 && agent.getCurrentLoad() < agent.getMaxConcurrentTasks();
    }
    
    private boolean isAgentIdle(Agent agent) {
        return agent.getCurrentLoad() == 0;
    }
    
    /**
     * ✅ EVENT INTEGRATION: Publish state change events
     */
    private void publishStateChangeEvent(Agent agent, AgentStatus previousState, String reason) {
        eventPublisher.publishAgentStatusChanged(agent, previousState)
            .onFailure(error -> log.warn("Failed to publish state change event for agent: {}", 
                agent.getAgentId(), error.getMessage()));
    }
    
    /**
     * ✅ BATCH STATE MANAGEMENT: Manage multiple agent states
     * Cognitive Complexity: 2
     */
    public CompletableFuture<Result<java.util.List<Agent>, AgentError>> batchStateManagement(
            java.util.List<Agent> agents) {
        
        java.util.List<CompletableFuture<Result<Agent, AgentError>>> futures = agents.stream()
            .map(this::autoManageState)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                java.util.List<Agent> managedAgents = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
                
                return Result.<java.util.List<Agent>, AgentError>success(managedAgents);
            });
    }
    
    /**
     * ✅ QUERY METHODS: State information and validation
     */
    
    public boolean isValidTransition(AgentStatus fromState, AgentStatus toState) {
        return stateTransitions.containsKey(new StateTransitionKey(fromState, toState));
    }
    
    public java.util.List<AgentStatus> getValidTransitions(AgentStatus currentState) {
        return stateTransitions.keySet().stream()
            .filter(key -> key.fromState() == currentState)
            .map(StateTransitionKey::toState)
            .toList();
    }
    
    public java.util.Map<String, Object> getStateStatistics(java.util.List<Agent> agents) {
        java.util.Map<AgentStatus, Long> stateCounts = agents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Agent::getStatus,
                java.util.stream.Collectors.counting()
            ));
        
        return java.util.Map.of(
            "totalAgents", agents.size(),
            "stateCounts", stateCounts,
            "availableTransitions", stateTransitions.size()
        );
    }
}
