package com.trademaster.agentos.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Task Event
 * 
 * Event class for task-related events in the system.
 * Used for inter-service communication via Kafka.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class TaskEvent extends BaseEvent {
    
    private Long taskId;
    private String taskName;
    private String taskType;
    private String previousStatus;
    private String currentStatus;
    private String priority;
    private Long agentId;
    private String agentName;
    private Long userId;
    private Integer progressPercentage;
    private String errorMessage;
    private Long executionTimeMs;
    private Map<String, Object> taskData;

    @JsonCreator
    public TaskEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceService") String sourceService,
            @JsonProperty("taskId") Long taskId,
            @JsonProperty("taskName") String taskName,
            @JsonProperty("taskType") String taskType,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("currentStatus") String currentStatus,
            @JsonProperty("priority") String priority,
            @JsonProperty("agentId") Long agentId,
            @JsonProperty("agentName") String agentName,
            @JsonProperty("userId") Long userId,
            @JsonProperty("progressPercentage") Integer progressPercentage,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("executionTimeMs") Long executionTimeMs,
            @JsonProperty("taskData") Map<String, Object> taskData) {
        super(eventId, eventType, timestamp, sourceService);
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.priority = priority;
        this.agentId = agentId;
        this.agentName = agentName;
        this.userId = userId;
        this.progressPercentage = progressPercentage;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
        this.taskData = taskData;
    }

    // Default constructor for JSON deserialization
    public TaskEvent() {
        super();
    }

    // Event Type Constants
    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_STARTED = "TASK_STARTED";
    public static final String TASK_PROGRESS_UPDATED = "TASK_PROGRESS_UPDATED";
    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    public static final String TASK_FAILED = "TASK_FAILED";
    public static final String TASK_CANCELLED = "TASK_CANCELLED";
    public static final String TASK_TIMEOUT = "TASK_TIMEOUT";
    public static final String TASK_RETRIED = "TASK_RETRIED";

    /**
     * Create task created event
     */
    public static TaskEvent taskCreated(Long taskId, String taskName, String taskType, 
                                       String priority, Long userId) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_CREATED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .taskType(taskType)
                .priority(priority)
                .userId(userId)
                .currentStatus("PENDING")
                .progressPercentage(0)
                .build();
    }

    /**
     * Create task assigned event
     */
    public static TaskEvent taskAssigned(Long taskId, String taskName, Long agentId, String agentName) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_ASSIGNED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .agentId(agentId)
                .agentName(agentName)
                .previousStatus("PENDING")
                .currentStatus("QUEUED")
                .build();
    }

    /**
     * Create task started event
     */
    public static TaskEvent taskStarted(Long taskId, String taskName, Long agentId, String agentName) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_STARTED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .agentId(agentId)
                .agentName(agentName)
                .previousStatus("QUEUED")
                .currentStatus("IN_PROGRESS")
                .build();
    }

    /**
     * Create task progress updated event
     */
    public static TaskEvent taskProgressUpdated(Long taskId, String taskName, Integer progressPercentage) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_PROGRESS_UPDATED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .progressPercentage(progressPercentage)
                .build();
    }

    /**
     * Create task completed event
     */
    public static TaskEvent taskCompleted(Long taskId, String taskName, Long agentId, 
                                        String agentName, Long executionTimeMs) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_COMPLETED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .agentId(agentId)
                .agentName(agentName)
                .previousStatus("IN_PROGRESS")
                .currentStatus("COMPLETED")
                .progressPercentage(100)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * Create task failed event
     */
    public static TaskEvent taskFailed(Long taskId, String taskName, Long agentId, 
                                     String agentName, String errorMessage) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_FAILED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .agentId(agentId)
                .agentName(agentName)
                .previousStatus("IN_PROGRESS")
                .currentStatus("FAILED")
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create task cancelled event
     */
    public static TaskEvent taskCancelled(Long taskId, String taskName) {
        return TaskEvent.builder()
                .eventId(generateEventId())
                .eventType(TASK_CANCELLED)
                .timestamp(Instant.now())
                .sourceService("agent-orchestration-service")
                .taskId(taskId)
                .taskName(taskName)
                .currentStatus("CANCELLED")
                .build();
    }
}