package com.trademaster.agentos.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ✅ MANDATORY: Circuit Breaker Configuration for External Service Calls
 * 
 * Implements resilience patterns for all external service integrations
 * including trading service, portfolio service, and market data providers.
 * Per TradeMaster coding standards - Rule 25.
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    private static final String TRADING_SERVICE_CB = "tradingService";
    private static final String PORTFOLIO_SERVICE_CB = "portfolioService";
    private static final String MARKET_DATA_SERVICE_CB = "marketDataService";
    private static final String NOTIFICATION_SERVICE_CB = "notificationService";

    /**
     * ✅ FUNCTIONAL: Trading Service Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer tradingServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
            .of(TRADING_SERVICE_CB, builder -> builder
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build());
    }

    /**
     * ✅ FUNCTIONAL: Portfolio Service Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer portfolioServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
            .of(PORTFOLIO_SERVICE_CB, builder -> builder
                .failureRateThreshold(60.0f)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .slowCallRateThreshold(40.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .build());
    }

    /**
     * ✅ FUNCTIONAL: Market Data Service Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer marketDataServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
            .of(MARKET_DATA_SERVICE_CB, builder -> builder
                .failureRateThreshold(40.0f)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .slidingWindowType(SlidingWindowType.TIME_BASED)
                .slidingWindowSize(60) // 60 seconds
                .minimumNumberOfCalls(10)
                .slowCallRateThreshold(30.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .build());
    }

    /**
     * ✅ FUNCTIONAL: Notification Service Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer notificationServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
            .of(NOTIFICATION_SERVICE_CB, builder -> builder
                .failureRateThreshold(70.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .slowCallRateThreshold(60.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .build());
    }

    /**
     * ✅ FUNCTIONAL: Circuit Breaker Event Listener for Monitoring
     */
    @Bean
    public CircuitBreakerEventListener circuitBreakerEventListener() {
        return new CircuitBreakerEventListener();
    }

    /**
     * ✅ OBSERVER PATTERN: Circuit breaker event monitoring
     */
    public static class CircuitBreakerEventListener {

        public CircuitBreakerEventListener() {
            log.info("Initializing Circuit Breaker Event Listener for monitoring");
        }

        /**
         * ✅ FUNCTIONAL: Monitor circuit breaker state transitions
         */
        public void logStateTransition(String circuitBreakerName, String fromState, String toState) {
            log.warn("Circuit Breaker '{}' state transition: {} -> {}",
                circuitBreakerName, fromState, toState);
        }

        /**
         * ✅ FUNCTIONAL: Log circuit breaker calls not permitted
         */
        public void logCallNotPermitted(String circuitBreakerName) {
            log.error("Circuit Breaker '{}' call not permitted - circuit is OPEN", circuitBreakerName);
        }

        /**
         * ✅ FUNCTIONAL: Log slow call rate exceeded
         */
        public void logSlowCallRateExceeded(String circuitBreakerName, float slowCallRate) {
            log.warn("Circuit Breaker '{}' slow call rate exceeded: {}%", circuitBreakerName, slowCallRate);
        }

        /**
         * ✅ FUNCTIONAL: Log failure rate exceeded
         */
        public void logFailureRateExceeded(String circuitBreakerName, float failureRate) {
            log.error("Circuit Breaker '{}' failure rate exceeded: {}%", circuitBreakerName, failureRate);
        }
    }
}