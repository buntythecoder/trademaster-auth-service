package com.trademaster.payment.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MANDATORY Metrics Configuration
 * 
 * Comprehensive Prometheus metrics for Grafana dashboards as per TradeMaster standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "service", "payment-service",
                        "application", "trademaster",
                        "environment", System.getProperty("spring.profiles.active", "local")
                );
    }
}