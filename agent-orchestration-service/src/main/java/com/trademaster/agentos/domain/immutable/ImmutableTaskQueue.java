package com.trademaster.agentos.domain.immutable;

import com.trademaster.agentos.domain.dto.TaskDto;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskStatus;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

/**
 * ✅ IMMUTABLE DATA STRUCTURE: Task Queue
 * 
 * Persistent priority queue implementation with immutable operations.
 * Supports priority-based ordering and efficient queue operations.
 */
public final class ImmutableTaskQueue {
    
    private final Map<Long, TaskDto> tasks;
    private final NavigableMap<QueueKey, Long> priorityQueue;
    private final Map<TaskStatus, Set<Long>> tasksByStatus;
    private final Instant lastModified;
    private final int version;
    private final int maxSize;
    
    /**
     * ✅ PRIORITY QUEUE KEY: Immutable key for priority ordering
     */
    public record QueueKey(
        int priority,
        Instant createdAt,
        Long taskId
    ) implements Comparable<QueueKey> {
        
        @Override
        public int compareTo(QueueKey other) {
            // ✅ PRIORITY ORDERING: Lower priority number = higher priority
            int priorityComparison = Integer.compare(this.priority, other.priority);
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            
            // ✅ FIFO FOR SAME PRIORITY: Earlier created tasks first
            int timeComparison = this.createdAt.compareTo(other.createdAt);
            if (timeComparison != 0) {
                return timeComparison;
            }
            
            // ✅ STABLE SORTING: Task ID as tie-breaker
            return Long.compare(this.taskId, other.taskId);
        }
        
        public static QueueKey of(TaskDto task) {
            return new QueueKey(
                task.getPriorityWeight(),
                task.createdAt(),
                task.taskId()
            );
        }
    }
    
    /**
     * ✅ PRIVATE CONSTRUCTOR: Enforce immutability
     */
    private ImmutableTaskQueue(
        Map<Long, TaskDto> tasks,
        NavigableMap<QueueKey, Long> priorityQueue,
        Map<TaskStatus, Set<Long>> tasksByStatus,
        Instant lastModified,
        int version,
        int maxSize
    ) {
        this.tasks = Map.copyOf(tasks);
        this.priorityQueue = new TreeMap<>(priorityQueue);
        this.tasksByStatus = tasksByStatus.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> Set.copyOf(entry.getValue())
            ));
        this.lastModified = lastModified;
        this.version = version;
        this.maxSize = maxSize;
    }
    
    /**
     * ✅ FACTORY METHOD: Create empty queue
     */
    public static ImmutableTaskQueue empty(int maxSize) {
        return new ImmutableTaskQueue(
            Map.of(),
            new TreeMap<>(),
            Map.of(),
            Instant.now(),
            0,
            maxSize
        );
    }
    
    /**
     * ✅ FACTORY METHOD: Create from collection
     */
    public static ImmutableTaskQueue of(Collection<TaskDto> tasks, int maxSize) {
        Map<Long, TaskDto> taskMap = new HashMap<>();
        NavigableMap<QueueKey, Long> queue = new TreeMap<>();
        Map<TaskStatus, Set<Long>> byStatus = new HashMap<>();
        
        for (TaskDto task : tasks) {
            taskMap.put(task.taskId(), task);
            
            if (task.isPending()) {
                queue.put(QueueKey.of(task), task.taskId());
            }
            
            byStatus.computeIfAbsent(task.status(), k -> new HashSet<>()).add(task.taskId());
        }
        
        return new ImmutableTaskQueue(
            taskMap,
            queue,
            byStatus,
            Instant.now(),
            1,
            maxSize
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Enqueue task (returns new instance)
     */
    public QueueOperationResult<ImmutableTaskQueue> enqueue(TaskDto task) {
        if (tasks.containsKey(task.taskId())) {
            return QueueOperationResult.unchanged(this, "Task already exists");
        }
        
        if (size() >= maxSize) {
            return QueueOperationResult.failed(this, "Queue is full");
        }
        
        Map<Long, TaskDto> newTasks = new HashMap<>(tasks);
        newTasks.put(task.taskId(), task);
        
        NavigableMap<QueueKey, Long> newPriorityQueue = new TreeMap<>(priorityQueue);
        if (task.isPending()) {
            newPriorityQueue.put(QueueKey.of(task), task.taskId());
        }
        
        Map<TaskStatus, Set<Long>> newByStatus = new HashMap<>(tasksByStatus);
        newByStatus.computeIfAbsent(task.status(), k -> new HashSet<>()).add(task.taskId());
        
        ImmutableTaskQueue newQueue = new ImmutableTaskQueue(
            newTasks,
            newPriorityQueue,
            newByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
        
        return QueueOperationResult.success(newQueue, "Task enqueued successfully");
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Dequeue highest priority task
     */
    public DequeueResult dequeue() {
        if (priorityQueue.isEmpty()) {
            return new DequeueResult(this, Optional.empty(), "Queue is empty");
        }
        
        Map.Entry<QueueKey, Long> firstEntry = priorityQueue.firstEntry();
        QueueKey queueKey = firstEntry.getKey();
        Long taskId = firstEntry.getValue();
        TaskDto task = tasks.get(taskId);
        
        if (task == null) {
            // Inconsistent state - remove from queue and try again
            NavigableMap<QueueKey, Long> cleanedQueue = new TreeMap<>(priorityQueue);
            cleanedQueue.remove(queueKey);
            
            ImmutableTaskQueue cleanedTaskQueue = new ImmutableTaskQueue(
                tasks,
                cleanedQueue,
                tasksByStatus,
                Instant.now(),
                version + 1,
                maxSize
            );
            
            return cleanedTaskQueue.dequeue();
        }
        
        // Remove from queue and update status to IN_PROGRESS
        NavigableMap<QueueKey, Long> newQueue = new TreeMap<>(priorityQueue);
        newQueue.remove(queueKey);
        
        TaskDto inProgressTask = TaskDto.minimal(
            task.taskId(),
            task.taskName(),
            task.taskType(),
            TaskStatus.IN_PROGRESS,
            task.priority(),
            0
        );
        
        Map<Long, TaskDto> newTasks = new HashMap<>(tasks);
        newTasks.put(taskId, inProgressTask);
        
        Map<TaskStatus, Set<Long>> newByStatus = updateStatusMap(tasksByStatus, taskId, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        
        ImmutableTaskQueue newTaskQueue = new ImmutableTaskQueue(
            newTasks,
            newQueue,
            newByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
        
        return new DequeueResult(newTaskQueue, Optional.of(task), "Task dequeued successfully");
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Update task status
     */
    public QueueOperationResult<ImmutableTaskQueue> updateTaskStatus(Long taskId, TaskStatus newStatus) {
        TaskDto existingTask = tasks.get(taskId);
        if (existingTask == null) {
            return QueueOperationResult.unchanged(this, "Task not found");
        }
        
        if (existingTask.status() == newStatus) {
            return QueueOperationResult.unchanged(this, "Status unchanged");
        }
        
        TaskDto updatedTask = TaskDto.minimal(
            existingTask.taskId(),
            existingTask.taskName(),
            existingTask.taskType(),
            newStatus,
            existingTask.priority(),
            newStatus == TaskStatus.COMPLETED ? 100 : existingTask.progressPercentage()
        );
        
        Map<Long, TaskDto> newTasks = new HashMap<>(tasks);
        newTasks.put(taskId, updatedTask);
        
        NavigableMap<QueueKey, Long> newQueue = new TreeMap<>(priorityQueue);
        
        // Remove from queue if it was pending and is now not pending
        if (existingTask.isPending() && !updatedTask.isPending()) {
            newQueue.remove(QueueKey.of(existingTask));
        }
        
        // Add to queue if it was not pending and is now pending
        if (!existingTask.isPending() && updatedTask.isPending()) {
            newQueue.put(QueueKey.of(updatedTask), taskId);
        }
        
        Map<TaskStatus, Set<Long>> newByStatus = updateStatusMap(tasksByStatus, taskId, existingTask.status(), newStatus);
        
        ImmutableTaskQueue newTaskQueue = new ImmutableTaskQueue(
            newTasks,
            newQueue,
            newByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
        
        return QueueOperationResult.success(newTaskQueue, "Task status updated");
    }
    
    /**
     * ✅ QUERY OPERATIONS: Immutable access methods
     */
    
    public Optional<TaskDto> getTask(Long taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
    
    public Optional<TaskDto> peek() {
        if (priorityQueue.isEmpty()) {
            return Optional.empty();
        }
        
        Long taskId = priorityQueue.firstEntry().getValue();
        return Optional.ofNullable(tasks.get(taskId));
    }
    
    public List<TaskDto> getPendingTasks() {
        return priorityQueue.values().stream()
            .map(tasks::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public List<TaskDto> getTasksByStatus(TaskStatus status) {
        Set<Long> taskIds = tasksByStatus.getOrDefault(status, Set.of());
        return taskIds.stream()
            .map(tasks::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public List<TaskDto> getTasksByPriority(TaskPriority priority) {
        return tasks.values().stream()
            .filter(task -> task.priority() == priority)
            .sorted((t1, t2) -> t1.createdAt().compareTo(t2.createdAt()))
            .toList();
    }
    
    public List<TaskDto> findTasks(Predicate<TaskDto> predicate) {
        return tasks.values().stream()
            .filter(predicate)
            .toList();
    }
    
    /**
     * ✅ METADATA ACCESS: Queue information
     */
    
    public int size() {
        return tasks.size();
    }
    
    public int pendingSize() {
        return priorityQueue.size();
    }
    
    public boolean isEmpty() {
        return tasks.isEmpty();
    }
    
    public boolean isPendingEmpty() {
        return priorityQueue.isEmpty();
    }
    
    public boolean isFull() {
        return tasks.size() >= maxSize;
    }
    
    public double utilizationPercentage() {
        return maxSize == 0 ? 0.0 : (double) tasks.size() / maxSize * 100.0;
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public Instant getLastModified() {
        return lastModified;
    }
    
    public int getVersion() {
        return version;
    }
    
    /**
     * ✅ HELPER METHODS
     */
    
    private Map<TaskStatus, Set<Long>> updateStatusMap(
        Map<TaskStatus, Set<Long>> original,
        Long taskId,
        TaskStatus oldStatus,
        TaskStatus newStatus
    ) {
        Map<TaskStatus, Set<Long>> newMap = new HashMap<>(original);
        
        // Remove from old status
        Set<Long> oldSet = newMap.get(oldStatus);
        if (oldSet != null) {
            Set<Long> updatedOldSet = new HashSet<>(oldSet);
            updatedOldSet.remove(taskId);
            if (updatedOldSet.isEmpty()) {
                newMap.remove(oldStatus);
            } else {
                newMap.put(oldStatus, updatedOldSet);
            }
        }
        
        // Add to new status
        newMap.computeIfAbsent(newStatus, k -> new HashSet<>()).add(taskId);
        
        return newMap;
    }
    
    /**
     * ✅ RESULT TYPES: Immutable operation results
     */
    
    public record QueueOperationResult<T>(
        T queue,
        boolean success,
        String message
    ) {
        public static <T> QueueOperationResult<T> success(T queue, String message) {
            return new QueueOperationResult<>(queue, true, message);
        }
        
        public static <T> QueueOperationResult<T> failed(T queue, String message) {
            return new QueueOperationResult<>(queue, false, message);
        }
        
        public static <T> QueueOperationResult<T> unchanged(T queue, String message) {
            return new QueueOperationResult<>(queue, true, message);
        }
    }
    
    public record DequeueResult(
        ImmutableTaskQueue queue,
        Optional<TaskDto> task,
        String message
    ) {
        public boolean hasTask() {
            return task.isPresent();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImmutableTaskQueue other)) return false;
        
        return Objects.equals(tasks, other.tasks) &&
               Objects.equals(priorityQueue, other.priorityQueue) &&
               version == other.version;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tasks, priorityQueue, version);
    }
    
    @Override
    public String toString() {
        return "ImmutableTaskQueue{" +
               "size=" + tasks.size() +
               ", pendingSize=" + priorityQueue.size() +
               ", version=" + version +
               ", utilizationPercentage=" + String.format("%.1f", utilizationPercentage()) +
               "%, lastModified=" + lastModified +
               '}';
    }
}