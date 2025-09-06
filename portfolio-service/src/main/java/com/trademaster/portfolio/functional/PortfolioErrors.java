package com.trademaster.portfolio.functional;

/**
 * Portfolio Domain Error Types
 * 
 * Sealed interface defining all possible portfolio operation errors.
 * Follows Rule #11 (Error Handling Patterns) - functional error handling.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Functional Programming)
 */
public sealed interface PortfolioErrors permits
    PortfolioErrors.NotFoundError,
    PortfolioErrors.ValidationError,
    PortfolioErrors.BusinessRuleError,
    PortfolioErrors.DataIntegrityError,
    PortfolioErrors.ExternalServiceError,
    PortfolioErrors.SecurityError,
    PortfolioErrors.SystemError {
    
    String getMessage();
    String getErrorCode();
    
    /**
     * Portfolio not found error
     */
    record NotFoundError(String message, String errorCode) implements PortfolioErrors {
        public static NotFoundError portfolioNotFound(Long portfolioId) {
            return new NotFoundError(
                "Portfolio not found: " + portfolioId, 
                "PORTFOLIO_NOT_FOUND"
            );
        }
        
        public static NotFoundError positionNotFound(String symbol) {
            return new NotFoundError(
                "Position not found for symbol: " + symbol, 
                "POSITION_NOT_FOUND"
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Input validation error
     */
    record ValidationError(String message, String errorCode, String field) implements PortfolioErrors {
        public static ValidationError invalidCurrency(String currency) {
            return new ValidationError(
                "Invalid currency: " + currency, 
                "INVALID_CURRENCY", 
                "currency"
            );
        }
        
        public static ValidationError insufficientFunds(String amount) {
            return new ValidationError(
                "Insufficient funds: " + amount, 
                "INSUFFICIENT_FUNDS", 
                "cashBalance"
            );
        }
        
        public static ValidationError invalidAmount(String amount) {
            return new ValidationError(
                "Invalid amount: " + amount, 
                "INVALID_AMOUNT", 
                "amount"
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Business rule violation error
     */
    record BusinessRuleError(String message, String errorCode, String rule) implements PortfolioErrors {
        public static BusinessRuleError portfolioNotActive(Long portfolioId) {
            return new BusinessRuleError(
                "Portfolio is not active: " + portfolioId, 
                "PORTFOLIO_NOT_ACTIVE", 
                "portfolio.status.active"
            );
        }
        
        public static BusinessRuleError tradingNotAllowed(String reason) {
            return new BusinessRuleError(
                "Trading not allowed: " + reason, 
                "TRADING_NOT_ALLOWED", 
                "trading.allowed"
            );
        }
        
        public static BusinessRuleError riskLimitExceeded(String limit) {
            return new BusinessRuleError(
                "Risk limit exceeded: " + limit, 
                "RISK_LIMIT_EXCEEDED", 
                "risk.limits"
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Data integrity error
     */
    record DataIntegrityError(String message, String errorCode, String constraint) implements PortfolioErrors {
        public static DataIntegrityError concurrentModification(Long portfolioId, Long version) {
            return new DataIntegrityError(
                "Concurrent modification detected for portfolio: " + portfolioId + ", version: " + version, 
                "CONCURRENT_MODIFICATION", 
                "version.constraint"
            );
        }
        
        public static DataIntegrityError invalidCalculation(String calculation) {
            return new DataIntegrityError(
                "Invalid calculation result: " + calculation, 
                "INVALID_CALCULATION", 
                "financial.precision"
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * External service error
     */
    record ExternalServiceError(String message, String errorCode, String service) implements PortfolioErrors {
        public static ExternalServiceError marketDataUnavailable(String symbol) {
            return new ExternalServiceError(
                "Market data unavailable for symbol: " + symbol, 
                "MARKET_DATA_UNAVAILABLE", 
                "market.data.service"
            );
        }
        
        public static ExternalServiceError brokerApiFailure(String broker) {
            return new ExternalServiceError(
                "Broker API failure: " + broker, 
                "BROKER_API_FAILURE", 
                "broker.api"
            );
        }
        
        public static ExternalServiceError circuitBreakerOpen(String service) {
            return new ExternalServiceError(
                "Circuit breaker open for service: " + service, 
                "CIRCUIT_BREAKER_OPEN", 
                service
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Security error
     */
    record SecurityError(String message, String errorCode, String operation) implements PortfolioErrors {
        public static SecurityError accessDenied(Long userId, Long portfolioId) {
            return new SecurityError(
                "Access denied for user: " + userId + " to portfolio: " + portfolioId, 
                "ACCESS_DENIED", 
                "portfolio.access"
            );
        }
        
        public static SecurityError unauthorizedOperation(String operation) {
            return new SecurityError(
                "Unauthorized operation: " + operation, 
                "UNAUTHORIZED_OPERATION", 
                operation
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * System error
     */
    record SystemError(String message, String errorCode, String details) implements PortfolioErrors {
        public static SystemError internalError(String details) {
            return new SystemError(
                "Internal system error: " + details, 
                "INTERNAL_ERROR", 
                details
            );
        }
        
        public static SystemError serviceUnavailable(String service) {
            return new SystemError(
                "Service unavailable: " + service, 
                "SERVICE_UNAVAILABLE", 
                service
            );
        }
        
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
    }
}