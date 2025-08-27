package com.trademaster.subscription.config;

import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.actuator.info.InfoContributor;
import org.springframework.boot.actuator.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

/**
 * Health Check Configuration
 * 
 * Configures comprehensive health checks for all service dependencies.
 * Provides detailed health information for monitoring and alerting.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class HealthConfig {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.version:1.0.0}")
    private String applicationVersion;

    /**
     * Database health indicator
     */
    @Bean("databaseHealthIndicator")
    public HealthIndicator databaseHealthIndicator() {
        return () -> {
            try {
                // Test database connection
                try (Connection connection = dataSource.getConnection()) {
                    if (connection.isValid(5)) {
                        // Test repository functionality
                        long subscriptionCount = subscriptionRepository.count();
                        
                        return Health.up()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("connection", "valid")
                                .withDetail("subscriptionCount", subscriptionCount)
                                .withDetail("responseTimeMs", getConnectionResponseTime())
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("connection", "invalid")
                                .build();
                    }
                }
            } catch (Exception e) {
                log.error("Database health check failed", e);
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Redis health indicator
     */
    @Bean("redisHealthIndicator")
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                // Test Redis connection
                redisConnectionFactory.getConnection().ping();
                
                return Health.up()
                        .withDetail("cache", "Redis")
                        .withDetail("connection", "valid")
                        .withDetail("host", redisConnectionFactory.getConnection().getServerCommands().info("server").getProperty("redis_version"))
                        .build();
                        
            } catch (Exception e) {
                log.error("Redis health check failed", e);
                return Health.down()
                        .withDetail("cache", "Redis")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Kafka health indicator
     */
    @Bean("kafkaHealthIndicator") 
    public HealthIndicator kafkaHealthIndicator() {
        return () -> {
            try {
                // Test Kafka connectivity by getting cluster metadata
                var metadata = kafkaTemplate.getProducerFactory()
                        .createProducer()
                        .partitionsFor("health-check-topic");
                
                return Health.up()
                        .withDetail("messaging", "Apache Kafka")
                        .withDetail("connection", "valid")
                        .withDetail("bootstrapServers", getKafkaBootstrapServers())
                        .build();
                        
            } catch (Exception e) {
                log.warn("Kafka health check failed", e);
                return Health.down()
                        .withDetail("messaging", "Apache Kafka")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Business logic health indicator
     */
    @Bean("subscriptionServiceHealthIndicator")
    public HealthIndicator subscriptionServiceHealthIndicator() {
        return () -> {
            try {
                // Test core service functionality
                boolean isHealthy = subscriptionService != null;
                
                // Additional business logic checks
                long activeSubscriptions = subscriptionRepository.countByStatus(
                        com.trademaster.subscription.enums.SubscriptionStatus.ACTIVE);
                long totalSubscriptions = subscriptionRepository.count();
                
                return Health.up()
                        .withDetail("service", "subscription-service")
                        .withDetail("status", "operational")
                        .withDetail("activeSubscriptions", activeSubscriptions)
                        .withDetail("totalSubscriptions", totalSubscriptions)
                        .withDetail("virtualThreads", System.getProperty("spring.threads.virtual.enabled"))
                        .build();
                        
            } catch (Exception e) {
                log.error("Subscription service health check failed", e);
                return Health.down()
                        .withDetail("service", "subscription-service")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * External services health indicator
     */
    @Bean("externalServicesHealthIndicator")
    public HealthIndicator externalServicesHealthIndicator() {
        return () -> {
            try {
                // Check critical external services
                var healthBuilder = Health.up();
                
                // Payment Gateway Health (simplified check)
                boolean paymentGatewayHealthy = checkPaymentGatewayHealth();
                healthBuilder.withDetail("paymentGateway", paymentGatewayHealthy ? "UP" : "DOWN");
                
                // Auth Service Health
                boolean authServiceHealthy = checkAuthServiceHealth();
                healthBuilder.withDetail("authService", authServiceHealthy ? "UP" : "DOWN");
                
                if (!paymentGatewayHealthy || !authServiceHealthy) {
                    return healthBuilder
                            .status("DEGRADED")
                            .withDetail("message", "Some external services are down")
                            .build();
                }
                
                return healthBuilder
                        .withDetail("allExternalServices", "UP")
                        .build();
                        
            } catch (Exception e) {
                log.error("External services health check failed", e);
                return Health.down()
                        .withDetail("externalServices", "ERROR")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Application info contributor
     */
    @Bean
    public InfoContributor applicationInfoContributor() {
        return (Info.Builder builder) -> {
            builder.withDetail("app", Map.of(
                    "name", applicationName,
                    "version", applicationVersion,
                    "description", "TradeMaster Subscription Management Service",
                    "java.version", System.getProperty("java.version"),
                    "java.vendor", System.getProperty("java.vendor"),
                    "spring.profiles.active", System.getProperty("spring.profiles.active", "default"),
                    "virtual.threads.enabled", System.getProperty("spring.threads.virtual.enabled", "false")
            ));
            
            builder.withDetail("build", Map.of(
                    "timestamp", Instant.now().toString(),
                    "gradle.version", "8.5",
                    "spring.boot.version", "3.5.3"
            ));
            
            builder.withDetail("system", Map.of(
                    "os.name", System.getProperty("os.name"),
                    "os.version", System.getProperty("os.version"),
                    "java.runtime.name", System.getProperty("java.runtime.name"),
                    "available.processors", Runtime.getRuntime().availableProcessors(),
                    "max.memory", formatBytes(Runtime.getRuntime().maxMemory()),
                    "free.memory", formatBytes(Runtime.getRuntime().freeMemory())
            ));
        };
    }

    /**
     * Metrics info contributor
     */
    @Bean
    public InfoContributor metricsInfoContributor() {
        return (Info.Builder builder) -> {
            try {
                builder.withDetail("metrics", Map.of(
                        "registry", "Prometheus",
                        "totalMeters", meterRegistry.getMeters().size(),
                        "counters", meterRegistry.getMeters().stream()
                                .filter(meter -> meter instanceof io.micrometer.core.instrument.Counter)
                                .count(),
                        "timers", meterRegistry.getMeters().stream()
                                .filter(meter -> meter instanceof io.micrometer.core.instrument.Timer)
                                .count(),
                        "gauges", meterRegistry.getMeters().stream()
                                .filter(meter -> meter instanceof io.micrometer.core.instrument.Gauge)
                                .count()
                ));
            } catch (Exception e) {
                log.warn("Failed to gather metrics info", e);
                builder.withDetail("metrics", Map.of("status", "error", "message", e.getMessage()));
            }
        };
    }

    // Helper methods
    private long getConnectionResponseTime() {
        try {
            long start = System.currentTimeMillis();
            subscriptionRepository.count();
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    private String getKafkaBootstrapServers() {
        try {
            return kafkaTemplate.getProducerFactory()
                    .getConfigurationProperties()
                    .get("bootstrap.servers")
                    .toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private boolean checkPaymentGatewayHealth() {
        // Simplified health check - in production, make actual HTTP call
        try {
            // This would be an actual HTTP health check
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkAuthServiceHealth() {
        // Simplified health check - in production, make actual HTTP call
        try {
            // This would be an actual HTTP health check
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}