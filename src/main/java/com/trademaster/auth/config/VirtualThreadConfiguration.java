package com.trademaster.auth.config;

import com.trademaster.auth.constants.AuthConstants;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

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
     * Factory method to create a configured virtual thread executor
     * Follows Template Method pattern for consistent configuration
     */
    private SimpleAsyncTaskExecutor createVirtualThreadExecutor(String namePrefix, Integer concurrencyLimit) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix(namePrefix);
        executor.setThreadFactory(VirtualThreadFactory.createFactory(namePrefix));

        Optional.ofNullable(concurrencyLimit)
            .ifPresent(executor::setConcurrencyLimit);

        return executor;
    }
    
    /**
     * Factory method to create a configured virtual thread scheduler
     */
    private SimpleAsyncTaskScheduler createVirtualThreadScheduler(String namePrefix) {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix(namePrefix);
        return scheduler;
    }

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
        return createVirtualThreadExecutor(AuthConstants.VT_AUTH_PREFIX, null);
    }

    /**
     * TaskScheduler using virtual threads for scheduled operations.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        return createVirtualThreadScheduler(AuthConstants.VT_AUTH_SCHEDULER_PREFIX);
    }

    /**
     * Dedicated executor for authentication operations.
     * Provides isolation for auth processing workloads.
     */
    @Bean(name = "authenticationExecutor")
    public AsyncTaskExecutor authenticationExecutor() {
        return createVirtualThreadExecutor(AuthConstants.VT_AUTH_PROC_PREFIX, 
                                         AuthConstants.AUTH_EXECUTOR_CONCURRENCY_LIMIT);
    }

    /**
     * Executor for notification operations (email, SMS).
     */
    @Bean(name = "notificationExecutor")
    public AsyncTaskExecutor notificationExecutor() {
        return createVirtualThreadExecutor(AuthConstants.VT_NOTIFICATION_PREFIX, 
                                         AuthConstants.NOTIFICATION_EXECUTOR_CONCURRENCY_LIMIT);
    }
}