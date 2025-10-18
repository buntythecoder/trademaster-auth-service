package com.trademaster.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Circuit Breaker Configuration Properties
 *
 * Externalizes circuit breaker configuration following Rule #16: Dynamic Configuration
 * All values can be overridden via application.yml or environment variables
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "trademaster.circuit-breaker")
@Data
@Validated
public class CircuitBreakerProperties {

    /**
     * Failure rate threshold (percentage)
     * Circuit opens when failure rate exceeds this threshold
     * Default: 50% failure rate
     */
    @Min(1)
    @Max(100)
    private int failureRateThreshold = 50;

    /**
     * Slow call rate threshold (percentage)
     * Circuit opens when slow call rate exceeds this threshold
     * Default: 50% slow calls
     */
    @Min(1)
    @Max(100)
    private int slowCallRateThreshold = 50;

    /**
     * Slow call duration threshold (milliseconds)
     * Calls slower than this are considered slow
     * Default: 2000ms (2 seconds)
     */
    @Min(100)
    private long slowCallDurationMs = 2000;

    /**
     * Wait duration in open state (milliseconds)
     * How long circuit stays open before transitioning to half-open
     * Default: 60000ms (1 minute)
     */
    @Min(1000)
    private long waitDurationInOpenStateMs = 60000;

    /**
     * Sliding window size
     * Number of calls used to calculate failure rate
     * Default: 100 calls
     */
    @Min(10)
    @Max(1000)
    private int slidingWindowSize = 100;

    /**
     * Minimum number of calls
     * Minimum calls before circuit breaker calculates failure rate
     * Default: 10 calls
     */
    @Min(1)
    @Max(100)
    private int minimumNumberOfCalls = 10;

    /**
     * Permitted calls in half-open state
     * Number of calls allowed when testing if service recovered
     * Default: 5 calls
     */
    @Min(1)
    @Max(50)
    private int permittedNumberOfCallsInHalfOpenState = 5;

    /**
     * Enable/disable circuit breaker globally
     * Default: true (enabled)
     */
    private boolean enabled = true;
}
