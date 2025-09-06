package com.trademaster.multibroker.controller;

import com.trademaster.multibroker.config.MonitoringConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Removed problematic actuator health import
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced Monitoring Dashboard Controller
 * 
 * MANDATORY: Real-time Metrics API + Production Observability
 * 
 * Provides comprehensive REST API endpoints for monitoring dashboards,
 * real-time system metrics, and operational observability across all
 * broker integrations and system components.
 * 
 * Dashboard Features:
 * - Real-time broker performance and availability metrics
 * - System health overview with SLA compliance tracking
 * - Portfolio synchronization success rates and latencies
 * - WebSocket connection health and message throughput
 * - Virtual thread pool utilization and performance analytics
 * 
 * API Endpoints:
 * - /api/v1/monitoring/dashboard/overview - System overview
 * - /api/v1/monitoring/dashboard/brokers - Broker-specific metrics
 * - /api/v1/monitoring/dashboard/performance - Performance analytics
 * - /api/v1/monitoring/dashboard/health - Health status summary
 * - /api/v1/monitoring/dashboard/websockets - WebSocket metrics
 * 
 * Response Time Requirements:
 * - All endpoints must respond within 200ms
 * - Real-time data with <5 second latency
 * - Cached responses for performance optimization
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Advanced Multi-Broker Monitoring)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Configure properly for production
public class MonitoringDashboardController {
    
    private final MonitoringConfig.MultiBrokerMetrics multiBrokerMetrics;
    private final MonitoringConfig.BrokerHealthIndicator brokerHealthIndicator;
    private final MeterRegistry meterRegistry;
    
    /**
     * Get system overview dashboard
     * 
     * MANDATORY: <200ms response time with comprehensive metrics
     * 
     * @return System overview with key performance indicators
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // System health summary
            Map<String, Object> health = brokerHealthIndicator.health();
            overview.put("system_status", health.get("status"));
            overview.put("timestamp", Instant.now().toEpochMilli());
            
            // Broker health metrics
            Map<String, Object> brokerMetrics = multiBrokerMetrics.getBrokerHealthMetrics();
            overview.put("total_brokers", countUniqueBrokers(brokerMetrics));
            overview.put("healthy_brokers", countHealthyBrokers(brokerMetrics));
            
            // WebSocket connection metrics
            overview.put("active_websocket_connections", 
                        brokerMetrics.get("active_websocket_connections"));
            overview.put("total_websocket_messages", 
                        brokerMetrics.get("total_websocket_messages"));
            
            // Virtual thread metrics
            overview.put("active_virtual_threads", 
                        brokerMetrics.get("active_virtual_threads"));
            
            // System performance indicators
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUtilization = ((double) (totalMemory - freeMemory) / totalMemory) * 100;
            
            overview.put("memory_utilization_percent", Math.round(memoryUtilization * 100.0) / 100.0);
            overview.put("active_jvm_threads", Thread.activeCount());
            overview.put("available_processors", runtime.availableProcessors());
            
            log.debug("System overview dashboard generated successfully");
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            log.error("Failed to generate system overview dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve system overview", 
                               "timestamp", Instant.now().toEpochMilli()));
        }
    }
    
    /**
     * Get broker-specific performance metrics
     * 
     * @return Detailed metrics for each broker integration
     */
    @GetMapping("/brokers")
    public ResponseEntity<Map<String, Object>> getBrokerMetrics() {
        try {
            Map<String, Object> brokerDashboard = new HashMap<>();
            Map<String, Object> healthMetrics = multiBrokerMetrics.getBrokerHealthMetrics();
            
            // Extract broker-specific data
            Map<String, Map<String, Object>> brokerData = new HashMap<>();
            
            healthMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().contains("_"))
                .forEach(entry -> {
                    String[] parts = entry.getKey().split("_");
                    if (parts.length >= 2) {
                        String brokerName = parts[0];
                        String metricType = parts[1] + "_" + parts[2];
                        
                        brokerData.computeIfAbsent(brokerName, k -> new HashMap<>())
                                 .put(metricType, entry.getValue());
                    }
                });
            
            brokerDashboard.put("brokers", brokerData);
            brokerDashboard.put("timestamp", Instant.now().toEpochMilli());
            brokerDashboard.put("total_brokers", brokerData.size());
            
            // Calculate overall broker health score
            double overallHealthScore = calculateOverallHealthScore(brokerData);
            brokerDashboard.put("overall_health_score", Math.round(overallHealthScore * 100.0) / 100.0);
            
            log.debug("Broker metrics dashboard generated: {} brokers, health_score={}", 
                     brokerData.size(), overallHealthScore);
            return ResponseEntity.ok(brokerDashboard);
            
        } catch (Exception e) {
            log.error("Failed to generate broker metrics dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve broker metrics", 
                               "timestamp", Instant.now().toEpochMilli()));
        }
    }
    
    /**
     * Get system performance analytics
     * 
     * @return Performance metrics and trends
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> performance = new HashMap<>();
            
            // JVM Performance Metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("total_memory_mb", runtime.totalMemory() / (1024 * 1024));
            jvmMetrics.put("free_memory_mb", runtime.freeMemory() / (1024 * 1024));
            jvmMetrics.put("used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            jvmMetrics.put("max_memory_mb", runtime.maxMemory() / (1024 * 1024));
            
            double memoryUtilization = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100;
            jvmMetrics.put("memory_utilization_percent", Math.round(memoryUtilization * 100.0) / 100.0);
            
            performance.put("jvm_metrics", jvmMetrics);
            
            // Thread Metrics
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("active_jvm_threads", Thread.activeCount());
            
            Map<String, Object> healthMetrics = multiBrokerMetrics.getBrokerHealthMetrics();
            threadMetrics.put("active_virtual_threads", healthMetrics.get("active_virtual_threads"));
            threadMetrics.put("completed_virtual_tasks", healthMetrics.get("completed_virtual_tasks"));
            
            performance.put("thread_metrics", threadMetrics);
            
            // System Metrics
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("available_processors", runtime.availableProcessors());
            systemMetrics.put("timestamp", Instant.now().toEpochMilli());
            
            performance.put("system_metrics", systemMetrics);
            
            // Performance Health Score
            double performanceScore = calculatePerformanceScore(memoryUtilization, Thread.activeCount());
            performance.put("performance_score", Math.round(performanceScore * 100.0) / 100.0);
            
            log.debug("Performance metrics dashboard generated: memory_util={}%, threads={}, score={}", 
                     memoryUtilization, Thread.activeCount(), performanceScore);
            return ResponseEntity.ok(performance);
            
        } catch (Exception e) {
            log.error("Failed to generate performance metrics dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve performance metrics", 
                               "timestamp", Instant.now().toEpochMilli()));
        }
    }
    
    /**
     * Get comprehensive health status
     * 
     * @return Health status for all system components
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> healthStatus = new HashMap<>();
            
            // Overall system health
            Map<String, Object> systemHealth = brokerHealthIndicator.health();
            healthStatus.put("system_status", systemHealth.get("status"));
            healthStatus.put("system_details", systemHealth);
            
            // Component health breakdown
            Map<String, String> componentHealth = new HashMap<>();
            Map<String, Object> healthMetrics = multiBrokerMetrics.getBrokerHealthMetrics();
            
            // Evaluate each component
            componentHealth.put("websockets", 
                (Integer) healthMetrics.get("active_websocket_connections") >= 0 ? "UP" : "DOWN");
            componentHealth.put("virtual_threads", 
                (Integer) healthMetrics.get("active_virtual_threads") < 1000 ? "UP" : "DEGRADED");
            
            // Memory health check
            Runtime runtime = Runtime.getRuntime();
            double memoryUtilization = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100;
            componentHealth.put("memory", memoryUtilization < 80 ? "UP" : (memoryUtilization < 95 ? "DEGRADED" : "CRITICAL"));
            
            healthStatus.put("components", componentHealth);
            healthStatus.put("timestamp", Instant.now().toEpochMilli());
            
            // Health summary
            long healthyComponents = componentHealth.values().stream().mapToLong(status -> "UP".equals(status) ? 1 : 0).sum();
            healthStatus.put("healthy_components", healthyComponents);
            healthStatus.put("total_components", componentHealth.size());
            healthStatus.put("health_percentage", Math.round((double) healthyComponents / componentHealth.size() * 100.0));
            
            log.debug("Health status dashboard generated: {}/{} components healthy", 
                     healthyComponents, componentHealth.size());
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Failed to generate health status dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve health status", 
                               "timestamp", Instant.now().toEpochMilli()));
        }
    }
    
    /**
     * Get WebSocket connection metrics
     * 
     * @return WebSocket performance and connection data
     */
    @GetMapping("/websockets")
    public ResponseEntity<Map<String, Object>> getWebSocketMetrics() {
        try {
            Map<String, Object> websocketMetrics = new HashMap<>();
            Map<String, Object> healthMetrics = multiBrokerMetrics.getBrokerHealthMetrics();
            
            websocketMetrics.put("active_connections", healthMetrics.get("active_websocket_connections"));
            websocketMetrics.put("total_messages_processed", healthMetrics.get("total_websocket_messages"));
            websocketMetrics.put("timestamp", Instant.now().toEpochMilli());
            
            // Calculate message throughput (approximate)
            long totalMessages = (Long) healthMetrics.get("total_websocket_messages");
            int activeConnections = (Integer) healthMetrics.get("active_websocket_connections");
            
            double avgMessagesPerConnection = activeConnections > 0 ? (double) totalMessages / activeConnections : 0;
            websocketMetrics.put("avg_messages_per_connection", Math.round(avgMessagesPerConnection * 100.0) / 100.0);
            
            // Connection health score
            String connectionHealth = activeConnections > 0 ? "ACTIVE" : "IDLE";
            websocketMetrics.put("connection_status", connectionHealth);
            
            log.debug("WebSocket metrics dashboard generated: {} active connections, {} total messages", 
                     activeConnections, totalMessages);
            return ResponseEntity.ok(websocketMetrics);
            
        } catch (Exception e) {
            log.error("Failed to generate WebSocket metrics dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve WebSocket metrics", 
                               "timestamp", Instant.now().toEpochMilli()));
        }
    }
    
    // Helper methods
    private int countUniqueBrokers(Map<String, Object> metrics) {
        return (int) metrics.keySet().stream()
            .map(key -> key.split("_")[0])
            .distinct()
            .count();
    }
    
    private int countHealthyBrokers(Map<String, Object> metrics) {
        return (int) metrics.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith("_success_rate"))
            .mapToDouble(entry -> (Double) entry.getValue())
            .filter(rate -> rate >= 95.0)
            .count();
    }
    
    private double calculateOverallHealthScore(Map<String, Map<String, Object>> brokerData) {
        if (brokerData.isEmpty()) return 0.0;
        
        double totalScore = brokerData.values().stream()
            .mapToDouble(brokerMetrics -> {
                Object successRate = brokerMetrics.get("success_rate");
                return successRate instanceof Double ? (Double) successRate : 0.0;
            })
            .average()
            .orElse(0.0);
            
        return totalScore;
    }
    
    private double calculatePerformanceScore(double memoryUtilization, int activeThreads) {
        double memoryScore = Math.max(0, 100 - memoryUtilization);
        double threadScore = Math.max(0, 100 - Math.min(activeThreads / 10.0, 100));
        return (memoryScore + threadScore) / 2.0;
    }
}