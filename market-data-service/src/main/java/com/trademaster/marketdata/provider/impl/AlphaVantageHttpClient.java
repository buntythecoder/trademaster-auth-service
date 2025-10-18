package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.functional.Try;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Alpha Vantage HTTP Client with Circuit Breaker Protection
 *
 * Single Responsibility: HTTP communication with Alpha Vantage API
 * Following Rule #2 (SRP), Rule #11 (Functional Error Handling), Rule #25 (Circuit Breakers)
 *
 * Features:
 * - Configured RestTemplate with timeouts
 * - URL building for different API endpoints
 * - HTTP request execution with error handling
 * - Connection testing
 * - Circuit breaker protection on all external calls
 * - Rate limiting coordination (delegates to metrics tracker)
 *
 * @author TradeMaster Development Team
 * @version 1.1.0 (Circuit Breaker Enhanced)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlphaVantageHttpClient {

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final RestTemplate restTemplate;
    private final CircuitBreakerService circuitBreakerService;

    // Constructor for dependency injection (Rule #25: Circuit Breaker)
    public AlphaVantageHttpClient(RestTemplateBuilder restTemplateBuilder,
                                  CircuitBreakerService circuitBreakerService) {
        // Modern Spring Boot 3.5+ timeout configuration (non-deprecated)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restTemplate = restTemplateBuilder
            .requestFactory(() -> factory)
            .build();
        this.circuitBreakerService = circuitBreakerService;
    }

    /**
     * Test connection to Alpha Vantage API
     * Rule #11: Functional error handling with Try monad
     * Rule #25: Circuit breaker on all external API calls
     */
    public Try<ResponseEntity<Map>> testConnection(String apiKey) {
        return Try.of(() -> {
            String testUrl = buildGlobalQuoteUrl("IBM", apiKey);

            // Wrap with circuit breaker for resilience
            ResponseEntity<Map> response = circuitBreakerService.executeAlphaVantageCall(
                () -> restTemplate.getForEntity(testUrl, Map.class)
            ).join();

            return Optional.ofNullable(response)
                .filter(r -> r.getStatusCode() == HttpStatus.OK)
                .orElseThrow(() -> new RuntimeException("Connection test failed"));
        });
    }

    /**
     * Execute GET request to Alpha Vantage API
     * Rule #11: Functional error handling with Try monad
     * Rule #25: Circuit breaker on all external API calls
     */
    public Try<ResponseEntity<Map>> executeRequest(String url) {
        return Try.of(() -> {
            // Wrap with circuit breaker for resilience
            ResponseEntity<Map> response = circuitBreakerService.executeAlphaVantageCall(
                () -> restTemplate.getForEntity(url, Map.class)
            ).join();

            return Optional.ofNullable(response)
                .filter(r -> r.getStatusCode() == HttpStatus.OK && r.getBody() != null)
                .orElseThrow(() -> new RuntimeException("Invalid response from Alpha Vantage API"));
        });
    }

    /**
     * Build URL for historical data (TIME_SERIES_DAILY)
     */
    public String buildHistoricalDataUrl(String symbol, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("function", "TIME_SERIES_DAILY")
            .queryParam("symbol", symbol)
            .queryParam("apikey", apiKey)
            .queryParam("outputsize", "full")
            .toUriString();
    }

    /**
     * Build URL for current price (GLOBAL_QUOTE)
     */
    public String buildCurrentPriceUrl(String symbol, String apiKey) {
        return buildGlobalQuoteUrl(symbol, apiKey);
    }

    /**
     * Build URL for global quote endpoint
     */
    private String buildGlobalQuoteUrl(String symbol, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("function", "GLOBAL_QUOTE")
            .queryParam("symbol", symbol)
            .queryParam("apikey", apiKey)
            .toUriString();
    }
}
