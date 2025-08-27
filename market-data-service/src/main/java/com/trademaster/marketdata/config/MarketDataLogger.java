package com.trademaster.marketdata.config;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Market Data Service Structured Logger
 * 
 * Provides market-data context aware logging methods for real-time operations.
 * All logs include correlation IDs and structured data for market analysis.
 */
@Component
@Slf4j
public class MarketDataLogger {
    
    private static final String CORRELATION_ID = "correlationId";
    private static final String SYMBOL = "symbol";
    private static final String EXCHANGE = "exchange";
    private static final String PROVIDER = "provider";
    private static final String CLIENT_ID = "clientId";
    private static final String OPERATION = "operation";
    private static final String DURATION_MS = "durationMs";
    private static final String STATUS = "status";
    private static final String MESSAGE_TYPE = "messageType";
    private static final String LATENCY_MS = "latencyMs";
    private static final String DATA_QUALITY = "dataQuality";
    
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
     * Set market data context for all subsequent logs
     */
    public void setMarketDataContext(String symbol, String exchange, String provider) {
        if (symbol != null) MDC.put(SYMBOL, symbol);
        if (exchange != null) MDC.put(EXCHANGE, exchange);
        if (provider != null) MDC.put(PROVIDER, provider);
    }
    
    /**
     * Set client context for WebSocket operations
     */
    public void setClientContext(String clientId) {
        if (clientId != null) MDC.put(CLIENT_ID, clientId);
    }
    
    /**
     * Clear all context from thread
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log market data message received
     */
    public void logMarketDataReceived(String symbol, String exchange, String messageType, 
                                    long latencyMs, int messageSize) {
        log.info("Market data message received",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(EXCHANGE, exchange),
            StructuredArguments.kv(MESSAGE_TYPE, messageType),
            StructuredArguments.kv(LATENCY_MS, latencyMs),
            StructuredArguments.kv("messageSize", messageSize),
            StructuredArguments.kv(OPERATION, "market_data_ingestion"),
            StructuredArguments.kv(STATUS, "received"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log price update
     */
    public void logPriceUpdate(String symbol, String exchange, double price, double previousPrice,
                              double volume, long timestamp) {
        log.info("Price update processed",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(EXCHANGE, exchange),
            StructuredArguments.kv("price", price),
            StructuredArguments.kv("previousPrice", previousPrice),
            StructuredArguments.kv("priceChange", price - previousPrice),
            StructuredArguments.kv("volume", volume),
            StructuredArguments.kv("marketTimestamp", timestamp),
            StructuredArguments.kv(OPERATION, "price_update"),
            StructuredArguments.kv(STATUS, "processed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log order book update
     */
    public void logOrderBookUpdate(String symbol, String exchange, String side, int levels,
                                  double bestPrice, double totalVolume, long processingTimeMs) {
        log.info("Order book update processed",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(EXCHANGE, exchange),
            StructuredArguments.kv("side", side),
            StructuredArguments.kv("levels", levels),
            StructuredArguments.kv("bestPrice", bestPrice),
            StructuredArguments.kv("totalVolume", totalVolume),
            StructuredArguments.kv(DURATION_MS, processingTimeMs),
            StructuredArguments.kv(OPERATION, "orderbook_update"),
            StructuredArguments.kv(STATUS, "processed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log trade event
     */
    public void logTradeEvent(String symbol, String exchange, double price, double quantity,
                             String side, long tradeTimestamp, long processingTimeMs) {
        log.info("Trade event processed",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(EXCHANGE, exchange),
            StructuredArguments.kv("price", price),
            StructuredArguments.kv("quantity", quantity),
            StructuredArguments.kv("side", side),
            StructuredArguments.kv("tradeValue", price * quantity),
            StructuredArguments.kv("tradeTimestamp", tradeTimestamp),
            StructuredArguments.kv(DURATION_MS, processingTimeMs),
            StructuredArguments.kv(OPERATION, "trade_event"),
            StructuredArguments.kv(STATUS, "processed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log WebSocket connection event
     */
    public void logWebSocketConnection(String clientId, String subscriptionType, String symbol,
                                     String action, boolean success, long durationMs) {
        log.info("WebSocket connection event",
            StructuredArguments.kv(CLIENT_ID, clientId),
            StructuredArguments.kv("subscriptionType", subscriptionType),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("action", action),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "websocket_connection"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log WebSocket message sent
     */
    public void logWebSocketMessageSent(String clientId, String messageType, String symbol,
                                       int messageSize, long latencyMs) {
        log.debug("WebSocket message sent",
            StructuredArguments.kv(CLIENT_ID, clientId),
            StructuredArguments.kv(MESSAGE_TYPE, messageType),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("messageSize", messageSize),
            StructuredArguments.kv(LATENCY_MS, latencyMs),
            StructuredArguments.kv(OPERATION, "websocket_message_send"),
            StructuredArguments.kv(STATUS, "sent"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log data quality issue
     */
    public void logDataQualityIssue(String symbol, String exchange, String provider,
                                   String issueType, String severity, String description,
                                   Map<String, Object> issueDetails) {
        var logBuilder = log.atWarn();
        
        logBuilder = logBuilder.addKeyValue(SYMBOL, symbol)
            .addKeyValue(EXCHANGE, exchange)
            .addKeyValue(PROVIDER, provider)
            .addKeyValue("issueType", issueType)
            .addKeyValue("severity", severity)
            .addKeyValue("description", description)
            .addKeyValue(OPERATION, "data_quality_check")
            .addKeyValue(DATA_QUALITY, "issue_detected")
            .addKeyValue("timestamp", Instant.now());
        
        if (issueDetails != null) {
            for (Map.Entry<String, Object> entry : issueDetails.entrySet()) {
                logBuilder = logBuilder.addKeyValue("issue_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Data quality issue detected");
    }
    
    /**
     * Log provider request
     */
    public void logProviderRequest(String provider, String endpoint, String method,
                                  int statusCode, long responseTimeMs, boolean success) {
        log.info("Provider request completed",
            StructuredArguments.kv(PROVIDER, provider),
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv("responseTime", responseTimeMs),
            StructuredArguments.kv(OPERATION, "provider_request"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log rate limit hit
     */
    public void logRateLimitHit(String provider, String endpoint, int requestCount,
                               int limitThreshold, long resetTimeMs) {
        log.warn("Rate limit hit",
            StructuredArguments.kv(PROVIDER, provider),
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("requestCount", requestCount),
            StructuredArguments.kv("limitThreshold", limitThreshold),
            StructuredArguments.kv("resetTimeMs", resetTimeMs),
            StructuredArguments.kv(OPERATION, "rate_limit_check"),
            StructuredArguments.kv(STATUS, "limit_exceeded"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log market event
     */
    public void logMarketEvent(String eventType, String market, String symbol, String description,
                              Map<String, Object> eventData) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("eventType", eventType)
            .addKeyValue("market", market)
            .addKeyValue(SYMBOL, symbol)
            .addKeyValue("description", description)
            .addKeyValue(OPERATION, "market_event")
            .addKeyValue(STATUS, "detected")
            .addKeyValue("timestamp", Instant.now());
        
        if (eventData != null) {
            for (Map.Entry<String, Object> entry : eventData.entrySet()) {
                logBuilder = logBuilder.addKeyValue("event_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Market event detected");
    }
    
    /**
     * Log streaming performance metrics
     */
    public void logStreamingMetrics(String messageType, int messagesPerSecond, 
                                   long averageLatencyMs, long maxLatencyMs,
                                   int activeConnections, int subscribedSymbols) {
        log.info("Streaming performance metrics",
            StructuredArguments.kv(MESSAGE_TYPE, messageType),
            StructuredArguments.kv("messagesPerSecond", messagesPerSecond),
            StructuredArguments.kv("averageLatency", averageLatencyMs),
            StructuredArguments.kv("maxLatency", maxLatencyMs),
            StructuredArguments.kv("activeConnections", activeConnections),
            StructuredArguments.kv("subscribedSymbols", subscribedSymbols),
            StructuredArguments.kv(OPERATION, "performance_metrics"),
            StructuredArguments.kv("category", "performance"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log cache operation
     */
    public void logCacheOperation(String operation, String key, boolean hit, 
                                 long durationMs, String dataType) {
        log.debug("Cache operation completed",
            StructuredArguments.kv("cacheOperation", operation),
            StructuredArguments.kv("key", key),
            StructuredArguments.kv("hit", hit),
            StructuredArguments.kv("dataType", dataType),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "cache_operation"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log database operation
     */
    public void logDatabaseOperation(String operation, String measurement, int pointCount,
                                   long durationMs, boolean success, String timeRange) {
        log.debug("Database operation completed",
            StructuredArguments.kv("dbOperation", operation),
            StructuredArguments.kv("measurement", measurement),
            StructuredArguments.kv("pointCount", pointCount),
            StructuredArguments.kv("timeRange", timeRange),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "database_operation"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log API request
     */
    public void logApiRequest(String endpoint, String method, String clientId, 
                             int statusCode, long durationMs, String userAgent) {
        log.info("API request processed",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv(CLIENT_ID, clientId),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv("userAgent", sanitizeUserAgent(userAgent)),
            StructuredArguments.kv(OPERATION, "api_request"),
            StructuredArguments.kv(STATUS, statusCode < 400 ? "success" : "error"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log subscription change
     */
    public void logSubscriptionChange(String clientId, String action, String symbol,
                                     String subscriptionType, boolean success) {
        log.info("Subscription change processed",
            StructuredArguments.kv(CLIENT_ID, clientId),
            StructuredArguments.kv("action", action),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("subscriptionType", subscriptionType),
            StructuredArguments.kv(OPERATION, "subscription_change"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log audit event for compliance
     */
    public void logAuditEvent(String eventType, String clientId, String action, String resource,
                             String outcome, Map<String, Object> auditData) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("eventType", eventType)
            .addKeyValue(CLIENT_ID, clientId)
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
     * Log error with context
     */
    public void logError(String operation, String errorType, String errorMessage,
                        String symbol, String provider, Exception exception) {
        log.error("Operation failed",
            StructuredArguments.kv(OPERATION, operation),
            StructuredArguments.kv("errorType", errorType),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(PROVIDER, provider),
            StructuredArguments.kv("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null),
            StructuredArguments.kv(STATUS, "error"),
            StructuredArguments.kv("timestamp", Instant.now()),
            exception
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
            .addKeyValue(STATUS, success ? "success" : "failure")
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "performance");
        
        if (additionalMetrics != null) {
            for (Map.Entry<String, Object> entry : additionalMetrics.entrySet()) {
                logBuilder = logBuilder.addKeyValue("metric_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Performance metrics recorded");
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
        
        logBuilder = logBuilder.addKeyValue("timestamp", Instant.now());
        
        for (Map.Entry<String, Object> entry : structuredData.entrySet()) {
            logBuilder = logBuilder.addKeyValue(entry.getKey(), entry.getValue());
        }
        
        logBuilder.log(message);
    }
    
    // Utility Methods
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        // Remove sensitive information and limit length
        return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
    }
}