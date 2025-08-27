package com.trademaster.agentos.repository;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskType;
import com.trademaster.agentos.domain.entity.AgentCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Task Repository
 * 
 * Data access layer for Task entities with specialized query methods
 * for task queue management, execution tracking, and performance analysis.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find task by name for a specific user
     */
    Optional<Task> findByTaskNameAndUserId(String taskName, Long userId);

    /**
     * Find all tasks by status
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * Find all tasks by type
     */
    List<Task> findByTaskType(TaskType taskType);

    /**
     * Find all tasks by priority
     */
    List<Task> findByPriority(TaskPriority priority);

    /**
     * Find all tasks by user ID
     */
    List<Task> findByUserId(Long userId);

    /**
     * Find all tasks assigned to specific agent
     */
    List<Task> findByAgentId(Long agentId);

    /**
     * Find all tasks in specific workflow
     */
    List<Task> findByWorkflowId(Long workflowId);

    /**
     * Find all subtasks of a parent task
     */
    List<Task> findByParentTaskId(Long parentTaskId);

    /**
     * Find pending tasks ordered by priority and creation time
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'PENDING' ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findPendingTasksOrderedByPriority();

    /**
     * Find tasks ready for execution (QUEUED status)
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'QUEUED' ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findQueuedTasks();

    /**
     * Find tasks currently in progress
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS'")
    List<Task> findTasksInProgress();

    /**
     * Find tasks that have exceeded their timeout
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS' AND t.startedAt < :timeoutThreshold")
    List<Task> findTimedOutTasks(@Param("timeoutThreshold") Instant timeoutThreshold);

    /**
     * Find tasks approaching deadline
     */
    @Query("SELECT t FROM Task t WHERE t.deadline IS NOT NULL AND t.deadline < :warningThreshold AND t.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    List<Task> findTasksApproachingDeadline(@Param("warningThreshold") Instant warningThreshold);

    /**
     * Find overdue tasks
     */
    @Query("SELECT t FROM Task t WHERE t.deadline IS NOT NULL AND t.deadline < CURRENT_TIMESTAMP AND t.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    List<Task> findOverdueTasks();

    /**
     * Find failed tasks that can be retried
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetries")
    List<Task> findRetriableTasks();

    /**
     * Find tasks by status and user
     */
    List<Task> findByStatusAndUserId(TaskStatus status, Long userId);

    /**
     * Find tasks by type and status
     */
    List<Task> findByTaskTypeAndStatus(TaskType taskType, TaskStatus status);

    /**
     * Find tasks requiring specific capabilities
     */
    @Query("SELECT DISTINCT t FROM Task t JOIN t.requiredCapabilities rc WHERE rc IN :capabilities")
    List<Task> findTasksRequiringCapabilities(@Param("capabilities") List<AgentCapability> capabilities);

    /**
     * Find tasks that can be executed by agent with given capabilities
     */
    @Query("""
        SELECT t FROM Task t 
        WHERE t.status IN ('PENDING', 'QUEUED') 
        AND (
            SIZE(t.requiredCapabilities) = 0 OR
            (
                SELECT COUNT(DISTINCT rc) 
                FROM Task t2 JOIN t2.requiredCapabilities rc 
                WHERE t2.taskId = t.taskId AND rc IN :agentCapabilities
            ) = SIZE(t.requiredCapabilities)
        )
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    List<Task> findExecutableTasksForCapabilities(@Param("agentCapabilities") List<AgentCapability> agentCapabilities);

    /**
     * Find high priority tasks for immediate execution
     */
    @Query("SELECT t FROM Task t WHERE t.status IN ('PENDING', 'QUEUED') AND t.priority IN ('CRITICAL', 'HIGH') ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findHighPriorityTasks();

    /**
     * Find tasks created within time range
     */
    @Query("SELECT t FROM Task t WHERE t.createdAt BETWEEN :startTime AND :endTime")
    List<Task> findTasksCreatedBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Find tasks completed within time range
     */
    @Query("SELECT t FROM Task t WHERE t.completedAt BETWEEN :startTime AND :endTime AND t.status = 'COMPLETED'")
    List<Task> findTasksCompletedBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Find long-running tasks
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS' AND t.startedAt < :longRunningThreshold")
    List<Task> findLongRunningTasks(@Param("longRunningThreshold") Instant longRunningThreshold);

    /**
     * Get task performance statistics
     */
    @Query("""
        SELECT t.taskType as taskType,
               COUNT(t) as totalTasks,
               COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) as completedTasks,
               COUNT(CASE WHEN t.status = 'FAILED' THEN 1 END) as failedTasks,
               AVG(t.actualDurationSeconds) as avgDuration,
               AVG(t.progressPercentage) as avgProgress
        FROM Task t 
        GROUP BY t.taskType
        """)
    List<Object[]> getTaskStatisticsByType();

    /**
     * Get system-wide task statistics
     */
    @Query("""
        SELECT COUNT(t) as totalTasks,
               COUNT(CASE WHEN t.status = 'PENDING' THEN 1 END) as pendingTasks,
               COUNT(CASE WHEN t.status = 'QUEUED' THEN 1 END) as queuedTasks,
               COUNT(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 END) as inProgressTasks,
               COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) as completedTasks,
               COUNT(CASE WHEN t.status = 'FAILED' THEN 1 END) as failedTasks,
               AVG(t.actualDurationSeconds) as avgDuration
        FROM Task t
        """)
    Object[] getSystemTaskStatistics();

    /**
     * Find tasks by agent with status filter
     */
    List<Task> findByAgentIdAndStatus(Long agentId, TaskStatus status);

    /**
     * Find tasks by workflow with status filter
     */
    List<Task> findByWorkflowIdAndStatus(Long workflowId, TaskStatus status);

    /**
     * Count tasks by status
     */
    long countByStatus(TaskStatus status);

    /**
     * Count tasks by type
     */
    long countByTaskType(TaskType taskType);

    /**
     * Count tasks by priority
     */
    long countByPriority(TaskPriority priority);

    /**
     * Count tasks by user
     */
    long countByUserId(Long userId);

    /**
     * Count tasks by agent
     */
    long countByAgentId(Long agentId);

    /**
     * Find tasks eligible for batch processing
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'QUEUED' AND t.taskType = :taskType ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findBatchEligibleTasks(@Param("taskType") TaskType taskType);

    /**
     * Update task status
     */
    @Query("UPDATE Task t SET t.status = :status, t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    void updateTaskStatus(@Param("taskId") Long taskId, @Param("status") TaskStatus status);

    /**
     * Update task progress
     */
    @Query("UPDATE Task t SET t.progressPercentage = :progress, t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    void updateTaskProgress(@Param("taskId") Long taskId, @Param("progress") Integer progress);

    /**
     * Assign task to agent
     */
    @Query("UPDATE Task t SET t.agentId = :agentId, t.status = 'QUEUED', t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    void assignTaskToAgent(@Param("taskId") Long taskId, @Param("agentId") Long agentId);

    /**
     * Increment task retry count
     */
    @Query("UPDATE Task t SET t.retryCount = t.retryCount + 1, t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    void incrementRetryCount(@Param("taskId") Long taskId);

    /**
     * Find optimal tasks for agent assignment based on capabilities and load
     */
    @Query("""
        SELECT t FROM Task t 
        WHERE t.status = 'PENDING'
        AND (
            SIZE(t.requiredCapabilities) = 0 OR
            (
                SELECT COUNT(DISTINCT rc) 
                FROM Task t2 JOIN t2.requiredCapabilities rc 
                WHERE t2.taskId = t.taskId AND rc IN :agentCapabilities
            ) = SIZE(t.requiredCapabilities)
        )
        ORDER BY 
            t.priority DESC,
            t.createdAt ASC
        """)
    List<Task> findOptimalTasksForAgent(@Param("agentCapabilities") List<AgentCapability> agentCapabilities);

    /**
     * Find tasks in error state that need attention
     */
    @Query("SELECT t FROM Task t WHERE t.status IN ('ERROR', 'FAILED') AND t.errorMessage IS NOT NULL ORDER BY t.priority DESC, t.updatedAt DESC")
    List<Task> findTasksInErrorState();

    /**
     * Find abandoned tasks (stuck in progress for too long)
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS' AND t.updatedAt < :abandonedThreshold")
    List<Task> findAbandonedTasks(@Param("abandonedThreshold") Instant abandonedThreshold);
}