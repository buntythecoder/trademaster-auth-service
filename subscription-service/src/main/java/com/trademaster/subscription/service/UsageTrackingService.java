package com.trademaster.subscription.service;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.enums.SubscriptionLimits;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.exception.SubscriptionNotFoundException;
import com.trademaster.subscription.exception.UsageLimitExceededException;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Usage Tracking Service
 * 
 * Provides comprehensive usage tracking and limit enforcement for subscription tiers.
 * Uses Virtual Threads for high-performance concurrent operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageTrackingService {

    private final SubscriptionRepository subscriptionRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final StructuredLoggingService loggingService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter usageCheckCounter;
    private final Counter usageIncrementCounter;
    private final Counter limitExceededCounter;
    private final Timer usageCheckTimer;
    private final Timer usageIncrementTimer;

    public UsageTrackingService(SubscriptionRepository subscriptionRepository,
                               UsageTrackingRepository usageTrackingRepository,
                               StructuredLoggingService loggingService,
                               MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageTrackingRepository = usageTrackingRepository;
        this.loggingService = loggingService;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.usageCheckCounter = Counter.builder("subscription.usage.checks")
            .description("Number of usage checks performed")
            .register(meterRegistry);
        
        this.usageIncrementCounter = Counter.builder("subscription.usage.increments")
            .description("Number of usage increments")
            .register(meterRegistry);
            
        this.limitExceededCounter = Counter.builder("subscription.limits.exceeded")
            .description("Number of times usage limits were exceeded")
            .register(meterRegistry);
        
        this.usageCheckTimer = Timer.builder("subscription.usage.check.duration")
            .description("Time taken to check usage limits")
            .register(meterRegistry);
            
        this.usageIncrementTimer = Timer.builder("subscription.usage.increment.duration")
            .description("Time taken to increment usage")
            .register(meterRegistry);
        
        // Register gauges for active usage tracking
        Gauge.builder("subscription.usage.active.tracks")
            .description("Number of active usage tracking records")
            .register(meterRegistry, this, UsageTrackingService::getActiveUsageTrackingCount);
    }

    /**
     * Check if a user has access to a feature based on their subscription limits
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Boolean> checkFeatureAccess(UUID userId, String featureName) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                usageCheckCounter.increment();
                
                Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
                    .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found for user: " + userId));

                if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                    loggingService.logBusinessEvent(
                        "feature_access_denied",
                        userId.toString(),
                        subscription.getId().toString(),
                        "subscription_inactive",
                        null
                    );
                    return false;
                }

                SubscriptionLimits limits = subscription.getTier().getLimits();
                Long featureLimit = getFeatureLimit(limits, featureName);
                
                if (featureLimit == null || featureLimit == -1) {
                    // Unlimited access
                    return true;
                }

                UsageTracking currentUsage = getCurrentUsageTracking(userId, featureName);
                boolean hasAccess = currentUsage.getUsageCount() < featureLimit;

                if (!hasAccess) {
                    limitExceededCounter.increment();
                    loggingService.logBusinessEvent(
                        "feature_limit_exceeded",
                        userId.toString(),
                        subscription.getId().toString(),
                        featureName,
                        null
                    );
                }

                loggingService.logBusinessEvent(
                    "feature_access_check",
                    userId.toString(),
                    subscription.getId().toString(),
                    featureName,
                    null
                );

                return hasAccess;
                
            } catch (Exception e) {
                log.error("Error checking feature access for user: {} and feature: {}", userId, featureName, e);
                throw e;
            } finally {
                sample.stop(usageCheckTimer);
            }
        });
    }

    /**
     * Increment usage for a specific feature
     */
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> incrementUsage(UUID userId, String featureName, Long incrementBy) {
        return CompletableFuture.runAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                usageIncrementCounter.increment();
                
                Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
                    .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found for user: " + userId));

                SubscriptionLimits limits = subscription.getTier().getLimits();
                Long featureLimit = getFeatureLimit(limits, featureName);
                
                UsageTracking usageTracking = getCurrentUsageTracking(userId, featureName);
                Long newUsageCount = usageTracking.getUsageCount() + incrementBy;
                
                // Check if increment would exceed limit
                if (featureLimit != null && featureLimit != -1 && newUsageCount > featureLimit) {
                    throw new UsageLimitExceededException(
                        userId, 
                        featureName, 
                        newUsageCount, 
                        featureLimit
                    );
                }
                
                // Update usage
                usageTracking.setUsageCount(newUsageCount);
                usageTracking.setLastUpdated(LocalDateTime.now());
                usageTrackingRepository.save(usageTracking);

                loggingService.logBusinessEvent(
                    "usage_incremented",
                    userId.toString(),
                    subscription.getId().toString(),
                    featureName,
                    null
                );

                log.info("Usage incremented for user: {}, feature: {}, new count: {}", 
                        userId, featureName, newUsageCount);
                        
            } catch (Exception e) {
                log.error("Error incrementing usage for user: {} and feature: {}", userId, featureName, e);
                throw e;
            } finally {
                sample.stop(usageIncrementTimer);
            }
        });
    }

    /**
     * Get current usage for a user and feature
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Long> getCurrentUsage(UUID userId, String featureName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UsageTracking usageTracking = getCurrentUsageTracking(userId, featureName);
                return usageTracking.getUsageCount();
            } catch (Exception e) {
                log.error("Error getting current usage for user: {} and feature: {}", userId, featureName, e);
                return 0L;
            }
        });
    }

    /**
     * Get usage statistics for a user
     */
    @Transactional(readOnly = true)
    public CompletableFuture<List<UsageTracking>> getUserUsageStats(UUID userId, LocalDate month) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return usageTrackingRepository.findByUserIdAndUsagePeriod(userId, month);
            } catch (Exception e) {
                log.error("Error getting usage stats for user: {} and month: {}", userId, month, e);
                return List.of();
            }
        });
    }

    /**
     * Reset usage for a specific period (called monthly)
     */
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> resetMonthlyUsage(LocalDate period) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<UsageTracking> usageRecords = usageTrackingRepository.findByUsagePeriod(period);
                
                for (UsageTracking usage : usageRecords) {
                    usage.setUsageCount(0L);
                    usage.setLastUpdated(LocalDateTime.now());
                }
                
                usageTrackingRepository.saveAll(usageRecords);
                
                loggingService.logSystemEvent(
                    "monthly_usage_reset",
                    "info",
                    "Monthly usage reset completed",
                    "SYSTEM",
                    null
                );

                log.info("Monthly usage reset completed for period: {}, records updated: {}", 
                        period, usageRecords.size());
                        
            } catch (Exception e) {
                log.error("Error resetting monthly usage for period: {}", period, e);
                loggingService.logError(
                    "monthly_usage_reset_error",
                    e.getMessage(),
                    "USAGE_RESET_ERROR",
                    e,
                    null
                );
                throw e;
            }
        });
    }

    /**
     * Get feature limit from subscription limits
     */
    private Long getFeatureLimit(SubscriptionLimits limits, String featureName) {
        return switch (featureName.toLowerCase()) {
            case "api_calls" -> limits.getApiCallsPerMonth();
            case "portfolios" -> limits.getMaxPortfolios();
            case "watchlists" -> limits.getMaxWatchlists();
            case "alerts" -> limits.getMaxAlerts();
            case "backtests" -> limits.getMaxBacktests();
            case "ai_insights" -> limits.getAiInsightsPerMonth();
            case "advanced_analytics" -> limits.getAdvancedAnalyticsAccess() ? -1L : 0L;
            case "priority_support" -> limits.getPrioritySupportAccess() ? -1L : 0L;
            case "data_export" -> limits.getDataExportAccess() ? -1L : 0L;
            case "api_access" -> limits.getApiAccess() ? -1L : 0L;
            default -> {
                log.warn("Unknown feature name: {}", featureName);
                yield 0L;
            }
        };
    }

    /**
     * Get or create usage tracking record
     */
    private UsageTracking getCurrentUsageTracking(UUID userId, String featureName) {
        LocalDate currentPeriod = LocalDate.now();
        
        return usageTrackingRepository.findByUserIdAndFeatureNameAndUsagePeriod(
            userId, featureName, currentPeriod
        ).orElseGet(() -> {
            UsageTracking newUsageTracking = new UsageTracking();
            newUsageTracking.setUserId(userId);
            newUsageTracking.setFeatureName(featureName);
            newUsageTracking.setUsagePeriod(currentPeriod);
            newUsageTracking.setUsageCount(0L);
            newUsageTracking.setLastUpdated(LocalDateTime.now());
            return usageTrackingRepository.save(newUsageTracking);
        });
    }

    /**
     * Get count of active usage tracking records for metrics
     */
    private double getActiveUsageTrackingCount() {
        try {
            return usageTrackingRepository.countByUsagePeriod(LocalDate.now()).doubleValue();
        } catch (Exception e) {
            log.warn("Error getting active usage tracking count", e);
            return 0.0;
        }
    }

    /**
     * Validate usage before performing an operation
     */
    @Transactional
    public CompletableFuture<Void> validateAndIncrementUsage(UUID userId, String featureName) {
        return checkFeatureAccess(userId, featureName)
            .thenCompose(hasAccess -> {
                if (!hasAccess) {
                    return CompletableFuture.failedFuture(
                        new UsageLimitExceededException(userId, featureName, "Usage limit exceeded")
                    );
                }
                return incrementUsage(userId, featureName, 1L);
            });
    }

    /**
     * Get comprehensive usage report for a user
     */
    @Transactional(readOnly = true)
    public CompletableFuture<List<UsageTracking>> getComprehensiveUsageReport(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return usageTrackingRepository.findByUserIdOrderByUsagePeriodDesc(userId);
            } catch (Exception e) {
                log.error("Error getting comprehensive usage report for user: {}", userId, e);
                return List.of();
            }
        });
    }
}