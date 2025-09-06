package com.trademaster.portfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Circuit Breaker Configuration Properties
 * 
 * Implements Rule #26 (Configuration Synchronization Audit) - MANDATORY config validation.
 * 
 * All circuit breaker properties externalized with validation and reasonable defaults.
 * Sync with application.yml configuration entries.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Spring Boot 3.5)
 */
@ConfigurationProperties(prefix = "portfolio.circuit-breaker")
@Data
@Validated
public class CircuitBreakerProperties {
    
    /**
     * Failure rate threshold percentage (0-100)
     * Default: 50% - Circuit opens when failure rate exceeds this
     */
    @NotNull
    @Min(value = 10, message = "Failure rate threshold must be at least 10%")
    @Max(value = 100, message = "Failure rate threshold cannot exceed 100%")
    private Float failureRateThreshold = 50.0f;
    
    /**
     * Wait duration in open state before transitioning to half-open
     * Default: 60 seconds
     */
    @NotNull
    private Duration waitDurationInOpenState = Duration.ofSeconds(60);
    
    /**
     * Sliding window size for failure rate calculation
     * Default: 10 calls
     */
    @NotNull
    @Min(value = 5, message = "Sliding window size must be at least 5")
    @Max(value = 100, message = "Sliding window size cannot exceed 100")
    private Integer slidingWindowSize = 10;
    
    /**
     * Minimum number of calls required before failure rate calculation
     * Default: 5 calls
     */
    @NotNull
    @Min(value = 1, message = "Minimum number of calls must be at least 1")
    private Integer minimumNumberOfCalls = 5;
    
    /**
     * Slow call rate threshold percentage (0-100)
     * Default: 100% - All calls must be slow to trigger
     */
    @NotNull
    @Min(value = 0, message = "Slow call rate threshold must be at least 0%")
    @Max(value = 100, message = "Slow call rate threshold cannot exceed 100%")
    private Float slowCallRateThreshold = 100.0f;
    
    /**
     * Slow call duration threshold
     * Default: 5 seconds - Calls taking longer are considered slow
     */
    @NotNull
    private Duration slowCallDurationThreshold = Duration.ofSeconds(5);
    
    /**
     * Number of permitted calls in half-open state
     * Default: 3 calls
     */
    @NotNull
    @Min(value = 1, message = "Permitted calls in half-open state must be at least 1")
    @Max(value = 10, message = "Permitted calls in half-open state cannot exceed 10")
    private Integer permittedCallsInHalfOpenState = 3;
    
    /**
     * Enable automatic transition from open to half-open state
     * Default: true
     */
    @NotNull
    private Boolean automaticTransitionEnabled = true;
    
    /**
     * Enable circuit breaker metrics collection
     * Default: true
     */
    @NotNull
    private Boolean metricsEnabled = true;
    
    /**
     * Circuit breaker event logging enabled
     * Default: true
     */
    @NotNull
    private Boolean eventLoggingEnabled = true;
}