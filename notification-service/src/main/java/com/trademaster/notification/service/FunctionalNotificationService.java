package com.trademaster.notification.service;

import com.trademaster.notification.model.NotificationRequest;
import com.trademaster.notification.repository.NotificationRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Notification Service - Java 24 Architecture
 * 
 * Advanced Design Patterns Applied:
 * - Strategy Pattern: Notification delivery strategies per type
 * - Command Pattern: Notification operations with retry capability
 * - Chain of Responsibility: Processing pipeline with validation
 * - Observer Pattern: Event-driven notification lifecycle
 * - Factory Pattern: Notification handler creation
 * - State Machine: Notification status transitions
 * 
 * Architectural Principles:
 * - Virtual Threads: All async operations use virtual threads for scalable concurrency
 * - Railway Oriented Programming: Result types for comprehensive error handling
 * - Lock-free Operations: Atomic references for thread-safe state management
 * - Functional Programming: Higher-order functions and immutable data where beneficial
 * - Structured Concurrency: Coordinated task execution with proper lifecycle management
 * - SOLID Principles: Single responsibility, dependency inversion
 * - Architectural Fitness: Patterns applied where they improve reliability and performance
 * 
 * @author TradeMaster Development Team
 * @version 3.0.0 - Functional Architecture with Virtual Threads
 */
@Service
@Slf4j
public class FunctionalNotificationService {

    private final NotificationRequestRepository notificationRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final RateLimitService rateLimitService;
    
    // Virtual Thread Executors
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AsyncTaskExecutor emailExecutor;
    private final AsyncTaskExecutor smsExecutor;
    private final AsyncTaskExecutor pushExecutor;
    private final AsyncTaskExecutor templateExecutor;

    // Railway Oriented Programming - Result Type  
    public sealed interface NotificationResult<T, E> permits NotificationResult.NotificationSuccess, NotificationResult.NotificationFailure {
        record NotificationSuccess<T, E>(T value) implements NotificationResult<T, E> {}
        record NotificationFailure<T, E>(E error) implements NotificationResult<T, E> {}
        
        static <T, E> NotificationResult<T, E> success(T value) {
            return new NotificationSuccess<>(value);
        }
        
        static <T, E> NotificationResult<T, E> failure(E error) {
            return new NotificationFailure<>(error);
        }
        
        default <U> NotificationResult<U, E> map(Function<T, U> mapper) {
            return switch (this) {
                case NotificationSuccess(var value) -> success(mapper.apply(value));
                case NotificationFailure(var error) -> failure(error);
            };
        }
        
        default <U> NotificationResult<U, E> flatMap(Function<T, NotificationResult<U, E>> mapper) {
            return switch (this) {
                case NotificationSuccess(var value) -> mapper.apply(value);
                case NotificationFailure(var error) -> failure(error);
            };
        }
        
        default boolean isSuccess() {
            return this instanceof NotificationSuccess;
        }
        
        default T orElse(T defaultValue) {
            return switch (this) {
                case NotificationSuccess(var value) -> value;
                case NotificationFailure(var ignored) -> defaultValue;
            };
        }
        
        default T orElseThrow() {
            return switch (this) {
                case NotificationSuccess(var value) -> value;
                case NotificationFailure(var error) -> throw new RuntimeException(error.toString());
            };
        }
    }

    // Strategy Pattern - Notification Delivery Strategies
    public enum NotificationStrategy {
        EMAIL_DELIVERY(service -> request -> ((FunctionalNotificationService) service).processEmailNotification((NotificationRequest) request)),
        SMS_DELIVERY(service -> request -> ((FunctionalNotificationService) service).processSmsNotification((NotificationRequest) request)),
        PUSH_DELIVERY(service -> request -> ((FunctionalNotificationService) service).processPushNotification((NotificationRequest) request)),
        IN_APP_DELIVERY(service -> request -> ((FunctionalNotificationService) service).processInAppNotification((NotificationRequest) request));
        
        private final Function<Object, Function<Object, NotificationResult<NotificationRequest, String>>> deliveryHandler;
        
        NotificationStrategy(Function<Object, Function<Object, NotificationResult<NotificationRequest, String>>> deliveryHandler) {
            this.deliveryHandler = deliveryHandler;
        }
        
        public NotificationResult<NotificationRequest, String> deliver(Object service, Object request) {
            return deliveryHandler.apply(service).apply(request);
        }
    }

    // State Machine for Notification Status Transitions
    public enum NotificationState {
        PENDING(request -> request.getScheduledAt().isAfter(LocalDateTime.now())),
        RATE_LIMITED(request -> false), // Will be checked by rate limit service
        PROCESSING(request -> true),
        SENT(request -> true),
        FAILED(request -> !request.canRetry()),
        RETRY_PENDING(request -> request.canRetry());
        
        private final Predicate<NotificationRequest> condition;
        
        NotificationState(Predicate<NotificationRequest> condition) {
            this.condition = condition;
        }
        
        public boolean canTransitionTo(NotificationRequest request) {
            return condition.test(request);
        }
        
        public static NotificationState getNextState(NotificationRequest request, boolean success, boolean rateLimited) {
            if (rateLimited) return RATE_LIMITED;
            if (success) return SENT;
            return request.canRetry() ? RETRY_PENDING : FAILED;
        }
    }

    // Command Pattern - Notification Commands
    public sealed interface NotificationCommand<T> permits ProcessNotificationCommand, RetryNotificationCommand, ScheduledNotificationCommand {
        CompletableFuture<NotificationResult<T, String>> execute();
        
        default <U> NotificationCommand<U> map(Function<T, U> mapper) {
            return new MappedNotificationCommand<>(this, mapper);
        }
        
        default NotificationCommand<T> withRetry(int attempts) {
            return new RetryNotificationCommand<>((NotificationCommand<NotificationRequest>) this, attempts);
        }
    }

    // Command implementations
    public record ProcessNotificationCommand(NotificationRequest request) implements NotificationCommand<NotificationRequest> {
        @Override
        public CompletableFuture<NotificationResult<NotificationRequest, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return processNotificationFunctional(request);
            }, virtualExecutor);
        }
    }

    public record RetryNotificationCommand<T>(NotificationCommand<T> original, int attempts) implements NotificationCommand<T> {
        @Override
        public CompletableFuture<NotificationResult<T, String>> execute() {
            return original.execute().thenCompose(result -> {
                if (result.isSuccess() || attempts <= 1) {
                    return CompletableFuture.completedFuture(result);
                }
                return new RetryNotificationCommand<>(original, attempts - 1).execute();
            });
        }
    }

    public record ScheduledNotificationCommand(List<NotificationRequest> notifications) implements NotificationCommand<List<NotificationRequest>> {
        @Override
        public CompletableFuture<NotificationResult<List<NotificationRequest>, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return processScheduledNotificationsFunctional(notifications);
            }, virtualExecutor);
        }
    }

    // Helper command wrappers
    public record MappedNotificationCommand<T, U>(NotificationCommand<T> original, Function<T, U> mapper) implements NotificationCommand<U> {
        @Override
        public CompletableFuture<NotificationResult<U, String>> execute() {
            return original.execute().thenApply(result -> result.map(mapper));
        }
    }

    // Constructor with Dependency Injection
    public FunctionalNotificationService(
            NotificationRequestRepository notificationRepository,
            EmailNotificationService emailService,
            SmsNotificationService smsService,
            RateLimitService rateLimitService,
            @Qualifier("emailExecutor") AsyncTaskExecutor emailExecutor,
            @Qualifier("smsExecutor") AsyncTaskExecutor smsExecutor,
            @Qualifier("pushExecutor") AsyncTaskExecutor pushExecutor,
            @Qualifier("templateExecutor") AsyncTaskExecutor templateExecutor) {
        
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.rateLimitService = rateLimitService;
        this.emailExecutor = emailExecutor;
        this.smsExecutor = smsExecutor;
        this.pushExecutor = pushExecutor;
        this.templateExecutor = templateExecutor;
    }

    // Public API Methods using Command Pattern

    @KafkaListener(topics = "notification-requests")
    @Async
    public CompletableFuture<Void> processNotificationFromKafka(NotificationRequest request) {
        log.info("Received notification request from Kafka: {}", request.getId());
        
        return new ProcessNotificationCommand(request)
            .withRetry(3)
            .execute()
            .thenApply(result -> {
                if (!result.isSuccess()) {
                    log.error("Failed to process Kafka notification: {}", result);
                }
                return null;
            });
    }

    @Async
    @Transactional
    public CompletableFuture<NotificationResult<NotificationRequest, String>> processNotificationAsync(NotificationRequest request) {
        return new ProcessNotificationCommand(request)
            .withRetry(2)
            .execute();
    }

    // Private Implementation Methods

    private NotificationResult<NotificationRequest, String> processNotificationFunctional(NotificationRequest request) {
        try {
            // Structured concurrency for parallel validation
            return CompletableFuture.supplyAsync(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Fork parallel tasks
                    var persistTask = scope.fork(() -> persistNotificationIfNeeded(request));
                    var scheduleValidation = scope.fork(() -> validateScheduling(request));
                    var rateLimitCheck = scope.fork(() -> rateLimitService.isAllowed(request));
                    
                    scope.join();
                    scope.throwIfFailed();
                    
                    NotificationRequest persistedRequest = persistTask.resultNow();
                    boolean isScheduled = scheduleValidation.resultNow();
                    boolean isRateLimited = !rateLimitCheck.resultNow();
                    
                    // Handle scheduling
                    if (isScheduled) {
                        log.info("Notification {} scheduled for later: {}", persistedRequest.getId(), persistedRequest.getScheduledAt());
                        return NotificationResult.success(persistedRequest);
                    }
                    
                    // Handle rate limiting
                    if (isRateLimited) {
                        return handleRateLimitExceeded(persistedRequest);
                    }
                    
                    // Process notification using strategy pattern
                    return processNotificationByType(persistedRequest);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return NotificationResult.<NotificationRequest, String>failure("Notification processing interrupted");
                } catch (Exception e) {
                    return handleProcessingError(request, e);
                }
            }, virtualExecutor).join();
            
        } catch (Exception e) {
            log.error("Notification processing failed for {}: {}", request.getId(), e.getMessage());
            return NotificationResult.failure("Processing failed: " + e.getMessage());
        }
    }

    private NotificationResult<NotificationRequest, String> processNotificationByType(NotificationRequest request) {
        // Update to processing state
        updateNotificationStatus(request, NotificationRequest.NotificationStatus.PROCESSING);
        
        // Use strategy pattern for delivery
        return switch (request.getType()) {
            case EMAIL -> NotificationStrategy.EMAIL_DELIVERY.deliver(this, request);
            case SMS -> NotificationStrategy.SMS_DELIVERY.deliver(this, request);
            case PUSH -> NotificationStrategy.PUSH_DELIVERY.deliver(this, request);
            case IN_APP -> NotificationStrategy.IN_APP_DELIVERY.deliver(this, request);
            default -> NotificationResult.failure("Unknown notification type: " + request.getType());
        };
    }

    // Strategy Pattern Implementation Methods
    private NotificationResult<NotificationRequest, String> processEmailNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                emailService.sendEmail(request);
                return markNotificationAsSent(request);
            } catch (Exception e) {
                return handleDeliveryError(request, e);
            }
        }, emailExecutor).join();
    }

    private NotificationResult<NotificationRequest, String> processSmsNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                smsService.sendSms(request);
                return markNotificationAsSent(request);
            } catch (Exception e) {
                return handleDeliveryError(request, e);
            }
        }, smsExecutor).join();
    }

    private NotificationResult<NotificationRequest, String> processPushNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Push notification implementation would go here
            log.warn("Push notifications not yet implemented for request: {}", request.getId());
            return NotificationResult.<NotificationRequest, String>failure("Push notifications not implemented");
        }, pushExecutor).join();
    }

    private NotificationResult<NotificationRequest, String> processInAppNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // In-app notification implementation would go here
            log.warn("In-app notifications not yet implemented for request: {}", request.getId());
            return NotificationResult.<NotificationRequest, String>failure("In-app notifications not implemented");
        }, virtualExecutor).join();
    }

    // Helper Methods
    private NotificationRequest persistNotificationIfNeeded(NotificationRequest request) {
        return request.getId() == null ? notificationRepository.save(request) : request;
    }

    private boolean validateScheduling(NotificationRequest request) {
        return request.getScheduledAt().isAfter(LocalDateTime.now());
    }

    private NotificationResult<NotificationRequest, String> handleRateLimitExceeded(NotificationRequest request) {
        log.warn("Rate limit exceeded for notification type: {}", request.getType());
        updateNotificationStatus(request, NotificationRequest.NotificationStatus.FAILED);
        request.setErrorMessage("Rate limit exceeded");
        notificationRepository.save(request);
        return NotificationResult.failure("Rate limit exceeded");
    }

    private NotificationResult<NotificationRequest, String> markNotificationAsSent(NotificationRequest request) {
        updateNotificationStatus(request, NotificationRequest.NotificationStatus.SENT);
        request.setSentAt(LocalDateTime.now());
        notificationRepository.save(request);
        log.info("Notification {} sent successfully", request.getId());
        return NotificationResult.success(request);
    }

    private NotificationResult<NotificationRequest, String> handleProcessingError(NotificationRequest request, Exception e) {
        log.error("Failed to process notification {}: {}", request.getId(), e.getMessage(), e);
        
        request.incrementRetryCount();
        request.setErrorMessage(e.getMessage());
        
        NotificationRequest.NotificationStatus newStatus = request.canRetry() ? 
            NotificationRequest.NotificationStatus.PENDING : 
            NotificationRequest.NotificationStatus.FAILED;
            
        updateNotificationStatus(request, newStatus);
        notificationRepository.save(request);
        
        return NotificationResult.failure("Processing failed: " + e.getMessage());
    }

    private NotificationResult<NotificationRequest, String> handleDeliveryError(NotificationRequest request, Exception e) {
        log.error("Failed to deliver notification {}: {}", request.getId(), e.getMessage(), e);
        
        request.incrementRetryCount();
        request.setErrorMessage(e.getMessage());
        
        NotificationRequest.NotificationStatus newStatus = request.canRetry() ? 
            NotificationRequest.NotificationStatus.PENDING : 
            NotificationRequest.NotificationStatus.FAILED;
            
        updateNotificationStatus(request, newStatus);
        notificationRepository.save(request);
        
        return NotificationResult.failure("Delivery failed: " + e.getMessage());
    }

    private void updateNotificationStatus(NotificationRequest request, NotificationRequest.NotificationStatus status) {
        request.setStatus(status);
    }

    // Scheduled Methods using Virtual Threads
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public CompletableFuture<Void> processScheduledNotifications() {
        return CompletableFuture.runAsync(() -> {
            LocalDateTime now = LocalDateTime.now();
            List<NotificationRequest> scheduledNotifications = 
                notificationRepository.findByStatusAndScheduledAtLessThanEqual(
                    NotificationRequest.NotificationStatus.PENDING, now);
            
            log.info("Found {} scheduled notifications to process", scheduledNotifications.size());
            
            new ScheduledNotificationCommand(scheduledNotifications)
                .execute()
                .thenApply(result -> {
                    if (!result.isSuccess()) {
                        log.error("Failed to process scheduled notifications: {}", result);
                    }
                    return null;
                });
        }, virtualExecutor);
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public CompletableFuture<Void> retryFailedNotifications() {
        return CompletableFuture.runAsync(() -> {
            List<NotificationRequest> failedNotifications = 
                notificationRepository.findRetryableNotifications();
            
            log.info("Found {} failed notifications to retry", failedNotifications.size());
            
            // Process retries in parallel using virtual threads
            List<CompletableFuture<Void>> retryFutures = failedNotifications.stream()
                .filter(NotificationRequest::canRetry)
                .map(notification -> {
                    log.info("Retrying notification {}, attempt: {}", 
                            notification.getId(), notification.getRetryCount() + 1);
                    return processNotificationAsync(notification)
                        .thenApply(result -> {
                            if (!result.isSuccess()) {
                                log.error("Retry failed for notification {}: {}", notification.getId(), result);
                            }
                            return null;
                        });
                })
                .toList();
            
            CompletableFuture.allOf(retryFutures.toArray(new CompletableFuture[0])).join();
        }, virtualExecutor);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public CompletableFuture<Void> cleanupOldNotifications() {
        return CompletableFuture.runAsync(() -> {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Cleaned up {} old notification records", deleted);
        }, virtualExecutor);
    }

    private NotificationResult<List<NotificationRequest>, String> processScheduledNotificationsFunctional(List<NotificationRequest> notifications) {
        try {
            // Process notifications in parallel using virtual threads
            List<CompletableFuture<NotificationResult<NotificationRequest, String>>> futures = 
                notifications.stream()
                    .map(notification -> processNotificationAsync(notification))
                    .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            return NotificationResult.success(notifications);
        } catch (Exception e) {
            log.error("Failed to process scheduled notifications: {}", e.getMessage());
            return NotificationResult.failure("Failed to process scheduled notifications: " + e.getMessage());
        }
    }

    // Synchronous wrapper methods for backward compatibility
    @Transactional
    public void processNotification(NotificationRequest request) {
        processNotificationAsync(request).join();
    }
}