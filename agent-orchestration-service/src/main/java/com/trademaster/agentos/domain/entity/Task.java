package com.trademaster.agentos.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Task Entity
 * 
 * Represents a unit of work that can be executed by an AI agent in the TradeMaster Agent OS.
 * Tasks encapsulate specific operations like market analysis, trade execution, or risk assessment.
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_status", columnList = "status"),
    @Index(name = "idx_task_type", columnList = "taskType"),
    @Index(name = "idx_task_priority", columnList = "priority"),
    @Index(name = "idx_task_agent", columnList = "agent_id"),
    @Index(name = "idx_task_user", columnList = "userId"),
    @Index(name = "idx_task_created", columnList = "createdAt"),
    @Index(name = "idx_task_deadline", columnList = "deadline")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String taskName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskType taskType;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.NORMAL;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    @JsonIgnore
    private Agent agent;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "task_required_capabilities", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "capability", length = 50)
    private List<AgentCapability> requiredCapabilities = new ArrayList<>();

    @Column(name = "input_parameters", columnDefinition = "TEXT")
    private String inputParameters; // JSON string

    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult; // JSON string

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300; // 5 minutes default

    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors
    public Task() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Task(String taskName, TaskType taskType, Long userId) {
        this();
        this.taskName = taskName;
        this.taskType = taskType;
        this.userId = userId;
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public boolean canRetry() {
        return this.retryCount < this.maxRetries && 
               (this.status == TaskStatus.FAILED || this.status == TaskStatus.ERROR);
    }

    public boolean isExpired() {
        return this.deadline != null && Instant.now().isAfter(this.deadline);
    }

    public boolean isTimedOut() {
        if (this.startedAt == null || this.status != TaskStatus.IN_PROGRESS) {
            return false;
        }
        
        long elapsedSeconds = Instant.now().getEpochSecond() - this.startedAt.getEpochSecond();
        return elapsedSeconds > this.timeoutSeconds;
    }

    public void markAsStarted() {
        this.status = TaskStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markAsCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.outputResult = result;
        this.progressPercentage = 100;
        
        if (this.startedAt != null) {
            this.actualDurationSeconds = (int) (this.completedAt.getEpochSecond() - this.startedAt.getEpochSecond());
        }
        
        this.updatedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markAsError(String errorMessage) {
        this.status = TaskStatus.ERROR;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public void updateProgress(int percentage) {
        this.progressPercentage = Math.max(0, Math.min(100, percentage));
        this.updatedAt = Instant.now();
    }

    public long getElapsedTimeSeconds() {
        if (this.startedAt == null) {
            return 0;
        }
        
        Instant endTime = this.completedAt != null ? this.completedAt : Instant.now();
        return endTime.getEpochSecond() - this.startedAt.getEpochSecond();
    }

    public boolean requiresCapability(AgentCapability capability) {
        return this.requiredCapabilities.contains(capability);
    }

    /**
     * Convenience method to get the agent ID
     */
    public Long getAgentId() {
        return this.agent != null ? this.agent.getAgentId() : null;
    }
    
    /**
     * Convenience method to set the agent by ID (sets to null for null ID)
     */
    public void setAgentId(Long agentId) {
        if (agentId == null) {
            this.agent = null;
        } else {
            // Create a minimal agent object for the relationship
            Agent tempAgent = new Agent();
            tempAgent.setAgentId(agentId);
            this.agent = tempAgent;
        }
    }
    
    /**
     * Get the assigned agent type as string
     */
    public String getAssignedAgentType() {
        return this.agent != null && this.agent.getAgentType() != null ? 
               this.agent.getAgentType().toString() : null;
    }
    
    /**
     * Get estimated duration in seconds
     */
    public Long getEstimatedDuration() {
        return this.estimatedDurationSeconds != null ? this.estimatedDurationSeconds.longValue() : null;
    }
    
    /**
     * Get actual duration in seconds  
     */
    public Long getActualDuration() {
        return this.actualDurationSeconds != null ? this.actualDurationSeconds.longValue() : null;
    }

    // Getters and Setters
    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public List<AgentCapability> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<AgentCapability> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities != null ? requiredCapabilities : new ArrayList<>();
    }

    public String getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(String inputParameters) {
        this.inputParameters = inputParameters;
    }

    public String getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(String outputResult) {
        this.outputResult = outputResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public Integer getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public void setActualDurationSeconds(Integer actualDurationSeconds) {
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", taskName='" + taskName + '\'' +
                ", taskType=" + taskType +
                ", status=" + status +
                ", priority=" + priority +
                ", progressPercentage=" + progressPercentage +
                ", userId=" + userId +
                '}';
    }
}