package com.trademaster.userprofile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Virtual Thread Configuration for TradeMaster User Profile Service
 * 
 * Enables virtual threads for all async operations following Java 24 and Spring Boot 3.5+ patterns.
 * Virtual threads provide lightweight concurrency ideal for I/O-bound operations like:
 * - Profile data retrieval and updates
 * - Document upload and processing
 * - Database operations
 * - File storage operations
 * - Event publishing
 * - External API calls
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
     * - Profile processing
     * - Background tasks
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-profile-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // Log any uncaught exceptions in virtual threads
                System.err.println("Uncaught exception in profile virtual thread: " + e.getMessage());
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
        scheduler.setThreadNamePrefix("vt-profile-scheduler-");
        return scheduler;
    }

    /**
     * Dedicated executor for profile operations.
     * Provides isolation for profile processing workloads.
     */
    @Bean(name = "profileExecutor")
    public AsyncTaskExecutor profileExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-profile-proc-");
        executor.setConcurrencyLimit(300); // Reasonable limit for profile operations
        return executor;
    }

    /**
     * Executor for file operations and document processing.
     */
    @Bean(name = "fileExecutor")
    public AsyncTaskExecutor fileExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-file-");
        executor.setConcurrencyLimit(150); // Limit for file I/O operations
        return executor;
    }

    /**
     * Executor for event publishing and message queue operations.
     */
    @Bean(name = "eventExecutor")
    public AsyncTaskExecutor eventExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-event-");
        executor.setConcurrencyLimit(200); // Limit for event processing
        return executor;
    }
}