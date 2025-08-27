package com.trademaster.subscription.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MANDATORY Subscription Metrics Service
 * 
 * Provides Prometheus metrics for subscription management as per TradeMaster standards.
 * Tracks business metrics, performance metrics, and operational health.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class SubscriptionMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counters for business metrics
    private final Counter subscriptionsCreated = Counter.builder("subscriptions_created_total")
        .description("Total number of subscriptions created")
        .register(meterRegistry);
    
    private final Counter subscriptionsActivated = Counter.builder("subscriptions_activated_total")
        .description("Total number of subscriptions activated")
        .register(meterRegistry);
    
    private final Counter subscriptionsCancelled = Counter.builder("subscriptions_cancelled_total")
        .description("Total number of subscriptions cancelled")
        .register(meterRegistry);
    
    private final Counter subscriptionsUpgraded = Counter.builder("subscriptions_upgraded_total")
        .description("Total number of subscription upgrades")
        .register(meterRegistry);
    
    private final Counter subscriptionsDowngraded = Counter.builder("subscriptions_downgraded_total")
        .description("Total number of subscription downgrades")
        .register(meterRegistry);
    
    private final Counter billingSuccessful = Counter.builder("billing_successful_total")
        .description("Total number of successful billing attempts")
        .register(meterRegistry);
    
    private final Counter billingFailed = Counter.builder("billing_failed_total")
        .description("Total number of failed billing attempts")
        .register(meterRegistry);
    
    private final Counter usageLimitExceeded = Counter.builder("usage_limit_exceeded_total")
        .description("Total number of usage limit violations")
        .register(meterRegistry);
    
    // Timers for performance metrics
    private final Timer subscriptionProcessingTimer = Timer.builder("subscription_processing_duration_seconds")
        .description("Time taken to process subscription operations")
        .register(meterRegistry);
    
    private final Timer billingProcessingTimer = Timer.builder("billing_processing_duration_seconds")
        .description("Time taken to process billing operations")
        .register(meterRegistry);
    
    private final Timer usageCheckTimer = Timer.builder("usage_check_duration_seconds")
        .description("Time taken to check usage limits")
        .register(meterRegistry);
    
    // Gauges for current state
    private final AtomicLong activeSubscriptions = new AtomicLong(0);
    private final AtomicLong trialSubscriptions = new AtomicLong(0);
    private final AtomicLong suspendedSubscriptions = new AtomicLong(0);
    private final AtomicLong currentMRR = new AtomicLong(0);
    private final AtomicLong currentARR = new AtomicLong(0);
    
    public SubscriptionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register gauges
        Gauge.builder("active_subscriptions")
            .description("Current number of active subscriptions")
            .register(meterRegistry, activeSubscriptions, AtomicLong::doubleValue);
        
        Gauge.builder("trial_subscriptions")
            .description("Current number of trial subscriptions")
            .register(meterRegistry, trialSubscriptions, AtomicLong::doubleValue);
        
        Gauge.builder("suspended_subscriptions")
            .description("Current number of suspended subscriptions")
            .register(meterRegistry, suspendedSubscriptions, AtomicLong::doubleValue);
        
        Gauge.builder("monthly_recurring_revenue")
            .description("Current monthly recurring revenue in INR")
            .register(meterRegistry, currentMRR, AtomicLong::doubleValue);
        
        Gauge.builder("annual_recurring_revenue")
            .description("Current annual recurring revenue in INR")
            .register(meterRegistry, currentARR, AtomicLong::doubleValue);
    }
    
    // Business Metrics Recording Methods
    
    public void recordSubscriptionCreated(String tier) {
        subscriptionsCreated.increment(
            "tier", tier
        );
        log.debug("Subscription created metric recorded for tier: {}", tier);
    }
    
    public void recordSubscriptionActivated(String tier, String billingCycle) {
        subscriptionsActivated.increment(
            "tier", tier,
            "billing_cycle", billingCycle
        );
        log.debug("Subscription activated metric recorded for tier: {}, cycle: {}", tier, billingCycle);
    }
    
    public void recordSubscriptionCancelled(String tier, String reason) {
        subscriptionsCancelled.increment(
            "tier", tier,
            "reason", reason != null ? reason.toLowerCase().replaceAll("\\s+", "_") : "unknown"
        );
        log.debug("Subscription cancelled metric recorded for tier: {}, reason: {}", tier, reason);
    }
    
    public void recordSubscriptionUpgraded(String fromTier, String toTier) {
        subscriptionsUpgraded.increment(
            "from_tier", fromTier,
            "to_tier", toTier
        );
        log.debug("Subscription upgraded metric recorded from: {} to: {}", fromTier, toTier);
    }
    
    public void recordSubscriptionDowngraded(String fromTier, String toTier) {
        subscriptionsDowngraded.increment(
            "from_tier", fromTier,
            "to_tier", toTier
        );
        log.debug("Subscription downgraded metric recorded from: {} to: {}", fromTier, toTier);
    }
    
    public void recordBillingSuccessful(String tier, String billingCycle, long processingTimeMs) {
        billingSuccessful.increment(
            "tier", tier,
            "billing_cycle", billingCycle
        );
        
        billingProcessingTimer.record(Duration.ofMillis(processingTimeMs));
        
        log.debug("Billing successful metric recorded for tier: {}, cycle: {}, time: {}ms", 
                 tier, billingCycle, processingTimeMs);
    }
    
    public void recordBillingFailed(String tier, String billingCycle, String errorType) {
        billingFailed.increment(
            "tier", tier,
            "billing_cycle", billingCycle,
            "error_type", errorType
        );
        log.debug("Billing failed metric recorded for tier: {}, cycle: {}, error: {}", 
                 tier, billingCycle, errorType);
    }
    
    public void recordUsageLimitExceeded(String userId, String feature, String tier) {
        usageLimitExceeded.increment(
            "feature", feature,
            "tier", tier
        );
        log.debug("Usage limit exceeded metric recorded for user: {}, feature: {}, tier: {}", 
                 userId, feature, tier);
    }
    
    // Performance Metrics Recording Methods
    
    public Timer.Sample startSubscriptionProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSubscriptionProcessingTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("subscription_processing_duration_seconds")
            .description("Time taken to process subscription operations")
            .tag("operation", operation)
            .register(meterRegistry));
    }
    
    public Timer.Sample startBillingProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordBillingProcessingTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("billing_processing_duration_seconds")
            .description("Time taken to process billing operations")
            .tag("operation", operation)
            .register(meterRegistry));
    }
    
    public Timer.Sample startUsageCheckTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordUsageCheckTime(Timer.Sample sample, String feature) {
        sample.stop(Timer.builder("usage_check_duration_seconds")
            .description("Time taken to check usage limits")
            .tag("feature", feature)
            .register(meterRegistry));
    }
    
    // Gauge Update Methods
    
    public void updateActiveSubscriptions(long count) {
        activeSubscriptions.set(count);
        log.debug("Updated active subscriptions gauge to: {}", count);
    }
    
    public void updateTrialSubscriptions(long count) {
        trialSubscriptions.set(count);
        log.debug("Updated trial subscriptions gauge to: {}", count);
    }
    
    public void updateSuspendedSubscriptions(long count) {
        suspendedSubscriptions.set(count);
        log.debug("Updated suspended subscriptions gauge to: {}", count);
    }
    
    public void updateMonthlyRecurringRevenue(long mrr) {
        currentMRR.set(mrr);
        log.debug("Updated MRR gauge to: {}", mrr);
    }
    
    public void updateAnnualRecurringRevenue(long arr) {
        currentARR.set(arr);
        log.debug("Updated ARR gauge to: {}", arr);
    }
    
    // Custom Metrics for Subscription Analytics
    
    public void recordTrialConversion(String tier, boolean converted) {
        Counter.builder("trial_conversions_total")
            .description("Total number of trial conversions")
            .tag("tier", tier)
            .tag("converted", String.valueOf(converted))
            .register(meterRegistry)
            .increment();
        
        log.debug("Trial conversion recorded for tier: {}, converted: {}", tier, converted);
    }
    
    public void recordChurnEvent(String tier, String churnReason, long subscriptionAgeInDays) {
        Counter.builder("churn_events_total")
            .description("Total number of churn events")
            .tag("tier", tier)
            .tag("reason", churnReason != null ? churnReason.toLowerCase().replaceAll("\\s+", "_") : "unknown")
            .register(meterRegistry)
            .increment();
        
        // Record subscription age at churn for analysis
        Timer.builder("subscription_lifetime_days")
            .description("Subscription lifetime in days at churn")
            .tag("tier", tier)
            .register(meterRegistry)
            .record(Duration.ofDays(subscriptionAgeInDays));
        
        log.debug("Churn event recorded for tier: {}, reason: {}, age: {} days", 
                 tier, churnReason, subscriptionAgeInDays);
    }
    
    public void recordPriceChange(String tier, double oldPrice, double newPrice) {
        Counter.builder("price_changes_total")
            .description("Total number of price changes")
            .tag("tier", tier)
            .tag("change_type", newPrice > oldPrice ? "increase" : "decrease")
            .register(meterRegistry)
            .increment();
        
        log.debug("Price change recorded for tier: {}, old: {}, new: {}", tier, oldPrice, newPrice);
    }
}