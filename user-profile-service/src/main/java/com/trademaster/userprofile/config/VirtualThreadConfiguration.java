package com.trademaster.userprofile.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class VirtualThreadConfiguration {
    
    // Constants for thread pool configuration - Rule #17
    private static final int SCHEDULER_POOL_SIZE = 10;
    private static final int PROFILE_OPERATIONS_CONCURRENCY_LIMIT = 300;
    private static final int FILE_OPERATIONS_CONCURRENCY_LIMIT = 150;
    private static final int EVENT_PROCESSING_CONCURRENCY_LIMIT = 200;

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
                log.error("Uncaught exception in profile virtual thread", e);
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
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        scheduler.setThreadNamePrefix("vt-profile-scheduler-");
        scheduler.setTaskDecorator(runnable -> Thread.ofVirtual().unstarted(runnable));
        scheduler.initialize();
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
        executor.setConcurrencyLimit(PROFILE_OPERATIONS_CONCURRENCY_LIMIT); // Reasonable limit for profile operations
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
        executor.setConcurrencyLimit(FILE_OPERATIONS_CONCURRENCY_LIMIT); // Limit for file I/O operations
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
        executor.setConcurrencyLimit(EVENT_PROCESSING_CONCURRENCY_LIMIT); // Limit for event processing
        return executor;
    }
}