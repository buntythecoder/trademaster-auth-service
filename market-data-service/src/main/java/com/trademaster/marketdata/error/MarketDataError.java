package com.trademaster.marketdata.error;

import java.time.Instant;

/**
 * Market Data Error Hierarchy (Rule #11: Functional Error Handling)
 *
 * Sealed hierarchy of errors that can occur in market data operations.
 * Used with Result type for railway-oriented programming without exceptions.
 *
 * Design Patterns:
 * - Sealed Classes: Exhaustive error types with pattern matching
 * - Value Objects: Immutable error records with rich context
 * - Error Hierarchy: Logical grouping of related errors
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface MarketDataError {

    /**
     * Symbol not found in the system
     */
    record SymbolNotFound(
        String symbol,
        String exchange,
        Instant timestamp
    ) implements MarketDataError {
        public String getMessage() {
            return "Symbol %s not found on exchange %s".formatted(symbol, exchange);
        }
    }

    /**
     * Market data not available for the requested time range
     */
    record DataNotAvailable(
        String symbol,
        String exchange,
        Instant from,
        Instant to,
        String reason
    ) implements MarketDataError {
        public String getMessage() {
            return "Data not available for %s on %s from %s to %s: %s"
                .formatted(symbol, exchange, from, to, reason);
        }
    }

    /**
     * Invalid request parameters
     */
    record InvalidRequest(
        String parameter,
        String value,
        String reason
    ) implements MarketDataError {
        public String getMessage() {
            return "Invalid %s value '%s': %s".formatted(parameter, value, reason);
        }
    }

    /**
     * Data quality issues
     */
    record DataQualityError(
        String symbol,
        String exchange,
        String qualityIssue,
        double qualityScore
    ) implements MarketDataError {
        public String getMessage() {
            return "Data quality issue for %s on %s: %s (score: %.2f)"
                .formatted(symbol, exchange, qualityIssue, qualityScore);
        }
    }

    /**
     * Cache operation failure
     */
    record CacheError(
        String operation,
        String key,
        String reason
    ) implements MarketDataError {
        public String getMessage() {
            return "Cache %s failed for key '%s': %s".formatted(operation, key, reason);
        }
    }

    /**
     * Database operation failure
     */
    record DatabaseError(
        String operation,
        String details,
        String sqlState
    ) implements MarketDataError {
        public String getMessage() {
            return "Database %s failed: %s (SQL state: %s)"
                .formatted(operation, details, sqlState);
        }
    }

    /**
     * Circuit breaker open (service unavailable)
     */
    record CircuitBreakerOpen(
        String service,
        Instant openedAt,
        long failureCount
    ) implements MarketDataError {
        public String getMessage() {
            return "Circuit breaker open for %s since %s (%d failures)"
                .formatted(service, openedAt, failureCount);
        }
    }

    /**
     * External provider error
     */
    record ProviderError(
        String providerId,
        String operation,
        int statusCode,
        String message
    ) implements MarketDataError {
        public String getMessage() {
            return "Provider %s failed for %s (HTTP %d): %s"
                .formatted(providerId, operation, statusCode, message);
        }
    }

    /**
     * Subscription tier insufficient
     */
    record InsufficientTier(
        String feature,
        String requiredTier,
        String currentTier
    ) implements MarketDataError {
        public String getMessage() {
            return "Feature '%s' requires %s tier (current: %s)"
                .formatted(feature, requiredTier, currentTier);
        }
    }

    /**
     * Rate limit exceeded
     */
    record RateLimitExceeded(
        String resource,
        long limit,
        long current,
        Instant resetAt
    ) implements MarketDataError {
        public String getMessage() {
            return "Rate limit exceeded for %s (%d/%d requests, resets at %s)"
                .formatted(resource, current, limit, resetAt);
        }
    }

    /**
     * Validation error for business rules
     */
    record ValidationError(
        String field,
        String value,
        String constraint,
        String message
    ) implements MarketDataError {
        public String getMessage() {
            return "Validation failed for %s='%s': %s (%s)"
                .formatted(field, value, message, constraint);
        }
    }

    /**
     * Timeout error for operations
     */
    record TimeoutError(
        String operation,
        long timeoutMs,
        long elapsedMs
    ) implements MarketDataError {
        public String getMessage() {
            return "Operation '%s' timed out after %dms (timeout: %dms)"
                .formatted(operation, elapsedMs, timeoutMs);
        }
    }

    /**
     * Unexpected error (last resort)
     */
    record UnexpectedError(
        String context,
        String message,
        String stackTrace
    ) implements MarketDataError {
        public String getMessage() {
            return "Unexpected error in %s: %s".formatted(context, message);
        }
    }

    /**
     * Get the error message (common interface method)
     */
    default String message() {
        return switch (this) {
            case SymbolNotFound e -> e.getMessage();
            case DataNotAvailable e -> e.getMessage();
            case InvalidRequest e -> e.getMessage();
            case DataQualityError e -> e.getMessage();
            case CacheError e -> e.getMessage();
            case DatabaseError e -> e.getMessage();
            case CircuitBreakerOpen e -> e.getMessage();
            case ProviderError e -> e.getMessage();
            case InsufficientTier e -> e.getMessage();
            case RateLimitExceeded e -> e.getMessage();
            case ValidationError e -> e.getMessage();
            case TimeoutError e -> e.getMessage();
            case UnexpectedError e -> e.getMessage();
        };
    }

    /**
     * Get error severity level
     */
    default ErrorSeverity severity() {
        return switch (this) {
            case SymbolNotFound ignored1 -> ErrorSeverity.WARNING;
            case DataNotAvailable ignored2 -> ErrorSeverity.WARNING;
            case InvalidRequest ignored3 -> ErrorSeverity.WARNING;
            case InsufficientTier ignored4 -> ErrorSeverity.WARNING;
            case DataQualityError ignored5 -> ErrorSeverity.DEGRADED;
            case RateLimitExceeded ignored6 -> ErrorSeverity.DEGRADED;
            case TimeoutError ignored7 -> ErrorSeverity.DEGRADED;
            case CacheError ignored8 -> ErrorSeverity.DEGRADED; // Cache failures are non-critical
            case DatabaseError ignored9 -> ErrorSeverity.ERROR;
            case ProviderError ignored10 -> ErrorSeverity.ERROR;
            case CircuitBreakerOpen ignored11 -> ErrorSeverity.CRITICAL;
            case ValidationError ignored12 -> ErrorSeverity.WARNING;
            case UnexpectedError ignored13 -> ErrorSeverity.CRITICAL;
        };
    }

    /**
     * Check if error is recoverable
     */
    default boolean isRecoverable() {
        return switch (this) {
            case CacheError ignored1 -> true;
            case TimeoutError ignored2 -> true;
            case RateLimitExceeded ignored3 -> true;
            case CircuitBreakerOpen ignored4 -> false;
            case DatabaseError ignored5 -> false;
            case ProviderError(var id, var op, var code, var msg) -> code >= 500; // 5xx are potentially recoverable
            case SymbolNotFound ignored6 -> false;
            case DataNotAvailable ignored7 -> false;
            case InvalidRequest ignored8 -> false;
            case DataQualityError ignored9 -> false;
            case InsufficientTier ignored10 -> false;
            case ValidationError ignored11 -> false;
            case UnexpectedError ignored12 -> false;
        };
    }

    /**
     * Error severity levels
     */
    enum ErrorSeverity {
        WARNING,    // Non-critical, user action needed
        DEGRADED,   // Reduced functionality
        ERROR,      // Operation failed but system stable
        CRITICAL    // System instability or data loss risk
    }
}
