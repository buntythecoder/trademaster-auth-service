package com.trademaster.payment.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MANDATORY Payment Metrics Service
 * 
 * Comprehensive Prometheus metrics for payment operations as per TradeMaster standards.
 * Provides business metrics, performance metrics, system health metrics, and security metrics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class PaymentMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Business Metrics - initialized in constructor
    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter refundsProcessed;
    private final Counter subscriptionsActivated;
    private final Counter subscriptionsCancelled;
    
    // Performance Metrics - initialized in constructor
    private final Timer paymentProcessingTime;
    private final Timer refundProcessingTime;
    private final Timer subscriptionProcessingTime;
    private final Timer webhookProcessingTime;
    
    // System Health Metrics - MANDATORY per standards
    private final AtomicInteger activePaymentSessions = new AtomicInteger(0);
    private final AtomicInteger databaseConnectionsActive = new AtomicInteger(0);
    private final AtomicInteger cacheHitCount = new AtomicInteger(0);
    private final AtomicInteger cacheMissCount = new AtomicInteger(0);
    
    // Security Metrics - initialized in constructor
    private final Counter securityIncidents;
    private final Counter rateLimitViolations;
    private final Counter suspiciousActivities;
    
    public PaymentMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize business metrics
        this.paymentsInitiated = Counter.builder("payments.initiated.total")
                .description("Total number of payment attempts")
                .register(meterRegistry);
        this.paymentsCompleted = Counter.builder("payments.completed.total")
                .description("Total number of successful payments")
                .register(meterRegistry);
        this.paymentsFailed = Counter.builder("payments.failed.total")
                .description("Total number of failed payments")
                .register(meterRegistry);
        this.refundsProcessed = Counter.builder("refunds.processed.total")
                .description("Total number of refunds processed")
                .register(meterRegistry);
        this.subscriptionsActivated = Counter.builder("subscriptions.activated.total")
                .description("Total number of subscriptions activated")
                .register(meterRegistry);
        this.subscriptionsCancelled = Counter.builder("subscriptions.cancelled.total")
                .description("Total number of subscriptions cancelled")
                .register(meterRegistry);
        
        // Initialize performance metrics
        this.paymentProcessingTime = Timer.builder("payments.processing.duration")
                .description("Time taken to process payments")
                .register(meterRegistry);
        this.refundProcessingTime = Timer.builder("refunds.processing.duration")
                .description("Time taken to process refunds")
                .register(meterRegistry);
        this.subscriptionProcessingTime = Timer.builder("subscriptions.processing.duration")
                .description("Time taken to process subscription operations")
                .register(meterRegistry);
        this.webhookProcessingTime = Timer.builder("webhooks.processing.duration")
                .description("Time taken to process webhooks")
                .register(meterRegistry);
        
        // Initialize security metrics
        this.securityIncidents = Counter.builder("security.incidents.total")
                .description("Total number of security incidents")
                .register(meterRegistry);
        this.rateLimitViolations = Counter.builder("security.rate_limit.violations.total")
                .description("Total number of rate limit violations")
                .register(meterRegistry);
        this.suspiciousActivities = Counter.builder("security.suspicious_activities.total")
                .description("Total number of suspicious activities detected")
                .register(meterRegistry);
        
        initializeGauges();
    }
    
    private void initializeGauges() {
        // System Health Gauges
        Gauge.builder("payments.sessions.active", activePaymentSessions, AtomicInteger::get)
                .description("Number of active payment sessions")
                .register(meterRegistry);
                
        Gauge.builder("database.connections.active", databaseConnectionsActive, AtomicInteger::get)
                .description("Number of active database connections")
                .register(meterRegistry);
                
        Gauge.builder("cache.hit_ratio", this, PaymentMetricsService::calculateCacheHitRatio)
                .description("Cache hit ratio")
                .register(meterRegistry);
    }
    
    // Business Metrics Recording Methods
    
    public void recordPaymentInitiated(String gateway, String paymentMethod) {
        Counter.builder("payments.initiated.total")
                .description("Total number of payment attempts")
                .tag("gateway", gateway)
                .tag("method", paymentMethod)
                .register(meterRegistry)
                .increment();
        
        activePaymentSessions.incrementAndGet();
        log.debug("Metrics: Payment initiated - gateway: {}, method: {}", gateway, paymentMethod);
    }
    
    public void recordPaymentCompleted(String gateway, String paymentMethod, String currency, long processingTimeMs) {
        Counter.builder("payments.completed.total")
                .description("Total number of successful payments")
                .tag("gateway", gateway)
                .tag("method", paymentMethod)
                .tag("currency", currency)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
        
        paymentProcessingTime.record(Duration.ofMillis(processingTimeMs));
        activePaymentSessions.decrementAndGet();
        log.debug("Metrics: Payment completed - gateway: {}, processing time: {}ms", gateway, processingTimeMs);
    }
    
    public void recordPaymentFailed(String gateway, String paymentMethod, String errorCode, long processingTimeMs) {
        Counter.builder("payments.failed.total")
                .description("Total number of failed payments")
                .tag("gateway", gateway)
                .tag("method", paymentMethod)
                .tag("error_code", errorCode)
                .tag("status", "failed")
                .register(meterRegistry)
                .increment();
        
        paymentProcessingTime.record(Duration.ofMillis(processingTimeMs));
        activePaymentSessions.decrementAndGet();
        log.debug("Metrics: Payment failed - gateway: {}, error: {}", gateway, errorCode);
    }
    
    public void recordRefundProcessed(String gateway, String refundType, long processingTimeMs) {
        Counter.builder("refunds.processed.total")
                .description("Total number of refunds processed")
                .tag("gateway", gateway)
                .tag("type", refundType)
                .tag("status", "processed")
                .register(meterRegistry)
                .increment();
        
        refundProcessingTime.record(Duration.ofMillis(processingTimeMs));
        log.debug("Metrics: Refund processed - gateway: {}, type: {}", gateway, refundType);
    }
    
    public void recordSubscriptionActivated(String planName, String billingCycle, long processingTimeMs) {
        Counter.builder("subscriptions.activated.total")
                .description("Total number of subscriptions activated")
                .tag("plan", planName)
                .tag("billing_cycle", billingCycle)
                .tag("status", "activated")
                .register(meterRegistry)
                .increment();
        
        subscriptionProcessingTime.record(Duration.ofMillis(processingTimeMs));
        log.debug("Metrics: Subscription activated - plan: {}, cycle: {}", planName, billingCycle);
    }
    
    public void recordSubscriptionCancelled(String planName, String reason, long processingTimeMs) {
        Counter.builder("subscriptions.cancelled.total")
                .description("Total number of subscriptions cancelled")
                .tag("plan", planName)
                .tag("reason", reason)
                .tag("status", "cancelled")
                .register(meterRegistry)
                .increment();
        
        subscriptionProcessingTime.record(Duration.ofMillis(processingTimeMs));
        log.debug("Metrics: Subscription cancelled - plan: {}, reason: {}", planName, reason);
    }
    
    // Security Metrics Recording Methods
    
    public void recordSecurityIncident(String incidentType, String severity, String source) {
        Counter.builder("security.incidents.total")
                .description("Total number of security incidents")
                .tag("type", incidentType)
                .tag("severity", severity)
                .tag("source", source)
                .register(meterRegistry)
                .increment();
        
        log.warn("Metrics: Security incident - type: {}, severity: {}", incidentType, severity);
    }
    
    public void recordRateLimitViolation(String endpoint, String userId, String ipAddress) {
        Counter.builder("security.rate_limit.violations.total")
                .description("Total number of rate limit violations")
                .tag("endpoint", endpoint)
                .tag("user_id", userId != null ? "authenticated" : "anonymous")
                .tag("source", "rate_limiter")
                .register(meterRegistry)
                .increment();
        
        log.warn("Metrics: Rate limit violation - endpoint: {}, user: {}, ip: {}", endpoint, userId, ipAddress);
    }
    
    public void recordSuspiciousActivity(String activityType, String userId, String details) {
        Counter.builder("security.suspicious_activities.total")
                .description("Total number of suspicious activities detected")
                .tag("type", activityType)
                .tag("user_id", userId != null ? "authenticated" : "anonymous")
                .register(meterRegistry)
                .increment();
        
        log.warn("Metrics: Suspicious activity - type: {}, user: {}, details: {}", activityType, userId, details);
    }
    
    // Webhook Processing Metrics
    
    public void recordWebhookProcessed(String gateway, String eventType, boolean successful, long processingTimeMs) {
        Counter.builder("webhooks.processed.total")
                .description("Total number of webhooks processed")
                .tag("gateway", gateway)
                .tag("event_type", eventType)
                .tag("status", successful ? "success" : "failed")
                .register(meterRegistry)
                .increment();
        
        webhookProcessingTime.record(Duration.ofMillis(processingTimeMs));
        log.debug("Metrics: Webhook processed - gateway: {}, event: {}, success: {}", gateway, eventType, successful);
    }
    
    // System Health Metrics
    
    public void recordDatabaseConnectionActive() {
        databaseConnectionsActive.incrementAndGet();
    }
    
    public void recordDatabaseConnectionReleased() {
        databaseConnectionsActive.decrementAndGet();
    }
    
    public void recordCacheHit() {
        cacheHitCount.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMissCount.incrementAndGet();
    }
    
    private double calculateCacheHitRatio() {
        int hits = cacheHitCount.get();
        int misses = cacheMissCount.get();
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    // Performance Tracking Helpers
    
    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }
    
    public Timer.Sample startRefundTimer() {
        return Timer.start(meterRegistry);
    }
    
    public Timer.Sample startWebhookTimer() {
        return Timer.start(meterRegistry);
    }
}