package com.trademaster.portfolio.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Structured Logging Configuration for Portfolio Service
 * 
 * Comprehensive logging setup optimized for Grafana/ELK stack integration.
 * Provides structured JSON logging with correlation IDs and business context.
 * 
 * Key Features:
 * - Zero-impact structured logging for Virtual Threads
 * - Business context preservation across async operations
 * - Correlation ID tracking for distributed tracing
 * - Performance metrics embedded in logs
 * - Financial compliance audit trail
 * 
 * Performance Targets:
 * - Logging overhead: <0.1ms per log entry
 * - No blocking operations in Virtual Threads
 * - Minimal memory allocation
 * - Structured JSON for machine processing
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@Slf4j
public class LoggingConfiguration {
    
    public LoggingConfiguration() {
        configureStructuredLogging();
    }
    
    private void configureStructuredLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Print logback configuration status for debugging
        log.info("Configuring structured logging for Portfolio Service");
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
}

/**
 * Portfolio Structured Logger
 * 
 * Provides business-context aware logging methods for portfolio operations.
 * All logs include correlation IDs and structured data for Grafana dashboards.
 */
@Component
@Slf4j
public class PortfolioLogger {
    
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String PORTFOLIO_ID = "portfolioId";
    private static final String OPERATION = "operation";
    private static final String SYMBOL = "symbol";
    private static final String AMOUNT = "amount";
    private static final String DURATION_MS = "durationMs";
    private static final String STATUS = "status";
    
    /**
     * Set correlation ID for the current thread context
     */
    public void setCorrelationId() {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
    }
    
    /**
     * Set correlation ID with custom value
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }
    
    /**
     * Clear correlation ID from thread context
     */
    public void clearCorrelationId() {
        MDC.remove(CORRELATION_ID);
    }
    
    /**
     * Set user context for all subsequent logs
     */
    public void setUserContext(Long userId) {
        MDC.put(USER_ID, String.valueOf(userId));
    }
    
    /**
     * Set portfolio context for all subsequent logs
     */
    public void setPortfolioContext(Long portfolioId) {
        MDC.put(PORTFOLIO_ID, String.valueOf(portfolioId));
    }
    
    /**
     * Clear all context from thread
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log portfolio creation with business context
     */
    public void logPortfolioCreated(Long userId, Long portfolioId, String portfolioName, 
                                   BigDecimal initialValue, long durationMs) {
        log.info("Portfolio created successfully",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv("portfolioName", portfolioName),
            StructuredArguments.kv("initialValue", initialValue),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "portfolio_creation"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log portfolio valuation update
     */
    public void logPortfolioValuation(Long portfolioId, BigDecimal oldValue, BigDecimal newValue, 
                                     BigDecimal unrealizedPnl, long durationMs) {
        log.info("Portfolio valuation updated",
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv("oldValue", oldValue),
            StructuredArguments.kv("newValue", newValue),
            StructuredArguments.kv("unrealizedPnl", unrealizedPnl),
            StructuredArguments.kv("valueChange", newValue.subtract(oldValue)),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "portfolio_valuation"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log position update from trade execution
     */
    public void logPositionUpdate(Long portfolioId, String symbol, String exchange, 
                                 Integer oldQuantity, Integer newQuantity, BigDecimal price,
                                 BigDecimal realizedPnl, long durationMs) {
        log.info("Position updated from trade execution",
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("exchange", exchange),
            StructuredArguments.kv("oldQuantity", oldQuantity),
            StructuredArguments.kv("newQuantity", newQuantity),
            StructuredArguments.kv("quantityChange", newQuantity - oldQuantity),
            StructuredArguments.kv("tradePrice", price),
            StructuredArguments.kv("realizedPnl", realizedPnl),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "position_update"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log position price update
     */
    public void logPriceUpdate(String symbol, BigDecimal oldPrice, BigDecimal newPrice, 
                              int positionsAffected, long durationMs) {
        log.debug("Position prices updated for symbol",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("oldPrice", oldPrice),
            StructuredArguments.kv("newPrice", newPrice),
            StructuredArguments.kv("priceChange", newPrice.subtract(oldPrice)),
            StructuredArguments.kv("positionsAffected", positionsAffected),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "price_update"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log P&L calculation
     */
    public void logPnLCalculation(Long portfolioId, BigDecimal realizedPnl, BigDecimal unrealizedPnl,
                                 BigDecimal totalPnl, int positionsProcessed, long durationMs) {
        log.info("P&L calculation completed",
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv("realizedPnl", realizedPnl),
            StructuredArguments.kv("unrealizedPnl", unrealizedPnl),
            StructuredArguments.kv("totalPnl", totalPnl),
            StructuredArguments.kv("positionsProcessed", positionsProcessed),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "pnl_calculation"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log transaction creation
     */
    public void logTransactionCreated(Long portfolioId, String transactionType, String symbol,
                                     Integer quantity, BigDecimal amount, String tradeId, long durationMs) {
        log.info("Portfolio transaction created",
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv("transactionType", transactionType),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("quantity", quantity),
            StructuredArguments.kv(AMOUNT, amount),
            StructuredArguments.kv("tradeId", tradeId),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "transaction_creation"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log risk assessment
     */
    public void logRiskAssessment(Long portfolioId, String riskType, String riskLevel,
                                 BigDecimal riskValue, String threshold, boolean violated) {
        log.info("Risk assessment completed",
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv("riskType", riskType),
            StructuredArguments.kv("riskLevel", riskLevel),
            StructuredArguments.kv("riskValue", riskValue),
            StructuredArguments.kv("threshold", threshold),
            StructuredArguments.kv("violated", violated),
            StructuredArguments.kv(OPERATION, "risk_assessment"),
            StructuredArguments.kv(STATUS, violated ? "violation" : "compliant")
        );
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformanceMetrics(String operation, long durationMs, boolean success,
                                     Map<String, Object> additionalMetrics) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue(OPERATION, operation)
            .addKeyValue(DURATION_MS, durationMs)
            .addKeyValue(STATUS, success ? "success" : "failure");
        
        if (additionalMetrics != null) {
            for (Map.Entry<String, Object> entry : additionalMetrics.entrySet()) {
                logBuilder = logBuilder.addKeyValue(entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Operation performance metrics recorded");
    }
    
    /**
     * Log API request
     */
    public void logApiRequest(String endpoint, String method, String userAgent,
                             Long userId, long durationMs, int statusCode) {
        log.info("API request processed",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv("userAgent", userAgent),
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv(OPERATION, "api_request"),
            StructuredArguments.kv(STATUS, statusCode < 400 ? "success" : "error")
        );
    }
    
    /**
     * Log database operation
     */
    public void logDatabaseOperation(String queryType, String tableName, int recordsAffected,
                                   long durationMs, boolean success) {
        log.debug("Database operation completed",
            StructuredArguments.kv("queryType", queryType),
            StructuredArguments.kv("tableName", tableName),
            StructuredArguments.kv("recordsAffected", recordsAffected),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "database_operation"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure")
        );
    }
    
    /**
     * Log cache operation
     */
    public void logCacheOperation(String cacheType, String operation, String key,
                                 boolean hit, long durationMs) {
        log.debug("Cache operation completed",
            StructuredArguments.kv("cacheType", cacheType),
            StructuredArguments.kv("cacheOperation", operation),
            StructuredArguments.kv("key", key),
            StructuredArguments.kv("hit", hit),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "cache_operation"),
            StructuredArguments.kv(STATUS, "success")
        );
    }
    
    /**
     * Log error with business context
     */
    public void logError(String operation, String errorType, String errorMessage,
                        Long portfolioId, String symbol, Exception exception) {
        log.error("Operation failed",
            StructuredArguments.kv(OPERATION, operation),
            StructuredArguments.kv("errorType", errorType),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv(PORTFOLIO_ID, portfolioId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null),
            StructuredArguments.kv(STATUS, "error"),
            exception
        );
    }
    
    /**
     * Log audit event for compliance
     */
    public void logAuditEvent(String eventType, Long userId, Long portfolioId,
                             String action, String resource, String outcome,
                             Map<String, Object> auditData) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("eventType", eventType)
            .addKeyValue(USER_ID, userId)
            .addKeyValue(PORTFOLIO_ID, portfolioId)
            .addKeyValue("action", action)
            .addKeyValue("resource", resource)
            .addKeyValue("outcome", outcome)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "audit");
        
        if (auditData != null) {
            for (Map.Entry<String, Object> entry : auditData.entrySet()) {
                logBuilder = logBuilder.addKeyValue("audit_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Audit event recorded for compliance");
    }
    
    /**
     * Log business metrics for analytics
     */
    public void logBusinessMetrics(String metricType, Map<String, Number> metrics) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("metricType", metricType)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "business_metrics");
        
        for (Map.Entry<String, Number> entry : metrics.entrySet()) {
            logBuilder = logBuilder.addKeyValue("metric_" + entry.getKey(), entry.getValue());
        }
        
        logBuilder.log("Business metrics recorded for analytics");
    }
    
    /**
     * Create structured argument for custom objects
     */
    public static StructuredArguments.ObjectAppendingMarker structuredObject(String key, Object value) {
        return StructuredArguments.kv(key, value);
    }
    
    /**
     * Log with custom structured data
     */
    public void logWithStructuredData(String message, String logLevel, 
                                     Map<String, Object> structuredData) {
        var logBuilder = switch (logLevel.toUpperCase()) {
            case "ERROR" -> log.atError();
            case "WARN" -> log.atWarn();
            case "INFO" -> log.atInfo();
            case "DEBUG" -> log.atDebug();
            case "TRACE" -> log.atTrace();
            default -> log.atInfo();
        };
        
        for (Map.Entry<String, Object> entry : structuredData.entrySet()) {
            logBuilder = logBuilder.addKeyValue(entry.getKey(), entry.getValue());
        }
        
        logBuilder.log(message);
    }
}