package com.trademaster.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Virtual Thread Configuration for TradeMaster Authentication Service
 * 
 * Enables virtual threads for all async operations following Java 24 and Spring Boot 3.5+ patterns.
 * Virtual threads provide lightweight concurrency ideal for I/O-bound operations like:
 * - Authentication requests
 * - Database operations
 * - External API calls
 * - Email sending
 * - Token validation
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
     * - Authentication processing
     * - Background tasks
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-auth-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // Log any uncaught exceptions in virtual threads
                System.err.println("Uncaught exception in auth virtual thread: " + e.getMessage());
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
        scheduler.setThreadNamePrefix("vt-auth-scheduler-");
        return scheduler;
    }

    /**
     * Dedicated executor for authentication operations.
     * Provides isolation for auth processing workloads.
     */
    @Bean(name = "authenticationExecutor")
    public AsyncTaskExecutor authenticationExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-auth-proc-");
        executor.setConcurrencyLimit(500); // Reasonable limit for auth operations
        return executor;
    }

    /**
     * Executor for notification operations (email, SMS).
     */
    @Bean(name = "notificationExecutor")
    public AsyncTaskExecutor notificationExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-notification-");
        executor.setConcurrencyLimit(200); // Limit for external notification services
        return executor;
    }
}