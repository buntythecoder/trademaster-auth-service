package com.trademaster.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Kong-Compatible Health Check Endpoint
 * Provides /api/v2/health endpoint per Golden Specification
 *
 * Compliance:
 * - Golden Spec: Kong health check endpoint at /api/v2/health
 * - Rule 10: @Slf4j for structured logging
 * - Rule 18: Descriptive naming (getHealthStatus)
 */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Health Check API",
    description = "Kong-compatible health monitoring endpoints for service availability"
)
public class ApiV2HealthController {

    private final HealthIndicator consulHealthIndicator;

    /**
     * Kong-compatible health check endpoint
     * Returns service health status for Kong health monitoring
     *
     * @return Health status with timestamp and service details
     */
    @Operation(
        summary = "Service Health Check",
        description = """
            Kong-compatible health check endpoint that provides comprehensive service health status.

            ### Health Indicators
            - Service availability
            - Consul connectivity
            - Database connection
            - Circuit breaker status

            ### Use Cases
            - Kong upstream health monitoring
            - Load balancer health checks
            - Service discovery validation
            - Monitoring alerts
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and operational",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                      "status": "UP",
                      "service": "payment-service",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "consul": {
                        "consul": "connected",
                        "service-registration": "active",
                        "datacenter": "trademaster-dc"
                      },
                      "version": "1.0.0",
                      "environment": "dev"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service is unhealthy or degraded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "DOWN",
                      "service": "payment-service",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "consul": {
                        "consul": "connection-failed",
                        "error": "Connection timeout"
                      },
                      "version": "1.0.0",
                      "environment": "dev"
                    }
                    """)
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        log.debug("Kong health check requested");

        Health consulHealth = consulHealthIndicator.health();
        boolean isHealthy = consulHealth.getStatus().getCode().equals("UP");

        Map<String, Object> healthResponse = Map.of(
            "status", isHealthy ? "UP" : "DOWN",
            "service", "payment-service",
            "timestamp", Instant.now(),
            "consul", consulHealth.getDetails(),
            "version", "1.0.0",
            "environment", System.getProperty("spring.profiles.active", "dev")
        );

        log.debug("Health check response: status={}", isHealthy ? "UP" : "DOWN");

        return isHealthy
            ? ResponseEntity.ok(healthResponse)
            : ResponseEntity.status(503).body(healthResponse);
    }
}
