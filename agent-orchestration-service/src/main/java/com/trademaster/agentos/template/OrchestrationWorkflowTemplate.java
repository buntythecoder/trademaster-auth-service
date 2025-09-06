package com.trademaster.agentos.template;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ TEMPLATE METHOD PATTERN: Orchestration Workflow Template
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Workflow orchestration skeleton only
 * - Open/Closed: Extensible via concrete implementations without modification
 * - Liskov Substitution: All concrete workflows interchangeable
 * - Interface Segregation: Focused on workflow orchestration
 * - Dependency Inversion: Uses service abstractions
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - Result monad for error handling
 * - Immutable workflow state
 * - CompletableFuture for async operations
 * - Stream operations for data processing
 * - Railway programming for workflow steps
 * 
 * The Template Method pattern defines the skeleton of the orchestration workflow
 * while allowing subclasses to override specific steps without changing the
 * overall algorithm structure.
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@RequiredArgsConstructor
@Slf4j
public abstract class OrchestrationWorkflowTemplate {
    
    protected final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ TEMPLATE METHOD: Main workflow execution algorithm
     * This method defines the skeleton of the workflow and cannot be overridden
     * Cognitive Complexity: 5
     */
    public final CompletableFuture<Result<WorkflowResult, AgentError>> executeWorkflow(
            WorkflowContext context) {
        
        Instant startTime = Instant.now();
        String workflowId = generateWorkflowId();
        
        structuredLogger.logInfo("workflow_started", Map.of(
            "workflowId", workflowId,
            "workflowType", getWorkflowType(),
            "startTime", startTime
        ));
        
        return CompletableFuture.supplyAsync(() -> 
            // ✅ RAILWAY PROGRAMMING: Chain workflow steps using Result monad
            initializeWorkflow(context)
                .flatMap(initializedContext -> validateWorkflowPreconditions(initializedContext))
                .flatMap(validatedContext -> prepareResources(validatedContext))
                .flatMap(preparedContext -> executeWorkflowSteps(preparedContext))
                .flatMap(executedContext -> validateWorkflowPostconditions(executedContext))
                .flatMap(validatedContext -> finalizeWorkflow(validatedContext))
                .map(finalizedContext -> {
                    Duration executionTime = Duration.between(startTime, Instant.now());
                    
                    structuredLogger.logInfo("workflow_completed", Map.of(
                        "workflowId", workflowId,
                        "workflowType", getWorkflowType(),
                        "executionTime", executionTime.toString(),
                        "status", "SUCCESS"
                    ));
                    
                    return new WorkflowResult(
                        workflowId,
                        getWorkflowType(),
                        WorkflowStatus.COMPLETED,
                        finalizedContext,
                        executionTime,
                        null
                    );
                })
                .onFailure(error -> {
                    Duration executionTime = Duration.between(startTime, Instant.now());
                    
                    structuredLogger.logError("workflow_failed", 
                        "Workflow execution failed: " + error.getMessage() + " (Code: " + error.getErrorCode() + ")", 
                        new RuntimeException("Workflow execution failed", new Exception(error.getMessage())), 
                        Map.of(
                        "workflowId", workflowId,
                        "workflowType", getWorkflowType(),
                        "executionTime", executionTime.toString(),
                        "errorType", error.getClass().getSimpleName(),
                        "errorMessage", error.getMessage(),
                        "errorCode", error.getErrorCode()
                    ));
                })
        );
    }
    
    /**
     * ✅ HOOK METHODS: Abstract methods that must be implemented by subclasses
     */
    
    protected abstract Result<WorkflowContext, AgentError> initializeWorkflow(WorkflowContext context);
    
    protected abstract Result<WorkflowContext, AgentError> validateWorkflowPreconditions(WorkflowContext context);
    
    protected abstract Result<WorkflowContext, AgentError> prepareResources(WorkflowContext context);
    
    protected abstract Result<WorkflowContext, AgentError> executeWorkflowSteps(WorkflowContext context);
    
    protected abstract Result<WorkflowContext, AgentError> validateWorkflowPostconditions(WorkflowContext context);
    
    protected abstract Result<WorkflowContext, AgentError> finalizeWorkflow(WorkflowContext context);
    
    protected abstract String getWorkflowType();
    
    /**
     * ✅ HOOK METHODS: Optional methods with default implementations
     * Subclasses can override these for custom behavior
     */
    
    protected String generateWorkflowId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    protected boolean requiresResourceCleanup() {
        return true;
    }
    
    protected Duration getWorkflowTimeout() {
        return Duration.ofMinutes(30);
    }
    
    protected int getMaxRetryAttempts() {
        return 3;
    }
    
    protected boolean shouldRetryOnFailure(AgentError error) {
        return error.isRetryable();
    }
    
    /**
     * ✅ COMMON METHODS: Shared workflow utilities
     */
    
    protected final Result<WorkflowContext, AgentError> logWorkflowStep(
            String stepName, 
            WorkflowContext context) {
        
        structuredLogger.logDebug("workflow_step_started", Map.of(
            "stepName", stepName,
            "workflowType", getWorkflowType(),
            "contextSize", context.getData().size()
        ));
        
        return Result.success(context);
    }
    
    protected final Result<WorkflowContext, AgentError> validateWorkflowTimeout(
            WorkflowContext context) {
        
        Duration elapsed = Duration.between(context.getStartTime(), Instant.now());
        Duration timeout = getWorkflowTimeout();
        
        return elapsed.compareTo(timeout) > 0 ?
            Result.failure(AgentError.timeoutError("WORKFLOW_TIMEOUT", 
                "Workflow exceeded timeout of " + timeout)) :
            Result.success(context);
    }
    
    protected final Result<WorkflowContext, AgentError> enrichContextWithMetadata(
            WorkflowContext context, 
            Map<String, Object> metadata) {
        
        WorkflowContext enrichedContext = context.withAdditionalData(metadata);
        return Result.success(enrichedContext);
    }
    
    /**
     * ✅ WORKFLOW CONTEXT: Immutable workflow execution context
     */
    public static final class WorkflowContext {
        private final String workflowId;
        private final Instant startTime;
        private final Map<String, Object> data;
        private final java.util.List<Agent> availableAgents;
        private final java.util.List<Task> pendingTasks;
        
        public WorkflowContext(
                String workflowId,
                Instant startTime,
                Map<String, Object> data,
                java.util.List<Agent> availableAgents,
                java.util.List<Task> pendingTasks) {
            this.workflowId = workflowId;
            this.startTime = startTime;
            this.data = Map.copyOf(data);
            this.availableAgents = java.util.List.copyOf(availableAgents);
            this.pendingTasks = java.util.List.copyOf(pendingTasks);
        }
        
        // Getters
        public String getWorkflowId() { return workflowId; }
        public Instant getStartTime() { return startTime; }
        public Map<String, Object> getData() { return data; }
        public java.util.List<Agent> getAvailableAgents() { return availableAgents; }
        public java.util.List<Task> getPendingTasks() { return pendingTasks; }
        
        // Immutable updates
        public WorkflowContext withAdditionalData(Map<String, Object> additionalData) {
            Map<String, Object> newData = new java.util.HashMap<>(this.data);
            newData.putAll(additionalData);
            return new WorkflowContext(workflowId, startTime, newData, availableAgents, pendingTasks);
        }
        
        public WorkflowContext withUpdatedAgents(java.util.List<Agent> newAgents) {
            return new WorkflowContext(workflowId, startTime, data, newAgents, pendingTasks);
        }
        
        public WorkflowContext withUpdatedTasks(java.util.List<Task> newTasks) {
            return new WorkflowContext(workflowId, startTime, data, availableAgents, newTasks);
        }
    }
    
    /**
     * ✅ WORKFLOW RESULT: Immutable workflow execution result
     */
    public record WorkflowResult(
        String workflowId,
        String workflowType,
        WorkflowStatus status,
        WorkflowContext finalContext,
        Duration executionTime,
        AgentError error
    ) {}
    
    /**
     * ✅ WORKFLOW STATUS: Workflow execution states
     */
    public enum WorkflowStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
    
    /**
     * ✅ BUILDER: Workflow context builder
     */
    public static class WorkflowContextBuilder {
        private String workflowId = java.util.UUID.randomUUID().toString();
        private Instant startTime = Instant.now();
        private Map<String, Object> data = Map.of();
        private java.util.List<Agent> availableAgents = java.util.List.of();
        private java.util.List<Task> pendingTasks = java.util.List.of();
        
        public WorkflowContextBuilder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }
        
        public WorkflowContextBuilder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public WorkflowContextBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }
        
        public WorkflowContextBuilder availableAgents(java.util.List<Agent> availableAgents) {
            this.availableAgents = availableAgents;
            return this;
        }
        
        public WorkflowContextBuilder pendingTasks(java.util.List<Task> pendingTasks) {
            this.pendingTasks = pendingTasks;
            return this;
        }
        
        public WorkflowContext build() {
            return new WorkflowContext(workflowId, startTime, data, availableAgents, pendingTasks);
        }
    }
    
    /**
     * ✅ FACTORY METHOD: Create workflow context builder
     */
    public static WorkflowContextBuilder contextBuilder() {
        return new WorkflowContextBuilder();
    }
}