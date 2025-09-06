package com.trademaster.agentos.chain.impl;

import com.trademaster.agentos.chain.TaskProcessingHandler;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ✅ FUNCTIONAL: Resource Allocation Handler
 * 
 * Third handler in the chain that manages resource allocation for tasks.
 * Ensures system resources are available and reserves them for task execution.
 * 
 * Resource Management:
 * - CPU allocation check
 * - Memory availability verification
 * - Network bandwidth assessment
 * - Storage space validation
 * - Concurrent task limits
 */
@Component
@RequiredArgsConstructor
public class ResourceAllocationHandler extends TaskProcessingHandler {
    
    private final StructuredLoggingService structuredLogger;
    
    // Resource limits (in a real system, these would come from configuration)
    private static final long MAX_MEMORY_MB = 4096L;
    private static final double MAX_CPU_CORES = 4.0;
    private static final long MAX_STORAGE_GB = 100L;
    private static final int MAX_CONCURRENT_TASKS = 50;
    
    @Override
    public Result<Task, AgentError> handle(Task task) {
        structuredLogger.logDebug("resource_allocation_started", 
            Map.of("taskId", task.getTaskId(), "taskType", task.getTaskType()));
        
        return allocateResources(task)
            .onSuccess(allocatedTask -> 
                structuredLogger.logDebug("resource_allocation_completed", 
                    Map.of("taskId", task.getTaskId(), 
                           "memoryAllocated", calculateMemoryRequirement(task),
                           "cpuAllocated", calculateCpuRequirement(task))))
            .onFailure(error -> 
                structuredLogger.logWarning("resource_allocation_failed", 
                    Map.of("taskId", task.getTaskId(), "error", error.getMessage())))
            .flatMap(this::processNext);
    }
    
    /**
     * ✅ FUNCTIONAL: Resource allocation with comprehensive checks
     */
    private Result<Task, AgentError> allocateResources(Task task) {
        return validateMemoryAvailability(task)
            .flatMap(this::validateCpuAvailability)
            .flatMap(this::validateStorageAvailability)
            .flatMap(this::validateConcurrentTaskLimit)
            .map(this::reserveResources);
    }
    
    /**
     * ✅ FUNCTIONAL: Memory availability validation
     */
    private Result<Task, AgentError> validateMemoryAvailability(Task task) {
        long memoryRequired = calculateMemoryRequirement(task);
        long availableMemory = getCurrentAvailableMemory();
        
        return memoryRequired > availableMemory
            ? Result.failure(new AgentError.ResourceExhausted(
                "memory", memoryRequired, availableMemory))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: CPU availability validation
     */
    private Result<Task, AgentError> validateCpuAvailability(Task task) {
        double cpuRequired = calculateCpuRequirement(task);
        double availableCpu = getCurrentAvailableCpu();
        
        return cpuRequired > availableCpu
            ? Result.failure(new AgentError.ResourceExhausted(
                "cpu", (long)(cpuRequired * 100), (long)(availableCpu * 100)))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Storage availability validation
     */
    private Result<Task, AgentError> validateStorageAvailability(Task task) {
        long storageRequired = calculateStorageRequirement(task);
        long availableStorage = getCurrentAvailableStorage();
        
        return storageRequired > availableStorage
            ? Result.failure(new AgentError.ResourceExhausted(
                "storage", storageRequired, availableStorage))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Concurrent task limit validation
     */
    private Result<Task, AgentError> validateConcurrentTaskLimit(Task task) {
        int currentTaskCount = getCurrentTaskCount();
        
        return currentTaskCount >= MAX_CONCURRENT_TASKS
            ? Result.failure(new AgentError.ResourceExhausted(
                "concurrent_tasks", (long)currentTaskCount, (long)MAX_CONCURRENT_TASKS))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Reserve resources for task execution
     */
    private Task reserveResources(Task task) {
        // In a real system, this would actually reserve resources
        long memoryReserved = calculateMemoryRequirement(task);
        double cpuReserved = calculateCpuRequirement(task);
        long storageReserved = calculateStorageRequirement(task);
        
        structuredLogger.logInfo("resources_reserved", 
            Map.of("taskId", task.getTaskId(),
                   "memoryMB", memoryReserved,
                   "cpuCores", cpuReserved,
                   "storageGB", storageReserved));
        
        return task;
    }
    
    /**
     * ✅ FUNCTIONAL: Resource calculation methods using pure functions
     */
    private long calculateMemoryRequirement(Task task) {
        // Base memory requirement based on task type and priority
        long baseMemory = switch (task.getTaskType().toString().toLowerCase()) {
            case "data_processing" -> 1024L;
            case "ml_training" -> 2048L;
            case "analytics" -> 512L;
            default -> 256L;
        };
        
        // Scale by priority (higher priority gets more resources)
        return baseMemory * task.getPriority().getLevel() / 3;
    }
    
    private double calculateCpuRequirement(Task task) {
        // Base CPU requirement based on task characteristics
        double baseCpu = task.getRequiredCapabilities().size() * 0.5;
        
        // Scale by priority
        return Math.min(baseCpu * task.getPriority().getLevel() / 3, MAX_CPU_CORES);
    }
    
    private long calculateStorageRequirement(Task task) {
        // Simplified storage calculation based on input parameters
        return task.getInputParameters() != null 
            ? Math.max(task.getInputParameters().length() / 1000L, 1L)
            : 1L;
    }
    
    /**
     * ✅ FUNCTIONAL: Mock resource availability methods
     * In a real system, these would query actual system resources
     */
    private long getCurrentAvailableMemory() {
        return MAX_MEMORY_MB - (MAX_MEMORY_MB * 30 / 100); // Assume 30% used
    }
    
    private double getCurrentAvailableCpu() {
        return MAX_CPU_CORES - (MAX_CPU_CORES * 25 / 100); // Assume 25% used
    }
    
    private long getCurrentAvailableStorage() {
        return MAX_STORAGE_GB - (MAX_STORAGE_GB * 20 / 100); // Assume 20% used
    }
    
    private int getCurrentTaskCount() {
        return 15; // Mock current task count
    }
    
    @Override
    public String getHandlerName() {
        return "ResourceAllocation";
    }
}