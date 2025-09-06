package com.trademaster.agentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ✅ MANDATORY: Unit Tests for Task Queue Service - Simplified
 * 
 * Focus on basic functionality with mocks to ensure compilation
 * Will be enhanced with TestContainers after basic tests pass
 */
@ExtendWith(MockitoExtension.class)
class TaskQueueServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private AgentOSMetrics metrics;
    
    @Mock
    private StructuredLoggingService structuredLogger;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private ListOperations<String, String> listOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;
    
    @InjectMocks
    private TaskQueueService taskQueueService;
    
    private Task testTask;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        // Set test configuration
        ReflectionTestUtils.setField(taskQueueService, "maxQueueSize", 1000);
        ReflectionTestUtils.setField(taskQueueService, "maxRetryAttempts", 3);
        
        testTask = Task.builder()
            .taskId(1L)
            .taskName("test-task")
            .taskType(TaskType.MARKET_ANALYSIS)
            .status(TaskStatus.PENDING)
            .priority(TaskPriority.HIGH)
            .progressPercentage(0)
            .retryCount(0)
            .maxRetries(3)
            .userId(1L)
            .createdAt(Instant.now())
            .build();
    }
    
    /**
     * ✅ TEST: Task enqueue with priority routing
     */
    @Test
    void enqueueTask_ShouldEnqueueToPriorityQueue() throws Exception {
        // Given
        String taskJson = "{\"taskId\":1,\"priority\":\"HIGH\"}";
        when(listOperations.size(anyString())).thenReturn(100L); // Queue not full
        when(objectMapper.writeValueAsString(testTask)).thenReturn(taskJson);
        when(listOperations.rightPush("agentos:task:queue:high", "1")).thenReturn(1L);
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
        
        // When
        CompletableFuture<Boolean> result = taskQueueService.enqueueTask(testTask);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get()).isTrue(),
            () -> verify(valueOperations).set(eq("agentos:task:data:1"), eq(taskJson), any()),
            () -> verify(listOperations).rightPush(eq("agentos:task:queue:high"), eq("1")),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), anyString(), anyString(), anyString(), anyMap())
        );
    }
    
    /**
     * ✅ TEST: Priority-based task dequeue
     */
    @Test
    void dequeueTask_ShouldReturnHighestPriorityTask() throws Exception {
        // Given
        String agentId = "agent-1";
        when(listOperations.leftPop("agentos:task:queue:high")).thenReturn("1");
        when(listOperations.leftPop("agentos:task:queue:normal")).thenReturn(null);
        when(listOperations.leftPop("agentos:task:queue:low")).thenReturn(null);
        when(valueOperations.get("agentos:task:data:1")).thenReturn("{\"taskId\":1}");
        when(objectMapper.readValue(anyString(), eq(Task.class))).thenReturn(testTask);
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
        
        // When
        CompletableFuture<Optional<Task>> result = taskQueueService.dequeueTask(agentId);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get()).isPresent(),
            () -> assertThat(result.get().get().getTaskId()).isEqualTo(1L),
            () -> verify(setOperations).add(eq("agentos:task:processing:agent-1"), eq("1")),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), anyString(), anyString(), eq(agentId), anyMap())
        );
    }
    
    /**
     * ✅ TEST: Task completion with cleanup
     */
    @Test
    void completeTask_ShouldUpdateStatusAndCleanup() throws Exception {
        // Given
        Long taskId = 1L;
        String agentId = "agent-1";
        String result = "Task completed successfully";
        when(valueOperations.get("agentos:task:data:1")).thenReturn("{\"taskId\":1}");
        when(objectMapper.readValue(anyString(), eq(Task.class))).thenReturn(testTask);
        when(objectMapper.writeValueAsString(any(Task.class))).thenReturn("{\"taskId\":1,\"status\":\"COMPLETED\"}");
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
        
        // When
        CompletableFuture<Boolean> taskResult = taskQueueService.completeTask(taskId, agentId, result);
        
        // Then
        assertAll(
            () -> assertThat(taskResult).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(taskResult.get()).isTrue(),
            () -> verify(setOperations).remove(eq("agentos:task:processing:agent-1"), eq("1")),
            () -> verify(valueOperations).set(eq("agentos:task:data:1"), anyString(), any()),
            () -> verify(structuredLogger).logTaskExecution(anyString(), anyString(), eq(agentId), anyString(), anyLong())
        );
    }
    
    /**
     * ✅ TEST: Task failure with retry logic
     */
    @Test
    void failTask_ShouldRetryWhenUnderMaxAttempts() throws Exception {
        // Given
        Long taskId = 1L;
        String agentId = "agent-1";
        String errorMessage = "Task failed";
        Task taskWithRetries = testTask.toBuilder().retryCount(1).build();
        
        when(valueOperations.get("agentos:task:data:1")).thenReturn("{\"taskId\":1}");
        when(objectMapper.readValue(anyString(), eq(Task.class))).thenReturn(taskWithRetries);
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
        
        // When
        CompletableFuture<Boolean> result = taskQueueService.failTask(taskId, agentId, errorMessage);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get()).isTrue(),
            () -> verify(structuredLogger).logBusinessTransaction(eq("task_retry"), anyString(), eq("retry"), eq(agentId), anyMap()),
            () -> verify(setOperations).remove(eq("agentos:task:processing:agent-1"), eq("1"))
        );
    }
    
    /**
     * ✅ TEST: Queue statistics
     */
    @Test
    void getQueueStats_ShouldReturnCorrectStatistics() {
        // Given
        when(listOperations.size("agentos:task:queue:high")).thenReturn(5L);
        when(listOperations.size("agentos:task:queue:normal")).thenReturn(10L);
        when(listOperations.size("agentos:task:queue:low")).thenReturn(3L);
        
        // When
        TaskQueueService.TaskQueueStats stats = taskQueueService.getQueueStats();
        
        // Then
        assertAll(
            () -> assertThat(stats.getHighPriorityTasks()).isEqualTo(5L),
            () -> assertThat(stats.getNormalPriorityTasks()).isEqualTo(10L),
            () -> assertThat(stats.getLowPriorityTasks()).isEqualTo(3L),
            () -> assertThat(stats.getTotalTasks()).isEqualTo(18L),
            () -> assertThat(stats.getUtilizationPercent()).isEqualTo(1.8) // 18/1000 * 100
        );
    }
    
    /**
     * ✅ TEST: Queue full handling
     */
    @Test
    void enqueueTask_ShouldRejectWhenQueueFull() throws Exception {
        // Given
        when(listOperations.size(anyString())).thenReturn(1000L); // Queue full
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
        
        // When
        CompletableFuture<Boolean> result = taskQueueService.enqueueTask(testTask);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get()).isFalse(),
            () -> verify(metrics).recordError(eq("task_enqueue"), anyString()),
            () -> verify(structuredLogger).logError(anyString(), anyString(), any(Exception.class), anyMap())
        );
    }
}