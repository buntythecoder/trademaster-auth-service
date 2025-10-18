package com.trademaster.marketdata.client;

import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Internal Service Client
 *
 * Provides authenticated HTTP client for service-to-service communication.
 * Follows Rule #6 (Zero Trust Security) with internal access pattern.
 *
 * Features:
 * - API key authentication for internal service calls
 * - Circuit breaker protection (Rule #25)
 * - Correlation ID propagation (Rule #15: Structured Logging)
 * - Functional error handling (Rule #11)
 * - Virtual thread optimization (Rule #12)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalServiceClient {

    private static final String API_KEY_HEADER = "X-Service-API-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final RestTemplate restTemplate;
    private final CircuitBreakerService circuitBreakerService;

    @Value("${trademaster.security.service-api-key:CHANGE_ME_IN_PRODUCTION}")
    private String serviceApiKey;

    /**
     * Execute GET request to internal service endpoint (Rule #5: Max 15 lines)
     */
    public <T> CompletableFuture<Optional<T>> get(String serviceUrl, String path,
            Class<T> responseType) {
        return executeRequest(serviceUrl, path, HttpMethod.GET, null, responseType);
    }

    /**
     * Execute POST request to internal service endpoint (Rule #5: Max 15 lines)
     */
    public <T, R> CompletableFuture<Optional<R>> post(String serviceUrl, String path,
            T requestBody, Class<R> responseType) {
        return executeRequest(serviceUrl, path, HttpMethod.POST, requestBody, responseType);
    }

    /**
     * Execute PUT request to internal service endpoint (Rule #5: Max 15 lines)
     */
    public <T, R> CompletableFuture<Optional<R>> put(String serviceUrl, String path,
            T requestBody, Class<R> responseType) {
        return executeRequest(serviceUrl, path, HttpMethod.PUT, requestBody, responseType);
    }

    /**
     * Execute DELETE request to internal service endpoint (Rule #5: Max 15 lines)
     */
    public CompletableFuture<Boolean> delete(String serviceUrl, String path) {
        return executeRequest(serviceUrl, path, HttpMethod.DELETE, null, Void.class)
            .thenApply(Optional::isPresent);
    }

    /**
     * Execute HTTP request with circuit breaker protection (Rule #3: Functional, no if-else)
     */
    private <T, R> CompletableFuture<Optional<R>> executeRequest(String serviceUrl, String path,
            HttpMethod method, T requestBody, Class<R> responseType) {

        String fullUrl = buildUrl(serviceUrl, path);
        String correlationId = generateCorrelationId();
        HttpEntity<T> entity = createHttpEntity(requestBody, correlationId);

        return circuitBreakerService.<Optional<R>>executeDatabaseOperationWithFallback(
            () -> performHttpRequest(fullUrl, method, entity, responseType, correlationId),
            () -> {
                log.warn("Circuit breaker fallback activated for {} {}, CorrelationID: {}",
                    method, fullUrl, correlationId);
                return Optional.empty();
            }
        ).exceptionally(ex -> {
            log.error("Service call failed - {} {}, CorrelationID: {}, Error: {}",
                method, fullUrl, correlationId, ex.getMessage());
            return Optional.empty();
        });
    }

    /**
     * Perform HTTP request using RestTemplate (Rule #5: Max 15 lines)
     */
    private <T, R> Optional<R> performHttpRequest(String url, HttpMethod method,
            HttpEntity<T> entity, Class<R> responseType, String correlationId) {

        try {
            ResponseEntity<R> response = restTemplate.exchange(url, method, entity, responseType);

            log.debug("Service call successful - {} {}, Status: {}, CorrelationID: {}",
                method, url, response.getStatusCode(), correlationId);

            return Optional.ofNullable(response.getBody());

        } catch (Exception e) {
            log.error("HTTP request failed - {} {}, CorrelationID: {}, Error: {}",
                method, url, correlationId, e.getMessage());
            throw new ServiceCallException("Service call failed", e);
        }
    }

    /**
     * Build full URL from service URL and path (Rule #3: Functional)
     */
    private String buildUrl(String serviceUrl, String path) {
        return Optional.of(serviceUrl)
            .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
            .map(url -> path.startsWith("/") ? url + path : url + "/" + path)
            .get();
    }

    /**
     * Create HTTP entity with authentication headers (Rule #5: Max 15 lines)
     */
    private <T> HttpEntity<T> createHttpEntity(T body, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, serviceApiKey);
        headers.set(CORRELATION_ID_HEADER, correlationId);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(HttpHeaders.ACCEPT, "application/json");

        return new HttpEntity<>(body, headers);
    }

    /**
     * Generate correlation ID for request tracking (Rule #15: Structured Logging)
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get service health status (Rule #5: Max 15 lines)
     */
    public CompletableFuture<Map<String, Object>> getServiceHealth(String serviceUrl) {
        return this.<Map<String, Object>>get(serviceUrl, "/api/v2/health", (Class<Map<String, Object>>)(Class<?>)Map.class)
            .thenApply(health -> health.orElse(Map.of("status", "UNKNOWN")))
            .exceptionally(ex -> {
                log.error("Failed to get health for service {}: {}", serviceUrl, ex.getMessage());
                return Map.of("status", "ERROR", "error", ex.getMessage());
            });
    }

    /**
     * Custom exception for service call failures (Rule #11: Error Handling)
     */
    public static class ServiceCallException extends RuntimeException {
        public ServiceCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
