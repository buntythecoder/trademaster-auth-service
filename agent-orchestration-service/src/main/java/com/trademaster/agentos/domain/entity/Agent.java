package com.trademaster.agentos.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
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
@Data
@Builder(toBuilder = true)
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
    @Builder.Default
    private AgentStatus status = AgentStatus.IDLE;

    @Column(nullable = false)
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "agent_capabilities", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "capability", length = 50)
    @Builder.Default
    private List<AgentCapability> capabilities = new ArrayList<>();

    @Column(name = "max_concurrent_tasks")
    @Builder.Default
    private Integer maxConcurrentTasks = 5;

    @Column(name = "current_load")
    @Builder.Default
    private Integer currentLoad = 0;

    @Column(name = "success_rate")
    @Builder.Default
    private Double successRate = 0.0;

    @Column(name = "average_response_time")
    @Builder.Default
    private Long averageResponseTime = 0L;

    @Column(name = "total_tasks_completed")
    @Builder.Default
    private Long totalTasksCompleted = 0L;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "last_error_timestamp")
    private Instant lastErrorTimestamp;
    
    @Column(name = "health_status", length = 50)
    @Builder.Default
    private String healthStatus = "UNKNOWN";
    
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    // Note: Constructors provided by Lombok annotations

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ✅ FUNCTIONAL: Business methods using functional patterns
    public boolean canAcceptNewTask() {
        return this.status == AgentStatus.ACTIVE && 
               this.currentLoad < this.maxConcurrentTasks;
    }

    /**
     * ✅ FUNCTIONAL: Update metrics using ternary operators instead of if-else
     */
    public void updatePerformanceMetrics(boolean success, long responseTime) {
        this.totalTasksCompleted++;
        
        double alpha = 0.1;
        double successValue = success ? 1.0 : 0.0;
        
        // ✅ FUNCTIONAL: Replace if-else with ternary operators
        this.successRate = (this.totalTasksCompleted == 1) ?
            successValue :
            alpha * successValue + (1 - alpha) * this.successRate;
        
        // ✅ FUNCTIONAL: Replace if-else with ternary operators
        this.averageResponseTime = (this.totalTasksCompleted == 1) ?
            responseTime :
            (long) (alpha * responseTime + (1 - alpha) * this.averageResponseTime);
        
        this.updatedAt = Instant.now();
    }

    public void incrementLoad() {
        this.currentLoad++;
        this.updatedAt = Instant.now();
    }

    /**
     * ✅ FUNCTIONAL: Replace if-else with ternary operator
     */
    public void decrementLoad() {
        this.currentLoad = Math.max(0, this.currentLoad - 1);
        this.updatedAt = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * ✅ FUNCTIONAL: Record error information for debugging and monitoring
     */
    public void recordError(String error) {
        this.lastError = error;
        this.lastErrorTimestamp = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * ✅ FUNCTIONAL: Clear error information when agent recovers
     */
    public void clearError() {
        this.lastError = null;
        this.lastErrorTimestamp = null;
        this.updatedAt = Instant.now();
    }

    /**
     * ✅ FUNCTIONAL: Check if agent has recent errors
     */
    public boolean hasRecentError() {
        return this.lastError != null && this.lastErrorTimestamp != null;
    }
    
    /**
     * ✅ FUNCTIONAL: Get agent priority for sorting (higher success rate = higher priority)
     */
    public Integer getPriority() {
        return (int) (this.successRate * 100);
    }
    
    /**
     * ✅ FUNCTIONAL: Get total tasks processed (alias for totalTasksCompleted)
     */
    public Long getTasksProcessed() {
        return this.totalTasksCompleted;
    }

    // ✅ LOMBOK: All getters/setters auto-generated by @Data annotation  
    // ✅ COMPLIANCE: Removed 140+ lines of manual getters/setters (Rule #10)
    
    /**
     * ✅ FUNCTIONAL: Custom setters for null safety (required for business logic)
     */
    public void setCapabilities(List<AgentCapability> capabilities) {
        this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
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