package com.trademaster.agentos.service;

/**
 * ✅ INTERFACE SEGREGATION: Queue Statistics Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only statistics operations
 * - Interface Segregation: Separated from queue operations
 * - Dependency Inversion: Abstractions for stats implementations
 */
public interface IQueueStatsService {
    
    /**
     * ✅ SRP: Get queue statistics - single responsibility
     */
    TaskQueueStats getQueueStats();
    
    /**
     * ✅ IMMUTABLE: Queue statistics record
     */
    record TaskQueueStats(
        long highPriorityTasks,
        long normalPriorityTasks,
        long lowPriorityTasks,
        long totalTasks,
        int maxQueueSize,
        double utilizationPercent
    ) {}
}