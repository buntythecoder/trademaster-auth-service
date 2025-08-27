package com.trademaster.brokerauth.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing Configuration
 * 
 * Configures OpenTelemetry for distributed tracing across
 * the TradeMaster microservices ecosystem.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class TracingConfig {
    
    @Value("${spring.application.name:broker-auth-service}")
    private String serviceName;
    
    @Value("${tracing.jaeger.endpoint:http://localhost:14268/api/traces}")
    private String jaegerEndpoint;
    
    @Value("${tracing.sampling.probability:0.1}")
    private double samplingProbability;
    
    /**
     * OpenTelemetry Tracer for manual instrumentation
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("trademaster.broker-auth-service", "1.0.0");
    }
    
    /**
     * Text Map Propagator for distributed tracing
     * Supports both B3 and Jaeger propagation formats
     */
    @Bean
    public TextMapPropagator textMapPropagator() {
        return TextMapPropagator.composite(
            B3Propagator.injectingSingleHeader(),
            JaegerPropagator.getInstance()
        );
    }
    
    /**
     * Custom tracing configuration
     */
    @Bean
    public TracingCustomizer tracingCustomizer() {
        return new TracingCustomizer();
    }
    
    public static class TracingCustomizer {
        
        /**
         * Get service name for tracing
         */
        public String getServiceName() {
            return "trademaster-broker-auth-service";
        }
        
        /**
         * Get resource attributes for tracing
         */
        public java.util.Map<String, String> getResourceAttributes() {
            return java.util.Map.of(
                "service.name", getServiceName(),
                "service.version", "1.0.0",
                "service.namespace", "trademaster",
                "service.instance.id", java.util.UUID.randomUUID().toString(),
                "deployment.environment", System.getProperty("spring.profiles.active", "development")
            );
        }
    }
}