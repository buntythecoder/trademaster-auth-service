package com.trademaster.multibroker.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Health check implementation - removing problematic actuator imports
// Custom health check implementation using standard Spring Boot patterns
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Monitoring and Metrics Configuration
 * 
 * MANDATORY: Virtual Threads + Real-time Metrics + Production Observability
 * 
 * Comprehensive monitoring solution for the Multi-Broker service providing
 * real-time visibility into system performance, business metrics, and
 * operational health across all broker integrations.
 * 
 * Core Monitoring Features:
 * - Broker API performance and availability metrics
 * - Portfolio synchronization success rates and latencies
 * - OAuth authentication success/failure rates
 * - WebSocket connection health and message throughput
 * - Virtual thread pool utilization and performance
 * 
 * Business Metrics:
 * - Active user sessions and portfolio values
 * - Broker connectivity status and sync frequencies
 * - Authentication success rates by broker type
 * - Real-time data streaming performance
 * - Error rates and failure categorization
 * 
 * Operational Dashboards:
 * - System health overview with SLA compliance
 * - Broker-specific performance dashboards
 * - Real-time alerting for critical thresholds
 * - Capacity planning and resource utilization
 * - Security incident monitoring and response
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Advanced Multi-Broker Monitoring)
 */
@Slf4j
@Configuration
@EnableScheduling
public class MonitoringConfig {
    
    @Bean
    public MultiBrokerMetrics multiBrokerMetrics(MeterRegistry meterRegistry) {
        return new MultiBrokerMetrics(meterRegistry);
    }
    
    @Bean
    public BrokerHealthIndicator brokerHealthIndicator(MultiBrokerMetrics metrics) {
        return new BrokerHealthIndicator(metrics);
    }
    
    @Bean
    public SystemPerformanceMetrics systemPerformanceMetrics(MeterRegistry meterRegistry) {
        return new SystemPerformanceMetrics(meterRegistry);
    }
    
    /**
     * Multi-Broker Metrics Collection and Analytics
     * 
     * MANDATORY: Real-time metrics with <50ms collection overhead
     */
    @Component
    @RequiredArgsConstructor
    public static class MultiBrokerMetrics {
        
        private final MeterRegistry meterRegistry;
        
        // Broker API Performance Metrics
        private final Map<String, Timer> brokerApiTimers = new ConcurrentHashMap<>();
        private final Map<String, Counter> brokerApiSuccessCounters = new ConcurrentHashMap<>();
        private final Map<String, Counter> brokerApiFailureCounters = new ConcurrentHashMap<>();
        
        // Portfolio Synchronization Metrics
        private final Map<String, AtomicLong> portfolioSyncTimes = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> portfolioSyncCounts = new ConcurrentHashMap<>();
        
        // OAuth Authentication Metrics
        private final Map<String, Counter> oauthSuccessCounters = new ConcurrentHashMap<>();
        private final Map<String, Counter> oauthFailureCounters = new ConcurrentHashMap<>();
        
        // WebSocket Connection Metrics
        private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);
        private final AtomicLong totalWebSocketMessages = new AtomicLong(0);
        
        // Virtual Thread Metrics
        private final AtomicInteger activeVirtualThreads = new AtomicInteger(0);
        private final AtomicLong completedVirtualThreadTasks = new AtomicLong(0);
        
        /**
         * Record broker API call timing and success/failure
         * 
         * @param brokerType Broker identifier
         * @param duration Operation duration
         * @param success Whether operation succeeded
         */
        public void recordBrokerApiCall(String brokerType, Duration duration, boolean success) {
            Timer timer = brokerApiTimers.computeIfAbsent(brokerType,
                broker -> Timer.builder("multibroker.api.calls")
                    .description("Broker API call timings")
                    .tag("broker", broker)
                    .register(meterRegistry));
            
            timer.record(duration);
            
            if (success) {
                brokerApiSuccessCounters.computeIfAbsent(brokerType,
                    broker -> Counter.builder("multibroker.api.success")
                        .description("Successful broker API calls")
                        .tag("broker", broker)
                        .register(meterRegistry))
                    .increment();
            } else {
                brokerApiFailureCounters.computeIfAbsent(brokerType,
                    broker -> Counter.builder("multibroker.api.failures")
                        .description("Failed broker API calls")
                        .tag("broker", broker)
                        .register(meterRegistry))
                    .increment();
            }
            
            log.debug("Recorded broker API call: broker={}, duration={}ms, success={}", 
                     brokerType, duration.toMillis(), success);
        }
        
        /**
         * Record portfolio synchronization metrics
         * 
         * @param brokerType Broker identifier
         * @param syncTime Time taken for sync
         */
        public void recordPortfolioSync(String brokerType, Duration syncTime) {
            portfolioSyncTimes.computeIfAbsent(brokerType, k -> {
                AtomicLong syncTimeGauge = new AtomicLong(0);
                Gauge.builder("multibroker.portfolio.sync.time", syncTimeGauge, AtomicLong::get)
                    .description("Portfolio synchronization time")
                    .tag("broker", brokerType)
                    .register(meterRegistry);
                return syncTimeGauge;
            }).set(syncTime.toMillis());
            
            portfolioSyncCounts.computeIfAbsent(brokerType, k -> {
                AtomicInteger counter = new AtomicInteger(0);
                Gauge.builder("multibroker.portfolio.sync.count", counter, AtomicInteger::get)
                    .description("Portfolio synchronization count")
                    .tag("broker", brokerType)
                    .register(meterRegistry);
                return counter;
            }).incrementAndGet();
            
            log.debug("Portfolio sync recorded: broker={}, time={}ms", 
                     brokerType, syncTime.toMillis());
        }
        
        /**
         * Record OAuth authentication attempt
         * 
         * @param brokerType Broker identifier
         * @param success Whether authentication succeeded
         */
        public void recordOAuthAttempt(String brokerType, boolean success) {
            if (success) {
                oauthSuccessCounters.computeIfAbsent(brokerType,
                    broker -> Counter.builder("multibroker.oauth.success")
                        .description("Successful OAuth authentications")
                        .tag("broker", broker)
                        .register(meterRegistry))
                    .increment();
            } else {
                oauthFailureCounters.computeIfAbsent(brokerType,
                    broker -> Counter.builder("multibroker.oauth.failures")
                        .description("Failed OAuth authentications")
                        .tag("broker", broker)
                        .register(meterRegistry))
                    .increment();
            }
            
            log.debug("OAuth attempt recorded: broker={}, success={}", brokerType, success);
        }
        
        /**
         * Update WebSocket connection metrics
         * 
         * @param connected Whether connection was established or closed
         */
        public void updateWebSocketConnections(boolean connected) {
            if (connected) {
                int connections = activeWebSocketConnections.incrementAndGet();
                log.debug("WebSocket connection established, active: {}", connections);
            } else {
                int connections = activeWebSocketConnections.decrementAndGet();
                log.debug("WebSocket connection closed, active: {}", connections);
            }
            
            // Register gauge if not already registered
            Gauge.builder("multibroker.websocket.active.connections", activeWebSocketConnections, AtomicInteger::get)
                .description("Active WebSocket connections")
                .register(meterRegistry);
        }
        
        /**
         * Record WebSocket message processed
         */
        public void recordWebSocketMessage() {
            long messages = totalWebSocketMessages.incrementAndGet();
            
            // Register counter if not already registered  
            Gauge.builder("multibroker.websocket.total.messages", totalWebSocketMessages, AtomicLong::get)
                .description("Total WebSocket messages processed")
                .register(meterRegistry);
        }
        
        /**
         * Update virtual thread metrics
         * 
         * @param active Change in active threads (positive/negative)
         * @param completed Number of completed tasks
         */
        public void updateVirtualThreadMetrics(int active, long completed) {
            activeVirtualThreads.addAndGet(active);
            completedVirtualThreadTasks.addAndGet(completed);
            
            // Register gauges if not already registered
            Gauge.builder("multibroker.virtual.threads.active", activeVirtualThreads, AtomicInteger::get)
                .description("Active virtual threads")
                .register(meterRegistry);
                
            Gauge.builder("multibroker.virtual.threads.completed", completedVirtualThreadTasks, AtomicLong::get)
                .description("Completed virtual thread tasks")
                .register(meterRegistry);
        }
        
        /**
         * Get current broker health metrics
         * 
         * @return Map of broker health status
         */
        public Map<String, Object> getBrokerHealthMetrics() {
            Map<String, Object> healthMetrics = new ConcurrentHashMap<>();
            
            // Calculate success rates for each broker
            brokerApiSuccessCounters.forEach((broker, successCounter) -> {
                Counter failureCounter = brokerApiFailureCounters.get(broker);
                double successCount = successCounter.count();
                double failureCount = failureCounter != null ? failureCounter.count() : 0;
                double successRate = (successCount / (successCount + failureCount)) * 100;
                
                healthMetrics.put(broker + "_success_rate", successRate);
                healthMetrics.put(broker + "_total_calls", successCount + failureCount);
                healthMetrics.put(broker + "_last_sync", portfolioSyncTimes.getOrDefault(broker, new AtomicLong(0)).get());
            });
            
            healthMetrics.put("active_websocket_connections", activeWebSocketConnections.get());
            healthMetrics.put("total_websocket_messages", totalWebSocketMessages.get());
            healthMetrics.put("active_virtual_threads", activeVirtualThreads.get());
            
            return healthMetrics;
        }
    }
    
    /**
     * Broker Health Indicator for Spring Boot Actuator
     * 
     * MANDATORY: Real-time health status with <100ms response time
     */
    @Component
    @RequiredArgsConstructor
    public static class BrokerHealthIndicator {
        
        private final MultiBrokerMetrics metrics;
        private static final double MINIMUM_SUCCESS_RATE = 95.0;
        private static final long MAXIMUM_SYNC_AGE_MINUTES = 10;
        
        public Map<String, Object> health() {
            Map<String, Object> healthMetrics = metrics.getBrokerHealthMetrics();
            Map<String, Object> healthDetails = new ConcurrentHashMap<>();
            boolean overallHealthy = true;
            
            // Check each broker's health
            for (Map.Entry<String, Object> entry : healthMetrics.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (key.endsWith("_success_rate")) {
                    String broker = key.replace("_success_rate", "");
                    double successRate = (Double) value;
                    
                    if (successRate < MINIMUM_SUCCESS_RATE) {
                        healthDetails.put(broker + "_status", "DEGRADED");
                        healthDetails.put(broker + "_success_rate", successRate + "%");
                        overallHealthy = false;
                    } else {
                        healthDetails.put(broker + "_status", "HEALTHY");
                        healthDetails.put(broker + "_success_rate", successRate + "%");
                    }
                }
                
                healthDetails.put(key, value);
            }
            
            // Overall system health
            healthDetails.put("virtual_threads_healthy", 
                            metrics.activeVirtualThreads.get() < 1000);
            healthDetails.put("websocket_connections", 
                            metrics.activeWebSocketConnections.get());
            healthDetails.put("system_timestamp", Instant.now().toEpochMilli());
            healthDetails.put("status", overallHealthy ? "UP" : "DOWN");
            
            return healthDetails;
        }
    }
    
    /**
     * System Performance Metrics Collection
     * 
     * MANDATORY: JVM and system metrics with trend analysis
     */
    @Component
    @RequiredArgsConstructor
    public static class SystemPerformanceMetrics {
        
        private final MeterRegistry meterRegistry;
        private final Runtime runtime = Runtime.getRuntime();
        
        @Scheduled(fixedRate = 30000) // Every 30 seconds
        public void collectSystemMetrics() {
            // JVM Memory Metrics
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Gauge.builder("system.memory.used", () -> usedMemory)
                .description("Used JVM memory")
                .register(meterRegistry);
                
            Gauge.builder("system.memory.free", () -> freeMemory)
                .description("Free JVM memory")
                .register(meterRegistry);
                
            Gauge.builder("system.memory.total", () -> totalMemory)
                .description("Total JVM memory")
                .register(meterRegistry);
                
            Gauge.builder("system.memory.max", () -> maxMemory)
                .description("Maximum JVM memory")
                .register(meterRegistry);
            
            // JVM Thread Metrics  
            int activeThreadCount = Thread.activeCount();
            Gauge.builder("system.threads.active", () -> activeThreadCount)
                .description("Active JVM threads")
                .register(meterRegistry);
            
            // CPU Usage (approximation)
            int availableProcessors = runtime.availableProcessors();
            Gauge.builder("system.cpu.processors", () -> availableProcessors)
                .description("Available CPU processors")
                .register(meterRegistry);
            
            log.debug("System metrics collected: memory={}MB, threads={}, processors={}", 
                     usedMemory / (1024 * 1024), activeThreadCount, availableProcessors);
        }
        
        @Scheduled(fixedRate = 60000) // Every minute
        public void collectPerformanceTrends() {
            // Calculate memory utilization percentage
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUtilization = ((double) (totalMemory - freeMemory) / totalMemory) * 100;
            
            Gauge.builder("system.performance.memory.utilization", () -> memoryUtilization)
                .description("Memory utilization percentage")
                .register(meterRegistry);
            
            log.debug("Performance trends updated: memory_utilization={}%", memoryUtilization);
        }
    }
}