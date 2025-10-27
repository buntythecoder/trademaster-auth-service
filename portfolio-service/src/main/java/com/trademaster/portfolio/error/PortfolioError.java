package com.trademaster.portfolio.error;

/**
 * Portfolio Service Error Types
 *
 * Comprehensive error enumeration for portfolio operations.
 * Used with Result type for functional error handling.
 *
 * Error Categories:
 * - NOT_FOUND: Resource doesn't exist
 * - VALIDATION: Invalid input data
 * - BUSINESS_RULE: Business rule violation
 * - INSUFFICIENT_FUNDS: Financial constraint violation
 * - RISK_LIMIT: Risk management constraint violation
 * - SYSTEM: Technical/infrastructure errors
 * - EXTERNAL: External service errors
 *
 * Rule Compliance:
 * - Rule #3: Functional error handling
 * - Rule #11: No exceptions in business logic
 * - Rule #18: Meaningful error names
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public enum PortfolioError {

    // ==================== NOT FOUND ERRORS ====================

    PORTFOLIO_NOT_FOUND("Portfolio not found", "PORTFOLIO_404", ErrorSeverity.MEDIUM),
    POSITION_NOT_FOUND("Position not found", "POSITION_404", ErrorSeverity.MEDIUM),
    TRANSACTION_NOT_FOUND("Transaction not found", "TRANSACTION_404", ErrorSeverity.LOW),
    USER_NOT_FOUND("User not found", "USER_404", ErrorSeverity.HIGH),

    // ==================== VALIDATION ERRORS ====================

    INVALID_PORTFOLIO_ID("Invalid portfolio ID", "PORTFOLIO_VAL_001", ErrorSeverity.MEDIUM),
    INVALID_SYMBOL("Invalid symbol", "SYMBOL_VAL_001", ErrorSeverity.MEDIUM),
    INVALID_QUANTITY("Invalid quantity", "QUANTITY_VAL_001", ErrorSeverity.MEDIUM),
    INVALID_PRICE("Invalid price", "PRICE_VAL_001", ErrorSeverity.MEDIUM),
    INVALID_DATE_RANGE("Invalid date range", "DATE_VAL_001", ErrorSeverity.LOW),
    INVALID_TRANSACTION_TYPE("Invalid transaction type", "TRANSACTION_VAL_001", ErrorSeverity.MEDIUM),
    NEGATIVE_AMOUNT("Amount cannot be negative", "AMOUNT_VAL_001", ErrorSeverity.MEDIUM),
    ZERO_QUANTITY("Quantity cannot be zero", "QUANTITY_VAL_002", ErrorSeverity.MEDIUM),

    // ==================== BUSINESS RULE ERRORS ====================

    PORTFOLIO_ALREADY_EXISTS("Portfolio already exists for user", "PORTFOLIO_BIZ_001", ErrorSeverity.MEDIUM),
    POSITION_ALREADY_EXISTS("Position already exists for symbol", "POSITION_BIZ_001", ErrorSeverity.MEDIUM),
    INSUFFICIENT_POSITION("Insufficient position quantity to sell", "POSITION_BIZ_002", ErrorSeverity.HIGH),
    PORTFOLIO_NOT_ACTIVE("Portfolio is not in active status", "PORTFOLIO_BIZ_002", ErrorSeverity.HIGH),
    POSITION_NOT_ACTIVE("Position is not in active status", "POSITION_BIZ_002", ErrorSeverity.HIGH),
    CANNOT_DELETE_ACTIVE_PORTFOLIO("Cannot delete portfolio with active positions", "PORTFOLIO_BIZ_003", ErrorSeverity.HIGH),
    CANNOT_CLOSE_POSITION_WITH_QUANTITY("Cannot close position with non-zero quantity", "POSITION_BIZ_003", ErrorSeverity.HIGH),

    // ==================== FINANCIAL ERRORS ====================

    INSUFFICIENT_FUNDS("Insufficient cash balance", "FUNDS_001", ErrorSeverity.HIGH),
    INSUFFICIENT_BUYING_POWER("Insufficient buying power", "FUNDS_002", ErrorSeverity.HIGH),
    INSUFFICIENT_MARGIN("Insufficient margin available", "MARGIN_001", ErrorSeverity.HIGH),
    MARGIN_CALL_TRIGGERED("Margin call triggered", "MARGIN_002", ErrorSeverity.CRITICAL),
    NEGATIVE_BALANCE("Account balance cannot be negative", "BALANCE_001", ErrorSeverity.CRITICAL),

    // ==================== RISK LIMIT ERRORS ====================

    POSITION_CONCENTRATION_EXCEEDED("Position concentration limit exceeded", "RISK_001", ErrorSeverity.HIGH),
    SECTOR_CONCENTRATION_EXCEEDED("Sector concentration limit exceeded", "RISK_002", ErrorSeverity.HIGH),
    LEVERAGE_LIMIT_EXCEEDED("Leverage limit exceeded", "RISK_003", ErrorSeverity.HIGH),
    DAILY_LOSS_LIMIT_EXCEEDED("Daily loss limit exceeded", "RISK_004", ErrorSeverity.CRITICAL),
    MAX_POSITION_SIZE_EXCEEDED("Maximum position size exceeded", "RISK_005", ErrorSeverity.HIGH),
    MAX_PORTFOLIO_SIZE_EXCEEDED("Maximum portfolio size exceeded", "RISK_006", ErrorSeverity.HIGH),
    RISK_ASSESSMENT_FAILED("Risk assessment failed validation", "RISK_007", ErrorSeverity.HIGH),
    DAY_TRADING_LIMIT_EXCEEDED("Day trading limit exceeded", "RISK_008", ErrorSeverity.HIGH),

    // ==================== CALCULATION ERRORS ====================

    VALUATION_CALCULATION_FAILED("Portfolio valuation calculation failed", "CALC_001", ErrorSeverity.MEDIUM),
    PNL_CALCULATION_FAILED("P&L calculation failed", "CALC_002", ErrorSeverity.MEDIUM),
    RISK_METRICS_CALCULATION_FAILED("Risk metrics calculation failed", "CALC_003", ErrorSeverity.MEDIUM),
    PERFORMANCE_CALCULATION_FAILED("Performance calculation failed", "CALC_004", ErrorSeverity.LOW),
    TAX_CALCULATION_FAILED("Tax calculation failed", "CALC_005", ErrorSeverity.MEDIUM),
    COST_BASIS_CALCULATION_FAILED("Cost basis calculation failed", "CALC_006", ErrorSeverity.MEDIUM),

    // ==================== EXTERNAL SERVICE ERRORS ====================

    MARKET_DATA_UNAVAILABLE("Market data service unavailable", "EXTERNAL_001", ErrorSeverity.HIGH),
    BROKER_API_UNAVAILABLE("Broker API unavailable", "EXTERNAL_002", ErrorSeverity.CRITICAL),
    PRICE_FEED_TIMEOUT("Price feed timeout", "EXTERNAL_003", ErrorSeverity.HIGH),
    ORDER_EXECUTION_FAILED("Order execution failed", "EXTERNAL_004", ErrorSeverity.CRITICAL),

    // ==================== SYSTEM ERRORS ====================

    DATABASE_ERROR("Database operation failed", "SYSTEM_001", ErrorSeverity.CRITICAL),
    CACHE_ERROR("Cache operation failed", "SYSTEM_002", ErrorSeverity.LOW),
    CIRCUIT_BREAKER_OPEN("Circuit breaker is open", "SYSTEM_003", ErrorSeverity.HIGH),
    TIMEOUT_ERROR("Operation timed out", "SYSTEM_004", ErrorSeverity.MEDIUM),
    CONCURRENCY_ERROR("Concurrent modification detected", "SYSTEM_005", ErrorSeverity.MEDIUM),
    INTERNAL_ERROR("Internal server error", "SYSTEM_999", ErrorSeverity.CRITICAL),

    // ==================== AUTHORIZATION ERRORS ====================

    UNAUTHORIZED_ACCESS("Unauthorized portfolio access", "AUTH_001", ErrorSeverity.CRITICAL),
    INSUFFICIENT_PERMISSIONS("Insufficient permissions", "AUTH_002", ErrorSeverity.HIGH),
    PORTFOLIO_ACCESS_DENIED("Portfolio access denied", "AUTH_003", ErrorSeverity.HIGH);

    private final String message;
    private final String code;
    private final ErrorSeverity severity;

    PortfolioError(String message, String code, ErrorSeverity severity) {
        this.message = message;
        this.code = code;
        this.severity = severity;
    }

    /**
     * Get human-readable error message
     *
     * Rule #18: Meaningful error messages
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get machine-readable error code
     *
     * Rule #18: Error code for API responses
     */
    public String getCode() {
        return code;
    }

    /**
     * Get error severity level
     *
     * Rule #18: Severity for monitoring and alerting
     */
    public ErrorSeverity getSeverity() {
        return severity;
    }

    /**
     * Get formatted error with code and message
     *
     * Rule #15: Structured error formatting
     */
    public String format() {
        return "[" + code + "] " + message;
    }

    /**
     * Create error detail with context
     *
     * Rule #15: Error context for debugging
     */
    public PortfolioErrorDetail withContext(String context) {
        return new PortfolioErrorDetail(this, context);
    }

    /**
     * Create error detail with exception
     *
     * Rule #11: Exception wrapping for functional handling
     */
    public PortfolioErrorDetail withException(Throwable exception) {
        return new PortfolioErrorDetail(this, exception.getMessage());
    }
}

/**
 * Error Severity Levels
 *
 * Rule #18: Severity classification for monitoring
 */
enum ErrorSeverity {
    LOW,      // Informational, no action needed
    MEDIUM,   // Warning, may need attention
    HIGH,     // Error, requires attention
    CRITICAL  // Critical, immediate action required
}

/**
 * Portfolio Error Detail
 *
 * Extended error information with context.
 *
 * Rule #9: Immutable record for error details
 * Rule #15: Structured error information
 */
record PortfolioErrorDetail(
    PortfolioError error,
    String context
) {
    public String format() {
        return error.format() + " - " + context;
    }

    public String getCode() {
        return error.getCode();
    }

    public String getMessage() {
        return error.getMessage();
    }

    public ErrorSeverity getSeverity() {
        return error.getSeverity();
    }
}
