package com.trademaster.agentos.service;

import com.trademaster.agentos.config.KafkaConfig;
import com.trademaster.agentos.events.AgentEvent;
import com.trademaster.agentos.events.TaskEvent;
import com.trademaster.agentos.events.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Event Publishing Service
 * 
 * Service for publishing events to Kafka topics for inter-service communication.
 * Handles all event publishing with error handling and logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish agent event
     */
    public void publishAgentEvent(AgentEvent event) {
        if (!event.isValid()) {
            log.error("Invalid agent event, not publishing: {}", event);
            return;
        }

        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(KafkaConfig.AGENT_EVENTS_TOPIC, event.getAgentId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Agent event published successfully: {} for agent: {}", 
                             event.getEventType(), event.getAgentId());
                } else {
                    log.error("Failed to publish agent event: {} for agent: {}", 
                             event.getEventType(), event.getAgentId(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing agent event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish task event
     */
    public void publishTaskEvent(TaskEvent event) {
        if (!event.isValid()) {
            log.error("Invalid task event, not publishing: {}", event);
            return;
        }

        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(KafkaConfig.TASK_EVENTS_TOPIC, event.getTaskId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Task event published successfully: {} for task: {}", 
                             event.getEventType(), event.getTaskId());
                } else {
                    log.error("Failed to publish task event: {} for task: {}", 
                             event.getEventType(), event.getTaskId(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing task event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish system event
     */
    public void publishSystemEvent(BaseEvent event) {
        if (!event.isValid()) {
            log.error("Invalid system event, not publishing: {}", event);
            return;
        }

        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(KafkaConfig.SYSTEM_EVENTS_TOPIC, event.getEventId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("System event published successfully: {}", event.getEventType());
                } else {
                    log.error("Failed to publish system event: {}", event.getEventType(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing system event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish notification event
     */
    public void publishNotificationEvent(BaseEvent event) {
        if (!event.isValid()) {
            log.error("Invalid notification event, not publishing: {}", event);
            return;
        }

        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, event.getEventId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Notification event published successfully: {}", event.getEventType());
                } else {
                    log.error("Failed to publish notification event: {}", event.getEventType(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing notification event: {}", event.getEventType(), e);
        }
    }

    // Helper Methods for Common Event Publishing

    /**
     * Publish agent registered event
     */
    public void publishAgentRegistered(Long agentId, String agentName, String agentType) {
        AgentEvent event = AgentEvent.agentRegistered(agentId, agentName, agentType);
        publishAgentEvent(event);
    }

    /**
     * Publish agent status changed event
     */
    public void publishAgentStatusChanged(Long agentId, String agentName, 
                                        String previousStatus, String currentStatus) {
        AgentEvent event = AgentEvent.agentStatusChanged(agentId, agentName, previousStatus, currentStatus);
        publishAgentEvent(event);
    }

    /**
     * Publish agent heartbeat received event
     */
    public void publishAgentHeartbeatReceived(Long agentId, String agentName) {
        AgentEvent event = AgentEvent.agentHeartbeatReceived(agentId, agentName);
        publishAgentEvent(event);
    }

    /**
     * Publish agent load changed event
     */
    public void publishAgentLoadChanged(Long agentId, String agentName, 
                                      Integer currentLoad, Integer maxConcurrentTasks) {
        AgentEvent event = AgentEvent.agentLoadChanged(agentId, agentName, currentLoad, maxConcurrentTasks);
        publishAgentEvent(event);
    }

    /**
     * Publish agent performance updated event
     */
    public void publishAgentPerformanceUpdated(Long agentId, String agentName, Double successRate) {
        AgentEvent event = AgentEvent.agentPerformanceUpdated(agentId, agentName, successRate);
        publishAgentEvent(event);
    }

    /**
     * Publish agent deregistered event
     */
    public void publishAgentDeregistered(Long agentId, String agentName) {
        AgentEvent event = AgentEvent.agentDeregistered(agentId, agentName);
        publishAgentEvent(event);
    }

    /**
     * Publish task created event
     */
    public void publishTaskCreated(Long taskId, String taskName, String taskType, 
                                 String priority, Long userId) {
        TaskEvent event = TaskEvent.taskCreated(taskId, taskName, taskType, priority, userId);
        publishTaskEvent(event);
    }

    /**
     * Publish task assigned event
     */
    public void publishTaskAssigned(Long taskId, String taskName, Long agentId, String agentName) {
        TaskEvent event = TaskEvent.taskAssigned(taskId, taskName, agentId, agentName);
        publishTaskEvent(event);
    }

    /**
     * Publish task started event
     */
    public void publishTaskStarted(Long taskId, String taskName, Long agentId, String agentName) {
        TaskEvent event = TaskEvent.taskStarted(taskId, taskName, agentId, agentName);
        publishTaskEvent(event);
    }

    /**
     * Publish task completed event
     */
    public void publishTaskCompleted(Long taskId, String taskName, Long agentId, 
                                   String agentName, Long executionTimeMs) {
        TaskEvent event = TaskEvent.taskCompleted(taskId, taskName, agentId, agentName, executionTimeMs);
        publishTaskEvent(event);
    }

    /**
     * Publish task failed event
     */
    public void publishTaskFailed(Long taskId, String taskName, Long agentId, 
                                String agentName, String errorMessage) {
        TaskEvent event = TaskEvent.taskFailed(taskId, taskName, agentId, agentName, errorMessage);
        publishTaskEvent(event);
    }

    /**
     * Publish task cancelled event
     */
    public void publishTaskCancelled(Long taskId, String taskName) {
        TaskEvent event = TaskEvent.taskCancelled(taskId, taskName);
        publishTaskEvent(event);
    }
}