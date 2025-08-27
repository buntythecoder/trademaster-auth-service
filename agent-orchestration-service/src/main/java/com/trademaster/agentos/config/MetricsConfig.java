package com.trademaster.agentos.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MANDATORY: Prometheus Metrics Configuration
 * 
 * Implements comprehensive monitoring as required by trademaster-coding-standards.md v2.0:
 * - Business Metrics: Agent operations, task execution, workflow completion
 * - Performance Metrics: API response times, Virtual Thread utilization
 * - System Health Metrics: JVM memory, database connections, error rates
 * - Security Metrics: Authentication attempts, security incidents
 */
@Configuration
public class MetricsConfig {

    @Bean
    public AgentOSMetrics agentOSMetrics(MeterRegistry meterRegistry) {
        return new AgentOSMetrics(meterRegistry);
    }
}