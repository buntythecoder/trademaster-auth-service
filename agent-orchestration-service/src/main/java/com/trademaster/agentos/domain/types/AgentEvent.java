package com.trademaster.agentos.domain.types;

import java.time.Instant;

/**
 * ✅ SEALED CLASS: Agent Event Type Hierarchy
 * 
 * Sealed interfaces provide compile-time type safety and exhaustive pattern matching.
 * All possible event types are defined at compilation time.
 */
public sealed interface AgentEvent 
    permits AgentEvent.AgentRegistered, 
            AgentEvent.AgentDeregistered, 
            AgentEvent.AgentHealthUpdated,
            AgentEvent.AgentTaskAssigned,
            AgentEvent.AgentTaskCompleted {
    
    Long agentId();
    Instant timestamp();
    
    /**
     * ✅ RECORD: Immutable agent registration event
     */
    record AgentRegistered(
        Long agentId,
        String agentName,
        String agentType,
        Instant timestamp
    ) implements AgentEvent {}
    
    /**
     * ✅ RECORD: Immutable agent deregistration event
     */
    record AgentDeregistered(
        Long agentId,
        String reason,
        Instant timestamp
    ) implements AgentEvent {}
    
    /**
     * ✅ RECORD: Immutable agent health update event
     */
    record AgentHealthUpdated(
        Long agentId,
        String status,
        int currentLoad,
        double successRate,
        Instant timestamp
    ) implements AgentEvent {}
    
    /**
     * ✅ RECORD: Immutable task assignment event
     */
    record AgentTaskAssigned(
        Long agentId,
        Long taskId,
        String taskType,
        String priority,
        Instant timestamp
    ) implements AgentEvent {}
    
    /**
     * ✅ RECORD: Immutable task completion event
     */
    record AgentTaskCompleted(
        Long agentId,
        Long taskId,
        boolean success,
        long durationMs,
        Instant timestamp
    ) implements AgentEvent {}
    
    /**
     * ✅ PATTERN MATCHING: Type-safe event processing
     */
    default String describe() {
        return switch (this) {
            case AgentRegistered(var agentId, var name, var type, var time) -> 
                "Agent %s (%s) registered as %s at %s".formatted(agentId, name, type, time);
            case AgentDeregistered(var agentId, var reason, var time) -> 
                "Agent %s deregistered: %s at %s".formatted(agentId, reason, time);
            case AgentHealthUpdated(var agentId, var status, var load, var rate, var time) -> 
                "Agent %s status: %s (load: %d, success: %.2f) at %s".formatted(agentId, status, load, rate, time);
            case AgentTaskAssigned(var agentId, var taskId, var type, var priority, var time) -> 
                "Task %s (%s, %s) assigned to agent %s at %s".formatted(taskId, type, priority, agentId, time);
            case AgentTaskCompleted(var agentId, var taskId, var success, var duration, var time) -> 
                "Task %s completed by agent %s: %s (%dms) at %s".formatted(taskId, agentId, success ? "SUCCESS" : "FAILED", duration, time);
        };
    }
    
    /**
     * ✅ PATTERN MATCHING: Extract event severity
     */
    default EventSeverity severity() {
        return switch (this) {
            case AgentRegistered(var agentId, var name, var type, var time) -> EventSeverity.INFO;
            case AgentDeregistered(var agentId, var reason, var time) -> EventSeverity.WARNING;
            case AgentHealthUpdated(var agentId, var status, var load, var rate, var time) -> 
                rate < 0.8 ? EventSeverity.WARNING : EventSeverity.INFO;
            case AgentTaskAssigned(var agentId, var taskId, var type, var priority, var time) -> 
                "CRITICAL".equals(priority) ? EventSeverity.CRITICAL : EventSeverity.INFO;
            case AgentTaskCompleted(var agentId, var taskId, var success, var duration, var time) -> 
                success ? EventSeverity.INFO : EventSeverity.ERROR;
        };
    }
    
    /**
     * ✅ SEALED ENUM: Event severity levels
     */
    enum EventSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
}