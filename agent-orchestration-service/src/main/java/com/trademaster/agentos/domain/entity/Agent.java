package com.trademaster.agentos.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent Entity
 * 
 * Represents an AI agent in the TradeMaster Agent OS.
 * Agents are autonomous entities that can perform specific trading-related tasks
 * such as market analysis, portfolio management, trading execution, and risk assessment.
 */
@Entity
@Table(name = "agents", indexes = {
    @Index(name = "idx_agent_type", columnList = "agentType"),
    @Index(name = "idx_agent_status", columnList = "status"),
    @Index(name = "idx_agent_user", columnList = "userId")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long agentId;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String agentName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentType agentType;

    @Column(length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status = AgentStatus.INACTIVE;

    @Column(nullable = false)
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "agent_capabilities", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "capability", length = 50)
    private List<AgentCapability> capabilities = new ArrayList<>();

    @Column(name = "max_concurrent_tasks")
    private Integer maxConcurrentTasks = 5;

    @Column(name = "current_load")
    private Integer currentLoad = 0;

    @Column(name = "success_rate")
    private Double successRate = 0.0;

    @Column(name = "average_response_time")
    private Long averageResponseTime = 0L;

    @Column(name = "total_tasks_completed")
    private Long totalTasksCompleted = 0L;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Task> tasks = new ArrayList<>();

    // Note: Constructors provided by Lombok annotations

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public boolean canAcceptNewTask() {
        return this.status == AgentStatus.ACTIVE && 
               this.currentLoad < this.maxConcurrentTasks;
    }

    public void updatePerformanceMetrics(boolean success, long responseTime) {
        this.totalTasksCompleted++;
        
        // Update success rate (exponential moving average)
        double alpha = 0.1;
        if (this.totalTasksCompleted == 1) {
            this.successRate = success ? 1.0 : 0.0;
        } else {
            this.successRate = alpha * (success ? 1.0 : 0.0) + (1 - alpha) * this.successRate;
        }
        
        // Update average response time (exponential moving average)
        if (this.totalTasksCompleted == 1) {
            this.averageResponseTime = responseTime;
        } else {
            this.averageResponseTime = (long) (alpha * responseTime + (1 - alpha) * this.averageResponseTime);
        }
        
        this.updatedAt = Instant.now();
    }

    public void incrementLoad() {
        this.currentLoad++;
        this.updatedAt = Instant.now();
    }

    public void decrementLoad() {
        if (this.currentLoad > 0) {
            this.currentLoad--;
        }
        this.updatedAt = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<AgentCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<AgentCapability> capabilities) {
        this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
    }

    public Integer getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(Integer maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public Integer getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(Integer currentLoad) {
        this.currentLoad = currentLoad;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Long getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(Long averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public Long getTotalTasksCompleted() {
        return totalTasksCompleted;
    }

    public void setTotalTasksCompleted(Long totalTasksCompleted) {
        this.totalTasksCompleted = totalTasksCompleted;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
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

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return Objects.equals(agentId, agent.agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "agentId=" + agentId +
                ", agentName='" + agentName + '\'' +
                ", agentType=" + agentType +
                ", status=" + status +
                ", currentLoad=" + currentLoad +
                ", successRate=" + successRate +
                '}';
    }
}