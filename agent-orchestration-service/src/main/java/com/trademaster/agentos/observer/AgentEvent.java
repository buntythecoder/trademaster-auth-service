package com.trademaster.agentos.observer;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import lombok.Value;
import lombok.NonNull;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ FUNCTIONAL: Immutable Agent Event
 * 
 * Represents an event in the agent lifecycle using functional programming principles.
 * Provides type-safe event creation and rich contextual information.
 * 
 * Features:
 * - Immutable event data structure
 * - Type-safe event types
 * - Rich contextual metadata
 * - Functional composition support
 */
@Value
public class AgentEvent {
    
    @NonNull Long agentId;
    @NonNull String agentName;
    @NonNull AgentEventType eventType;
    @NonNull Instant timestamp;
    @NonNull AgentStatus previousStatus;
    @NonNull AgentStatus currentStatus;
    @NonNull Map<String, Object> metadata;
    Optional<String> errorMessage;
    Optional<String> source;
    
    /**
     * ✅ FUNCTIONAL: Agent Event Types
     */
    public enum AgentEventType {
        // Lifecycle Events
        AGENT_CREATED("Agent created and initialized"),
        AGENT_STARTED("Agent started successfully"),
        AGENT_STOPPED("Agent stopped"),
        AGENT_SHUTDOWN("Agent shutdown completed"),
        
        // State Transition Events
        STATUS_CHANGED("Agent status changed"),
        ENTERING_MAINTENANCE("Agent entering maintenance mode"),
        EXITING_MAINTENANCE("Agent exiting maintenance mode"),
        OVERLOAD_DETECTED("Agent overload detected"),
        OVERLOAD_RESOLVED("Agent overload resolved"),
        
        // Task Events
        TASK_ASSIGNED("Task assigned to agent"),
        TASK_COMPLETED("Task completed by agent"),
        TASK_FAILED("Task failed on agent"),
        
        // Health Events
        HEARTBEAT_RECEIVED("Agent heartbeat received"),
        HEARTBEAT_MISSED("Agent heartbeat missed"),
        RECOVERY_INITIATED("Agent recovery initiated"),
        RECOVERY_COMPLETED("Agent recovery completed"),
        RECOVERY_FAILED("Agent recovery failed"),
        
        // Error Events
        ERROR_OCCURRED("Error occurred in agent"),
        CRITICAL_ERROR("Critical error in agent"),
        
        // Performance Events
        PERFORMANCE_DEGRADATION("Agent performance degraded"),
        PERFORMANCE_IMPROVED("Agent performance improved"),
        
        // Security Events
        SECURITY_VIOLATION("Security violation detected"),
        UNAUTHORIZED_ACCESS("Unauthorized access attempt"),
        
        // Configuration Events
        CONFIGURATION_UPDATED("Agent configuration updated"),
        CAPABILITY_CHANGED("Agent capability changed");
        
        private final String description;
        
        AgentEventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * ✅ FUNCTIONAL: Check if event type is critical
         */
        public boolean isCritical() {
            return switch (this) {
                case CRITICAL_ERROR, SECURITY_VIOLATION, UNAUTHORIZED_ACCESS, 
                     RECOVERY_FAILED, AGENT_SHUTDOWN -> true;
                default -> false;
            };
        }
        
        /**
         * ✅ FUNCTIONAL: Check if event type is related to health
         */
        public boolean isHealthRelated() {
            return switch (this) {
                case HEARTBEAT_RECEIVED, HEARTBEAT_MISSED, RECOVERY_INITIATED,
                     RECOVERY_COMPLETED, RECOVERY_FAILED, PERFORMANCE_DEGRADATION,
                     PERFORMANCE_IMPROVED -> true;
                default -> false;
            };
        }
        
        /**
         * ✅ FUNCTIONAL: Check if event type is related to tasks
         */
        public boolean isTaskRelated() {
            return switch (this) {
                case TASK_ASSIGNED, TASK_COMPLETED, TASK_FAILED -> true;
                default -> false;
            };
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Create event builder for functional composition
     */
    public static EventBuilder builder() {
        return new EventBuilder();
    }
    
    /**
     * ✅ FUNCTIONAL: Create event from agent state change
     */
    public static AgentEvent statusChanged(Agent agent, AgentStatus previousStatus) {
        return builder()
            .agentId(agent.getAgentId())
            .agentName(agent.getAgentName())
            .eventType(AgentEventType.STATUS_CHANGED)
            .previousStatus(previousStatus)
            .currentStatus(agent.getStatus())
            .metadata(Map.of(
                "currentLoad", agent.getCurrentLoad(),
                "maxConcurrentTasks", agent.getMaxConcurrentTasks(),
                "successRate", agent.getSuccessRate(),
                "averageResponseTime", agent.getAverageResponseTime()
            ))
            .build();
    }
    
    /**
     * ✅ FUNCTIONAL: Create error event
     */
    public static AgentEvent errorOccurred(Agent agent, String errorMessage) {
        return builder()
            .agentId(agent.getAgentId())
            .agentName(agent.getAgentName())
            .eventType(AgentEventType.ERROR_OCCURRED)
            .previousStatus(agent.getStatus())
            .currentStatus(agent.getStatus())
            .errorMessage(errorMessage)
            .metadata(Map.of(
                "lastError", agent.getLastError(),
                "errorCount", getErrorCountFromAgent(agent)
            ))
            .build();
    }
    
    /**
     * ✅ FUNCTIONAL: Create task event
     */
    public static AgentEvent taskEvent(Agent agent, AgentEventType eventType, Map<String, Object> taskMetadata) {
        return builder()
            .agentId(agent.getAgentId())
            .agentName(agent.getAgentName())
            .eventType(eventType)
            .previousStatus(agent.getStatus())
            .currentStatus(agent.getStatus())
            .metadata(taskMetadata)
            .build();
    }
    
    /**
     * ✅ FUNCTIONAL: Create performance event
     */
    public static AgentEvent performanceEvent(Agent agent, AgentEventType eventType, 
                                            long executionTime, boolean success) {
        return builder()
            .agentId(agent.getAgentId())
            .agentName(agent.getAgentName())
            .eventType(eventType)
            .previousStatus(agent.getStatus())
            .currentStatus(agent.getStatus())
            .metadata(Map.of(
                "executionTime", executionTime,
                "success", success,
                "averageResponseTime", agent.getAverageResponseTime(),
                "successRate", agent.getSuccessRate()
            ))
            .build();
    }
    
    /**
     * ✅ FUNCTIONAL: Check if this event indicates a problem
     */
    public boolean isProblematic() {
        return eventType.isCritical() || 
               eventType == AgentEventType.TASK_FAILED ||
               eventType == AgentEventType.HEARTBEAT_MISSED ||
               eventType == AgentEventType.PERFORMANCE_DEGRADATION ||
               errorMessage.isPresent();
    }
    
    /**
     * ✅ FUNCTIONAL: Get severity level
     */
    public Severity getSeverity() {
        if (eventType.isCritical()) return Severity.CRITICAL;
        if (isProblematic()) return Severity.WARNING;
        if (eventType.isHealthRelated() || eventType.isTaskRelated()) return Severity.INFO;
        return Severity.DEBUG;
    }
    
    public enum Severity {
        DEBUG, INFO, WARNING, CRITICAL
    }
    
    /**
     * ✅ FUNCTIONAL: Event Builder using functional composition
     */
    public static class EventBuilder {
        private Long agentId;
        private String agentName;
        private AgentEventType eventType;
        private AgentStatus previousStatus;
        private AgentStatus currentStatus;
        private Map<String, Object> metadata = Map.of();
        private Optional<String> errorMessage = Optional.empty();
        private Optional<String> source = Optional.empty();
        
        public EventBuilder agentId(Long agentId) {
            this.agentId = agentId;
            return this;
        }
        
        public EventBuilder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }
        
        public EventBuilder eventType(AgentEventType eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public EventBuilder previousStatus(AgentStatus previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }
        
        public EventBuilder currentStatus(AgentStatus currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }
        
        public EventBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public EventBuilder errorMessage(String errorMessage) {
            this.errorMessage = Optional.ofNullable(errorMessage);
            return this;
        }
        
        public EventBuilder source(String source) {
            this.source = Optional.ofNullable(source);
            return this;
        }
        
        public EventBuilder severity(Severity severity) {
            // Note: This method exists for builder compatibility but severity is computed automatically in getSeverity()
            return this;
        }
        
        public EventBuilder timestamp(java.time.Instant timestamp) {
            // Note: Timestamp is set automatically in constructor, this method exists for builder compatibility
            return this;
        }
        
        public AgentEvent build() {
            return new AgentEvent(
                agentId,
                agentName,
                eventType,
                Instant.now(),
                previousStatus,
                currentStatus,
                metadata,
                errorMessage,
                source
            );
        }
    }
    
    private static int getErrorCountFromAgent(Agent agent) {
        // In a real implementation, this would fetch from agent metrics
        return agent.getLastError() != null ? 1 : 0;
    }
}