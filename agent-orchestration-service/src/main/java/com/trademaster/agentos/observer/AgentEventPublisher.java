package com.trademaster.agentos.observer;

import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ✅ FUNCTIONAL: Event Publisher for Agent Events
 * 
 * Thread-safe event publisher using functional programming principles.
 * Provides asynchronous event broadcasting with error handling.
 * 
 * Features:
 * - Thread-safe observer management
 * - Asynchronous event publishing
 * - Functional composition support
 * - Comprehensive error handling
 * - Event filtering and routing
 */
@Service
@RequiredArgsConstructor
public class AgentEventPublisher {
    
    private final StructuredLoggingService structuredLogger;
    private final List<AgentEventObserver> observers = new CopyOnWriteArrayList<>();
    private final Map<String, List<AgentEventObserver>> topicObservers = new ConcurrentHashMap<>();
    private final Executor eventExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * ✅ FUNCTIONAL: Register observer for all events
     */
    public Result<Void, AgentError> registerObserver(AgentEventObserver observer) {
        try {
            observers.add(observer);
            structuredLogger.logDebug("observer_registered", 
                Map.of("observerClass", observer.getClass().getSimpleName(),
                       "totalObservers", observers.size()));
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "observerRegistration", "Failed to register observer: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Register observer for specific event types (topic-based)
     */
    public Result<Void, AgentError> registerObserver(String topic, AgentEventObserver observer) {
        try {
            topicObservers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(observer);
            structuredLogger.logDebug("topic_observer_registered", 
                Map.of("topic", topic,
                       "observerClass", observer.getClass().getSimpleName(),
                       "topicObservers", topicObservers.get(topic).size()));
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "topicObserverRegistration", "Failed to register topic observer: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Unregister observer from all events
     */
    public Result<Void, AgentError> unregisterObserver(AgentEventObserver observer) {
        try {
            boolean removed = observers.remove(observer);
            
            // Remove from all topics
            topicObservers.values().forEach(list -> list.remove(observer));
            
            structuredLogger.logDebug("observer_unregistered", 
                Map.of("removed", removed,
                       "remainingObservers", observers.size()));
            
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "observerUnregistration", "Failed to unregister observer: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Unregister observer from specific topic
     */
    public Result<Void, AgentError> unregisterObserver(String topic, AgentEventObserver observer) {
        try {
            List<AgentEventObserver> topicList = topicObservers.get(topic);
            boolean removed = topicList != null && topicList.remove(observer);
            
            structuredLogger.logDebug("topic_observer_unregistered", 
                Map.of("topic", topic,
                       "removed", removed,
                       "remainingTopicObservers", topicList != null ? topicList.size() : 0));
            
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "topicObserverUnregistration", "Failed to unregister topic observer: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Publish event to all observers asynchronously
     */
    public Result<Void, AgentError> publishEvent(AgentEvent event) {
        try {
            structuredLogger.logDebug("event_publishing_started", 
                Map.of("agentId", event.getAgentId(),
                       "eventType", event.getEventType(),
                       "severity", event.getSeverity(),
                       "observerCount", observers.size()));
            
            // Publish to general observers
            observers.forEach(observer -> 
                eventExecutor.execute(() -> notifyObserver(observer, event)));
            
            // Publish to topic-specific observers
            String topic = getTopicForEvent(event);
            List<AgentEventObserver> topicList = topicObservers.get(topic);
            if (topicList != null) {
                topicList.forEach(observer -> 
                    eventExecutor.execute(() -> notifyObserver(observer, event)));
            }
            
            structuredLogger.logInfo("event_published", 
                Map.of("agentId", event.getAgentId(),
                       "eventType", event.getEventType(),
                       "topic", topic,
                       "notifiedObservers", observers.size() + (topicList != null ? topicList.size() : 0)));
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new AgentError.CommunicationFailed(
                event.getAgentId(), "event_publishing", "Failed to publish event: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Publish event synchronously with result aggregation
     */
    public Result<List<Result<Void, AgentError>>, AgentError> publishEventSync(AgentEvent event) {
        try {
            List<Result<Void, AgentError>> results = observers.stream()
                .map(observer -> notifyObserverSync(observer, event))
                .toList();
            
            // Also notify topic observers
            String topic = getTopicForEvent(event);
            List<AgentEventObserver> topicList = topicObservers.get(topic);
            if (topicList != null) {
                List<Result<Void, AgentError>> topicResults = topicList.stream()
                    .map(observer -> notifyObserverSync(observer, event))
                    .toList();
                
                results = new java.util.ArrayList<>(results);
                results.addAll(topicResults);
            }
            
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = results.size() - successCount;
            
            structuredLogger.logInfo("event_published_sync", 
                Map.of("agentId", event.getAgentId(),
                       "eventType", event.getEventType(),
                       "successCount", successCount,
                       "failureCount", failureCount));
            
            return Result.success(results);
            
        } catch (Exception e) {
            return Result.failure(new AgentError.CommunicationFailed(
                event.getAgentId(), "sync_event_publishing", "Failed to publish event synchronously: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Clear all observers
     */
    public Result<Void, AgentError> clearObservers() {
        try {
            int totalObservers = observers.size() + topicObservers.values().stream()
                .mapToInt(List::size).sum();
            
            observers.clear();
            topicObservers.clear();
            
            structuredLogger.logInfo("observers_cleared", 
                Map.of("clearedObservers", totalObservers));
            
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "observerClear", "Failed to clear observers: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Get observer statistics
     */
    public Result<Map<String, Object>, AgentError> getObserverStatistics() {
        try {
            Map<String, Integer> topicCounts = topicObservers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));
            
            Map<String, Object> stats = Map.of(
                "totalObservers", observers.size(),
                "topicObservers", topicCounts,
                "totalTopicObservers", topicCounts.values().stream().mapToInt(Integer::intValue).sum(),
                "topics", topicObservers.keySet()
            );
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.failure(new AgentError.ValidationError(
                "observerStatistics", "Failed to get observer statistics: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Notify observer with error handling
     */
    private void notifyObserver(AgentEventObserver observer, AgentEvent event) {
        try {
            observer.handleEvent(event)
                .onFailure(error -> structuredLogger.logWarning("observer_notification_failed", 
                    Map.of("agentId", event.getAgentId(),
                           "eventType", event.getEventType(),
                           "observerClass", observer.getClass().getSimpleName(),
                           "error", error.getMessage())));
        } catch (Exception e) {
            structuredLogger.logError("observer_exception", 
                "Observer notification failed", e,
                Map.of("agentId", event.getAgentId(),
                       "eventType", event.getEventType(),
                       "observerClass", observer.getClass().getSimpleName()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Notify observer synchronously with result
     */
    private Result<Void, AgentError> notifyObserverSync(AgentEventObserver observer, AgentEvent event) {
        try {
            return observer.handleEvent(event);
        } catch (Exception e) {
            structuredLogger.logError("observer_sync_exception", 
                "Synchronous observer notification failed", e,
                Map.of("agentId", event.getAgentId(),
                       "eventType", event.getEventType(),
                       "observerClass", observer.getClass().getSimpleName()));
            
            return Result.failure(new AgentError.CommunicationFailed(
                event.getAgentId(), "observer_notification", 
                "Observer notification failed: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Get topic for event (for topic-based routing)
     */
    private String getTopicForEvent(AgentEvent event) {
        return switch (event.getEventType()) {
            case TASK_ASSIGNED, TASK_COMPLETED, TASK_FAILED -> "tasks";
            case HEARTBEAT_RECEIVED, HEARTBEAT_MISSED, RECOVERY_INITIATED, 
                 RECOVERY_COMPLETED, RECOVERY_FAILED -> "health";
            case SECURITY_VIOLATION, UNAUTHORIZED_ACCESS -> "security";
            case PERFORMANCE_DEGRADATION, PERFORMANCE_IMPROVED -> "performance";
            case ERROR_OCCURRED, CRITICAL_ERROR -> "errors";
            default -> "general";
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Publish agent status changed event
     */
    public Result<Void, AgentError> publishAgentStatusChanged(
            com.trademaster.agentos.domain.entity.Agent agent, 
            com.trademaster.agentos.domain.entity.AgentStatus previousState) {
        
        AgentEvent statusEvent = AgentEvent.builder()
            .agentId(agent.getAgentId())
            .eventType(AgentEvent.AgentEventType.STATUS_CHANGED)
            .severity(AgentEvent.Severity.INFO)
            .timestamp(java.time.Instant.now())
            .metadata(Map.of(
                "previousStatus", previousState.toString(),
                "newStatus", agent.getStatus().toString(),
                "agentName", agent.getAgentName()
            ))
            .build();
            
        return publishEvent(statusEvent);
    }
    
    /**
     * ✅ FUNCTIONAL: Create event publisher with pre-configured observers
     */
    public static AgentEventPublisher withObservers(StructuredLoggingService logger,
                                                   AgentEventObserver... observers) {
        AgentEventPublisher publisher = new AgentEventPublisher(logger);
        java.util.Arrays.stream(observers).forEach(publisher::registerObserver);
        return publisher;
    }
}