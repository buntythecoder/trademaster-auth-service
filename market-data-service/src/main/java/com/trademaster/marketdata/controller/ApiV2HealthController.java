package com.trademaster.marketdata.controller;

import com.trademaster.common.health.AbstractHealthController;
import com.trademaster.common.integration.kong.KongAdminClient;
import com.trademaster.common.properties.CommonServiceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kong-Compatible Health Check Controller
 *
 * Extends AbstractHealthController from common library with circuit breaker status.
 * Follows Golden Specification health check standards.
 *
 * Features:
 * - Comprehensive health status (from AbstractHealthController)
 * - Kong and Consul integration status
 * - Circuit breaker status reporting (market-data-service specific)
 * - Performance metrics and uptime
 * - OpenAPI documentation (from AbstractHealthController)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Using Common Library)
 */
@Slf4j
@RestController
public class ApiV2HealthController extends AbstractHealthController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ApiV2HealthController(
            HealthEndpoint healthEndpoint,
            CommonServiceProperties properties,
            KongAdminClient kongClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        super(healthEndpoint, properties, kongClient);
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Override to add circuit breaker health checks
     * MANDATORY Rule #5: Max 15 lines per method
     */
    @Override
    protected Map<String, Object> createCustomHealthChecks() {
        return Map.of(
            "circuitBreakers", getCircuitBreakerStatus(),
            "dataProviders", getDataProviderStatus()
        );
    }

    /**
     * Get detailed circuit breaker status (Rule #5: Max 15 lines)
     */
    private Map<String, Object> getCircuitBreakerStatus() {
        return circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> Map.of(
                    "state", cb.getState().name(),
                    "failureRate", cb.getMetrics().getFailureRate(),
                    "slowCallRate", cb.getMetrics().getSlowCallRate(),
                    "bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls()
                )
            ));
    }

    /**
     * Get data provider availability status (Rule #5: Max 15 lines)
     */
    private Map<String, Object> getDataProviderStatus() {
        return Map.of(
            "nse", getProviderState("nseProvider"),
            "bse", getProviderState("bseProvider"),
            "alphaVantage", getProviderState("alphaVantageProvider"),
            "influxDB", getProviderState("influxDB"),
            "redis", getProviderState("redisCache")
        );
    }

    /**
     * Get circuit breaker state for a provider (Rule #5: Max 15 lines)
     */
    private String getProviderState(String providerName) {
        return circuitBreakerRegistry.find(providerName)
            .map(cb -> cb.getState().name())
            .orElse("NOT_CONFIGURED");
    }

    /**
     * Readiness probe for Kubernetes/Docker (Rule #5: Max 15 lines)
     */
    @GetMapping("/api/v2/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        boolean allCircuitBreakersClosed = circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .allMatch(cb -> cb.getState() == CircuitBreaker.State.CLOSED ||
                           cb.getState() == CircuitBreaker.State.HALF_OPEN);

        String status = allCircuitBreakersClosed ? "READY" : "NOT_READY";
        Map<String, Object> response = Map.of(
            "status", status,
            "timestamp", Instant.now()
        );

        return allCircuitBreakersClosed
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(503).body(response);
    }

    /**
     * Liveness probe for Kubernetes/Docker (Rule #5: Max 15 lines)
     */
    @GetMapping("/api/v2/health/live")
    public ResponseEntity<Map<String, Object>> live() {
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", Instant.now()
        ));
    }

    /**
     * Startup probe for Kubernetes/Docker (Rule #5: Max 15 lines)
     */
    @GetMapping("/api/v2/health/startup")
    public ResponseEntity<Map<String, Object>> startup() {
        return ResponseEntity.ok(Map.of(
            "status", "STARTED",
            "service", "market-data-service",
            "timestamp", Instant.now()
        ));
    }
}
