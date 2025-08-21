package com.trademaster.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Virtual Thread Configuration for TradeMaster Market Data Service
 * 
 * Enables virtual threads for all async operations following Java 24 and Spring Boot 3.5+ patterns.
 * Virtual threads provide lightweight concurrency ideal for I/O-bound operations like market data fetching.
 * 
 * Benefits:
 * - Massive scalability for concurrent I/O operations
 * - Reduced memory footprint compared to platform threads
 * - Better resource utilization for blocking operations
 * - Simplified concurrency model without thread pooling complexity
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
     * - @Async methods
     * - Spring MVC async operations  
     * - Spring WebSocket operations
     * - Background initialization tasks
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-async-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // Log any uncaught exceptions in virtual threads
                System.err.println("Uncaught exception in virtual thread: " + e.getMessage());
                throw e;
            }
        });
        return executor;
    }

    /**
     * TaskScheduler using virtual threads for scheduled operations.
     * Used for:
     * - @Scheduled methods
     * - Market data polling
     * - Periodic health checks
     */
    @Bean
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskExecutor scheduler = new SimpleAsyncTaskExecutor();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("vt-scheduler-");
        return scheduler;
    }

    /**
     * Dedicated executor for market data operations.
     * Provides isolation for market data processing workloads.
     */
    @Bean(name = "marketDataExecutor")
    public AsyncTaskExecutor marketDataExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-market-data-");
        executor.setConcurrencyLimit(1000); // High concurrency limit for virtual threads
        return executor;
    }

    /**
     * Executor for external API calls.
     * Optimized for I/O-bound operations with external services.
     */
    @Bean(name = "externalApiExecutor")  
    public AsyncTaskExecutor externalApiExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("vt-ext-api-");
        executor.setConcurrencyLimit(500); // Reasonable limit for external API calls
        return executor;
    }
}