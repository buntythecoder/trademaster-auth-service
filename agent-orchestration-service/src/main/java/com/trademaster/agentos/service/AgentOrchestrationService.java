package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.service.IAgentHealthService.AgentHealthSummary;
import com.trademaster.agentos.mediator.AgentInteractionMediator;
import com.trademaster.agentos.mediator.AgentInteractionMediator.InteractionContext;
import com.trademaster.agentos.mediator.AgentInteractionMediator.InteractionType;
import com.trademaster.agentos.mediator.AgentInteractionMediator.InteractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    private final AgentInteractionMediator interactionMediator;

    // Core Orchestration Methods
    
    /**
     * Process agent heartbeat asynchronously using virtual threads.
     * Used by the secure controller for non-blocking heartbeat processing.
     */
    public CompletableFuture<Void> processAgentHeartbeatAsync(Long agentId) {
        return CompletableFuture.runAsync(() -> {
            try {
                processAgentHeartbeat(agentId);
                log.debug("Async heartbeat processed for agent: {}", agentId);
            } catch (Exception e) {
                log.error("Error processing async heartbeat for agent: {}", agentId, e);
                throw new RuntimeException("Heartbeat processing failed", e);
            }
        });
    }

    /**
     * ✅ FUNCTIONAL: Submit task with priority-based assignment strategy
     */
    public Task submitTask(Task task) {
        log.info("Submitting task for orchestration: {} (type: {}, priority: {})", 
                task.getTaskName(), task.getTaskType(), task.getPriority());
        
        Task createdTask = taskService.createTask(task);
        
        // ✅ FUNCTIONAL: Replace if-else with strategy pattern
        Optional.of(task)
            .filter(this::isHighPriorityTask)
            .ifPresent(t -> tryAssignTaskImmediately(createdTask));
        
        return createdTask;
    }
    
    /**
     * ✅ FUNCTIONAL: Pure function to check high priority
     */
    private boolean isHighPriorityTask(Task task) {
        return Set.of(TaskPriority.CRITICAL, TaskPriority.HIGH)
            .contains(task.getPriority());
    }

    /**
     * ✅ FUNCTIONAL: Immediate task assignment using functional pipeline
     */
    private void tryAssignTaskImmediately(Task task) {
        log.debug("Attempting immediate assignment for task: {}", task.getTaskName());
        
        AgentType requiredAgentType = determineRequiredAgentType(task.getTaskType());
        
        // ✅ FUNCTIONAL: Replace if-else with functional pipeline
        agentService.findOptimalAgentForTask(requiredAgentType, task.getRequiredCapabilities())
            .toOptional()
            .ifPresentOrElse(
                agent -> {
                    assignTaskToAgentFunctional(task.getTaskId(), agent.getAgentId());
                    log.info("Immediately assigned task {} to agent {}", task.getTaskName(), agent.getAgentName());
                },
                () -> log.debug("No available agents for immediate assignment of task: {}", task.getTaskName())
            );
    }

    /**
     * ✅ FUNCTIONAL: Functional task assignment using Result monad
     */
    public Result<String, AgentError> assignTaskToAgentFunctional(Long taskId, Long agentId) {
        log.info("Assigning task {} to agent {}", taskId, agentId);
        
        return validateTaskAndAgent(taskId, agentId)
            .flatMap(pair -> validateAgentCapabilities(pair.task(), pair.agent()))
            .flatMap(pair -> validateAgentAvailability(pair.task(), pair.agent()))
            .flatMap(pair -> validateAgentCapacity(pair.task(), pair.agent()))
            .map(pair -> performAssignment(pair.task(), pair.agent()))
            .onFailure(error -> log.error("Task assignment failed: {}", error.getMessage()))
            .onSuccess(msg -> log.info("Task assignment successful: {}", msg));
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void assignTaskToAgent(Long taskId, Long agentId) {
        assignTaskToAgentFunctional(taskId, agentId);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate task and agent existence
     */
    private Result<TaskAgentPair, AgentError> validateTaskAndAgent(Long taskId, Long agentId) {
        Optional<Task> taskOpt = taskService.findById(taskId);
        Optional<Agent> agentOpt = agentService.findById(agentId);
        
        return taskOpt.isEmpty() ?
            Result.failure(new AgentError.ValidationError("taskId", "Task not found: " + taskId)) :
            agentOpt.isEmpty() ?
                Result.failure(new AgentError.ValidationError("agentId", "Agent not found: " + agentId)) :
                Result.success(new TaskAgentPair(taskOpt.get(), agentOpt.get()));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent capabilities
     */
    private Result<TaskAgentPair, AgentError> validateAgentCapabilities(Task task, Agent agent) {
        return canAgentHandleTask(agent, task) ?
            Result.success(new TaskAgentPair(task, agent)) :
            Result.failure(new AgentError.ValidationError("capabilities",
                String.format("Agent %s cannot handle task %s - capability mismatch", 
                    agent.getAgentName(), task.getTaskName())));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent availability
     */
    private Result<TaskAgentPair, AgentError> validateAgentAvailability(Task task, Agent agent) {
        return Set.of(AgentStatus.ACTIVE, AgentStatus.OVERLOADED).contains(agent.getStatus()) ?
            Result.success(new TaskAgentPair(task, agent)) :
            Result.failure(new AgentError.ValidationError("availability",
                String.format("Agent %s not available (status: %s)", 
                    agent.getAgentName(), agent.getStatus())));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent capacity
     */
    private Result<TaskAgentPair, AgentError> validateAgentCapacity(Task task, Agent agent) {
        return agent.getCurrentLoad() < agent.getMaxConcurrentTasks() ?
            Result.success(new TaskAgentPair(task, agent)) :
            Result.failure(new AgentError.ValidationError("capacity",
                String.format("Agent %s at maximum capacity (%d/%d)", 
                    agent.getAgentName(), agent.getCurrentLoad(), agent.getMaxConcurrentTasks())));
    }
    
    /**
     * ✅ FUNCTIONAL: Perform the assignment
     */
    private String performAssignment(Task task, Agent agent) {
        taskService.assignTaskToAgent(task.getTaskId(), agent.getAgentId());
        agentService.incrementAgentLoad(agent.getAgentId());
        return String.format("Successfully assigned task %s to agent %s", 
            task.getTaskName(), agent.getAgentName());
    }
    
    /**
     * ✅ IMMUTABLE: Task-Agent pair record
     */
    private record TaskAgentPair(Task task, Agent agent) {}

    /**
     * ✅ FUNCTIONAL: Process task completion using functional pipeline
     */
    public Result<String, AgentError> notifyTaskCompletion(Long taskId, boolean success, String result, Long responseTimeMs) {
        log.info("Processing task completion notification for task: {} (success: {})", taskId, success);
        
        Result<Task, AgentError> taskResult = Result.fromOptional(taskService.findById(taskId), 
                new AgentError.NotFound(taskId, "notifyTaskCompletion"));
        
        Result<Task, AgentError> processedResult = taskResult
            .map(task -> processTaskCompletion(task, success, result))
            .flatMap(task -> updateAgentMetricsIfAssigned(task, success, responseTimeMs));
        
        return processedResult
            .<String>map(task -> {
                log.info("Processed task completion for: {}", task.getTaskName());
                return "Task completion processed successfully";
            })
            .onFailure(error -> log.error("Task completion processing failed: {}", error.getMessage()));
    }
    
    /**
     * ✅ FUNCTIONAL: Process task completion outcome
     */
    private Task processTaskCompletion(Task task, boolean success, String result) {
        if (success) {
            taskService.completeTask(task.getTaskId(), result);
        } else {
            taskService.failTask(task.getTaskId(), result);
        }
        return task; // Return the original task since service methods are void
    }
    
    /**
     * ✅ FUNCTIONAL: Update agent metrics if task was assigned
     */
    private Result<Task, AgentError> updateAgentMetricsIfAssigned(Task task, boolean success, Long responseTimeMs) {
        return Optional.ofNullable(task.getAgentId())
            .map(agentId -> {
                agentService.decrementAgentLoad(agentId);
                agentService.updatePerformanceMetrics(agentId, success, responseTimeMs);
                return Result.<Task, AgentError>success(task);
            })
            .orElse(Result.success(task));
    }

    // Scheduled Operations

    /**
     * ✅ FUNCTIONAL: Process task queue using streams and functional composition
     */
    @Scheduled(fixedRate = 10000)
    public void processTaskQueue() {
        log.debug("Processing task queue");
        
        Result.catching(() -> {
            processHighPriorityTasks();
            processRegularTaskQueue();
            return "Task queue processed successfully";
        })
        .onFailure(error -> log.error("Error processing task queue", error));
    }
    
    /**
     * ✅ FUNCTIONAL: Process high priority tasks using streams
     */
    private void processHighPriorityTasks() {
        taskService.getHighPriorityTasks().stream()
            .filter(task -> task.getStatus() == TaskStatus.PENDING)
            .forEach(this::tryAssignTaskImmediately);
    }
    
    /**
     * ✅ FUNCTIONAL: Process regular queue using recursive tail call
     */
    private void processRegularTaskQueue() {
        processNextTaskRecursively();
    }
    
    /**
     * ✅ FUNCTIONAL: Recursive task processing (tail call optimization)
     */
    private void processNextTaskRecursively() {
        Optional<Task> nextTask = taskService.getNextTaskFromQueue();
        
        nextTask.ifPresent(task -> {
            AgentType requiredType = determineRequiredAgentType(task.getTaskType());
            
            agentService.findOptimalAgentForTask(requiredType, task.getRequiredCapabilities())
                .toOptional()
                .ifPresentOrElse(
                    agent -> {
                        taskService.updateTaskStatus(task.getTaskId(), TaskStatus.IN_PROGRESS);
                        log.debug("Started execution of task: {}", task.getTaskName());
                        processNextTaskRecursively(); // Continue processing
                    },
                    () -> log.debug("No available agents for task: {}", task.getTaskName())
                );
        });
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
     * ✅ FUNCTIONAL: Deregister agent using functional pipeline
     */
    public Result<String, AgentError> deregisterAgent(Long agentId) {
        log.info("Deregistering agent from orchestration system: {}", agentId);
        
        return reassignTasksFromAgent(agentId)
            .flatMap(count -> agentService.deregisterAgent(agentId)
                .map(result -> String.format("Agent %d deregistered successfully, %d tasks reassigned", agentId, count)))
            .onSuccess(result -> log.info(result))
            .onFailure(error -> log.error("Agent deregistration failed: {}", error.getMessage()));
    }
    
    /**
     * ✅ FUNCTIONAL: Reassign tasks using streams
     */
    private Result<Integer, AgentError> reassignTasksFromAgent(Long agentId) {
        List<Task> reassignedTasks = taskService.findByAgentId(agentId).stream()
            .filter(task -> Set.of(TaskStatus.IN_PROGRESS, TaskStatus.QUEUED).contains(task.getStatus()))
            .peek(task -> log.warn("Reassigning task {} due to agent deregistration", task.getTaskName()))
            .peek(task -> {
                taskService.updateTaskStatus(task.getTaskId(), TaskStatus.PENDING);
                task.setAgentId(null);
            })
            .toList();
        
        return Result.success(reassignedTasks.size());
    }

    // System Metrics and Monitoring

    /**
     * Get system orchestration metrics
     */
    public OrchestrationMetrics getOrchestrationMetrics() {
        AgentHealthSummary agentHealth = agentService.getSystemHealthSummary();
        TaskService.TaskStatisticsSummary taskStats = taskService.getTaskStatistics();
        
        return OrchestrationMetrics.builder()
                .totalAgents(agentHealth.totalAgents())
                .activeAgents(agentHealth.activeAgents())
                .busyAgents(agentHealth.busyAgents())
                .errorAgents(agentHealth.errorAgents())
                .averageAgentLoad(agentHealth.averageLoad())
                .averageSuccessRate(agentHealth.averageSuccessRate())
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
     * ✅ FUNCTIONAL: Check agent capabilities using streams
     */
    private boolean canAgentHandleTask(Agent agent, Task task) {
        return new HashSet<>(agent.getCapabilities()).containsAll(task.getRequiredCapabilities());
    }

    /**
     * Calculate system utilization percentage
     */
    private Double calculateSystemUtilization(AgentHealthSummary agentHealth) {
        if (agentHealth.totalAgents() == 0) {
            return 0.0;
        }
        
        long busyAgents = agentHealth.busyAgents();
        long totalAgents = agentHealth.totalAgents();
        
        return (busyAgents * 100.0) / totalAgents;
    }

    /**
     * ✅ MEDIATOR PATTERN: Coordinate complex multi-agent interactions
     * Uses the Mediator pattern to handle sophisticated agent collaborations
     */
    public CompletableFuture<Result<InteractionResult, AgentError>> coordinateAgentCollaboration(
            InteractionType interactionType,
            List<Agent> participants,
            String requestId,
            String messageType,
            Long initiatorId) {
        
        log.info("Coordinating agent collaboration: type={}, participants={}", 
            interactionType, participants.size());
        
        InteractionContext context = new InteractionContext(
            requestId,
            interactionType,
            messageType,
            initiatorId,
            java.util.Map.of(
                "orchestrationTimestamp", Instant.now(),
                "systemInitiated", true,
                "priority", "HIGH"
            ),
            java.time.Duration.ofMinutes(10)
        );
        
        return interactionMediator.mediateInteraction(context, participants)
            .thenApply(result -> {
                result.onSuccess(interactionResult -> 
                    log.info("Agent collaboration completed successfully: {}", 
                        interactionResult.status()));
                result.onFailure(error -> 
                    log.error("Agent collaboration failed: {}", error.getMessage()));
                return result;
            });
    }
    
    /**
     * ✅ MEDIATOR PATTERN: Voting consensus for critical decisions
     * Uses voting consensus pattern for important system decisions
     */
    public CompletableFuture<Result<InteractionResult, AgentError>> initiateVotingConsensus(
            String decisionTopic,
            List<Agent> eligibleVoters,
            String requestId) {
        
        log.info("Initiating voting consensus for: {} with {} voters", 
            decisionTopic, eligibleVoters.size());
        
        return coordinateAgentCollaboration(
            InteractionType.VOTING_CONSENSUS,
            eligibleVoters,
            requestId,
            "DECISION_VOTE",
            1L // System agent ID
        );
    }
    
    /**
     * ✅ MEDIATOR PATTERN: Hierarchical coordination for system commands
     * Uses hierarchical pattern for structured command execution
     */
    public CompletableFuture<Result<InteractionResult, AgentError>> executeHierarchicalCommand(
            String command,
            List<Agent> agentHierarchy,
            String requestId) {
        
        log.info("Executing hierarchical command: {} with {} agents", 
            command, agentHierarchy.size());
        
        return coordinateAgentCollaboration(
            InteractionType.HIERARCHICAL_COORDINATION,
            agentHierarchy,
            requestId,
            "SYSTEM_COMMAND",
            1L // System agent ID
        );
    }
    
    /**
     * ✅ MEDIATOR PATTERN: Chain collaboration for sequential processing
     * Uses chain pattern for ordered task execution across agents
     */
    public CompletableFuture<Result<InteractionResult, AgentError>> initiateChainProcessing(
            Task complexTask,
            List<Agent> processingChain,
            String requestId) {
        
        log.info("Initiating chain processing for task: {} with {} agents", 
            complexTask.getTaskName(), processingChain.size());
        
        return coordinateAgentCollaboration(
            InteractionType.CHAIN_COLLABORATION,
            processingChain,
            requestId,
            "CHAIN_PROCESSING",
            1L // System agent ID
        );
    }
    
    /**
     * ✅ MEDIATOR PATTERN: Get collaboration statistics
     */
    public java.util.Map<String, Object> getCollaborationStatistics() {
        return interactionMediator.getCollaborationStatistics();
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