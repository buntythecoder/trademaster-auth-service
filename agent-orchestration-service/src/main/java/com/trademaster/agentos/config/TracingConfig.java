package com.trademaster.agentos.config;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ MANDATORY: Distributed Tracing Configuration
 * 
 * Implements OpenTelemetry tracing with Zipkin export for 
 * distributed request tracking across TradeMaster services.
 */
@Configuration
@Slf4j
public class TracingConfig {

    @Value("${spring.application.name:agent-orchestration-service}")
    private String serviceName;

    @Value("${tracing.zipkin.endpoint:http://localhost:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${tracing.enabled:true}")
    private boolean tracingEnabled;

    /**
     * ✅ FUNCTIONAL: OpenTelemetry SDK configuration
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        if (!tracingEnabled) {
            log.info("Distributed tracing disabled for service: {}", serviceName);
            return OpenTelemetry.noop();
        }

        log.info("Configuring distributed tracing for service: {} with Zipkin endpoint: {}", 
            serviceName, zipkinEndpoint);

        Resource resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(
                ZipkinSpanExporter.builder()
                    .setEndpoint(zipkinEndpoint)
                    .build())
                .build())
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();
    }

    /**
     * ✅ FUNCTIONAL: Micrometer Tracer bridge
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return new OtelTracer(openTelemetry.getTracer(serviceName),
            new OtelCurrentTraceContext(), 
            event -> log.debug("Span finished: {}", event.toString()));
    }

    /**
     * ✅ FUNCTIONAL: Current Trace Context
     */
    @Bean
    public CurrentTraceContext currentTraceContext() {
        return new OtelCurrentTraceContext();
    }

    /**
     * ✅ FUNCTIONAL: Custom span tags for Agent OS operations
     */
    @Bean
    public AgentOSTracingEnhancer agentOSTracingEnhancer() {
        return new AgentOSTracingEnhancer();
    }

    /**
     * ✅ DECORATOR PATTERN: Enhance tracing with Agent OS specific context
     */
    public static class AgentOSTracingEnhancer {

        /**
         * ✅ FUNCTIONAL: Add Agent OS specific tags to spans
         */
        public void enhanceSpan(io.micrometer.tracing.Span span, String operation, String agentId, String taskId) {
            if (span == null) return;

            span.tag("agentos.operation", operation)
                .tag("agentos.agent_id", agentId != null ? agentId : "system")
                .tag("agentos.task_id", taskId != null ? taskId : "none")
                .tag("agentos.service", "agent-orchestration")
                .tag("component", "agent-orchestration-service");
        }

        /**
         * ✅ FUNCTIONAL: Add error information to spans
         */
        public void recordError(io.micrometer.tracing.Span span, Throwable error) {
            if (span == null || error == null) return;

            span.tag("error", "true")
                .tag("error.class", error.getClass().getSimpleName())
                .tag("error.message", error.getMessage() != null ? error.getMessage() : "unknown");
        }

        /**
         * ✅ FUNCTIONAL: Add business metrics to spans
         */
        public void addBusinessMetrics(io.micrometer.tracing.Span span, String metricType, String value) {
            if (span == null) return;

            span.tag("business.metric.type", metricType)
                .tag("business.metric.value", value);
        }
    }
}