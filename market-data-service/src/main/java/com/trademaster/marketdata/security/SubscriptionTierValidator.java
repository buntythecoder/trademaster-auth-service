package com.trademaster.marketdata.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Subscription Tier Validation and Access Control
 * 
 * Features:
 * - Tier-based data access control
 * - Real-time vs delayed data authorization
 * - Historical data range restrictions
 * - Bulk request limitations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionTierValidator {

    /**
     * Data access types requiring tier validation
     */
    public enum DataAccess {
        CURRENT_PRICE("Current price data"),
        HISTORICAL_DATA("Historical data access"),
        ORDER_BOOK("Order book data"),
        BULK_DATA("Bulk data requests"),
        SYMBOL_LIST("Symbol list access"),
        MARKET_STATS("Market statistics"),
        REAL_TIME_FEED("Real-time data feed");

        private final String description;

        DataAccess(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Subscription tiers with capabilities
     */
    public enum SubscriptionTier {
        FREE("Free", false, false, false, 10, 7, 100),
        SMART_TRADER("Smart Trader", true, true, false, 50, 30, 1000),
        PROFESSIONAL("Professional", true, true, true, 200, 365, 5000),
        INSTITUTIONAL("Institutional", true, true, true, 1000, 1825, -1);

        private final String displayName;
        private final boolean realtimeAccess;
        private final boolean historicalAccess;
        private final boolean orderBookAccess;
        private final int maxSymbolsPerRequest;
        private final int maxHistoricalDays;
        private final int dailyRequestLimit;

        SubscriptionTier(String displayName, boolean realtimeAccess, boolean historicalAccess,
                        boolean orderBookAccess, int maxSymbolsPerRequest, int maxHistoricalDays,
                        int dailyRequestLimit) {
            this.displayName = displayName;
            this.realtimeAccess = realtimeAccess;
            this.historicalAccess = historicalAccess;
            this.orderBookAccess = orderBookAccess;
            this.maxSymbolsPerRequest = maxSymbolsPerRequest;
            this.maxHistoricalDays = maxHistoricalDays;
            this.dailyRequestLimit = dailyRequestLimit;
        }

        public String getDisplayName() { return displayName; }
        public boolean hasRealtimeAccess() { return realtimeAccess; }
        public boolean hasHistoricalAccess() { return historicalAccess; }
        public boolean hasOrderBookAccess() { return orderBookAccess; }
        public int getMaxSymbolsPerRequest() { return maxSymbolsPerRequest; }
        public int getMaxHistoricalDays() { return maxHistoricalDays; }
        public int getDailyRequestLimit() { return dailyRequestLimit; }
    }

    /**
     * Validate and execute operation based on subscription tier
     */
    public <T> T validateAndExecute(UserDetails userDetails, DataAccess dataAccess, 
                                   Supplier<T> operation) {
        try {
            SubscriptionTier tier = getUserSubscriptionTier(userDetails);
            
            if (!isAccessAllowed(tier, dataAccess)) {
                throw new SubscriptionTierException(
                    String.format("Access denied: %s requires %s or higher", 
                        dataAccess.getDescription(), getRequiredTier(dataAccess).getDisplayName()),
                    tier, getRequiredTier(dataAccess)
                );
            }
            
            log.debug("Access granted for {} - Tier: {}, Access: {}", 
                userDetails.getUsername(), tier.getDisplayName(), dataAccess);
            
            return operation.get();
            
        } catch (SubscriptionTierException e) {
            log.warn("Subscription tier access denied for {}: {}", 
                userDetails.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error during tier validation for {}: {}", 
                userDetails.getUsername(), e.getMessage());
            throw new RuntimeException("Access validation failed", e);
        }
    }

    /**
     * Async version of validateAndExecute
     */
    public <T> CompletableFuture<T> validateAndExecuteAsync(UserDetails userDetails, 
                                                           DataAccess dataAccess,
                                                           Supplier<CompletableFuture<T>> operation) {
        try {
            SubscriptionTier tier = getUserSubscriptionTier(userDetails);
            
            if (!isAccessAllowed(tier, dataAccess)) {
                return CompletableFuture.failedFuture(
                    new SubscriptionTierException(
                        String.format("Access denied: %s requires %s or higher", 
                            dataAccess.getDescription(), getRequiredTier(dataAccess).getDisplayName()),
                        tier, getRequiredTier(dataAccess)
                    )
                );
            }
            
            return operation.get();
            
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Check if user has free tier
     */
    public boolean isFreeTier(UserDetails userDetails) {
        return getUserSubscriptionTier(userDetails) == SubscriptionTier.FREE;
    }

    /**
     * Validate historical data range based on subscription tier
     */
    public boolean isHistoricalRangeAllowed(UserDetails userDetails, Instant from, Instant to) {
        SubscriptionTier tier = getUserSubscriptionTier(userDetails);
        
        if (!tier.hasHistoricalAccess()) {
            return false;
        }
        
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        return daysBetween <= tier.getMaxHistoricalDays();
    }

    /**
     * Validate bulk request symbol count
     */
    public boolean isBulkRequestAllowed(UserDetails userDetails, int symbolCount) {
        SubscriptionTier tier = getUserSubscriptionTier(userDetails);
        return symbolCount <= tier.getMaxSymbolsPerRequest();
    }

    /**
     * Get user's subscription tier from security context
     */
    private SubscriptionTier getUserSubscriptionTier(UserDetails userDetails) {
        // Extract subscription tier from user authorities/roles
        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::mapRoleToTier)
            .findFirst()
            .orElse(SubscriptionTier.FREE);
    }

    /**
     * Map Spring Security role to subscription tier
     */
    private SubscriptionTier mapRoleToTier(String role) {
        return switch (role.toUpperCase()) {
            case "ROLE_INSTITUTIONAL", "TIER_INSTITUTIONAL" -> SubscriptionTier.INSTITUTIONAL;
            case "ROLE_PROFESSIONAL", "TIER_PROFESSIONAL" -> SubscriptionTier.PROFESSIONAL;
            case "ROLE_SMART_TRADER", "TIER_SMART_TRADER" -> SubscriptionTier.SMART_TRADER;
            case "ROLE_FREE", "TIER_FREE", "ROLE_USER" -> SubscriptionTier.FREE;
            default -> {
                log.warn("Unknown role '{}', defaulting to FREE tier", role);
                yield SubscriptionTier.FREE;
            }
        };
    }

    /**
     * Check if subscription tier allows specific data access
     */
    private boolean isAccessAllowed(SubscriptionTier tier, DataAccess dataAccess) {
        return switch (dataAccess) {
            case CURRENT_PRICE, SYMBOL_LIST, MARKET_STATS -> true; // All tiers
            case HISTORICAL_DATA -> tier.hasHistoricalAccess();
            case ORDER_BOOK -> tier.hasOrderBookAccess();
            case BULK_DATA -> tier != SubscriptionTier.FREE;
            case REAL_TIME_FEED -> tier.hasRealtimeAccess();
        };
    }

    /**
     * Get required tier for specific data access
     */
    private SubscriptionTier getRequiredTier(DataAccess dataAccess) {
        return switch (dataAccess) {
            case CURRENT_PRICE, SYMBOL_LIST, MARKET_STATS -> SubscriptionTier.FREE;
            case HISTORICAL_DATA, BULK_DATA -> SubscriptionTier.SMART_TRADER;
            case ORDER_BOOK, REAL_TIME_FEED -> SubscriptionTier.PROFESSIONAL;
        };
    }

    /**
     * Get subscription tier information for response metadata
     */
    public SubscriptionInfo getSubscriptionInfo(UserDetails userDetails) {
        SubscriptionTier tier = getUserSubscriptionTier(userDetails);
        
        return new SubscriptionInfo(
            tier.getDisplayName(),
            tier.hasRealtimeAccess(),
            tier.hasHistoricalAccess(),
            tier.hasOrderBookAccess(),
            tier.getMaxSymbolsPerRequest(),
            tier.getMaxHistoricalDays(),
            tier.getDailyRequestLimit()
        );
    }

    /**
     * Subscription information record
     */
    public record SubscriptionInfo(
        String tier,
        boolean realtimeAccess,
        boolean historicalAccess,
        boolean orderBookAccess,
        int maxSymbolsPerRequest,
        int maxHistoricalDays,
        int dailyRequestLimit
    ) {}

    /**
     * Custom exception for subscription tier violations
     */
    public static class SubscriptionTierException extends RuntimeException {
        private final SubscriptionTier currentTier;
        private final SubscriptionTier requiredTier;

        public SubscriptionTierException(String message, SubscriptionTier currentTier, 
                                       SubscriptionTier requiredTier) {
            super(message);
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
        }

        public SubscriptionTier getCurrentTier() {
            return currentTier;
        }

        public SubscriptionTier getRequiredTier() {
            return requiredTier;
        }
    }
}