package com.trademaster.subscription.event;

import com.trademaster.subscription.service.StructuredLoggingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Event Publisher
 * 
 * Publishes subscription events to Kafka topics with proper error handling and metrics.
 * Uses Virtual Threads for non-blocking event publishing.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;
    private final StructuredLoggingService loggingService;
    private final MeterRegistry meterRegistry;

    @Value("${app.kafka.topics.subscription-events:subscription-events}")
    private String subscriptionEventsTopic;

    @Value("${app.kafka.topics.usage-events:usage-events}")
    private String usageEventsTopic;

    @Value("${app.kafka.topics.billing-events:billing-events}")
    private String billingEventsTopic;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    // Metrics
    private final Counter eventPublishedCounter;
    private final Counter eventFailedCounter;
    private final Timer eventPublishTimer;

    public SubscriptionEventPublisher(KafkaTemplate<String, SubscriptionEvent> kafkaTemplate,
                                     StructuredLoggingService loggingService,
                                     MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.loggingService = loggingService;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.eventPublishedCounter = Counter.builder("subscription.events.published")
            .description("Number of subscription events published")
            .register(meterRegistry);
            
        this.eventFailedCounter = Counter.builder("subscription.events.failed")
            .description("Number of subscription events that failed to publish")
            .register(meterRegistry);
            
        this.eventPublishTimer = Timer.builder("subscription.events.publish.duration")
            .description("Time taken to publish subscription events")
            .register(meterRegistry);
    }

    /**
     * Publish subscription lifecycle event
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> publishSubscriptionEvent(SubscriptionEvent event) {
        return publishEvent(subscriptionEventsTopic, event, "subscription");
    }

    /**
     * Publish usage-related event
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> publishUsageEvent(SubscriptionEvent event) {
        return publishEvent(usageEventsTopic, event, "usage");
    }

    /**
     * Publish billing-related event
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> publishBillingEvent(SubscriptionEvent event) {
        return publishEvent(billingEventsTopic, event, "billing");
    }

    /**
     * Publish notification-related event
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> publishNotificationEvent(SubscriptionEvent event) {
        return publishEvent(notificationEventsTopic, event, "notification");
    }

    /**
     * Generic event publishing with error handling and metrics
     */
    private CompletableFuture<Void> publishEvent(String topic, SubscriptionEvent event, String eventCategory) {
        return CompletableFuture.runAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                String key = generateEventKey(event);
                
                log.info("Publishing {} event: {} to topic: {}", 
                        eventCategory, event.getEventType(), topic);
                
                CompletableFuture<SendResult<String, SubscriptionEvent>> future = 
                    kafkaTemplate.send(topic, key, event);
                
                future.whenComplete((result, throwable) -> {
                    sample.stop(eventPublishTimer);
                    
                    if (throwable != null) {
                        handlePublishFailure(event, topic, throwable, eventCategory);
                    } else {
                        handlePublishSuccess(event, topic, result, eventCategory);
                    }
                });
                
            } catch (Exception e) {
                sample.stop(eventPublishTimer);
                handlePublishFailure(event, topic, e, eventCategory);
                throw e;
            }
        });
    }

    /**
     * Handle successful event publishing
     */
    private void handlePublishSuccess(SubscriptionEvent event, String topic, 
                                    SendResult<String, SubscriptionEvent> result, String eventCategory) {
        eventPublishedCounter.increment();
        
        loggingService.logBusinessEvent(
            "event_published",
            event.getUserId().toString(),
            event.getSubscriptionId().toString(),
            event.getEventType(),
            Map.of(
                "topic", topic,
                "partition", result.getRecordMetadata().partition(),
                "offset", result.getRecordMetadata().offset(),
                "eventCategory", eventCategory
            )
        );
        
        log.info("Successfully published {} event: {} to topic: {}, partition: {}, offset: {}",
                eventCategory, event.getEventType(), topic, 
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
    }

    /**
     * Handle failed event publishing
     */
    private void handlePublishFailure(SubscriptionEvent event, String topic, 
                                    Throwable throwable, String eventCategory) {
        eventFailedCounter.increment();
        
        loggingService.logError(
            "event_publish_failed",
            throwable.getMessage(),
            "EVENT_PUBLISH_ERROR",
            (Exception) throwable,
            Map.of(
                "eventId", event.getEventId(),
                "eventType", event.getEventType(),
                "topic", topic,
                "eventCategory", eventCategory,
                "subscriptionId", event.getSubscriptionId().toString(),
                "userId", event.getUserId().toString()
            )
        );
        
        log.error("Failed to publish {} event: {} to topic: {}", 
                eventCategory, event.getEventType(), topic, throwable);
        
        // Consider implementing retry logic or dead letter queue here
        // For now, we'll just log the failure
    }

    /**
     * Generate partition key for event
     */
    private String generateEventKey(SubscriptionEvent event) {
        // Use user ID as partition key to ensure all events for a user go to same partition
        // This maintains event ordering for each user
        return event.getUserId().toString();
    }

    /**
     * Publish subscription created event with proper routing
     */
    public CompletableFuture<Void> publishSubscriptionCreated(SubscriptionEvent event) {
        CompletableFuture<Void> subscriptionPublish = publishSubscriptionEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(subscriptionPublish, notificationPublish);
    }

    /**
     * Publish subscription activated event with proper routing
     */
    public CompletableFuture<Void> publishSubscriptionActivated(SubscriptionEvent event) {
        CompletableFuture<Void> subscriptionPublish = publishSubscriptionEvent(event);
        CompletableFuture<Void> billingPublish = publishBillingEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(subscriptionPublish, billingPublish, notificationPublish);
    }

    /**
     * Publish subscription upgraded event with proper routing
     */
    public CompletableFuture<Void> publishSubscriptionUpgraded(SubscriptionEvent event) {
        CompletableFuture<Void> subscriptionPublish = publishSubscriptionEvent(event);
        CompletableFuture<Void> billingPublish = publishBillingEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(subscriptionPublish, billingPublish, notificationPublish);
    }

    /**
     * Publish subscription cancelled event with proper routing
     */
    public CompletableFuture<Void> publishSubscriptionCancelled(SubscriptionEvent event) {
        CompletableFuture<Void> subscriptionPublish = publishSubscriptionEvent(event);
        CompletableFuture<Void> billingPublish = publishBillingEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(subscriptionPublish, billingPublish, notificationPublish);
    }

    /**
     * Publish usage limit exceeded event
     */
    public CompletableFuture<Void> publishUsageLimitExceeded(SubscriptionEvent event) {
        CompletableFuture<Void> usagePublish = publishUsageEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(usagePublish, notificationPublish);
    }

    /**
     * Publish trial started event
     */
    public CompletableFuture<Void> publishTrialStarted(SubscriptionEvent event) {
        CompletableFuture<Void> subscriptionPublish = publishSubscriptionEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(subscriptionPublish, notificationPublish);
    }

    /**
     * Publish payment failed event
     */
    public CompletableFuture<Void> publishPaymentFailed(SubscriptionEvent event) {
        CompletableFuture<Void> billingPublish = publishBillingEvent(event);
        CompletableFuture<Void> notificationPublish = publishNotificationEvent(event);
        
        return CompletableFuture.allOf(billingPublish, notificationPublish);
    }

    /**
     * Health check method
     */
    public boolean isHealthy() {
        try {
            // Simple health check - could be expanded
            return kafkaTemplate != null;
        } catch (Exception e) {
            log.warn("Event publisher health check failed", e);
            return false;
        }
    }
}