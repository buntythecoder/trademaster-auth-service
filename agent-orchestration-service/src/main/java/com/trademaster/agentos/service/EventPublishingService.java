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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
     * ✅ FUNCTIONAL: Publish agent event using Optional chain
     */
    public void publishAgentEvent(AgentEvent event) {
        Optional.of(event)
            .filter(AgentEvent::isValid)
            .ifPresentOrElse(
                this::sendAgentEvent,
                () -> log.error("Invalid agent event, not publishing: {}", event)
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to send agent event with functional error handling
     */
    private void sendAgentEvent(AgentEvent event) {
        Optional.of(event)
            .map(e -> kafkaTemplate.send(KafkaConfig.AGENT_EVENTS_TOPIC, e.getAgentId().toString(), e))
            .ifPresentOrElse(
                future -> future.whenComplete(createAgentEventCallback(event)),
                () -> log.error("Failed to create Kafka future for agent event: {}", event.getEventType())
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Create callback for agent event publishing
     */
    private java.util.function.BiConsumer<SendResult<String, Object>, Throwable> createAgentEventCallback(AgentEvent event) {
        return (result, ex) -> 
            Optional.ofNullable(ex)
                .ifPresentOrElse(
                    exception -> log.error("Failed to publish agent event: {} for agent: {}", 
                                          event.getEventType(), event.getAgentId(), exception),
                    () -> log.debug("Agent event published successfully: {} for agent: {}", 
                                   event.getEventType(), event.getAgentId())
                );
    }

    /**
     * ✅ FUNCTIONAL: Publish task event using Optional chain
     */
    public void publishTaskEvent(TaskEvent event) {
        Optional.of(event)
            .filter(TaskEvent::isValid)
            .ifPresentOrElse(
                this::sendTaskEvent,
                () -> log.error("Invalid task event, not publishing: {}", event)
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to send task event with functional error handling
     */
    private void sendTaskEvent(TaskEvent event) {
        Optional.of(event)
            .map(e -> kafkaTemplate.send(KafkaConfig.TASK_EVENTS_TOPIC, e.getTaskId().toString(), e))
            .ifPresentOrElse(
                future -> future.whenComplete(createTaskEventCallback(event)),
                () -> log.error("Failed to create Kafka future for task event: {}", event.getEventType())
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Create callback for task event publishing
     */
    private java.util.function.BiConsumer<SendResult<String, Object>, Throwable> createTaskEventCallback(TaskEvent event) {
        return (result, ex) -> 
            Optional.ofNullable(ex)
                .ifPresentOrElse(
                    exception -> log.error("Failed to publish task event: {} for task: {}", 
                                          event.getEventType(), event.getTaskId(), exception),
                    () -> log.debug("Task event published successfully: {} for task: {}", 
                                   event.getEventType(), event.getTaskId())
                );
    }

    /**
     * ✅ FUNCTIONAL: Publish system event using Optional chain
     */
    public void publishSystemEvent(BaseEvent event) {
        Optional.of(event)
            .filter(BaseEvent::isValid)
            .ifPresentOrElse(
                this::sendSystemEvent,
                () -> log.error("Invalid system event, not publishing: {}", event)
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to send system event with functional error handling
     */
    private void sendSystemEvent(BaseEvent event) {
        Optional.of(event)
            .map(e -> kafkaTemplate.send(KafkaConfig.SYSTEM_EVENTS_TOPIC, e.getEventId(), e))
            .ifPresentOrElse(
                future -> future.whenComplete(createSystemEventCallback(event)),
                () -> log.error("Failed to create Kafka future for system event: {}", event.getEventType())
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Create callback for system event publishing
     */
    private java.util.function.BiConsumer<SendResult<String, Object>, Throwable> createSystemEventCallback(BaseEvent event) {
        return (result, ex) -> 
            Optional.ofNullable(ex)
                .ifPresentOrElse(
                    exception -> log.error("Failed to publish system event: {}", event.getEventType(), exception),
                    () -> log.debug("System event published successfully: {}", event.getEventType())
                );
    }

    /**
     * ✅ FUNCTIONAL: Publish notification event using Optional chain
     */
    public void publishNotificationEvent(BaseEvent event) {
        Optional.of(event)
            .filter(BaseEvent::isValid)
            .ifPresentOrElse(
                this::sendNotificationEvent,
                () -> log.error("Invalid notification event, not publishing: {}", event)
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to send notification event with functional error handling
     */
    private void sendNotificationEvent(BaseEvent event) {
        Optional.of(event)
            .map(e -> kafkaTemplate.send(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, e.getEventId(), e))
            .ifPresentOrElse(
                future -> future.whenComplete(createNotificationEventCallback(event)),
                () -> log.error("Failed to create Kafka future for notification event: {}", event.getEventType())
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Create callback for notification event publishing
     */
    private java.util.function.BiConsumer<SendResult<String, Object>, Throwable> createNotificationEventCallback(BaseEvent event) {
        return (result, ex) -> 
            Optional.ofNullable(ex)
                .ifPresentOrElse(
                    exception -> log.error("Failed to publish notification event: {}", event.getEventType(), exception),
                    () -> log.debug("Notification event published successfully: {}", event.getEventType())
                );
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
     * Publish agent created event
     */
    public void publishAgentCreated(com.trademaster.agentos.domain.entity.Agent agent) {
        AgentEvent event = AgentEvent.agentRegistered(
            agent.getAgentId(), 
            agent.getAgentName(), 
            agent.getAgentType().toString()
        );
        publishAgentEvent(event);
    }
    
    /**
     * Publish agent started event
     */
    public void publishAgentStarted(com.trademaster.agentos.domain.entity.Agent agent) {
        AgentEvent event = AgentEvent.agentStatusChanged(
            agent.getAgentId(), 
            agent.getAgentName(), 
            "IDLE",
            "ACTIVE"
        );
        publishAgentEvent(event);
    }
    
    /**
     * Publish agent stopped event
     */
    public void publishAgentStopped(com.trademaster.agentos.domain.entity.Agent agent) {
        AgentEvent event = AgentEvent.agentStatusChanged(
            agent.getAgentId(), 
            agent.getAgentName(), 
            agent.getStatus().toString(),
            "SHUTDOWN"
        );
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

    /**
     * Publish task delegated event
     */
    public void publishTaskDelegated(com.trademaster.agentos.domain.entity.Task task, 
                                   com.trademaster.agentos.domain.entity.Agent agent) {
        TaskEvent event = TaskEvent.taskAssigned(
            task.getTaskId(), 
            task.getTaskName(), 
            agent.getAgentId(), 
            agent.getAgentName()
        );
        publishTaskEvent(event);
    }

    /**
     * Publish resource allocated event
     */
    public void publishResourceAllocated(Object allocation) {
        BaseEvent event = BaseEvent.systemEvent("RESOURCE_ALLOCATED", "Resource allocated: " + allocation);
        publishSystemEvent(event);
    }

    /**
     * Publish resources released event
     */
    public void publishResourcesReleased(Long agentId, Object allocation) {
        BaseEvent event = BaseEvent.systemEvent("RESOURCES_RELEASED", 
            "Resources released for agent: " + agentId + ", allocation: " + allocation);
        publishSystemEvent(event);
    }

    /**
     * Publish scaling completed event
     */
    public void publishScalingCompleted(Object action) {
        BaseEvent event = BaseEvent.systemEvent("SCALING_COMPLETED", "Scaling completed: " + action);
        publishSystemEvent(event);
    }

    /**
     * Publish performance prediction event
     */
    public void publishPerformancePrediction(Long agentId, Object prediction) {
        BaseEvent event = BaseEvent.systemEvent("PERFORMANCE_PREDICTION", 
            "Performance prediction for agent: " + agentId + ", prediction: " + prediction);
        publishSystemEvent(event);
    }

    /**
     * Publish workflow step completed event
     */
    public void publishWorkflowStepCompleted(Object execution) {
        BaseEvent event = BaseEvent.systemEvent("WORKFLOW_STEP_COMPLETED", 
            "Workflow step completed: " + execution);
        publishSystemEvent(event);
    }

    /**
     * Publish communication completed event
     */
    public void publishCommunicationCompleted(String messageId) {
        BaseEvent event = BaseEvent.systemEvent("COMMUNICATION_COMPLETED", 
            "Communication completed for message: " + messageId);
        publishSystemEvent(event);
    }

    /**
     * Publish workflow started event
     */
    public void publishWorkflowStarted(Object execution) {
        BaseEvent event = BaseEvent.systemEvent("WORKFLOW_STARTED", 
            "Workflow started: " + execution);
        publishSystemEvent(event);
    }

    /**
     * Publish approval requested event
     */
    public void publishApprovalRequested(Object approvalRequest) {
        BaseEvent event = BaseEvent.systemEvent("APPROVAL_REQUESTED", 
            "Approval requested: " + approvalRequest);
        publishSystemEvent(event);
    }

    /**
     * Publish workflow completed event
     */
    public void publishWorkflowCompleted(Object result) {
        BaseEvent event = BaseEvent.systemEvent("WORKFLOW_COMPLETED", 
            "Workflow completed: " + result);
        publishSystemEvent(event);
    }
}