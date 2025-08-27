package com.trademaster.brokerauth.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Comprehensive Monitoring Configuration
 * 
 * Configures Prometheus metrics, custom application metrics,
 * and monitoring dashboards for TradeMaster Broker Auth Service.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class MonitoringConfig {

    /**
     * Configure common tags for metrics
     * Note: Spring Boot will automatically apply these through application properties:
     * management.metrics.tags.application=trademaster-broker-auth-service
     * management.metrics.tags.service=broker-auth
     * management.metrics.tags.version=1.0.0
     */

    /**
     * Enable @Timed annotation support
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * JVM Memory Metrics
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM Garbage Collection Metrics
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * JVM Thread Metrics
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * Class Loader Metrics
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * System Processor Metrics
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * System Uptime Metrics
     */
    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * File Descriptor Metrics
     */
    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }

    /**
     * Business Metrics Configuration
     */
    @Configuration
    public static class BusinessMetricsConfig {

        /**
         * Authentication Success Counter
         */
        @Bean
        public Counter authenticationSuccessCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_auth_success_total")
                .description("Total number of successful broker authentications")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Authentication Failure Counter
         */
        @Bean
        public Counter authenticationFailureCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_auth_failure_total")
                .description("Total number of failed broker authentications")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Session Creation Counter
         */
        @Bean
        public Counter sessionCreationCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_session_created_total")
                .description("Total number of broker sessions created")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Session Expiry Counter
         */
        @Bean
        public Counter sessionExpiryCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_session_expired_total")
                .description("Total number of broker sessions expired")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Token Refresh Counter
         */
        @Bean
        public Counter tokenRefreshCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_token_refresh_total")
                .description("Total number of broker token refreshes")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Broker API Call Timer
         */
        @Bean
        public Timer brokerApiCallTimer(MeterRegistry meterRegistry) {
            return Timer.builder("broker_api_call_duration_seconds")
                .description("Duration of broker API calls")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Database Operation Timer
         */
        @Bean
        public Timer databaseOperationTimer(MeterRegistry meterRegistry) {
            return Timer.builder("database_operation_duration_seconds")
                .description("Duration of database operations")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Encryption/Decryption Timer
         */
        @Bean
        public Timer encryptionTimer(MeterRegistry meterRegistry) {
            return Timer.builder("encryption_operation_duration_seconds")
                .description("Duration of encryption/decryption operations")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        // Note: Active Sessions Gauge will be implemented in the service layer
        // using Gauge.builder() and registered dynamically
    }

    /**
     * Health Check Metrics Configuration
     */
    @Configuration
    public static class HealthMetricsConfig {

        /**
         * Database Health Counter
         */
        @Bean
        public Counter databaseHealthCounter(MeterRegistry meterRegistry) {
            return Counter.builder("database_health_check_total")
                .description("Total database health checks performed")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Redis Health Counter
         */
        @Bean
        public Counter redisHealthCounter(MeterRegistry meterRegistry) {
            return Counter.builder("redis_health_check_total")
                .description("Total Redis health checks performed")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Broker Connectivity Counter
         */
        @Bean
        public Counter brokerConnectivityCounter(MeterRegistry meterRegistry) {
            return Counter.builder("broker_connectivity_check_total")
                .description("Total broker connectivity checks performed")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }
    }

    /**
     * Security Metrics Configuration
     */
    @Configuration
    public static class SecurityMetricsConfig {

        /**
         * Security Violation Counter
         */
        @Bean
        public Counter securityViolationCounter(MeterRegistry meterRegistry) {
            return Counter.builder("security_violation_total")
                .description("Total security violations detected")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Rate Limit Exceeded Counter
         */
        @Bean
        public Counter rateLimitExceededCounter(MeterRegistry meterRegistry) {
            return Counter.builder("rate_limit_exceeded_total")
                .description("Total rate limit violations")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        /**
         * Invalid Token Counter
         */
        @Bean
        public Counter invalidTokenCounter(MeterRegistry meterRegistry) {
            return Counter.builder("invalid_token_total")
                .description("Total invalid token attempts")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }
    }

    /**
     * Performance Metrics Configuration
     */
    @Configuration
    public static class PerformanceMetricsConfig {

        /**
         * Virtual Thread Pool Timer
         */
        @Bean
        public Timer virtualThreadTimer(MeterRegistry meterRegistry) {
            return Timer.builder("virtual_thread_execution_duration_seconds")
                .description("Duration of virtual thread executions")
                .tag("service", "broker-auth")
                .register(meterRegistry);
        }

        // Note: Connection Pool Metrics (HikariCP) are automatically registered
        // by Spring Boot's metrics auto-configuration
    }
}