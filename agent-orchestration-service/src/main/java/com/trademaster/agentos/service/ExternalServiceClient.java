package com.trademaster.agentos.service;

import com.trademaster.agentos.config.TracingConfig;
import com.trademaster.agentos.functional.Result;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ MANDATORY: External Service Client with Circuit Breakers
 * 
 * Implements circuit breaker patterns for all external service calls
 * as per TradeMaster Rule 25 - Zero tolerance policy for unprotected calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    @LoadBalanced
    private final RestTemplate restTemplate;
    
    private final Tracer tracer;
    private final TracingConfig.AgentOSTracingEnhancer tracingEnhancer;

    @Value("${agentos.integration.trading-service.url}")
    private String tradingServiceUrl;

    @Value("${agentos.integration.portfolio-service.url}")
    private String portfolioServiceUrl;

    @Value("${agentos.integration.market-data-service.url}")
    private String marketDataServiceUrl;

    @Value("${agentos.integration.notification-service.url}")
    private String notificationServiceUrl;

    /**
     * ✅ CIRCUIT BREAKER: Trading Service calls with fallback
     */
    @CircuitBreaker(name = "tradingService", fallbackMethod = "fallbackTradingService")
    @Retry(name = "tradingService")
    public CompletableFuture<Result<String, ServiceError>> callTradingService(String endpoint, Object payload) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = tracer.nextSpan().name("trading-service-call").start();
            
            try {
                tracingEnhancer.enhanceSpan(span, "trading-service-call", null, null);
                
                String url = tradingServiceUrl + endpoint;
                HttpHeaders headers = createHeaders();
                HttpEntity<Object> request = new HttpEntity<>(payload, headers);
                
                log.debug("Calling trading service: {}", url);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
                    
                tracingEnhancer.addBusinessMetrics(span, "response_status", 
                    String.valueOf(response.getStatusCode().value()));
                
                return Result.success(response.getBody());
                
            } catch (Exception e) {
                tracingEnhancer.recordError(span, e);
                log.error("Trading service call failed: {}", e.getMessage(), e);
                return Result.failure(new ServiceError("TRADING_SERVICE_ERROR", e.getMessage()));
            } finally {
                span.end();
            }
        });
    }

    /**
     * ✅ CIRCUIT BREAKER: Portfolio Service calls with fallback
     */
    @CircuitBreaker(name = "portfolioService", fallbackMethod = "fallbackPortfolioService")
    @Retry(name = "portfolioService")
    public CompletableFuture<Result<String, ServiceError>> callPortfolioService(String endpoint, Object payload) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = tracer.nextSpan().name("portfolio-service-call").start();
            
            try {
                tracingEnhancer.enhanceSpan(span, "portfolio-service-call", null, null);
                
                String url = portfolioServiceUrl + endpoint;
                HttpHeaders headers = createHeaders();
                HttpEntity<Object> request = new HttpEntity<>(payload, headers);
                
                log.debug("Calling portfolio service: {}", url);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
                    
                tracingEnhancer.addBusinessMetrics(span, "response_status", 
                    String.valueOf(response.getStatusCode().value()));
                
                return Result.success(response.getBody());
                
            } catch (Exception e) {
                tracingEnhancer.recordError(span, e);
                log.error("Portfolio service call failed: {}", e.getMessage(), e);
                return Result.failure(new ServiceError("PORTFOLIO_SERVICE_ERROR", e.getMessage()));
            } finally {
                span.end();
            }
        });
    }

    /**
     * ✅ CIRCUIT BREAKER: Market Data Service calls with fallback
     */
    @CircuitBreaker(name = "marketDataService", fallbackMethod = "fallbackMarketDataService")
    @Retry(name = "marketDataService")
    public CompletableFuture<Result<String, ServiceError>> callMarketDataService(String endpoint) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = tracer.nextSpan().name("market-data-service-call").start();
            
            try {
                tracingEnhancer.enhanceSpan(span, "market-data-service-call", null, null);
                
                String url = marketDataServiceUrl + endpoint;
                HttpHeaders headers = createHeaders();
                HttpEntity<Object> request = new HttpEntity<>(headers);
                
                log.debug("Calling market data service: {}", url);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);
                    
                tracingEnhancer.addBusinessMetrics(span, "response_status", 
                    String.valueOf(response.getStatusCode().value()));
                
                return Result.success(response.getBody());
                
            } catch (Exception e) {
                tracingEnhancer.recordError(span, e);
                log.error("Market data service call failed: {}", e.getMessage(), e);
                return Result.failure(new ServiceError("MARKET_DATA_SERVICE_ERROR", e.getMessage()));
            } finally {
                span.end();
            }
        });
    }

    /**
     * ✅ CIRCUIT BREAKER: Notification Service calls with fallback
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "fallbackNotificationService")
    @Retry(name = "notificationService")
    public CompletableFuture<Result<String, ServiceError>> callNotificationService(String endpoint, Object payload) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = tracer.nextSpan().name("notification-service-call").start();
            
            try {
                tracingEnhancer.enhanceSpan(span, "notification-service-call", null, null);
                
                String url = notificationServiceUrl + endpoint;
                HttpHeaders headers = createHeaders();
                HttpEntity<Object> request = new HttpEntity<>(payload, headers);
                
                log.debug("Calling notification service: {}", url);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
                    
                tracingEnhancer.addBusinessMetrics(span, "response_status", 
                    String.valueOf(response.getStatusCode().value()));
                
                return Result.success(response.getBody());
                
            } catch (Exception e) {
                tracingEnhancer.recordError(span, e);
                log.error("Notification service call failed: {}", e.getMessage(), e);
                return Result.failure(new ServiceError("NOTIFICATION_SERVICE_ERROR", e.getMessage()));
            } finally {
                span.end();
            }
        });
    }

    // ✅ FALLBACK METHODS - Required for circuit breakers

    public CompletableFuture<Result<String, ServiceError>> fallbackTradingService(String endpoint, Object payload, Exception ex) {
        log.warn("Trading service fallback triggered for endpoint: {} - {}", endpoint, ex.getMessage());
        return CompletableFuture.completedFuture(
            Result.failure(new ServiceError("TRADING_SERVICE_FALLBACK", "Trading service unavailable")));
    }

    public CompletableFuture<Result<String, ServiceError>> fallbackPortfolioService(String endpoint, Object payload, Exception ex) {
        log.warn("Portfolio service fallback triggered for endpoint: {} - {}", endpoint, ex.getMessage());
        return CompletableFuture.completedFuture(
            Result.failure(new ServiceError("PORTFOLIO_SERVICE_FALLBACK", "Portfolio service unavailable")));
    }

    public CompletableFuture<Result<String, ServiceError>> fallbackMarketDataService(String endpoint, Exception ex) {
        log.warn("Market data service fallback triggered for endpoint: {} - {}", endpoint, ex.getMessage());
        return CompletableFuture.completedFuture(
            Result.failure(new ServiceError("MARKET_DATA_SERVICE_FALLBACK", "Market data service unavailable")));
    }

    public CompletableFuture<Result<String, ServiceError>> fallbackNotificationService(String endpoint, Object payload, Exception ex) {
        log.warn("Notification service fallback triggered for endpoint: {} - {}", endpoint, ex.getMessage());
        return CompletableFuture.completedFuture(
            Result.failure(new ServiceError("NOTIFICATION_SERVICE_FALLBACK", "Notification service unavailable")));
    }

    /**
     * ✅ FUNCTIONAL: Create standard HTTP headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Service-Name", "agent-orchestration-service");
        headers.set("X-Request-ID", generateRequestId());
        return headers;
    }

    /**
     * ✅ FUNCTIONAL: Generate correlation ID for request tracking
     */
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * ✅ IMMUTABLE: Service Error record
     */
    public record ServiceError(String code, String message) {}
}