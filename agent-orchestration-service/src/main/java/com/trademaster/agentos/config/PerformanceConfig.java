package com.trademaster.agentos.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * ✅ MANDATORY: Performance Configuration for <200ms Response Guarantees
 * 
 * Rule 22 Compliance: API responses must be <200ms
 * Implements performance monitoring and alerting
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PerformanceConfig {

    private static final Duration MAX_RESPONSE_TIME = Duration.ofMillis(200);
    private static final String PERFORMANCE_ALERT = "performance_alert";
    
    /**
     * ✅ PERFORMANCE FILTER: Monitors all HTTP requests for response time compliance
     */
    @Bean
    public OncePerRequestFilter performanceMonitoringFilter(MeterRegistry meterRegistry) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, 
                    HttpServletResponse response, 
                    FilterChain filterChain) throws ServletException, IOException {
                
                long startTime = System.currentTimeMillis();
                String requestPath = request.getRequestURI();
                
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    recordResponseTime(meterRegistry, requestPath, duration, response.getStatus());
                    
                    // ✅ PERFORMANCE GUARANTEE: Alert if >200ms
                    if (duration > MAX_RESPONSE_TIME.toMillis()) {
                        log.warn("{} - Slow API response detected: {}ms for {} ({})", 
                            PERFORMANCE_ALERT,
                            duration, 
                            requestPath,
                            response.getStatus());
                        
                        // Increment slow response counter
                        meterRegistry.counter("api.slow_responses", 
                            "path", requestPath,
                            "status", String.valueOf(response.getStatus()))
                            .increment();
                    }
                }
            }
            
            /**
             * ✅ METRICS RECORDING: Track response times with detailed labels
             */
            private void recordResponseTime(MeterRegistry registry, String path, long duration, int status) {
                Timer.Sample sample = Timer.start(registry);
                sample.stop(Timer.builder("api.response_time")
                    .description("API response time in milliseconds")
                    .tag("path", path)
                    .tag("status", String.valueOf(status))
                    .tag("performance_tier", getPerformanceTier(duration))
                    .register(registry));
                    
                // Record histogram for percentile analysis
                registry.timer("api.response_histogram", 
                    "path", path,
                    "status", String.valueOf(status))
                    .record(duration, TimeUnit.MILLISECONDS);
            }
            
            /**
             * ✅ PERFORMANCE CLASSIFICATION: Categorize response times
             */
            private String getPerformanceTier(long duration) {
                if (duration < 50) return "excellent";
                if (duration < 100) return "good";
                if (duration < 200) return "acceptable";
                return "slow";
            }
        };
    }
    
    /**
     * ✅ PERFORMANCE METRICS: Custom metrics for monitoring
     */
    @Bean
    public PerformanceMetrics performanceMetrics(MeterRegistry meterRegistry) {
        return new PerformanceMetrics(meterRegistry);
    }
    
    /**
     * ✅ PERFORMANCE METRICS SERVICE
     */
    public static class PerformanceMetrics {
        private final MeterRegistry meterRegistry;
        private final Timer orderProcessingTimer;
        private final Timer agentRegistrationTimer;
        private final Timer taskExecutionTimer;
        
        public PerformanceMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.orderProcessingTimer = Timer.builder("business.order_processing_time")
                .description("Order processing time in milliseconds")
                .register(meterRegistry);
            this.agentRegistrationTimer = Timer.builder("business.agent_registration_time")
                .description("Agent registration time in milliseconds")
                .register(meterRegistry);
            this.taskExecutionTimer = Timer.builder("business.task_execution_time")
                .description("Task execution time in milliseconds")
                .register(meterRegistry);
        }
        
        /**
         * ✅ BUSINESS OPERATION TIMING: Track critical business operations
         */
        public Timer.Sample startOrderProcessing() {
            return Timer.start(meterRegistry);
        }
        
        public void recordOrderProcessing(Timer.Sample sample, boolean success) {
            sample.stop(Timer.builder("business.order_processing")
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        }
        
        public Timer.Sample startAgentRegistration() {
            return Timer.start(meterRegistry);
        }
        
        public void recordAgentRegistration(Timer.Sample sample, String agentType, boolean success) {
            sample.stop(Timer.builder("business.agent_registration")
                .tag("agent_type", agentType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        }
        
        public Timer.Sample startTaskExecution() {
            return Timer.start(meterRegistry);
        }
        
        public void recordTaskExecution(Timer.Sample sample, String taskType, boolean success) {
            sample.stop(Timer.builder("business.task_execution")
                .tag("task_type", taskType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        }
        
        /**
         * ✅ PERFORMANCE ALERTS: Track SLA violations
         */
        public void recordSlaViolation(String operation, long actualTime) {
            meterRegistry.counter("sla.violations",
                "operation", operation,
                "actual_time", String.valueOf(actualTime))
                .increment();
                
            log.error("{} - SLA violation: {} took {}ms (max: 200ms)", 
                PERFORMANCE_ALERT, operation, actualTime);
        }
    }
}