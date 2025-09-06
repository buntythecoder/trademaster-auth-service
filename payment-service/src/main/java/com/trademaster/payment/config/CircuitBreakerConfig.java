package com.trademaster.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration
 * 
 * MANDATORY: Resilience Patterns - Rule #24
 * MANDATORY: Enterprise-grade fault tolerance
 */
@Configuration
public class CircuitBreakerConfig {
    
    private static final String PAYMENT_GATEWAY = "payment-gateway";
    private static final String RAZORPAY_SERVICE = "razorpay-service";
    private static final String STRIPE_SERVICE = "stripe-service";
    
    /**
     * Circuit Breaker Registry with enterprise-grade configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Payment Gateway Circuit Breaker
        registry.circuitBreaker(PAYMENT_GATEWAY, 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class
                )
                .build());
        
        // Razorpay-specific Circuit Breaker
        registry.circuitBreaker(RAZORPAY_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f)
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        
        // Stripe-specific Circuit Breaker  
        registry.circuitBreaker(STRIPE_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(40.0f)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .build());
        
        return registry;
    }
    
    /**
     * Retry Registry with exponential backoff
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        
        // Payment Gateway Retry
        registry.retry(PAYMENT_GATEWAY,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build());
        
        // Razorpay Retry
        registry.retry(RAZORPAY_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(2))
                .build());
        
        // Stripe Retry
        registry.retry(STRIPE_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(4)
                .waitDuration(Duration.ofMillis(500))
                .build());
        
        return registry;
    }
    
    /**
     * TimeLimiter Registry for timeout handling
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        
        // Payment Gateway TimeLimiter
        registry.timeLimiter(PAYMENT_GATEWAY,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build());
        
        // Razorpay TimeLimiter
        registry.timeLimiter(RAZORPAY_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(15))
                .cancelRunningFuture(true)
                .build());
        
        // Stripe TimeLimiter
        registry.timeLimiter(STRIPE_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(12))
                .cancelRunningFuture(true)
                .build());
        
        return registry;
    }
    
    /**
     * Individual CircuitBreaker beans for injection
     */
    @Bean
    public CircuitBreaker paymentGatewayCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(PAYMENT_GATEWAY);
    }
    
    @Bean
    public CircuitBreaker razorpayCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(RAZORPAY_SERVICE);
    }
    
    @Bean
    public CircuitBreaker stripeCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(STRIPE_SERVICE);
    }
    
    /**
     * Individual Retry beans for injection
     */
    @Bean
    public Retry paymentGatewayRetry(RetryRegistry registry) {
        return registry.retry(PAYMENT_GATEWAY);
    }
    
    @Bean
    public Retry razorpayRetry(RetryRegistry registry) {
        return registry.retry(RAZORPAY_SERVICE);
    }
    
    @Bean
    public Retry stripeRetry(RetryRegistry registry) {
        return registry.retry(STRIPE_SERVICE);
    }
    
    /**
     * Individual TimeLimiter beans for injection
     */
    @Bean
    public TimeLimiter paymentGatewayTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(PAYMENT_GATEWAY);
    }
    
    @Bean
    public TimeLimiter razorpayTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(RAZORPAY_SERVICE);
    }
    
    @Bean
    public TimeLimiter stripeTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(STRIPE_SERVICE);
    }
}