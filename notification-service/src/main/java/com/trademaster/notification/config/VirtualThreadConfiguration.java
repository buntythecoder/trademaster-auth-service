package com.trademaster.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Virtual Thread Configuration for TradeMaster Notification Service
 * 
 * Enables virtual threads for all async operations following Java 24 and Spring Boot 3.5+ patterns.
 * Virtual threads provide lightweight concurrency ideal for I/O-bound operations like:
 * - Email sending
 * - SMS delivery
 * - Push notifications
 * - External service calls
 * - Message queue processing
 * - Template rendering
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
@EnableScheduling
public class VirtualThreadConfiguration {

    /**
     * Primary AsyncTaskExecutor using virtual threads.
     * This executor will be used by:
     * - @Async methods in services
     * - Spring MVC async operations  
     * - Notification processing
     * - Background tasks
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-notification-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // Log any uncaught exceptions in virtual threads
                System.err.println("Uncaught exception in notification virtual thread: " + e.getMessage());
                throw e;
            }
        });
        return executor;
    }

    /**
     * TaskScheduler using virtual threads for scheduled operations.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskExecutor scheduler = new SimpleAsyncTaskExecutor();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("vt-notification-scheduler-");
        return scheduler;
    }

    /**
     * Dedicated executor for email operations.
     * Provides isolation for email sending workloads.
     */
    @Bean(name = "emailExecutor")
    public AsyncTaskExecutor emailExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-email-");
        executor.setConcurrencyLimit(100); // Conservative limit for email sending
        return executor;
    }

    /**
     * Executor for SMS and messaging operations.
     */
    @Bean(name = "smsExecutor")
    public AsyncTaskExecutor smsExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-sms-");
        executor.setConcurrencyLimit(50); // Limit for SMS service rate limits
        return executor;
    }

    /**
     * Executor for push notification operations.
     */
    @Bean(name = "pushExecutor")
    public AsyncTaskExecutor pushExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-push-");
        executor.setConcurrencyLimit(200); // Higher limit for push notifications
        return executor;
    }

    /**
     * Executor for template rendering and processing.
     */
    @Bean(name = "templateExecutor")
    public AsyncTaskExecutor templateExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-template-");
        executor.setConcurrencyLimit(150); // Limit for CPU-intensive template rendering
        return executor;
    }
}