package com.trademaster.auth.service;

import com.trademaster.auth.config.KongConfiguration.KongServiceConfig;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Kong Integration Service
 *
 * MANDATORY: Golden Specification - Kong API Gateway Integration
 * MANDATORY: Rule #12 - Virtual Threads
 * MANDATORY: Rule #3 - Functional Programming
 * MANDATORY: Rule #25 - Circuit Breaker
 *
 * Features:
 * - Kong service registration and health monitoring
 * - Upstream health reporting to Kong
 * - Rate limit status monitoring
 * - Configuration synchronization
 * - Circuit breaker integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KongIntegrationService implements HealthIndicator {

    @Qualifier("kongAdminClient")
    private final RestTemplate kongAdminClient;

    @Qualifier("kongGatewayClient")
    private final RestTemplate kongGatewayClient;

    private final KongServiceConfig kongConfig;
    private final CircuitBreakerService circuitBreakerService;

    private volatile KongServiceStatus lastKnownStatus = KongServiceStatus.UNKNOWN;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();

    /**
     * Register service with Kong on startup using Virtual Threads
     */
    public CompletableFuture<Result<String, String>> registerServiceWithKong() {
        return CompletableFuture.supplyAsync(() ->
                SafeOperations.safelyToResult(this::performServiceRegistration)
                        .map(response -> "Service registered successfully with Kong")
                        .mapError(error -> {
                            log.error("Failed to register service with Kong: {}", error);
                            return "Kong service registration failed: " + error;
                        }),
                Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Report health status to Kong upstream using circuit breaker
     */
    public CompletableFuture<Result<String, String>> reportHealthToKong(boolean isHealthy) {
        return circuitBreakerService.executeExternalApiOperation(
                "kong-health-report",
                () -> performHealthReport(isHealthy)
        );
    }

    /**
     * Get Kong service status using functional approach
     */
    public CompletableFuture<Result<KongServiceStatus, String>> getKongServiceStatus() {
        return circuitBreakerService.executeExternalApiOperation(
                "kong-service-status",
                this::fetchKongServiceStatus
        ).thenApply(result -> result.map(this::parseServiceStatus));
    }

    /**
     * Scheduled health check reporting to Kong (every 30 seconds)
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledHealthReport() {
        Optional.of(kongConfig)
                .filter(KongServiceConfig::isConfigured)
                .ifPresentOrElse(
                        config -> performScheduledHealthCheck(),
                        () -> log.debug("Kong not configured, skipping health report")
                );
    }

    /**
     * Spring Boot Health Indicator implementation
     */
    @Override
    public Health health() {
        return Optional.of(lastKnownStatus)
                .map(this::mapStatusToHealth)
                .orElse(Health.unknown().build());
    }

    // Private implementation methods

    /**
     * Perform service registration with Kong
     */
    private String performServiceRegistration() {
        Map<String, Object> serviceConfig = Map.of(
                "name", kongConfig.serviceName(),
                "url", kongConfig.serviceUrl(),
                "protocol", "http",
                "connect_timeout", 30000,
                "write_timeout", 30000,
                "read_timeout", 30000,
                "retries", 3
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        addAuthHeaderIfEnabled(headers);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(serviceConfig, headers);

        ResponseEntity<String> response = kongAdminClient.exchange(
                kongConfig.getAdminServiceUrl(),
                HttpMethod.PUT,
                request,
                String.class
        );

        log.info("Kong service registration response: {}",
                StructuredArguments.kv("status", response.getStatusCode()),
                StructuredArguments.kv("service", kongConfig.serviceName()));

        return response.getBody();
    }

    /**
     * Perform health report to Kong upstream
     */
    private String performHealthReport(boolean isHealthy) {
        // Use target address format: auth-service:8080 instead of target ID
        String targetAddress = "auth-service:8080";
        String targetUrl = kongConfig.getAdminUpstreamUrl() + "/targets/" + targetAddress + "/" +
                (isHealthy ? "healthy" : "unhealthy");

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaderIfEnabled(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = kongAdminClient.exchange(
                targetUrl,
                HttpMethod.PUT,
                request,
                String.class
        );

        lastKnownStatus = isHealthy ? KongServiceStatus.HEALTHY : KongServiceStatus.UNHEALTHY;
        lastHealthCheck = LocalDateTime.now();

        log.debug("Kong health report sent: {}",
                StructuredArguments.kv("healthy", isHealthy),
                StructuredArguments.kv("targetAddress", targetAddress),
                StructuredArguments.kv("status", response.getStatusCode()));

        return response.getBody();
    }


    /**
     * Fetch Kong service status
     */
    private String fetchKongServiceStatus() {
        HttpHeaders headers = new HttpHeaders();
        addAuthHeaderIfEnabled(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = kongAdminClient.exchange(
                kongConfig.getAdminServiceUrl(),
                HttpMethod.GET,
                request,
                String.class
        );

        return response.getBody();
    }

    /**
     * Parse service status from Kong response
     */
    private KongServiceStatus parseServiceStatus(String response) {
        return Optional.ofNullable(response)
                .filter(resp -> resp.contains("\"enabled\":true"))
                .map(resp -> KongServiceStatus.HEALTHY)
                .orElse(KongServiceStatus.UNHEALTHY);
    }

    /**
     * Perform scheduled health check
     */
    private void performScheduledHealthCheck() {
        CompletableFuture.supplyAsync(() ->
                reportHealthToKong(isServiceHealthy()),
                Executors.newVirtualThreadPerTaskExecutor()
        ).whenComplete((result, throwable) ->
                Optional.ofNullable(throwable)
                        .ifPresentOrElse(
                                error -> log.warn("Scheduled Kong health report failed: {}", error),
                                () -> log.debug("Scheduled Kong health report completed")
                        )
        );
    }

    /**
     * Check if service is healthy
     */
    private boolean isServiceHealthy() {
        // Simple health check - can be enhanced with circuit breaker status
        return circuitBreakerService.getHealthStatus().isHealthy();
    }

    /**
     * Map Kong service status to Spring Health
     */
    private Health mapStatusToHealth(KongServiceStatus status) {
        return switch (status) {
            case HEALTHY -> Health.up()
                    .withDetail("kong.service", kongConfig.serviceName())
                    .withDetail("kong.lastCheck", lastHealthCheck)
                    .withDetail("kong.admin", kongConfig.adminUrl())
                    .build();
            case UNHEALTHY -> Health.down()
                    .withDetail("kong.service", kongConfig.serviceName())
                    .withDetail("kong.lastCheck", lastHealthCheck)
                    .withDetail("kong.admin", kongConfig.adminUrl())
                    .build();
            case UNKNOWN -> Health.unknown()
                    .withDetail("kong.service", kongConfig.serviceName())
                    .withDetail("kong.configured", kongConfig.isConfigured())
                    .build();
        };
    }

    /**
     * Add authentication header if enabled
     */
    private void addAuthHeaderIfEnabled(HttpHeaders headers) {
        Optional.of(kongConfig)
                .filter(KongServiceConfig::adminAuthEnabled)
                .map(KongServiceConfig::adminAuthToken)
                .filter(token -> !token.isEmpty())
                .ifPresent(token -> headers.set("Kong-Admin-Token", token));
    }

    /**
     * Kong service status enumeration
     */
    public enum KongServiceStatus {
        HEALTHY,
        UNHEALTHY,
        UNKNOWN
    }
}