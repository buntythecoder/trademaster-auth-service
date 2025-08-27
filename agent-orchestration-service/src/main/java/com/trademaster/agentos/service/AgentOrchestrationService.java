package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.AgentCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Agent Orchestration Service
 * 
 * Core orchestration engine that coordinates task assignment, agent selection,
 * load balancing, and system-wide agent management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentOrchestrationService {

    private final AgentService agentService;
    private final TaskService taskService;

    // Core Orchestration Methods

    /**
     * Submit a task for execution by the agent system
     */
    public Task submitTask(Task task) {
        log.info("Submitting task for orchestration: {} (type: {}, priority: {})", 
                task.getTaskName(), task.getTaskType(), task.getPriority());
        
        // Create the task
        Task createdTask = taskService.createTask(task);
        
        // Try to assign immediately if high priority
        if (task.getPriority() == TaskPriority.CRITICAL || task.getPriority() == TaskPriority.HIGH) {
            tryAssignTaskImmediately(createdTask);
        }
        
        return createdTask;
    }

    /**
     * Attempt to assign a task immediately to an available agent
     */
    private void tryAssignTaskImmediately(Task task) {
        log.debug("Attempting immediate assignment for task: {}", task.getTaskName());
        
        // Determine required agent type based on task type
        AgentType requiredAgentType = determineRequiredAgentType(task.getTaskType());
        
        // Find optimal agent
        Optional<Agent> optimalAgent = agentService.findOptimalAgentForTask(
                requiredAgentType, 
                task.getRequiredCapabilities()
        );
        
        if (optimalAgent.isPresent()) {
            Agent agent = optimalAgent.get();
            assignTaskToAgent(task.getTaskId(), agent.getAgentId());
            log.info("Immediately assigned task {} to agent {}", task.getTaskName(), agent.getAgentName());
        } else {
            log.debug("No available agents for immediate assignment of task: {}", task.getTaskName());
        }
    }

    /**
     * Assign a task to a specific agent
     */
    public void assignTaskToAgent(Long taskId, Long agentId) {
        log.info("Assigning task {} to agent {}", taskId, agentId);
        
        Optional<Task> taskOpt = taskService.findById(taskId);
        Optional<Agent> agentOpt = agentService.findById(agentId);
        
        if (taskOpt.isEmpty()) {
            log.error("Cannot assign task - task not found: {}", taskId);
            return;
        }
        
        if (agentOpt.isEmpty()) {
            log.error("Cannot assign task - agent not found: {}", agentId);
            return;
        }
        
        Task task = taskOpt.get();
        Agent agent = agentOpt.get();
        
        // Validate agent can handle the task
        if (!canAgentHandleTask(agent, task)) {
            log.error("Agent {} cannot handle task {} - capability mismatch", 
                     agent.getAgentName(), task.getTaskName());
            return;
        }
        
        // Check agent availability
        if (agent.getStatus() != AgentStatus.ACTIVE && agent.getStatus() != AgentStatus.BUSY) {
            log.error("Cannot assign task to agent {} - agent not available (status: {})", 
                     agent.getAgentName(), agent.getStatus());
            return;
        }
        
        if (agent.getCurrentLoad() >= agent.getMaxConcurrentTasks()) {
            log.error("Cannot assign task to agent {} - agent at maximum capacity ({}/{})", 
                     agent.getAgentName(), agent.getCurrentLoad(), agent.getMaxConcurrentTasks());
            return;
        }
        
        // Perform the assignment
        taskService.assignTaskToAgent(taskId, agentId);
        agentService.incrementAgentLoad(agentId);
        
        log.info("Successfully assigned task {} to agent {}", task.getTaskName(), agent.getAgentName());
    }

    /**
     * Process task completion notification
     */
    public void notifyTaskCompletion(Long taskId, boolean success, String result, Long responseTimeMs) {
        log.info("Processing task completion notification for task: {} (success: {})", taskId, success);
        
        Optional<Task> taskOpt = taskService.findById(taskId);
        if (taskOpt.isEmpty()) {
            log.error("Cannot process completion - task not found: {}", taskId);
            return;
        }
        
        Task task = taskOpt.get();
        
        if (success) {
            taskService.completeTask(taskId, result);
        } else {
            taskService.failTask(taskId, result);
        }
        
        // Update agent metrics if task was assigned to an agent
        if (task.getAgentId() != null) {
            agentService.decrementAgentLoad(task.getAgentId());
            agentService.updatePerformanceMetrics(task.getAgentId(), success, responseTimeMs);
        }
        
        log.info("Processed task completion for: {}", task.getTaskName());
    }

    // Scheduled Operations

    /**
     * Periodic task queue processing
     * Runs every 10 seconds to process pending tasks
     */
    @Scheduled(fixedRate = 10000) // 10 seconds
    public void processTaskQueue() {
        log.debug("Processing task queue");
        
        try {
            // Get high priority tasks first
            List<Task> highPriorityTasks = taskService.getHighPriorityTasks();
            for (Task task : highPriorityTasks) {
                if (task.getStatus() == TaskStatus.PENDING) {
                    tryAssignTaskImmediately(task);
                }
            }
            
            // Process regular queue
            Optional<Task> nextTask = taskService.getNextTaskFromQueue();
            while (nextTask.isPresent()) {
                Task task = nextTask.get();
                
                // Find available agent for the task
                AgentType requiredType = determineRequiredAgentType(task.getTaskType());
                Optional<Agent> availableAgent = agentService.findOptimalAgentForTask(
                        requiredType, task.getRequiredCapabilities());
                
                if (availableAgent.isPresent()) {
                    // Start task execution
                    taskService.updateTaskStatus(task.getTaskId(), TaskStatus.IN_PROGRESS);
                    log.debug("Started execution of task: {}", task.getTaskName());
                } else {
                    log.debug("No available agents for task: {}", task.getTaskName());
                    break; // Wait for agents to become available
                }
                
                nextTask = taskService.getNextTaskFromQueue();
            }
            
        } catch (Exception e) {
            log.error("Error processing task queue", e);
        }
    }

    /**
     * Periodic health check for agents and tasks
     * Runs every minute to check system health
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void performSystemHealthCheck() {
        log.debug("Performing system health check");
        
        try {
            // Check agent health
            agentService.performHealthCheck();
            
            // Handle timed out tasks
            taskService.handleTimedOutTasks();
            
            // Handle retriable tasks
            taskService.handleRetriableTasks();
            
            // Check for overdue tasks
            taskService.handleOverdueTasks();
            
        } catch (Exception e) {
            log.error("Error during system health check", e);
        }
    }

    /**
     * Periodic cleanup of old data
     * Runs every hour to clean up old completed tasks
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void performSystemCleanup() {
        log.debug("Performing system cleanup");
        
        try {
            taskService.cleanupOldTasks();
        } catch (Exception e) {
            log.error("Error during system cleanup", e);
        }
    }

    // Agent Management

    /**
     * Register a new agent with the orchestration system
     */
    public Agent registerAgent(Agent agent) {
        log.info("Registering agent with orchestration system: {}", agent.getAgentName());
        
        CompletableFuture<Agent> registeredAgentFuture = agentService.registerAgent(agent);
        Agent registeredAgent = registeredAgentFuture.join();
        
        // Agent starts in STARTING status, will become ACTIVE when it sends first heartbeat
        log.info("Agent {} registered successfully and ready for task assignment", 
                registeredAgent.getAgentName());
        
        return registeredAgent;
    }

    /**
     * Process agent heartbeat
     */
    public void processAgentHeartbeat(Long agentId) {
        agentService.processHeartbeat(agentId);
    }

    /**
     * Deregister an agent from the orchestration system
     */
    public void deregisterAgent(Long agentId) {
        log.info("Deregistering agent from orchestration system: {}", agentId);
        
        // First, handle any tasks assigned to this agent
        List<Task> assignedTasks = taskService.findByAgentId(agentId);
        for (Task task : assignedTasks) {
            if (task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.QUEUED) {
                log.warn("Reassigning task {} due to agent deregistration", task.getTaskName());
                // Reset task for reassignment
                taskService.updateTaskStatus(task.getTaskId(), TaskStatus.PENDING);
                task.setAgentId(null);
            }
        }
        
        agentService.deregisterAgent(agentId);
        
        log.info("Agent {} deregistered successfully", agentId);
    }

    // System Metrics and Monitoring

    /**
     * Get system orchestration metrics
     */
    public OrchestrationMetrics getOrchestrationMetrics() {
        AgentService.AgentHealthSummary agentHealth = agentService.getSystemHealthSummary();
        TaskService.TaskStatisticsSummary taskStats = taskService.getTaskStatistics();
        
        return OrchestrationMetrics.builder()
                .totalAgents(agentHealth.getTotalAgents())
                .activeAgents(agentHealth.getActiveAgents())
                .busyAgents(agentHealth.getBusyAgents())
                .errorAgents(agentHealth.getErrorAgents())
                .averageAgentLoad(agentHealth.getAverageLoad())
                .averageSuccessRate(agentHealth.getAverageSuccessRate())
                .totalTasks(taskStats.getTotalTasks())
                .pendingTasks(taskStats.getPendingTasks())
                .queuedTasks(taskStats.getQueuedTasks())
                .inProgressTasks(taskStats.getInProgressTasks())
                .completedTasks(taskStats.getCompletedTasks())
                .failedTasks(taskStats.getFailedTasks())
                .averageTaskDuration(taskStats.getAverageDuration())
                .systemUtilization(calculateSystemUtilization(agentHealth))
                .build();
    }

    // Helper Methods

    /**
     * Determine required agent type based on task type
     */
    private AgentType determineRequiredAgentType(com.trademaster.agentos.domain.entity.TaskType taskType) {
        return switch (taskType) {
            case MARKET_ANALYSIS, TECHNICAL_ANALYSIS, FUNDAMENTAL_ANALYSIS, SENTIMENT_ANALYSIS, 
                 MARKET_SCREENING, PRICE_PREDICTION -> AgentType.MARKET_ANALYSIS;
            
            case PORTFOLIO_ANALYSIS, PORTFOLIO_OPTIMIZATION, ASSET_ALLOCATION, 
                 REBALANCING, PERFORMANCE_ANALYSIS, DIVERSIFICATION_ANALYSIS -> AgentType.PORTFOLIO_MANAGEMENT;
            
            case ORDER_EXECUTION, SMART_ROUTING, EXECUTION_MONITORING, 
                 SLIPPAGE_ANALYSIS, LIQUIDITY_ANALYSIS, ALGORITHMIC_EXECUTION -> AgentType.TRADING_EXECUTION;
            
            case RISK_ASSESSMENT, RISK_MONITORING, VAR_CALCULATION, 
                 STRESS_TESTING, COMPLIANCE_CHECK, DRAWDOWN_MONITORING -> AgentType.RISK_MANAGEMENT;
            
            case ALERT_GENERATION, REPORT_GENERATION, EMAIL_NOTIFICATION, 
                 SMS_NOTIFICATION, PUSH_NOTIFICATION -> AgentType.NOTIFICATION;
            
            default -> AgentType.CUSTOM;
        };
    }

    /**
     * Check if an agent can handle a specific task
     */
    private boolean canAgentHandleTask(Agent agent, Task task) {
        // Check if agent has required capabilities
        for (AgentCapability requiredCapability : task.getRequiredCapabilities()) {
            if (!agent.getCapabilities().contains(requiredCapability)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate system utilization percentage
     */
    private Double calculateSystemUtilization(AgentService.AgentHealthSummary agentHealth) {
        if (agentHealth.getTotalAgents() == 0) {
            return 0.0;
        }
        
        long busyAgents = agentHealth.getBusyAgents();
        long totalAgents = agentHealth.getTotalAgents();
        
        return (busyAgents * 100.0) / totalAgents;
    }

    // Helper Classes

    @lombok.Data
    @lombok.Builder
    public static class OrchestrationMetrics {
        // Agent Metrics
        private Long totalAgents;
        private Long activeAgents;
        private Long busyAgents;
        private Long errorAgents;
        private Double averageAgentLoad;
        private Double averageSuccessRate;
        
        // Task Metrics
        private Long totalTasks;
        private Long pendingTasks;
        private Long queuedTasks;
        private Long inProgressTasks;
        private Long completedTasks;
        private Long failedTasks;
        private Double averageTaskDuration;
        
        // System Metrics
        private Double systemUtilization;
    }
}