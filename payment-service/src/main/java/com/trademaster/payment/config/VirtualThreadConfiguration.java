package com.trademaster.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * MANDATORY Virtual Thread Configuration
 * 
 * Configures Virtual Threads for unlimited scalability as per TradeMaster standards.
 * Replaces traditional reactive programming with blocking I/O + Virtual Threads.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
public class VirtualThreadConfiguration implements AsyncConfigurer {

    /**
     * MANDATORY: Virtual Thread Executor for @Async methods
     * 
     * Provides unlimited scalability for blocking operations without reactive complexity.
     * Each @Async method runs on its own Virtual Thread.
     */
    @Bean(name = "virtualThreadExecutor")
    public TaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Default async executor using Virtual Threads
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadExecutor();
    }

    /**
     * Virtual Thread executor for payment processing
     */
    @Bean(name = "paymentProcessingExecutor")
    public TaskExecutor paymentProcessingExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Virtual Thread executor for webhook processing
     */
    @Bean(name = "webhookProcessingExecutor")  
    public TaskExecutor webhookProcessingExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Virtual Thread executor for notifications
     */
    @Bean(name = "notificationExecutor")
    public TaskExecutor notificationExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}