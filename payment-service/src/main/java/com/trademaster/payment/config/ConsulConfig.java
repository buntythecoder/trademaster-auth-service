package com.trademaster.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Consul Service Discovery Configuration
 * Leverages Spring Cloud Consul auto-configuration from application.yml
 *
 * Compliance:
 * - Rule 1: Java 24 with Virtual Threads
 * - Rule 16: Dynamic Configuration with @Value
 * - Rule 10: Lombok @Slf4j for logging
 * - Rule 25: Configuration sync with application.yml
 *
 * Note: Consul properties are fully configured in application.yml.
 * Spring Cloud Consul auto-configures ConsulDiscoveryProperties bean.
 * No manual bean creation needed (avoids private constructor issue).
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.consul.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ConsulConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private int serverPort;

    @PostConstruct
    public void logConsulConfiguration() {
        log.info("Consul service discovery enabled for {} on port {}", serviceName, serverPort);
        log.info("Consul configuration loaded from application.yml");
        log.debug("Service tags and metadata configured via Spring Cloud Consul auto-configuration");
    }
}
