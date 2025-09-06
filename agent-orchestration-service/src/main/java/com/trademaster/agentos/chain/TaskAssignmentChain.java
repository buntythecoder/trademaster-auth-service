package com.trademaster.agentos.chain;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chain of Responsibility implementation for Task Assignment in TradeMaster Agent OS
 * 
 * Processes task assignment requests through a series of validation and selection handlers.
 * Each handler in the chain performs a specific validation or selection step.
 */
@Component
@Slf4j
public class TaskAssignmentChain {
    
    private final AgentService agentService;
    private final RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> chain;
    
    public TaskAssignmentChain(AgentService agentService) {
        this.agentService = agentService;
        this.chain = buildChain();
    }
    
    /**
     * Processes a task assignment request through the complete chain
     */
    public Result<TaskAssignmentResult, TaskAssignmentError> assignTask(TaskAssignmentRequest request) {
        log.debug("Processing task assignment request for task: {}", request.task().getTaskId());
        return chain.handle(request);
    }
    
    /**
     * Builds the complete handler chain
     */
    private RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> buildChain() {
        return new TaskValidationHandler()
            .then(new AgentAvailabilityHandler(agentService))
            .then(new CapabilityMatchHandler())
            .then(new LoadBalancingHandler())
            .then(new FinalAssignmentHandler(agentService));
    }
    
    /**
     * Request object for task assignment
     */
    public record TaskAssignmentRequest(
        Task task,
        List<Agent> availableAgents,
        AssignmentContext context
    ) {}
    
    /**
     * Context information for task assignment
     */
    public record AssignmentContext(
        boolean preferLowLoadAgents,
        boolean requireExactCapabilityMatch,
        int maxRetryAttempts,
        long timeoutMillis
    ) {
        public static AssignmentContext defaultContext() {
            return new AssignmentContext(true, false, 3, 30_000);
        }
    }
    
    /**
     * Result of task assignment
     */
    public sealed interface TaskAssignmentResult 
        permits TaskAssignmentResult.Success, TaskAssignmentResult.Queued, TaskAssignmentResult.Rejected {
        
        record Success(Agent assignedAgent, Task updatedTask, String reason) implements TaskAssignmentResult {}
        record Queued(Task queuedTask, String reason) implements TaskAssignmentResult {}
        record Rejected(Task rejectedTask, String reason) implements TaskAssignmentResult {}
    }
    
    /**
     * Possible errors in task assignment
     */
    public enum TaskAssignmentError {
        INVALID_TASK("Task validation failed"),
        NO_AVAILABLE_AGENTS("No agents available for assignment"),
        NO_CAPABLE_AGENTS("No agents have required capabilities"),
        ALL_AGENTS_OVERLOADED("All capable agents are overloaded"),
        ASSIGNMENT_FAILED("Failed to assign task to agent"),
        TIMEOUT("Task assignment timed out"),
        SYSTEM_ERROR("System error during assignment");
        
        private final String message;
        
        TaskAssignmentError(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Validates the task before assignment
     */
    private static class TaskValidationHandler extends RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> {
        
        @Override
        protected Result<TaskAssignmentResult, TaskAssignmentError> doHandle(TaskAssignmentRequest request) {
            Task task = request.task();
            
            // Validate task is in assignable state
            if (task.getStatus() != TaskStatus.PENDING) {
                log.warn("Task {} is not in PENDING status: {}", task.getTaskId(), task.getStatus());
                return Result.failure(TaskAssignmentError.INVALID_TASK);
            }
            
            // Validate task has required capabilities defined
            if (task.getRequiredCapabilities().isEmpty()) {
                log.warn("Task {} has no required capabilities defined", task.getTaskId());
                return Result.failure(TaskAssignmentError.INVALID_TASK);
            }
            
            // Task is valid, pass to next handler
            return Result.failure(TaskAssignmentError.SYSTEM_ERROR); // Will be passed to next handler
        }
    }
    
    /**
     * Filters available agents based on status
     */
    private static class AgentAvailabilityHandler extends RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> {
        
        private final AgentService agentService;
        
        public AgentAvailabilityHandler(AgentService agentService) {
            this.agentService = agentService;
        }
        
        @Override
        protected Result<TaskAssignmentResult, TaskAssignmentError> doHandle(TaskAssignmentRequest request) {
            List<Agent> activeAgents = request.availableAgents().stream()
                .filter(agent -> agent.getStatus() == AgentStatus.ACTIVE)
                .filter(agent -> agent.getCurrentLoad() < agent.getMaxConcurrentTasks())
                .toList();
            
            if (activeAgents.isEmpty()) {
                log.info("No available agents found for task {}", request.task().getTaskId());
                return Result.failure(TaskAssignmentError.NO_AVAILABLE_AGENTS);
            }
            
            // Create updated request with filtered agents
            TaskAssignmentRequest updatedRequest = new TaskAssignmentRequest(
                request.task(),
                activeAgents,
                request.context()
            );
            
            // Pass to next handler with filtered agents
            return nextHandler != null 
                ? nextHandler.handle(updatedRequest)
                : Result.failure(TaskAssignmentError.SYSTEM_ERROR);
        }
        
        @Override
        protected boolean canPassToNext() {
            return false; // This handler manages the flow itself
        }
    }
    
    /**
     * Matches agents based on required capabilities
     */
    private static class CapabilityMatchHandler extends RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> {
        
        @Override
        protected Result<TaskAssignmentResult, TaskAssignmentError> doHandle(TaskAssignmentRequest request) {
            Task task = request.task();
            boolean requireExact = request.context().requireExactCapabilityMatch();
            
            List<Agent> capableAgents = request.availableAgents().stream()
                .filter(agent -> hasRequiredCapabilities(agent, task, requireExact))
                .toList();
            
            if (capableAgents.isEmpty()) {
                log.info("No agents with required capabilities found for task {}", task.getTaskId());
                return Result.failure(TaskAssignmentError.NO_CAPABLE_AGENTS);
            }
            
            // Create updated request with capability-matched agents
            TaskAssignmentRequest updatedRequest = new TaskAssignmentRequest(
                task,
                capableAgents,
                request.context()
            );
            
            // Pass to next handler
            return nextHandler != null 
                ? nextHandler.handle(updatedRequest)
                : Result.failure(TaskAssignmentError.SYSTEM_ERROR);
        }
        
        private boolean hasRequiredCapabilities(Agent agent, Task task, boolean requireExact) {
            if (requireExact) {
                return agent.getCapabilities().containsAll(task.getRequiredCapabilities());
            } else {
                // Allow partial match - agent just needs at least one required capability
                return task.getRequiredCapabilities().stream()
                    .anyMatch(required -> agent.getCapabilities().contains(required));
            }
        }
        
        @Override
        protected boolean canPassToNext() {
            return false; // This handler manages the flow itself
        }
    }
    
    /**
     * Applies load balancing logic to select the best agent
     */
    private static class LoadBalancingHandler extends RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> {
        
        @Override
        protected Result<TaskAssignmentResult, TaskAssignmentError> doHandle(TaskAssignmentRequest request) {
            List<Agent> agents = request.availableAgents();
            
            // Check if all agents are overloaded
            boolean allOverloaded = agents.stream()
                .allMatch(agent -> agent.getCurrentLoad() >= agent.getMaxConcurrentTasks() * 0.9); // 90% threshold
            
            if (allOverloaded && request.context().preferLowLoadAgents()) {
                log.warn("All capable agents are near capacity for task {}", request.task().getTaskId());
                return Result.failure(TaskAssignmentError.ALL_AGENTS_OVERLOADED);
            }
            
            // Select agent with lowest load
            Agent selectedAgent = agents.stream()
                .min((a1, a2) -> {
                    // Primary: compare current load percentage
                    double load1 = (double) a1.getCurrentLoad() / a1.getMaxConcurrentTasks();
                    double load2 = (double) a2.getCurrentLoad() / a2.getMaxConcurrentTasks();
                    int loadCompare = Double.compare(load1, load2);
                    
                    if (loadCompare != 0) return loadCompare;
                    
                    // Secondary: prefer agents with better success rate
                    return Double.compare(a2.getSuccessRate(), a1.getSuccessRate());
                })
                .orElseThrow(() -> new IllegalStateException("No agent found despite non-empty list"));
            
            // Create updated request with selected agent
            TaskAssignmentRequest updatedRequest = new TaskAssignmentRequest(
                request.task(),
                List.of(selectedAgent),
                request.context()
            );
            
            // Pass to final assignment handler
            return nextHandler != null 
                ? nextHandler.handle(updatedRequest)
                : Result.failure(TaskAssignmentError.SYSTEM_ERROR);
        }
        
        @Override
        protected boolean canPassToNext() {
            return false; // This handler manages the flow itself
        }
    }
    
    /**
     * Performs the final task assignment
     */
    private static class FinalAssignmentHandler extends RequestHandler<TaskAssignmentRequest, TaskAssignmentResult, TaskAssignmentError> {
        
        private final AgentService agentService;
        
        public FinalAssignmentHandler(AgentService agentService) {
            this.agentService = agentService;
        }
        
        @Override
        protected Result<TaskAssignmentResult, TaskAssignmentError> doHandle(TaskAssignmentRequest request) {
            if (request.availableAgents().size() != 1) {
                log.error("Final assignment handler expects exactly one agent, got: {}", 
                    request.availableAgents().size());
                return Result.failure(TaskAssignmentError.SYSTEM_ERROR);
            }
            
            Agent selectedAgent = request.availableAgents().get(0);
            Task task = request.task();
            
            try {
                // Assign the task
                task.setStatus(TaskStatus.QUEUED);
                task.setAgent(selectedAgent);
                
                // Update agent load
                selectedAgent.setCurrentLoad(selectedAgent.getCurrentLoad() + 1);
                
                log.info("Successfully assigned task {} to agent {}", task.getTaskId(), selectedAgent.getAgentId());
                
                return Result.success(new TaskAssignmentResult.Success(
                    selectedAgent, 
                    task,
                    String.format("Task assigned to agent %s (load: %d/%d)", 
                        selectedAgent.getAgentName(),
                        selectedAgent.getCurrentLoad(),
                        selectedAgent.getMaxConcurrentTasks())
                ));
                
            } catch (Exception e) {
                log.error("Failed to assign task {} to agent {}", task.getTaskId(), selectedAgent.getAgentId(), e);
                return Result.failure(TaskAssignmentError.ASSIGNMENT_FAILED);
            }
        }
    }
}