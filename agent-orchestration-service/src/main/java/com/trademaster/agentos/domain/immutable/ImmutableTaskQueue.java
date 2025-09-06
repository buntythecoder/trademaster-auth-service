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
            // ✅ FUNCTIONAL: Chain comparisons using functional composition
            return java.util.Comparator.<QueueKey>comparingInt(QueueKey::priority)  // Lower priority number = higher priority
                .thenComparing(QueueKey::createdAt)  // FIFO for same priority
                .thenComparingLong(QueueKey::taskId)  // Stable sorting tie-breaker
                .compare(this, other);
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
        // ✅ FUNCTIONAL: Replace for loop with stream operations
        Map<Long, TaskDto> taskMap = tasks.stream()
            .collect(java.util.stream.Collectors.toMap(
                TaskDto::taskId,
                task -> task
            ));
            
        NavigableMap<QueueKey, Long> queue = tasks.stream()
            .filter(TaskDto::isPending)
            .collect(java.util.stream.Collectors.toMap(
                QueueKey::of,
                TaskDto::taskId,
                (existing, replacement) -> existing,  // Keep existing for duplicates
                TreeMap::new
            ));
            
        Map<TaskStatus, Set<Long>> byStatus = tasks.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TaskDto::status,
                java.util.stream.Collectors.mapping(
                    TaskDto::taskId,
                    java.util.stream.Collectors.toSet()
                )
            ));
        
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
        // ✅ FUNCTIONAL: Replace if-else with Optional and validation chain
        return Optional.of(task)
            .filter(t -> !tasks.containsKey(t.taskId()))
            .map(t -> Optional.of(size())
                .filter(s -> s < maxSize)
                .map(s -> createEnqueuedQueue(t))
                .map(queue -> QueueOperationResult.success(queue, "Task enqueued successfully"))
                .orElse(QueueOperationResult.failed(this, "Queue is full"))
            )
            .orElse(QueueOperationResult.unchanged(this, "Task already exists"));
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to create new queue with enqueued task
     */
    private ImmutableTaskQueue createEnqueuedQueue(TaskDto task) {
        Map<Long, TaskDto> newTasks = new HashMap<>(tasks);
        newTasks.put(task.taskId(), task);
        
        NavigableMap<QueueKey, Long> newPriorityQueue = new TreeMap<>(priorityQueue);
        Optional.of(task)
            .filter(TaskDto::isPending)
            .ifPresent(t -> newPriorityQueue.put(QueueKey.of(t), t.taskId()));
        
        Map<TaskStatus, Set<Long>> newByStatus = new HashMap<>(tasksByStatus);
        newByStatus.computeIfAbsent(task.status(), k -> new HashSet<>()).add(task.taskId());
        
        return new ImmutableTaskQueue(
            newTasks,
            newPriorityQueue,
            newByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Dequeue highest priority task
     */
    public DequeueResult dequeue() {
        // ✅ FUNCTIONAL: Replace if-else with Optional chain
        return Optional.of(priorityQueue)
            .filter(queue -> !queue.isEmpty())
            .map(NavigableMap::firstEntry)
            .map(entry -> processDequeue(entry.getKey(), entry.getValue()))
            .orElse(new DequeueResult(this, Optional.empty(), "Queue is empty"));
    }
    
    /**
     * ✅ FUNCTIONAL: Process dequeue operation using functional composition
     */
    private DequeueResult processDequeue(QueueKey queueKey, Long taskId) {
        return Optional.ofNullable(tasks.get(taskId))
            .map(task -> createDequeuedQueue(queueKey, taskId, task))
            .orElseGet(() -> {
                // Handle inconsistent state functionally
                ImmutableTaskQueue cleanedQueue = createCleanedQueue(queueKey);
                return cleanedQueue.dequeue();
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Create new queue with dequeued task
     */
    private DequeueResult createDequeuedQueue(QueueKey queueKey, Long taskId, TaskDto task) {
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
        
        Map<TaskStatus, Set<Long>> newByStatus = updateStatusMap(
            tasksByStatus, taskId, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        
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
     * ✅ FUNCTIONAL: Create cleaned queue removing inconsistent entry
     */
    private ImmutableTaskQueue createCleanedQueue(QueueKey queueKey) {
        NavigableMap<QueueKey, Long> cleanedQueue = new TreeMap<>(priorityQueue);
        cleanedQueue.remove(queueKey);
        
        return new ImmutableTaskQueue(
            tasks,
            cleanedQueue,
            tasksByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
    }
    
    /**
     * ✅ IMMUTABLE OPERATION: Update task status
     */
    public QueueOperationResult<ImmutableTaskQueue> updateTaskStatus(Long taskId, TaskStatus newStatus) {
        // ✅ FUNCTIONAL: Replace if-else with Optional validation chain
        return Optional.ofNullable(tasks.get(taskId))
            .filter(existingTask -> existingTask.status() != newStatus)
            .map(existingTask -> createUpdatedTaskQueue(taskId, existingTask, newStatus))
            .map(queue -> QueueOperationResult.success(queue, "Task status updated"))
            .orElse(Optional.ofNullable(tasks.get(taskId))
                .map(task -> QueueOperationResult.unchanged(this, "Status unchanged"))
                .orElse(QueueOperationResult.unchanged(this, "Task not found"))
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Create new queue with updated task status
     */
    private ImmutableTaskQueue createUpdatedTaskQueue(Long taskId, TaskDto existingTask, TaskStatus newStatus) {
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
        
        NavigableMap<QueueKey, Long> newQueue = updatePriorityQueue(existingTask, updatedTask, taskId);
        Map<TaskStatus, Set<Long>> newByStatus = updateStatusMap(
            tasksByStatus, taskId, existingTask.status(), newStatus);
        
        return new ImmutableTaskQueue(
            newTasks,
            newQueue,
            newByStatus,
            Instant.now(),
            version + 1,
            maxSize
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Update priority queue based on task status changes
     */
    private NavigableMap<QueueKey, Long> updatePriorityQueue(TaskDto existingTask, TaskDto updatedTask, Long taskId) {
        NavigableMap<QueueKey, Long> newQueue = new TreeMap<>(priorityQueue);
        
        // Handle removal from queue when task becomes non-pending
        Optional.of(existingTask)
            .filter(TaskDto::isPending)
            .filter(task -> !updatedTask.isPending())
            .ifPresent(task -> newQueue.remove(QueueKey.of(task)));
        
        // Handle addition to queue when task becomes pending
        Optional.of(existingTask)
            .filter(task -> !task.isPending())
            .filter(task -> updatedTask.isPending())
            .ifPresent(task -> newQueue.put(QueueKey.of(updatedTask), taskId));
        
        return newQueue;
    }
    
    /**
     * ✅ QUERY OPERATIONS: Immutable access methods
     */
    
    public Optional<TaskDto> getTask(Long taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
    
    public Optional<TaskDto> peek() {
        // ✅ FUNCTIONAL: Replace if-else with Optional chain
        return Optional.of(priorityQueue)
            .filter(queue -> !queue.isEmpty())
            .map(NavigableMap::firstEntry)
            .map(Map.Entry::getValue)
            .flatMap(taskId -> Optional.ofNullable(tasks.get(taskId)));
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
        // ✅ FUNCTIONAL: Replace if-else with functional operations
        Map<TaskStatus, Set<Long>> newMap = new HashMap<>(original);
        
        // Remove from old status using Optional chain
        Optional.ofNullable(newMap.get(oldStatus))
            .ifPresent(oldSet -> {
                Set<Long> updatedOldSet = new HashSet<>(oldSet);
                updatedOldSet.remove(taskId);
                Optional.of(updatedOldSet)
                    .filter(Set::isEmpty)
                    .ifPresentOrElse(
                        empty -> newMap.remove(oldStatus),
                        () -> newMap.put(oldStatus, updatedOldSet)
                    );
            });
        
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
        // ✅ FUNCTIONAL: Replace if-else with pattern matching and Optional
        return Optional.of(obj)
            .filter(o -> o == this)
            .map(o -> true)
            .orElseGet(() -> 
                Optional.of(obj)
                    .filter(ImmutableTaskQueue.class::isInstance)
                    .map(ImmutableTaskQueue.class::cast)
                    .filter(other -> Objects.equals(tasks, other.tasks) &&
                                   Objects.equals(priorityQueue, other.priorityQueue) &&
                                   version == other.version)
                    .isPresent()
            );
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