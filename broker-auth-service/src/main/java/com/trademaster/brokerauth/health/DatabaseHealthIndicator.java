package com.trademaster.brokerauth.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Database Health Indicator
 * 
 * Monitors the health and performance of the PostgreSQL database.
 * Includes connection pool status and query performance metrics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public Health health() {
        try {
            LocalDateTime start = LocalDateTime.now();
            
            // Test basic connectivity
            String result = jdbcTemplate.queryForObject("SELECT 'OK'", String.class);
            Duration queryDuration = Duration.between(start, LocalDateTime.now());
            
            // Get database statistics
            Map<String, Object> stats = getDatabaseStats();
            
            boolean isHealthy = "OK".equals(result) && queryDuration.toMillis() < 1000;
            
            Health.Builder healthBuilder = isHealthy ? Health.up() : Health.down();
            
            return healthBuilder
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", isHealthy ? "UP" : "DOWN")
                    .withDetail("response_time_ms", queryDuration.toMillis())
                    .withDetail("stats", stats)
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
        }
    }
    
    private Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get active connections count
            String activeConnectionsQuery = """
                SELECT count(*) 
                FROM pg_stat_activity 
                WHERE state = 'active'
            """;
            
            Integer activeConnections = jdbcTemplate.queryForObject(activeConnectionsQuery, Integer.class);
            stats.put("active_connections", activeConnections);
            
            // Get table row counts
            String brokerAccountsCountQuery = "SELECT COUNT(*) FROM broker_accounts";
            String brokerSessionsCountQuery = "SELECT COUNT(*) FROM broker_sessions";
            
            Integer accountsCount = jdbcTemplate.queryForObject(brokerAccountsCountQuery, Integer.class);
            Integer sessionsCount = jdbcTemplate.queryForObject(brokerSessionsCountQuery, Integer.class);
            
            stats.put("broker_accounts_count", accountsCount);
            stats.put("broker_sessions_count", sessionsCount);
            
            // Get database version
            String versionQuery = "SELECT version()";
            String version = jdbcTemplate.queryForObject(versionQuery, String.class);
            String[] versionParts = version.split(" ");
            stats.put("postgresql_version", versionParts.length > 1 ? versionParts[1] : "Unknown");
            
            // Check for locked queries
            String lockedQueriesQuery = """
                SELECT count(*) 
                FROM pg_stat_activity 
                WHERE waiting = true OR state = 'active' AND query_start < now() - interval '30 seconds'
            """;
            
            try {
                Integer lockedQueries = jdbcTemplate.queryForObject(lockedQueriesQuery, Integer.class);
                stats.put("slow_or_locked_queries", lockedQueries);
            } catch (Exception e) {
                // Older PostgreSQL versions might not have all columns
                stats.put("slow_or_locked_queries", "N/A");
            }
            
        } catch (Exception e) {
            log.warn("Failed to collect database statistics: {}", e.getMessage());
            stats.put("stats_error", e.getMessage());
        }
        
        return stats;
    }
}