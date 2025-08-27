package com.trademaster.brokerauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Async Configuration
 * 
 * Configures Virtual Thread executors for async operations.
 * Optimized for high-throughput broker authentication operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    
    /**
     * Virtual Thread executor for broker authentication operations
     * Optimized for I/O-bound operations like HTTP calls to broker APIs
     */
    @Bean(name = "brokerAuthExecutor")
    public Executor brokerAuthExecutor() {
        log.info("Creating Virtual Thread executor for broker authentication operations");
        
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("broker-auth-", 0)
                .factory();
        
        return task -> {
            Thread virtualThread = virtualThreadFactory.newThread(task);
            virtualThread.start();
        };
    }
    
    /**
     * Traditional thread pool executor for CPU-intensive operations
     * Used for encryption/decryption and other compute-heavy tasks
     */
    @Bean(name = "brokerComputeExecutor")
    public ThreadPoolTaskExecutor brokerComputeExecutor() {
        log.info("Creating ThreadPool executor for compute-intensive operations");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("broker-compute-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return executor;
    }
    
    /**
     * Virtual Thread executor for rate limiting operations
     * High concurrency for rate limit checks and Redis operations
     */
    @Bean(name = "rateLimitExecutor")
    public Executor rateLimitExecutor() {
        log.info("Creating Virtual Thread executor for rate limiting operations");
        
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("rate-limit-", 0)
                .factory();
        
        return task -> {
            Thread virtualThread = virtualThreadFactory.newThread(task);
            virtualThread.start();
        };
    }
    
    /**
     * Virtual Thread executor for session management operations
     * Optimized for session validation and refresh operations
     */
    @Bean(name = "sessionManagementExecutor")
    public Executor sessionManagementExecutor() {
        log.info("Creating Virtual Thread executor for session management operations");
        
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("session-mgmt-", 0)
                .factory();
        
        return task -> {
            Thread virtualThread = virtualThreadFactory.newThread(task);
            virtualThread.start();
        };
    }
}