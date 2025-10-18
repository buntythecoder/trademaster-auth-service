package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.resilience.CircuitBreakerService;
import com.trademaster.marketdata.resilience.CircuitBreakerService.CircuitBreakerStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Circuit Breaker Status Controller
 *
 * Provides monitoring endpoints for circuit breaker health and metrics
 * following Rule #25: Circuit Breaker Implementation
 *
 * Features:
 * - Real-time circuit breaker status
 * - Individual circuit breaker metrics
 * - Manual circuit breaker reset (admin only)
 * - Health check integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/circuit-breakers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Circuit Breaker Monitoring", description = "Circuit breaker status and management API")
public class CircuitBreakerStatusController {

    private final CircuitBreakerService circuitBreakerService;

    /**
     * Get status of all circuit breakers
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get all circuit breaker status",
        description = "Retrieve current status and metrics for all registered circuit breakers"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Circuit breaker status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakerStatus() {
        log.debug("Fetching status for all circuit breakers");

        Map<String, CircuitBreakerStatus> statusMap = circuitBreakerService.getAllCircuitBreakerStatus();

        long healthyCount = statusMap.values().stream()
            .filter(CircuitBreakerStatus::isHealthy)
            .count();

        long openCount = statusMap.values().stream()
            .filter(CircuitBreakerStatus::isOpen)
            .count();

        long halfOpenCount = statusMap.values().stream()
            .filter(CircuitBreakerStatus::isHalfOpen)
            .count();

        return ResponseEntity.ok(Map.of(
            "timestamp", Instant.now(),
            "totalCircuitBreakers", statusMap.size(),
            "healthyCount", healthyCount,
            "openCount", openCount,
            "halfOpenCount", halfOpenCount,
            "overallHealthy", openCount == 0,
            "circuitBreakers", statusMap
        ));
    }

    /**
     * Get status of specific circuit breaker
     */
    @GetMapping("/status/{name}")
    @Operation(
        summary = "Get specific circuit breaker status",
        description = "Retrieve status and metrics for a specific circuit breaker"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Circuit breaker status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Circuit breaker not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus(
            @Parameter(description = "Circuit breaker name", example = "nseDataProvider")
            @PathVariable String name) {

        log.debug("Fetching status for circuit breaker: {}", name);

        try {
            CircuitBreakerStatus status = circuitBreakerService.getCircuitBreakerStatus(name);

            return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now(),
                "circuitBreaker", status,
                "healthy", status.isHealthy(),
                "requiresAttention", status.isOpen() || status.failureRate() > 25.0f
            ));
        } catch (Exception e) {
            log.error("Circuit breaker not found: {}", name);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reset circuit breaker (Admin only)
     * Use with caution - manually transitions circuit to CLOSED state
     */
    @PostMapping("/reset/{name}")
    @Operation(
        summary = "Reset circuit breaker",
        description = "Manually reset a circuit breaker to CLOSED state (Admin only - use with caution)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Circuit breaker reset successfully"),
        @ApiResponse(responseCode = "404", description = "Circuit breaker not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker(
            @Parameter(description = "Circuit breaker name to reset", example = "nseDataProvider")
            @PathVariable String name) {

        log.warn("Manual circuit breaker reset requested for: {}", name);

        try {
            CircuitBreakerStatus beforeReset = circuitBreakerService.getCircuitBreakerStatus(name);

            circuitBreakerService.resetCircuitBreaker(name);

            CircuitBreakerStatus afterReset = circuitBreakerService.getCircuitBreakerStatus(name);

            return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now(),
                "circuitBreaker", name,
                "action", "RESET",
                "previousState", beforeReset.state(),
                "currentState", afterReset.state(),
                "message", "Circuit breaker reset to CLOSED state - Monitor closely for failures"
            ));
        } catch (Exception e) {
            log.error("Failed to reset circuit breaker: {}", name, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Health check - Returns unhealthy if any critical circuit breakers are open
     */
    @GetMapping("/health")
    @Operation(
        summary = "Circuit breaker health check",
        description = "Check if all critical circuit breakers are healthy"
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, CircuitBreakerStatus> statusMap = circuitBreakerService.getAllCircuitBreakerStatus();

        // Critical circuit breakers that must be healthy
        String[] criticalCircuitBreakers = {
            "database",
            "nseDataProvider",
            "bseDataProvider"
        };

        boolean allCriticalHealthy = java.util.Arrays.stream(criticalCircuitBreakers)
            .allMatch(name -> {
                CircuitBreakerStatus status = statusMap.get(name);
                return status != null && status.isHealthy();
            });

        long openCount = statusMap.values().stream()
            .filter(CircuitBreakerStatus::isOpen)
            .count();

        if (!allCriticalHealthy || openCount > 0) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "DEGRADED",
                "timestamp", Instant.now(),
                "allCriticalHealthy", allCriticalHealthy,
                "openCircuitBreakers", openCount,
                "message", "One or more circuit breakers are OPEN - Service may be degraded"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "HEALTHY",
            "timestamp", Instant.now(),
            "allCriticalHealthy", true,
            "openCircuitBreakers", 0,
            "message", "All circuit breakers are healthy"
        ));
    }
}
