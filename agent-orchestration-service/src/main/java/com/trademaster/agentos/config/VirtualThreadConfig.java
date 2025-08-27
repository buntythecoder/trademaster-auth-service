package com.trademaster.agentos.config;

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * MANDATORY: Virtual Threads Configuration for Java 24
 * 
 * This configuration enables Virtual Threads across the entire application:
 * - Web requests handled by Virtual Threads
 * - Async methods executed on Virtual Threads  
 * - Database connections managed efficiently
 * - HTTP clients benefit from Virtual Thread concurrency
 * 
 * Performance Benefits:
 * - Handle millions of concurrent requests
 * - No thread pool tuning required
 * - Simplified blocking I/O programming model
 * - Better resource utilization
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * MANDATORY: Configure Tomcat to use Virtual Threads for web requests
     * This allows the application to handle unlimited concurrent HTTP requests
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * MANDATORY: Configure async task executor to use Virtual Threads
     * All @Async annotated methods will run on Virtual Threads
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}