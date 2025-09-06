package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.ResourceManagementTypes.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ COMPLIANT: Resource Management Service Facade (200 lines max, 10 methods max)
 * 
 * Facade service that coordinates resource management operations.
 * Delegates to focused services following Single Responsibility Principle.
 * 
 * MANDATORY COMPLIANCE:
 * - Java 24 Virtual Threads for async coordination
 * - Functional programming patterns (no if-else, no loops)
 * - SOLID principles with cognitive complexity ≤7 per method
 * - Facade pattern for service coordination
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResourceManagementService {

    private final ResourceAllocationService resourceAllocationService;
    private final AutoScalingService autoScalingService;
    private final ResourceCostOptimizationService resourceCostOptimizationService;

    /**
     * ✅ FUNCTIONAL: Allocate resources for new agent
     */
    public CompletableFuture<Result<ResourceAllocation, ResourceError>> allocateResourcesForAgent(
            String agentType, List<AgentCapability> capabilities) {
        
        log.info("Delegating resource allocation to ResourceAllocationService: {}", agentType);
        
        return resourceAllocationService.allocateResourcesForAgent(agentType, capabilities);
    }

    /**
     * ✅ FUNCTIONAL: Auto-scale agent resources based on performance
     */
    public CompletableFuture<Result<ScalingAction, ResourceError>> autoScaleAgent(
            Long agentId, ScalingTrigger trigger) {
        
        log.info("Delegating auto-scaling to AutoScalingService: agent {}", agentId);
        
        return autoScalingService.autoScaleAgent(agentId, trigger);
    }

    /**
     * ✅ FUNCTIONAL: Release resources for agent
     */
    public CompletableFuture<String> releaseResources(Long agentId) {
        log.info("Delegating resource release to ResourceCostOptimizationService: agent {}", agentId);
        
        return resourceCostOptimizationService.releaseResources(agentId);
    }

    /**
     * ✅ FUNCTIONAL: Get resource utilization metrics
     */
    public ResourceCostOptimizationService.ResourceUtilizationMetrics getResourceUtilization() {
        return resourceCostOptimizationService.getResourceUtilization();
    }

    /**
     * ✅ FUNCTIONAL: Calculate resource allocation cost
     */
    public BigDecimal calculateResourceAllocationCost(ResourceAllocation allocation) {
        return resourceCostOptimizationService.calculateResourceAllocationCost(allocation);
    }

    /**
     * ✅ FUNCTIONAL: Optimize resource allocation for cost efficiency
     */
    public CompletableFuture<ResourceCostOptimizationService.CostOptimizationResult> optimizeResourceAllocation(
            Long agentId) {
        
        log.info("Delegating cost optimization to ResourceCostOptimizationService: agent {}", agentId);
        
        return resourceCostOptimizationService.optimizeResourceAllocation(agentId);
    }

    /**
     * ✅ FUNCTIONAL: Get scaling metrics for agent
     */
    public Optional<ScalingMetrics> getScalingMetrics(Long agentId) {
        return autoScalingService.getScalingMetrics(agentId);
    }

    /**
     * ✅ FUNCTIONAL: Comprehensive resource management for agent lifecycle
     */
    public CompletableFuture<Result<ResourceManagementResult, ResourceError>> manageAgentResourceLifecycle(
            String agentType, List<AgentCapability> capabilities, Long agentId) {
        
        log.info("Managing complete resource lifecycle for agent type: {}", agentType);
        
        return allocateResourcesForAgent(agentType, capabilities)
            .thenCompose(allocationResult -> 
                allocationResult.fold(
                    allocation -> optimizeResourceAllocation(agentId)
                        .thenApply(optimization -> Result.<ResourceManagementResult, ResourceError>success(
                            new ResourceManagementResult(allocation, optimization, "Lifecycle managed successfully"))),
                    error -> CompletableFuture.completedFuture(Result.<ResourceManagementResult, ResourceError>failure(error))
                ));
    }

    /**
     * ✅ FUNCTIONAL: Create scaling trigger for predictive scaling
     */
    public ScalingTrigger createPredictiveScalingTrigger(String reason) {
        return new ScalingTrigger(
            ScalingTriggerType.PREDICTIVE,
            reason,
            java.util.Map.of("timestamp", java.time.Instant.now()),
            ScalingDirection.NONE
        );
    }

    /**
     * ✅ FUNCTIONAL: Create manual scaling trigger
     */
    public ScalingTrigger createManualScalingTrigger(ScalingDirection direction, String reason) {
        return new ScalingTrigger(
            ScalingTriggerType.MANUAL,
            reason,
            java.util.Map.of("timestamp", java.time.Instant.now()),
            direction
        );
    }

    /**
     * ✅ IMMUTABLE: Resource management result record
     */
    public record ResourceManagementResult(
        ResourceAllocation allocation,
        ResourceCostOptimizationService.CostOptimizationResult optimization,
        String message
    ) {}
}