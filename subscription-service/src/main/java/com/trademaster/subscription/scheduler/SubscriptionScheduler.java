package com.trademaster.subscription.scheduler;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.event.SubscriptionEvent;
import com.trademaster.subscription.event.SubscriptionEventPublisher;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.BillingService;
import com.trademaster.subscription.service.StructuredLoggingService;
import com.trademaster.subscription.service.UsageTrackingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Subscription Scheduler
 * 
 * Handles scheduled tasks for subscription management including:
 * - Recurring billing processing
 * - Trial expiration checks
 * - Subscription cleanup
 * - Usage reset
 * - Health monitoring
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingService billingService;
    private final UsageTrackingService usageTrackingService;
    private final SubscriptionEventPublisher eventPublisher;
    private final StructuredLoggingService loggingService;
    private final MeterRegistry meterRegistry;

    @Value("${app.scheduler.billing.enabled:true}")
    private boolean billingSchedulerEnabled;

    @Value("${app.scheduler.trial-expiration.enabled:true}")
    private boolean trialExpirationEnabled;

    @Value("${app.scheduler.usage-reset.enabled:true}")
    private boolean usageResetEnabled;

    @Value("${app.scheduler.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.subscription.trial-warning-days:3}")
    private int trialWarningDays;

    @Value("${app.subscription.grace-period-days:7}")
    private int gracePeriodDays;

    // Metrics
    private final Counter billingProcessedCounter;
    private final Counter billingFailedCounter;
    private final Counter trialExpiredCounter;
    private final Counter cleanupProcessedCounter;
    private final AtomicLong lastBillingRun = new AtomicLong(0);
    private final AtomicLong lastUsageReset = new AtomicLong(0);

    public SubscriptionScheduler(SubscriptionRepository subscriptionRepository,
                                BillingService billingService,
                                UsageTrackingService usageTrackingService,
                                SubscriptionEventPublisher eventPublisher,
                                StructuredLoggingService loggingService,
                                MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingService = billingService;
        this.usageTrackingService = usageTrackingService;
        this.eventPublisher = eventPublisher;
        this.loggingService = loggingService;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.billingProcessedCounter = Counter.builder("subscription.scheduler.billing.processed")
            .description("Number of billing operations processed by scheduler")
            .register(meterRegistry);
            
        this.billingFailedCounter = Counter.builder("subscription.scheduler.billing.failed")
            .description("Number of billing operations failed in scheduler")
            .register(meterRegistry);
            
        this.trialExpiredCounter = Counter.builder("subscription.scheduler.trials.expired")
            .description("Number of trial subscriptions expired by scheduler")
            .register(meterRegistry);
            
        this.cleanupProcessedCounter = Counter.builder("subscription.scheduler.cleanup.processed")
            .description("Number of cleanup operations processed by scheduler")
            .register(meterRegistry);
        
        // Register gauges for monitoring
        Gauge.builder("subscription.scheduler.billing.last.run")
            .description("Timestamp of last billing run")
            .register(meterRegistry, this, s -> s.lastBillingRun.get());
            
        Gauge.builder("subscription.scheduler.usage.reset.last.run")
            .description("Timestamp of last usage reset run")
            .register(meterRegistry, this, s -> s.lastUsageReset.get());
    }

    /**
     * Process recurring billing - runs every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public void processRecurringBilling() {
        if (!billingSchedulerEnabled) {
            return;
        }

        log.info("Starting recurring billing processing");
        lastBillingRun.set(System.currentTimeMillis());
        
        try {
            List<Subscription> dueSubscriptions = subscriptionRepository
                .findByNextBillingDateBeforeAndStatus(LocalDateTime.now(), SubscriptionStatus.ACTIVE);
            
            log.info("Found {} subscriptions due for billing", dueSubscriptions.size());
            
            for (Subscription subscription : dueSubscriptions) {
                try {
                    CompletableFuture<Boolean> billingResult = billingService
                        .processRecurringBilling(subscription.getId());
                    
                    billingResult.thenAccept(success -> {
                        if (success) {
                            billingProcessedCounter.increment();
                            log.info("Successfully processed billing for subscription: {}", subscription.getId());
                        } else {
                            billingFailedCounter.increment();
                            log.warn("Failed to process billing for subscription: {}", subscription.getId());
                        }
                    });
                    
                } catch (Exception e) {
                    billingFailedCounter.increment();
                    log.error("Error processing billing for subscription: {}", subscription.getId(), e);
                    
                    loggingService.logError(
                        "recurring_billing_error",
                        e.getMessage(),
                        "BILLING_ERROR",
                        e,
                        Map.of("subscriptionId", subscription.getId().toString())
                    );
                }
            }
            
            loggingService.logSystemEvent(
                "recurring_billing_completed",
                "info",
                "Recurring billing processing completed",
                "SCHEDULER",
                Map.of("subscriptionsProcessed", dueSubscriptions.size())
            );
            
        } catch (Exception e) {
            log.error("Error in recurring billing scheduler", e);
            loggingService.logError(
                "recurring_billing_scheduler_error",
                e.getMessage(),
                "SCHEDULER_ERROR",
                e,
                null
            );
        }
    }

    /**
     * Check for trial expirations - runs twice daily
     */
    @Scheduled(cron = "0 0 8,20 * * *") // 8 AM and 8 PM daily
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public void checkTrialExpirations() {
        if (!trialExpirationEnabled) {
            return;
        }

        log.info("Checking for trial expirations");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningThreshold = now.plusDays(trialWarningDays);
            
            // Check for trials ending soon
            List<Subscription> trialsEndingSoon = subscriptionRepository
                .findTrialsEndingBetween(now, warningThreshold);
            
            for (Subscription subscription : trialsEndingSoon) {
                try {
                    eventPublisher.publishNotificationEvent(
                        SubscriptionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType(SubscriptionEvent.TRIAL_ENDING_SOON)
                            .subscriptionId(subscription.getId())
                            .userId(subscription.getUserId())
                            .tier(subscription.getTier())
                            .status(subscription.getStatus())
                            .timestamp(now)
                            .source("subscription-service")
                            .version("1.0")
                            .payload(Map.of(
                                "trialEndDate", subscription.getTrialEndDate().toString(),
                                "daysRemaining", java.time.temporal.ChronoUnit.DAYS.between(now, subscription.getTrialEndDate())
                            ))
                            .build()
                    );
                    
                } catch (Exception e) {
                    log.error("Error publishing trial ending warning for subscription: {}", subscription.getId(), e);
                }
            }
            
            // Check for expired trials
            List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(now);
            
            for (Subscription subscription : expiredTrials) {
                try {
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    subscription.setEndDate(now);
                    subscriptionRepository.save(subscription);
                    
                    trialExpiredCounter.increment();
                    
                    eventPublisher.publishSubscriptionEvent(
                        SubscriptionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType(SubscriptionEvent.TRIAL_EXPIRED)
                            .subscriptionId(subscription.getId())
                            .userId(subscription.getUserId())
                            .tier(subscription.getTier())
                            .status(SubscriptionStatus.EXPIRED)
                            .previousStatus(SubscriptionStatus.TRIAL)
                            .timestamp(now)
                            .source("subscription-service")
                            .version("1.0")
                            .payload(Map.of("trialEndDate", subscription.getTrialEndDate().toString()))
                            .build()
                    );
                    
                    log.info("Expired trial subscription: {}", subscription.getId());
                    
                } catch (Exception e) {
                    log.error("Error expiring trial subscription: {}", subscription.getId(), e);
                }
            }
            
            loggingService.logSystemEvent(
                "trial_expiration_check_completed",
                "info",
                "Trial expiration check completed",
                "SCHEDULER",
                Map.of(
                    "trialsEndingSoon", trialsEndingSoon.size(),
                    "expiredTrials", expiredTrials.size()
                )
            );
            
        } catch (Exception e) {
            log.error("Error in trial expiration scheduler", e);
            loggingService.logError(
                "trial_expiration_scheduler_error",
                e.getMessage(),
                "SCHEDULER_ERROR",
                e,
                null
            );
        }
    }

    /**
     * Reset monthly usage - runs on first day of each month at 2 AM
     */
    @Scheduled(cron = "0 0 2 1 * *") // 2 AM on 1st day of each month
    @Async("subscriptionProcessingExecutor")
    public void resetMonthlyUsage() {
        if (!usageResetEnabled) {
            return;
        }

        log.info("Starting monthly usage reset");
        lastUsageReset.set(System.currentTimeMillis());
        
        try {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            
            usageTrackingService.resetMonthlyUsage(lastMonth)
                .thenRun(() -> {
                    loggingService.logSystemEvent(
                        "monthly_usage_reset_completed",
                        "info",
                        "Monthly usage reset completed",
                        "SCHEDULER",
                        Map.of("period", lastMonth.toString())
                    );
                    
                    log.info("Monthly usage reset completed for period: {}", lastMonth);
                })
                .exceptionally(throwable -> {
                    log.error("Error during monthly usage reset", throwable);
                    loggingService.logError(
                        "monthly_usage_reset_error",
                        throwable.getMessage(),
                        "SCHEDULER_ERROR",
                        (Exception) throwable,
                        Map.of("period", lastMonth.toString())
                    );
                    return null;
                });
                
        } catch (Exception e) {
            log.error("Error in monthly usage reset scheduler", e);
            loggingService.logError(
                "monthly_usage_reset_scheduler_error",
                e.getMessage(),
                "SCHEDULER_ERROR",
                e,
                null
            );
        }
    }

    /**
     * Cleanup old data - runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public void cleanupOldData() {
        if (!cleanupEnabled) {
            return;
        }

        log.info("Starting subscription data cleanup");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
            
            // Clean up old cancelled subscriptions
            List<Subscription> oldCancelledSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.CANCELLED, cutoffDate);
            
            int cleanedUp = 0;
            
            for (Subscription subscription : oldCancelledSubscriptions) {
                try {
                    // Archive subscription data (in real implementation, move to archive table)
                    subscription.setArchivedAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                    cleanedUp++;
                    
                } catch (Exception e) {
                    log.error("Error archiving subscription: {}", subscription.getId(), e);
                }
            }
            
            cleanupProcessedCounter.increment(cleanedUp);
            
            loggingService.logSystemEvent(
                "cleanup_completed",
                "info",
                "Subscription cleanup completed",
                "SCHEDULER",
                Map.of("subscriptionsArchived", cleanedUp)
            );
            
            log.info("Cleanup completed, archived {} subscriptions", cleanedUp);
            
        } catch (Exception e) {
            log.error("Error in cleanup scheduler", e);
            loggingService.logError(
                "cleanup_scheduler_error",
                e.getMessage(),
                "SCHEDULER_ERROR",
                e,
                null
            );
        }
    }

    /**
     * Health monitoring - runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Async("subscriptionProcessingExecutor")
    public void healthMonitoring() {
        try {
            // Check subscription service health metrics
            long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
            long trialSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
            long suspendedSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);
            long failedPaymentSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.PAYMENT_FAILED);
            
            // Update health metrics
            Gauge.builder("subscription.count.active")
                .description("Number of active subscriptions")
                .register(meterRegistry, activeSubscriptions, count -> count);
                
            Gauge.builder("subscription.count.trial")
                .description("Number of trial subscriptions")
                .register(meterRegistry, trialSubscriptions, count -> count);
                
            Gauge.builder("subscription.count.suspended")
                .description("Number of suspended subscriptions")
                .register(meterRegistry, suspendedSubscriptions, count -> count);
                
            Gauge.builder("subscription.count.payment.failed")
                .description("Number of subscriptions with failed payments")
                .register(meterRegistry, failedPaymentSubscriptions, count -> count);
            
            // Log health status
            if (failedPaymentSubscriptions > (activeSubscriptions * 0.05)) { // More than 5% failed payments
                loggingService.logSystemEvent(
                    "health_alert_high_payment_failures",
                    "warning",
                    "High payment failure rate detected",
                    "SCHEDULER",
                    Map.of("failedPaymentCount", failedPaymentSubscriptions, "activeCount", activeSubscriptions)
                );
            }
            
        } catch (Exception e) {
            log.error("Error in health monitoring", e);
        }
    }

    /**
     * Get scheduler status
     */
    public Map<String, Object> getSchedulerStatus() {
        return Map.of(
            "billingEnabled", billingSchedulerEnabled,
            "trialExpirationEnabled", trialExpirationEnabled,
            "usageResetEnabled", usageResetEnabled,
            "cleanupEnabled", cleanupEnabled,
            "lastBillingRun", lastBillingRun.get(),
            "lastUsageReset", lastUsageReset.get()
        );
    }
}