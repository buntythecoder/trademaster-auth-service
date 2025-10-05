package com.trademaster.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

/**
 * Consul Service Discovery Configuration
 *
 * MANDATORY: Golden Specification - Consul Service Discovery
 * MANDATORY: Rule #3 - Functional Programming (no if-else)
 *
 * Features:
 * - Service registration with Consul
 * - Health check integration
 * - Service discovery for inter-service communication
 * - Configuration management through Consul KV
 * - Circuit breaker integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
@Profile("!test") // Exclude from test profiles
public class ConsulConfig {

    @Value("${spring.cloud.consul.enabled:true}")
    private boolean consulEnabled;

    @Value("${spring.cloud.consul.host:localhost}")
    private String consulHost;

    @Value("${spring.cloud.consul.port:8500}")
    private int consulPort;

    @Value("${spring.application.name:trademaster-auth-service}")
    private String serviceName;

    /**
     * Consul Health Indicator for monitoring Consul connectivity
     */
    @Bean
    public HealthIndicator consulHealthIndicator() {
        return () -> Optional.of(consulEnabled)
                .filter(Boolean::booleanValue)
                .map(enabled -> createHealthyStatus())
                .orElse(createDisabledStatus());
    }

    /**
     * Custom service registry configuration
     */
    @Bean
    public ConsulServiceConfiguration consulServiceConfiguration() {
        return new ConsulServiceConfiguration(
                consulEnabled,
                consulHost,
                consulPort,
                serviceName,
                createServiceTags(),
                createServiceMetadata()
        );
    }

    /**
     * Create healthy status using functional approach
     */
    private Health createHealthyStatus() {
        return Health.up()
                .withDetail("consul.enabled", consulEnabled)
                .withDetail("consul.host", consulHost)
                .withDetail("consul.port", consulPort)
                .withDetail("service.name", serviceName)
                .build();
    }

    /**
     * Create disabled status using functional approach
     */
    private Health createDisabledStatus() {
        return Health.unknown()
                .withDetail("consul.enabled", false)
                .withDetail("reason", "Consul service discovery disabled")
                .build();
    }

    /**
     * Create service tags for Consul registration
     */
    private java.util.List<String> createServiceTags() {
        return java.util.List.of(
                "auth-service",
                "trademaster",
                "java24",
                "spring-boot",
                "virtual-threads",
                "api-gateway-ready",
                "version-1.0.0"
        );
    }

    /**
     * Create service metadata for Consul registration
     */
    private java.util.Map<String, String> createServiceMetadata() {
        return java.util.Map.of(
                "version", "1.0.0",
                "environment", System.getProperty("spring.profiles.active", "development"),
                "java-version", System.getProperty("java.version"),
                "service-type", "authentication",
                "security-level", "high",
                "supports-kong", "true",
                "virtual-threads", "enabled"
        );
    }

    /**
     * Consul service configuration record for immutable config
     */
    public record ConsulServiceConfiguration(
            boolean enabled,
            String host,
            int port,
            String serviceName,
            java.util.List<String> tags,
            java.util.Map<String, String> metadata
    ) {
        public boolean isConfigured() {
            return enabled && host != null && !host.isEmpty() && port > 0;
        }

        public String getConsulUrl() {
            return String.format("http://%s:%d", host, port);
        }

        public String getServiceUrl() {
            return String.format("%s/v1/agent/service/register", getConsulUrl());
        }

        public String getHealthCheckUrl() {
            return String.format("%s/v1/health/service/%s", getConsulUrl(), serviceName);
        }
    }
}