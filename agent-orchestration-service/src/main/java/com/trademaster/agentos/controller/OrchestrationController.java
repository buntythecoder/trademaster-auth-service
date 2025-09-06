package com.trademaster.agentos.controller;

import com.trademaster.agentos.service.*;
import com.trademaster.agentos.service.ResourceManagementTypes.*;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * ✅ AI-005: Enhanced Orchestration Controller
 * 
 * REST API endpoints for comprehensive agent orchestration,
 * workflow management, and performance analytics.
 * Implements all AI-005 specification endpoints.
 * 
 * MANDATORY COMPLIANCE:
 * - Java 24 Virtual Threads for API handling
 * - Functional programming patterns for request processing
 * - SOLID principles with single responsibility per endpoint
 * - Comprehensive AI-005 orchestration capabilities
 */
@RestController
@RequestMapping("/api/v1/orchestration")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrchestrationController {

    private final AgentOrchestrationService orchestrationService;
    private final AgentLifecycleManager agentLifecycleManager;
    private final IntelligentTaskDelegationService taskDelegationService;
    private final MultiAgentCommunicationService communicationService;
    private final WorkflowOrchestrationEngine workflowEngine;
    private final ResourceManagementService resourceManagementService;
    private final PerformanceAnalyticsService performanceAnalyticsService;

    /**
     * Get comprehensive orchestration metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<AgentOrchestrationService.OrchestrationMetrics> getOrchestrationMetrics() {
        log.debug("REST: Getting orchestration metrics");
        
        try {
            AgentOrchestrationService.OrchestrationMetrics metrics = orchestrationService.getOrchestrationMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting orchestration metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthStatus> getSystemHealth() {
        log.debug("REST: Getting system health status");
        
        try {
            AgentOrchestrationService.OrchestrationMetrics metrics = orchestrationService.getOrchestrationMetrics();
            
            // Determine system health based on metrics
            SystemHealthStatus health = SystemHealthStatus.builder()
                    .status(determineSystemStatus(metrics))
                    .totalAgents(metrics.getTotalAgents())
                    .activeAgents(metrics.getActiveAgents())
                    .errorAgents(metrics.getErrorAgents())
                    .systemUtilization(metrics.getSystemUtilization())
                    .pendingTasks(metrics.getPendingTasks())
                    .failedTasks(metrics.getFailedTasks())
                    .averageSuccessRate(metrics.getAverageSuccessRate())
                    .build();
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting system health", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Trigger manual health check
     */
    @PostMapping("/health-check")
    public ResponseEntity<Void> triggerHealthCheck() {
        log.info("REST: Triggering manual health check");
        
        try {
            // Health check is performed by scheduled tasks
            // This endpoint could trigger immediate health check if needed
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error triggering health check", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get system configuration info
     */
    @GetMapping("/info")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        log.debug("REST: Getting system info");
        
        try {
            SystemInfo info = SystemInfo.builder()
                    .serviceName("TradeMaster Agent Orchestration Service")
                    .version("1.0.0")
                    .javaVersion(System.getProperty("java.version"))
                    .springBootVersion(getClass().getPackage().getImplementationVersion())
                    .uptime(getSystemUptime())
                    .build();
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error getting system info", e);
            return ResponseEntity.status(500).build();
        }
    }

    // Helper Methods

    /**
     * Determine overall system status based on metrics
     */
    /**
     * ✅ FUNCTIONAL: Determine system status using pattern matching - NO IF-ELSE
     */
    private String determineSystemStatus(AgentOrchestrationService.OrchestrationMetrics metrics) {
        return Stream.of(
                new StatusRule(
                    m -> m.getErrorAgents() > 0 || m.getFailedTasks() > m.getCompletedTasks(),
                    "DEGRADED"
                ),
                new StatusRule(
                    m -> m.getActiveAgents() == 0,
                    "DOWN"
                ),
                new StatusRule(
                    m -> m.getSystemUtilization() > 90,
                    "HIGH_LOAD"
                ),
                new StatusRule(
                    m -> true,
                    "HEALTHY"
                )
            )
            .filter(rule -> rule.condition().test(metrics))
            .findFirst()
            .map(StatusRule::status)
            .orElse("UNKNOWN");
    }
    
    /**
     * ✅ FUNCTIONAL: Status rule record for functional pattern matching
     */
    private record StatusRule(
        java.util.function.Predicate<AgentOrchestrationService.OrchestrationMetrics> condition,
        String status
    ) {}

    /**
     * Get system uptime in milliseconds
     */
    private Long getSystemUptime() {
        return System.currentTimeMillis() - 
               java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    // ✅ AI-005: Agent Lifecycle Management Endpoints
    
    /**
     * Create new agent with dynamic configuration
     */
    @PostMapping("/agents/create")
    public CompletableFuture<ResponseEntity<Object>> createAgent(
            @RequestBody AgentCreationRequest request) {
        
        log.info("Creating agent: {} of type: {}", request.agentName(), request.agentType());
        
        return agentLifecycleManager
            .createAgent(
                request.agentName(),
                request.agentType(),
                request.capabilities(),
                new AgentLifecycleManager.AgentConfiguration(
                    request.maxConcurrentTasks(),
                    request.healthCheckInterval(),
                    request.resourceLimits()
                )
            )
            .thenApply(result -> result.fold(
                agent -> ResponseEntity.ok().body(Map.of(
                    "agentId", agent.getAgentId(),
                    "agentName", agent.getAgentName(),
                    "status", agent.getStatus(),
                    "message", "Agent created successfully"
                )),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Start agent execution
     */
    @PostMapping("/agents/{agentId}/start")
    public CompletableFuture<ResponseEntity<Object>> startAgent(@PathVariable Long agentId) {
        log.info("Starting agent: {}", agentId);
        
        return agentLifecycleManager.startAgent(agentId)
            .thenApply(result -> result.fold(
                message -> ResponseEntity.ok().body(Map.of("message", message)),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Stop agent gracefully or immediately
     */
    @PostMapping("/agents/{agentId}/stop")
    public CompletableFuture<ResponseEntity<Object>> stopAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "true") boolean graceful) {
        
        log.info("Stopping agent: {} (graceful: {})", agentId, graceful);
        
        return agentLifecycleManager.stopAgent(agentId, graceful)
            .thenApply(result -> result.fold(
                message -> ResponseEntity.ok().body(Map.of("message", message)),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Get agent lifecycle state
     */
    @GetMapping("/agents/{agentId}/lifecycle")
    public ResponseEntity<Object> getAgentLifecycleState(@PathVariable Long agentId) {
        return agentLifecycleManager.getLifecycleState(agentId)
            .map(state -> ResponseEntity.ok().body((Object) state))
            .orElse(ResponseEntity.notFound().build());
    }

    // ✅ AI-005: Intelligent Task Delegation Endpoints
    
    /**
     * Delegate task with intelligent routing
     */
    @PostMapping("/tasks/delegate")
    public CompletableFuture<ResponseEntity<Object>> delegateTask(
            @RequestBody TaskDelegationRequest request) {
        
        log.info("Delegating task: {} with strategy: {}", request.taskName(), request.strategy());
        
        return taskDelegationService
            .delegateTask(request.task(), request.strategy())
            .thenApply(result -> result.fold(
                delegationResult -> ResponseEntity.ok().body(Map.of(
                    "taskId", delegationResult.task().getTaskId(),
                    "assignedAgent", delegationResult.agent().getAgentName(),
                    "selectionScore", delegationResult.selectionScore(),
                    "message", delegationResult.message()
                )),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Get delegation statistics
     */
    @GetMapping("/delegation/statistics")
    public ResponseEntity<IntelligentTaskDelegationService.DelegationStatistics> getDelegationStatistics() {
        return ResponseEntity.ok(taskDelegationService.getDelegationStatistics());
    }

    // ✅ AI-005: Multi-Agent Communication Endpoints
    
    /**
     * Send request-response message between agents
     */
    @PostMapping("/communication/request")
    public CompletableFuture<ResponseEntity<Object>> sendAgentRequest(
            @RequestBody AgentCommunicationRequest request) {
        
        log.info("Sending communication from agent {} to agent {}", 
            request.senderId(), request.receiverId());
        
        return communicationService
            .sendRequest(request.senderId(), request.receiverId(), request.mcpRequest())
            .thenApply(result -> result.fold(
                response -> ResponseEntity.ok().body(response),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Broadcast message to multiple agents
     */
    @PostMapping("/communication/broadcast")
    public CompletableFuture<ResponseEntity<Object>> broadcastMessage(
            @RequestBody AgentBroadcastRequest request) {
        
        log.info("Broadcasting message from agent {} to {} agents", 
            request.senderId(), request.receiverIds().size());
        
        return communicationService
            .broadcastMessage(request.senderId(), request.receiverIds(), request.broadcast())
            .thenApply(result -> result.fold(
                results -> ResponseEntity.ok().body(Map.of(
                    "totalReceivers", results.size(),
                    "successfulDeliveries", results.stream().mapToInt(r -> r.delivered() ? 1 : 0).sum(),
                    "results", results
                )),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }

    // ✅ AI-005: Workflow Orchestration Endpoints
    
    /**
     * Execute complex multi-agent workflow
     */
    @PostMapping("/workflows/execute")
    public CompletableFuture<ResponseEntity<Object>> executeWorkflow(
            @RequestBody WorkflowExecutionRequest request) {
        
        log.info("Executing workflow: {} with trigger: {}", 
            request.workflowName(), request.trigger().type());
        
        return workflowEngine
            .executeWorkflow(request.workflowName(), request.context(), request.trigger())
            .thenApply(result -> result.fold(
                executionResult -> ResponseEntity.ok().body(Map.of(
                    "executionId", executionResult.executionId(),
                    "status", executionResult.status(),
                    "duration", executionResult.duration().toString(),
                    "message", executionResult.message()
                )),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }

    // ✅ AI-005: Resource Management Endpoints
    
    /**
     * Auto-scale agent resources
     */
    @PostMapping("/resources/agents/{agentId}/scale")
    public CompletableFuture<ResponseEntity<Object>> autoScaleAgent(
            @PathVariable Long agentId,
            @RequestBody ResourceScalingRequest request) {
        
        log.info("Auto-scaling agent: {} with trigger: {}", agentId, request.trigger().type());
        
        return resourceManagementService
            .autoScaleAgent(agentId, request.trigger())
            .thenApply(result -> result.fold(
                scalingAction -> ResponseEntity.ok().body(Map.of(
                    "agentId", scalingAction.agentId(),
                    "direction", scalingAction.direction(),
                    "scalingFactor", scalingAction.scalingFactor(),
                    "message", scalingAction.message()
                )),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            ));
    }
    
    /**
     * Get resource utilization metrics
     */
    @GetMapping("/resources/utilization")
    public ResponseEntity<ResourceCostOptimizationService.ResourceUtilizationMetrics> getResourceUtilization() {
        return ResponseEntity.ok(resourceManagementService.getResourceUtilization());
    }

    // ✅ AI-005: Performance Analytics Endpoints
    
    /**
     * Analyze agent performance trends
     */
    @GetMapping("/analytics/agents/{agentId}/trends")
    public ResponseEntity<Object> getPerformanceTrends(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "PT1H") String timeWindow) {
        
        Duration window = Duration.parse(timeWindow);
        
        return performanceAnalyticsService
            .analyzePerformanceTrends(agentId, window)
            .fold(
                analysis -> ResponseEntity.ok().body(analysis),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            );
    }
    
    /**
     * Get performance predictions
     */
    @GetMapping("/analytics/agents/{agentId}/predictions")
    public ResponseEntity<Object> getPerformancePredictions(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "PT30M") String forecastWindow) {
        
        Duration window = Duration.parse(forecastWindow);
        
        return performanceAnalyticsService
            .predictPerformance(agentId, window)
            .fold(
                prediction -> ResponseEntity.ok().body(prediction),
                error -> ResponseEntity.badRequest().body(Map.of("error", error.getMessage()))
            );
    }
    
    /**
     * Detect performance anomalies
     */
    @GetMapping("/analytics/agents/{agentId}/anomalies")
    public ResponseEntity<List<PerformanceAnalyticsService.PerformanceAnomaly>> getPerformanceAnomalies(
            @PathVariable Long agentId) {
        
        return ResponseEntity.ok(performanceAnalyticsService.detectAnomalies(agentId));
    }
    
    /**
     * Get optimization recommendations
     */
    @GetMapping("/analytics/agents/{agentId}/recommendations")
    public ResponseEntity<List<PerformanceAnalyticsService.OptimizationRecommendation>> getOptimizationRecommendations(
            @PathVariable Long agentId) {
        
        return ResponseEntity.ok(performanceAnalyticsService.generateOptimizationRecommendations(agentId));
    }
    
    /**
     * Get system-wide performance summary
     */
    @GetMapping("/analytics/system/performance")
    public ResponseEntity<PerformanceAnalyticsService.SystemPerformanceSummary> getSystemPerformance() {
        return ResponseEntity.ok(performanceAnalyticsService.getSystemPerformanceSummary());
    }

    // ✅ IMMUTABLE: Request/Response DTOs
    
    public record AgentCreationRequest(
        String agentName,
        String agentType,
        List<com.trademaster.agentos.domain.entity.AgentCapability> capabilities,
        Integer maxConcurrentTasks,
        Long healthCheckInterval,
        Object resourceLimits
    ) {}
    
    public record TaskDelegationRequest(
        String taskName,
        com.trademaster.agentos.domain.entity.Task task,
        IntelligentTaskDelegationService.DelegationStrategy strategy
    ) {}
    
    public record AgentCommunicationRequest(
        Long senderId,
        Long receiverId,
        MultiAgentCommunicationService.MCPRequest mcpRequest
    ) {}
    
    public record AgentBroadcastRequest(
        Long senderId,
        List<Long> receiverIds,
        MultiAgentCommunicationService.MCPBroadcast broadcast
    ) {}
    
    public record WorkflowExecutionRequest(
        String workflowName,
        Map<String, Object> context,
        WorkflowOrchestrationEngine.WorkflowTrigger trigger
    ) {}
    
    public record ResourceScalingRequest(
        ScalingTrigger trigger
    ) {}

    // ✅ EXISTING: Enhanced Helper Classes

    @lombok.Data
    @lombok.Builder
    public static class SystemHealthStatus {
        private String status;
        private Long totalAgents;
        private Long activeAgents;
        private Long errorAgents;
        private Double systemUtilization;
        private Long pendingTasks;
        private Long failedTasks;
        private Double averageSuccessRate;
        // ✅ AI-005: Enhanced health metrics
        private Integer activeWorkflows;
        private Double resourceUtilization;
        private Integer communicationChannels;
    }

    @lombok.Data
    @lombok.Builder
    public static class SystemInfo {
        private String serviceName;
        private String version;
        private String javaVersion;
        private String springBootVersion;
        private Long uptime;
        // ✅ AI-005: Enhanced system info
        private String orchestrationVersion;
        private List<String> enabledFeatures;
        private Map<String, Object> performanceMetrics;
    }
}