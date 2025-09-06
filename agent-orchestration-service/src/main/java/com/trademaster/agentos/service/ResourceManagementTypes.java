package com.trademaster.agentos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * ✅ COMPLIANT: Resource Management Domain Types
 * 
 * Shared immutable record types for resource management domain.
 * Used by ResourceAllocationService, AutoScalingService, and ResourceCostOptimizationService.
 * 
 * MANDATORY COMPLIANCE:
 * - Immutable records only (no mutable data structures)
 * - BigDecimal for all financial calculations
 * - Functional composition patterns
 */
public final class ResourceManagementTypes {

    // ✅ IMMUTABLE: Resource domain records
    
    public record ResourceRequirement(
        Map<ResourceType, Double> requirements
    ) {}
    
    public record ResourceAvailability(
        Map<ResourceType, Double> available,
        boolean sufficient
    ) {}
    
    public record ResourceReservation(
        String reservationId,
        Map<ResourceType, ResourceReservationDetail> reservationDetails,
        Instant reservedAt
    ) {}
    
    public record ResourceReservationDetail(
        ResourceType resourceType,
        double amount,
        String poolId,
        boolean success,
        String errorMessage
    ) {}
    
    public record ResourceAllocation(
        String allocationId,
        Map<ResourceType, ResourceReservationDetail> reservationDetails,
        AllocationStatus status,
        Instant allocatedAt,
        Instant releasedAt,
        BigDecimal cost
    ) {}
    
    public record ResourcePool(
        String poolId,
        ResourceType resourceType,
        double capacity,
        double allocated,
        String region,
        BigDecimal costPerHour
    ) {
        public ResourcePool withAllocated(double newAllocated) {
            return new ResourcePool(poolId, resourceType, capacity, newAllocated, region, costPerHour);
        }
    }
    
    public record ResourceLimits(
        double min,
        double max,
        String unit
    ) {}
    
    public record ScalingTrigger(
        ScalingTriggerType type,
        String reason,
        Map<String, Object> parameters,
        ScalingDirection direction
    ) {
        public ScalingTrigger(ScalingTriggerType type, String reason, Map<String, Object> parameters) {
            this(type, reason, parameters, ScalingDirection.NONE);
        }
    }
    
    public record ScalingAnalysis(
        com.trademaster.agentos.domain.entity.Agent agent,
        ResourceAllocation allocation,
        PerformanceMetrics metrics,
        ScalingDirection direction,
        double scalingFactor,
        ScalingTrigger trigger
    ) {}
    
    public record ScalingAction(
        Long agentId,
        ScalingDirection direction,
        double scalingFactor,
        String message
    ) {
        public ScalingAction withMessage(String newMessage) {
            return new ScalingAction(agentId, direction, scalingFactor, newMessage);
        }
    }
    
    public record ScalingMetrics(
        int totalScalings,
        int successfulScalings,
        Instant lastScaling
    ) {
        public ScalingMetrics addScaling() {
            return new ScalingMetrics(totalScalings + 1, successfulScalings + 1, Instant.now());
        }
    }
    
    public record PerformanceMetrics(
        double cpuUsage,
        double memoryUsage,
        double networkUsage,
        double responseTime
    ) {}
    
    // ✅ IMMUTABLE: Enums for type safety
    
    public enum ResourceType {
        CPU, MEMORY, GPU, DISK, NETWORK
    }
    
    public enum AllocationStatus {
        PENDING, ACTIVE, RELEASED, FAILED
    }
    
    public enum ScalingDirection {
        UP, DOWN, NONE
    }
    
    public enum ScalingTriggerType {
        HIGH_CPU_USAGE, HIGH_MEMORY_USAGE, LOW_UTILIZATION, PREDICTIVE, MANUAL
    }
    
    // ✅ IMMUTABLE: Resource error types
    
    public sealed interface ResourceError {
        String getMessage();
        
        record LimitViolation(ResourceType resourceType, String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record InsufficientResources(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record ReservationFailed(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record AllocationFailed(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record AllocationNotFound(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record AgentNotFound(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
        
        record ScalingFailed(String message) implements ResourceError {
            @Override
            public String getMessage() { return message; }
        }
    }
    
    // Prevent instantiation
    private ResourceManagementTypes() {}
}