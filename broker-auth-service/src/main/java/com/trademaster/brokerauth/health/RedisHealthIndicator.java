package com.trademaster.brokerauth.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis Health Indicator
 * 
 * Monitors the health and performance of the Redis cache.
 * Includes memory usage, connection status, and rate limiting data.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {
    
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public Health health() {
        try {
            LocalDateTime start = LocalDateTime.now();
            
            // Test basic connectivity with ping
            String pingResult = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            Duration pingDuration = Duration.between(start, LocalDateTime.now());
            
            // Get Redis info
            Properties info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info();
            
            Map<String, Object> redisStats = parseRedisInfo(info);
            
            boolean isHealthy = "PONG".equals(pingResult) && pingDuration.toMillis() < 500;
            
            Health.Builder healthBuilder = isHealthy ? Health.up() : Health.down();
            
            return healthBuilder
                    .withDetail("cache", "Redis")
                    .withDetail("status", isHealthy ? "UP" : "DOWN")
                    .withDetail("ping_result", pingResult)
                    .withDetail("response_time_ms", pingDuration.toMillis())
                    .withDetail("stats", redisStats)
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
        }
    }
    
    private Map<String, Object> parseRedisInfo(Properties info) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Memory information
            String usedMemory = info.getProperty("used_memory_human");
            String maxMemory = info.getProperty("maxmemory_human");
            String memoryUsagePercent = info.getProperty("used_memory_rss_human");
            
            if (usedMemory != null) stats.put("used_memory", usedMemory);
            if (maxMemory != null) stats.put("max_memory", maxMemory);
            if (memoryUsagePercent != null) stats.put("memory_usage_rss", memoryUsagePercent);
            
            // Connection information
            String connectedClients = info.getProperty("connected_clients");
            String blockedClients = info.getProperty("blocked_clients");
            
            if (connectedClients != null) stats.put("connected_clients", connectedClients);
            if (blockedClients != null) stats.put("blocked_clients", blockedClients);
            
            // Server information
            String redisVersion = info.getProperty("redis_version");
            String uptimeInSeconds = info.getProperty("uptime_in_seconds");
            
            if (redisVersion != null) stats.put("redis_version", redisVersion);
            if (uptimeInSeconds != null) {
                try {
                    long uptime = Long.parseLong(uptimeInSeconds);
                    stats.put("uptime_hours", uptime / 3600);
                } catch (NumberFormatException e) {
                    stats.put("uptime_seconds", uptimeInSeconds);
                }
            }
            
            // Performance statistics
            String totalCommandsProcessed = info.getProperty("total_commands_processed");
            String instantaneousOpsPerSec = info.getProperty("instantaneous_ops_per_sec");
            
            if (totalCommandsProcessed != null) stats.put("total_commands_processed", totalCommandsProcessed);
            if (instantaneousOpsPerSec != null) stats.put("ops_per_second", instantaneousOpsPerSec);
            
            // Keyspace information
            String db0Info = info.getProperty("db0");
            if (db0Info != null) {
                // Parse db0 info like "keys=1000,expires=100,avg_ttl=3600000"
                Map<String, String> keyspaceInfo = parseKeyspaceInfo(db0Info);
                stats.put("keyspace_db0", keyspaceInfo);
            }
            
            // Rate limiting keys count (approximate)
            try {
                // Count rate limiting keys
                Long rateLimitKeysCount = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .dbSize();
                stats.put("total_keys", rateLimitKeysCount);
            } catch (Exception e) {
                log.debug("Could not get key count: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse Redis info: {}", e.getMessage());
            stats.put("parse_error", e.getMessage());
        }
        
        return stats;
    }
    
    private Map<String, String> parseKeyspaceInfo(String keyspaceInfo) {
        Map<String, String> parsed = new HashMap<>();
        
        try {
            String[] parts = keyspaceInfo.split(",");
            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    parsed.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse keyspace info: {}", keyspaceInfo);
        }
        
        return parsed;
    }
}