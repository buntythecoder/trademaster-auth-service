package com.trademaster.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal Service Client
 * Handles service-to-service communication with API key authentication
 *
 * Compliance:
 * - Rule 6: Zero Trust Security - API key for internal calls
 * - Rule 10: @Slf4j for structured logging
 * - Rule 15: Correlation IDs in all requests
 * - Rule 16: Dynamic configuration
 * - Rule 24: Circuit breaker for resilience (future enhancement)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalServiceClient {

    @Value("${trademaster.security.internal-api-key:${INTERNAL_API_KEY:changeme}}")
    private String apiKey;

    @Value("${trademaster.services.portfolio-service-url:http://localhost:8083}")
    private String portfolioServiceUrl;

    @Value("${trademaster.services.notification-service-url:http://localhost:8086}")
    private String notificationServiceUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * Call Portfolio Service internal API
     * @param endpoint Internal endpoint path
     * @param requestBody Request payload
     * @param responseType Expected response type
     * @return Response wrapped in Optional
     */
    public <T, R> Optional<R> callPortfolioService(
            String endpoint,
            T requestBody,
            Class<R> responseType
    ) {
        String correlationId = UUID.randomUUID().toString();
        String url = portfolioServiceUrl + "/api/internal/v1" + endpoint;

        log.debug("Calling Portfolio Service: {} | correlation_id={}", url, correlationId);

        try {
            R response = restClient.post()
                .uri(url)
                .header("X-API-Key", apiKey)
                .header("X-Correlation-ID", correlationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .body(responseType);

            log.debug("Portfolio Service response received | correlation_id={}", correlationId);
            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("Portfolio Service call failed: {} | correlation_id={}",
                     e.getMessage(), correlationId, e);
            return Optional.empty();
        }
    }

    /**
     * Call Notification Service internal API
     * @param endpoint Internal endpoint path
     * @param requestBody Request payload
     * @param responseType Expected response type
     * @return Response wrapped in Optional
     */
    public <T, R> Optional<R> callNotificationService(
            String endpoint,
            T requestBody,
            Class<R> responseType
    ) {
        String correlationId = UUID.randomUUID().toString();
        String url = notificationServiceUrl + "/api/internal/v1" + endpoint;

        log.debug("Calling Notification Service: {} | correlation_id={}", url, correlationId);

        try {
            R response = restClient.post()
                .uri(url)
                .header("X-API-Key", apiKey)
                .header("X-Correlation-ID", correlationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .body(responseType);

            log.debug("Notification Service response received | correlation_id={}", correlationId);
            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("Notification Service call failed: {} | correlation_id={}",
                     e.getMessage(), correlationId, e);
            return Optional.empty();
        }
    }

    /**
     * GET request to internal service
     * @param serviceUrl Base service URL
     * @param endpoint Internal endpoint path
     * @param responseType Expected response type
     * @return Response wrapped in Optional
     */
    public <R> Optional<R> getFromInternalService(
            String serviceUrl,
            String endpoint,
            Class<R> responseType
    ) {
        String correlationId = UUID.randomUUID().toString();
        String url = serviceUrl + "/api/internal/v1" + endpoint;

        log.debug("GET from internal service: {} | correlation_id={}", url, correlationId);

        try {
            R response = restClient.get()
                .uri(url)
                .header("X-API-Key", apiKey)
                .header("X-Correlation-ID", correlationId)
                .retrieve()
                .body(responseType);

            log.debug("Internal service GET response received | correlation_id={}", correlationId);
            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("Internal service GET failed: {} | correlation_id={}",
                     e.getMessage(), correlationId, e);
            return Optional.empty();
        }
    }
}
