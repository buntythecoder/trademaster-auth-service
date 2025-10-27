package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Risk Assessment Service - Evaluates security risks
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {

    // Risk assessment constants
    private static final int MAX_RISK_SCORE = 100;
    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MEDIUM_RISK_THRESHOLD = 40;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int TIMING_RISK_THRESHOLD_MINUTES = 60;

    // Thread-safe rate limiting
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastRequestTime = new ConcurrentHashMap<>();

    /**
     * Assess risk asynchronously using Virtual Threads
     *
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<SecurityResult<SecurityContext>> assess(SecurityContext context) {
        return CompletableFuture.supplyAsync(
            () -> assessSync(context),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Assess risk synchronously using pattern matching
     *
     * MANDATORY: No if-else - Rule #3
     * MANDATORY: Pattern matching - Rule #14
     */
    public SecurityResult<SecurityContext> assessSync(SecurityContext context) {
        log.debug("Assessing risk for correlation: {}", context.correlationId());

        int riskScore = calculateRiskScore(context);

        return switch (classifyRiskLevel(riskScore)) {
            case LOW -> SecurityResult.success(context);
            case MEDIUM -> {
                log.warn("Medium risk detected: correlation={}, score={}",
                    context.correlationId(), riskScore);
                yield SecurityResult.success(context);
            }
            case HIGH -> {
                log.error("High risk detected: correlation={}, score={}",
                    context.correlationId(), riskScore);
                yield SecurityResult.failure(SecurityError.RISK_TOO_HIGH,
                    String.format("Risk score %d exceeds threshold", riskScore));
            }
            case RATE_LIMITED -> SecurityResult.failure(SecurityError.RATE_LIMIT_EXCEEDED,
                "Rate limit exceeded");
        };
    }

    /**
     * Calculate risk score using functional composition
     */
    private int calculateRiskScore(SecurityContext context) {
        return Stream.of(
                assessIpRisk(context.ipAddress()),
                assessRateLimitRisk(context.userId()),
                assessTimingRisk(context.timestamp()),
                assessUserAgentRisk(context.userAgent())
            )
            .mapToInt(Integer::intValue)
            .sum();
    }

    /**
     * IP-based risk assessment using functional approach
     */
    private int assessIpRisk(String ipAddress) {
        return Optional.ofNullable(ipAddress)
            .filter(ip -> !ip.trim().isEmpty())
            .map(this::isPrivateIp)
            .map(isPrivate -> isPrivate ? 5 : 15)
            .orElse(20);
    }

    /**
     * Rate limiting risk assessment using functional patterns
     */
    private int assessRateLimitRisk(String userId) {
        return Optional.ofNullable(userId)
            .map(id -> "rate_limit_" + id)
            .map(this::checkRateLimit)
            .orElse(10);
    }

    /**
     * Timing risk assessment using functional approach
     */
    private int assessTimingRisk(LocalDateTime timestamp) {
        return Optional.ofNullable(timestamp)
            .map(ts -> java.time.Duration.between(ts, LocalDateTime.now()).toMinutes())
            .map(this::calculateTimingRiskScore)
            .orElse(10);
    }

    /**
     * User agent risk assessment using functional patterns
     */
    private int assessUserAgentRisk(String userAgent) {
        return Optional.ofNullable(userAgent)
            .map(String::trim)
            .filter(ua -> !ua.isEmpty())
            .map(ua -> 0)
            .orElse(5);
    }

    /**
     * Check rate limit using functional approach
     */
    private int checkRateLimit(String key) {
        AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        LocalDateTime lastRequest = lastRequestTime.get(key);
        LocalDateTime now = LocalDateTime.now();

        // Functional reset counter logic
        Optional.ofNullable(lastRequest)
            .filter(request -> request.isBefore(now.minusMinutes(1)))
            .ifPresentOrElse(
                request -> {
                    count.set(0);
                    lastRequestTime.put(key, now);
                },
                () -> lastRequestTime.putIfAbsent(key, now)
            );

        int currentCount = count.incrementAndGet();
        return currentCount > MAX_REQUESTS_PER_MINUTE ? MAX_RISK_SCORE :
               (currentCount * MAX_RISK_SCORE / MAX_REQUESTS_PER_MINUTE);
    }

    /**
     * Timing risk calculation using functional pattern
     */
    private int calculateTimingRiskScore(long minutesAgo) {
        return Optional.of(minutesAgo)
            .filter(minutes -> minutes >= 0 && minutes <= TIMING_RISK_THRESHOLD_MINUTES)
            .map(minutes -> 0)
            .orElse(15);
    }

    /**
     * Private IP check using functional approach
     */
    private boolean isPrivateIp(String ip) {
        return Optional.ofNullable(ip)
            .filter(address ->
                address.startsWith("192.168.") ||
                address.startsWith("10.") ||
                address.startsWith("172.") ||
                address.equals("127.0.0.1")
            )
            .isPresent();
    }

    /**
     * Risk level classification using functional approach with Map-based threshold lookup
     */
    private RiskLevel classifyRiskLevel(int riskScore) {
        return java.util.stream.Stream.of(
                Map.entry(MAX_RISK_SCORE, RiskLevel.RATE_LIMITED),
                Map.entry(HIGH_RISK_THRESHOLD, RiskLevel.HIGH),
                Map.entry(MEDIUM_RISK_THRESHOLD, RiskLevel.MEDIUM)
            )
            .filter(entry -> riskScore >= entry.getKey())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(RiskLevel.LOW);
    }

    /**
     * Risk level enumeration for pattern matching
     */
    private enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        RATE_LIMITED
    }
}