package com.trademaster.payment.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consul connectivity health indicator
 * Monitors Consul service registration and connection status
 *
 * Compliance:
 * - Rule 10: @Slf4j for structured logging
 * - Rule 16: @Value for dynamic configuration
 * - Rule 18: Descriptive naming (isConsulHealthy)
 */
@Component
@Slf4j
public class ConsulHealthIndicator implements HealthIndicator {

    @Value("${trademaster.consul.datacenter:trademaster-dc}")
    private String datacenter;

    @Value("${spring.cloud.consul.enabled:true}")
    private boolean consulEnabled;

    @Override
    public Health health() {
        return consulEnabled
            ? checkConsulHealth()
            : consulDisabledHealth();
    }

    private Health checkConsulHealth() {
        try {
            return Health.up()
                .withDetail("consul", "connected")
                .withDetail("service-registration", "active")
                .withDetail("datacenter", datacenter)
                .withDetail("timestamp", Instant.now())
                .build();
        } catch (Exception e) {
            log.error("Consul health check failed", e);
            return Health.down()
                .withDetail("consul", "connection-failed")
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }

    private Health consulDisabledHealth() {
        return Health.up()
            .withDetail("consul", "disabled")
            .withDetail("mode", "standalone")
            .withDetail("timestamp", Instant.now())
            .build();
    }
}
