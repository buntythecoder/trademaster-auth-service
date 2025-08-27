package com.trademaster.agentos.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Agent Event
 * 
 * Event class for agent-related events in the system.
 * Used for inter-service communication via Kafka.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class AgentEvent extends BaseEvent {
    
    private Long agentId;
    private String agentName;
    private String agentType;
    private String previousStatus;
    private String currentStatus;
    private Integer currentLoad;
    private Integer maxConcurrentTasks;
    private Double successRate;
    private Map<String, Object> agentData;

    @JsonCreator
    public AgentEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceService") String sourceService,
            @JsonProperty("agentId") Long agentId,
            @JsonProperty("agentName") String agentName,
            @JsonProperty("agentType") String agentType,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("currentStatus") String currentStatus,
            @JsonProperty("currentLoad") Integer currentLoad,
            @JsonProperty("maxConcurrentTasks") Integer maxConcurrentTasks,
            @JsonProperty("successRate") Double successRate,
            @JsonProperty("agentData") Map<String, Object> agentData) {
        super(eventId, eventType, timestamp, sourceService);
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentType = agentType;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.currentLoad = currentLoad;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.successRate = successRate;
        this.agentData = agentData;
    }

    // Default constructor for JSON deserialization
    public AgentEvent() {
        super();
    }

    // Event Type Constants
    public static final String AGENT_REGISTERED = "AGENT_REGISTERED";
    public static final String AGENT_STATUS_CHANGED = "AGENT_STATUS_CHANGED";
    public static final String AGENT_HEARTBEAT_RECEIVED = "AGENT_HEARTBEAT_RECEIVED";
    public static final String AGENT_LOAD_CHANGED = "AGENT_LOAD_CHANGED";
    public static final String AGENT_PERFORMANCE_UPDATED = "AGENT_PERFORMANCE_UPDATED";
    public static final String AGENT_DEREGISTERED = "AGENT_DEREGISTERED";
    public static final String AGENT_UNRESPONSIVE = "AGENT_UNRESPONSIVE";
    public static final String AGENT_RECOVERED = "AGENT_RECOVERED";

    /**
     * Create agent registered event
     */
    public static AgentEvent agentRegistered(Long agentId, String agentName, String agentType) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_REGISTERED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .agentType(agentType)
                .currentStatus("STARTING")
                .build();
    }

    /**
     * Create agent status changed event
     */
    public static AgentEvent agentStatusChanged(Long agentId, String agentName, 
                                               String previousStatus, String currentStatus) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_STATUS_CHANGED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .build();
    }

    /**
     * Create agent heartbeat received event
     */
    public static AgentEvent agentHeartbeatReceived(Long agentId, String agentName) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_HEARTBEAT_RECEIVED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .build();
    }

    /**
     * Create agent load changed event
     */
    public static AgentEvent agentLoadChanged(Long agentId, String agentName, 
                                            Integer currentLoad, Integer maxConcurrentTasks) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_LOAD_CHANGED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .currentLoad(currentLoad)
                .maxConcurrentTasks(maxConcurrentTasks)
                .build();
    }

    /**
     * Create agent performance updated event
     */
    public static AgentEvent agentPerformanceUpdated(Long agentId, String agentName, Double successRate) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_PERFORMANCE_UPDATED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .successRate(successRate)
                .build();
    }

    /**
     * Create agent deregistered event
     */
    public static AgentEvent agentDeregistered(Long agentId, String agentName) {
        return AgentEvent.builder()
                .eventId(generateEventId())
                .eventType(AGENT_DEREGISTERED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .agentId(agentId)
                .agentName(agentName)
                .currentStatus("STOPPED")
                .build();
    }
}