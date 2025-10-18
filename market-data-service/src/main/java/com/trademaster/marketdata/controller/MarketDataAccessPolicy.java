package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.controller.MarketDataConstants.TimeWindows;
import com.trademaster.marketdata.security.SubscriptionTierValidator;
import com.trademaster.marketdata.service.MarketDataCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Market Data Access Policy
 *
 * Single Responsibility: Encapsulate business rules for market data access control
 * Following Rule #2 (SRP) and Rule #3 (Functional Programming)
 *
 * Responsibilities:
 * - Validate realtime data access based on subscription tier
 * - Validate historical date range access
 * - Check if data is considered "realtime" based on age threshold
 * - Apply tiered access control policies
 *
 * Benefits:
 * - Centralizes all access control business logic
 * - Easy to test independently
 * - Clear separation from HTTP and orchestration concerns
 * - Functional programming patterns (no if-else, immutable)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MarketDataAccessPolicy {

    private final SubscriptionTierValidator tierValidator;

    /**
     * Check if user can access realtime data based on tier and data freshness
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Helper method
     */
    public boolean canAccessRealtimeData(
            MarketDataCacheService.CachedPrice price, UserDetails userDetails) {
        return !tierValidator.isFreeTier(userDetails) || !isRealtimeData(price.marketTime());
    }

    /**
     * Check if data timestamp qualifies as "realtime" (within threshold)
     * Rule #3: Functional pattern, no if-else
     */
    public boolean isRealtimeData(Instant marketTime) {
        return marketTime.isAfter(Instant.now().minus(
            TimeWindows.REALTIME_DATA_THRESHOLD_MINUTES,
            TimeWindows.TIME_UNIT_MINUTES
        ));
    }

    /**
     * Check if cached price should trigger forbidden response for free tier
     * Rule #3: Functional pattern, no if-else
     */
    public boolean shouldRestrictRealtimeAccess(
            MarketDataCacheService.CachedPrice price, UserDetails userDetails) {
        return tierValidator.isFreeTier(userDetails) && isRealtimeData(price.marketTime());
    }

    /**
     * Validate if historical date range is allowed for user's subscription tier
     * Returns empty Optional if allowed, error Optional if not allowed
     * Rule #3: Functional pattern with Optional
     */
    public Optional<String> validateHistoricalDateRange(
            UserDetails userDetails, Instant from, Instant to) {
        return Optional.of(tierValidator.isHistoricalRangeAllowed(userDetails, from, to))
            .filter(allowed -> !allowed)
            .map(notAllowed -> "Historical date range exceeds subscription tier limit");
    }

    /**
     * Validate if bulk request symbol count is allowed for user's subscription tier
     * Returns empty Optional if allowed, error Optional if not allowed
     * Rule #3: Functional pattern with Optional
     */
    public Optional<String> validateBulkRequestSize(
            UserDetails userDetails, int symbolCount) {
        return Optional.of(tierValidator.isBulkRequestAllowed(userDetails, symbolCount))
            .filter(allowed -> !allowed)
            .map(notAllowed -> "Symbol count exceeds subscription tier limit");
    }
}
