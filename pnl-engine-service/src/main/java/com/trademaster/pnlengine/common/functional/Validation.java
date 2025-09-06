package com.trademaster.pnlengine.common.functional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional validation chains for P&L data integrity
 * 
 * MANDATORY: Java 24 + Functional Programming + Zero if-else statements
 * 
 * Provides composable validation functions using Result type for
 * financial data validation with clear error messages.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
public final class Validation {
    
    /**
     * Validation error types
     */
    public sealed interface ValidationError {
        record NullValue(String field) implements ValidationError {}
        record InvalidRange(String field, Object value, String constraint) implements ValidationError {}
        record BusinessRule(String rule, String details) implements ValidationError {}
        record DataIntegrity(String message) implements ValidationError {}
    }
    
    private Validation() {} // Utility class
    
    // ============================================================================
    // GENERIC VALIDATION COMBINATORS
    // ============================================================================
    
    public static <T> Function<T, Result<T, ValidationError>> notNull(String fieldName) {
        return value -> value != null ? 
            Result.success(value) : 
            Result.failure(new ValidationError.NullValue(fieldName));
    }
    
    public static <T> Function<T, Result<T, ValidationError>> check(
            Predicate<T> condition, 
            Function<T, ValidationError> errorMapper) {
        return value -> condition.test(value) ? 
            Result.success(value) : 
            Result.failure(errorMapper.apply(value));
    }
    
    public static <T> Function<Result<T, ValidationError>, Result<T, ValidationError>> andThen(
            Function<T, Result<T, ValidationError>> validator) {
        return result -> result.flatMap(validator);
    }
    
    // ============================================================================
    // FINANCIAL DATA VALIDATORS
    // ============================================================================
    
    public static final Function<String, Result<String, ValidationError>> USER_ID = 
        notNull("userId")
            .andThen(check(
                userId -> !userId.trim().isEmpty(),
                userId -> new ValidationError.InvalidRange("userId", userId, "must not be empty")
            ));
    
    public static final Function<BigDecimal, Result<BigDecimal, ValidationError>> POSITIVE_AMOUNT = 
        notNull("amount")
            .andThen(check(
                amount -> amount.compareTo(BigDecimal.ZERO) > 0,
                amount -> new ValidationError.InvalidRange("amount", amount, "must be positive")
            ));
    
    public static final Function<BigDecimal, Result<BigDecimal, ValidationError>> NON_NEGATIVE_AMOUNT = 
        notNull("amount")
            .andThen(check(
                amount -> amount.compareTo(BigDecimal.ZERO) >= 0,
                amount -> new ValidationError.InvalidRange("amount", amount, "must be non-negative")
            ));
    
    public static final Function<BigDecimal, Result<BigDecimal, ValidationError>> PORTFOLIO_VALUE = 
        notNull("portfolioValue")
            .andThen(check(
                value -> value.compareTo(BigDecimal.ZERO) >= 0,
                value -> new ValidationError.BusinessRule("portfolio_value_negative", 
                    "Portfolio value cannot be negative: " + value)
            ))
            .andThen(check(
                value -> value.compareTo(BigDecimal.valueOf(1_000_000_000)) <= 0,
                value -> new ValidationError.BusinessRule("portfolio_value_excessive", 
                    "Portfolio value exceeds maximum: " + value)
            ));
    
    public static final Function<BigDecimal, Result<BigDecimal, ValidationError>> RETURN_PERCENTAGE = 
        notNull("returnPercentage")
            .andThen(check(
                percent -> percent.compareTo(BigDecimal.valueOf(-100)) >= 0,
                percent -> new ValidationError.BusinessRule("return_below_negative_100", 
                    "Return percentage cannot be below -100%: " + percent)
            ))
            .andThen(check(
                percent -> percent.compareTo(BigDecimal.valueOf(1000)) <= 0,
                percent -> new ValidationError.BusinessRule("return_above_1000", 
                    "Return percentage exceeds 1000%: " + percent)
            ));
    
    public static final Function<Integer, Result<Integer, ValidationError>> POSITIVE_QUANTITY = 
        notNull("quantity")
            .andThen(check(
                qty -> qty > 0,
                qty -> new ValidationError.InvalidRange("quantity", qty, "must be positive")
            ));
    
    public static final Function<Integer, Result<Integer, ValidationError>> NON_NEGATIVE_COUNT = 
        notNull("count")
            .andThen(check(
                count -> count >= 0,
                count -> new ValidationError.InvalidRange("count", count, "must be non-negative")
            ));
    
    public static final Function<Long, Result<Long, ValidationError>> CALCULATION_TIME = 
        notNull("calculationTime")
            .andThen(check(
                time -> time > 0,
                time -> new ValidationError.InvalidRange("calculationTime", time, "must be positive")
            ))
            .andThen(check(
                time -> time <= 60_000, // 60 seconds max
                time -> new ValidationError.BusinessRule("calculation_timeout", 
                    "Calculation time exceeds 60 seconds: " + time + "ms")
            ));
    
    public static final Function<Instant, Result<Instant, ValidationError>> VALID_TIMESTAMP = 
        notNull("timestamp")
            .andThen(check(
                timestamp -> timestamp.isBefore(Instant.now().plusSeconds(60)), // Allow 1 min future
                timestamp -> new ValidationError.BusinessRule("future_timestamp", 
                    "Timestamp cannot be more than 1 minute in future: " + timestamp)
            ))
            .andThen(check(
                timestamp -> timestamp.isAfter(Instant.parse("2020-01-01T00:00:00Z")), // Reasonable past limit
                timestamp -> new ValidationError.BusinessRule("invalid_past_timestamp", 
                    "Timestamp too far in past: " + timestamp)
            ));
    
    // ============================================================================
    // BUSINESS RULE VALIDATORS
    // ============================================================================
    
    public static Function<BigDecimal, Result<BigDecimal, ValidationError>> positionValueCheck(
            BigDecimal quantity, BigDecimal price) {
        return marketValue -> {
            var expectedValue = price.multiply(quantity);
            var tolerance = expectedValue.multiply(BigDecimal.valueOf(0.001)); // 0.1% tolerance
            var difference = marketValue.subtract(expectedValue).abs();
            
            return difference.compareTo(tolerance) <= 0 ?
                Result.success(marketValue) :
                Result.failure(new ValidationError.DataIntegrity(
                    String.format("Market value mismatch: expected %s, got %s", expectedValue, marketValue)
                ));
        };
    }
    
    public static Function<BigDecimal, Result<BigDecimal, ValidationError>> unrealizedPnLCheck(
            BigDecimal marketValue, BigDecimal costBasis) {
        return unrealizedPnL -> {
            var expectedPnL = marketValue.subtract(costBasis);
            var tolerance = BigDecimal.valueOf(0.01); // $0.01 tolerance
            var difference = unrealizedPnL.subtract(expectedPnL).abs();
            
            return difference.compareTo(tolerance) <= 0 ?
                Result.success(unrealizedPnL) :
                Result.failure(new ValidationError.DataIntegrity(
                    String.format("Unrealized P&L calculation error: expected %s, got %s", expectedPnL, unrealizedPnL)
                ));
        };
    }
    
    // ============================================================================
    // VALIDATION CHAIN HELPERS
    // ============================================================================
    
    public static <T> Result<T, ValidationError> validate(T value, 
            @SuppressWarnings("unchecked") Function<T, Result<T, ValidationError>>... validators) {
        var result = Result.success(value);
        for (var validator : validators) {
            result = result.flatMap(validator);
        }
        return result;
    }
    
    public static String formatValidationError(ValidationError error) {
        return switch (error) {
            case ValidationError.NullValue(var field) -> 
                String.format("Required field '%s' cannot be null", field);
            case ValidationError.InvalidRange(var field, var value, var constraint) -> 
                String.format("Field '%s' with value '%s' violates constraint: %s", field, value, constraint);
            case ValidationError.BusinessRule(var rule, var details) -> 
                String.format("Business rule '%s' violated: %s", rule, details);
            case ValidationError.DataIntegrity(var message) -> 
                String.format("Data integrity error: %s", message);
        };
    }
}