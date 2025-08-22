package com.trademaster.marketdata.streaming;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-Time Market Data Streaming Service
 * 
 * Provides live market data streaming via WebSocket connections with
 * intelligent subscription management, data throttling, and performance optimization.
 * 
 * Features:
 * - Multi-symbol subscription management
 * - Intelligent data throttling and batching
 * - Connection health monitoring
 * - Real-time price alerts integration
 * - Performance analytics and metrics
 * - Graceful connection handling
 * - Memory-efficient data streaming
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class MarketDataStreaming implements WebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService messageProcessor;
    
    // Connection management
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UserSubscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> symbolSubscribers = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong bytesTransferred = new AtomicLong(0);
    
    // Market data cache
    private final Map<String, MarketDataSnapshot> marketDataCache = new ConcurrentHashMap<>();
    private final Map<String, PriceHistory> priceHistories = new ConcurrentHashMap<>();
    
    public MarketDataStreaming() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.messageProcessor = Executors.newCachedThreadPool();
        
        // Start background tasks
        startMarketDataGeneration();
        startConnectionHealthCheck();
        startPerformanceMonitoring();
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var sessionId = session.getId();
        var userId = extractUserId(session);
        
        log.info("WebSocket connection established - Session: {} User: {}", sessionId, userId);
        
        sessions.put(sessionId, session);
        subscriptions.put(sessionId, UserSubscription.builder()
            .sessionId(sessionId)
            .userId(userId)
            .connectedAt(Instant.now())
            .subscribedSymbols(new ConcurrentHashMap<>())
            .messagesSent(0L)
            .bytesTransferred(0L)
            .isActive(true)
            .build());
        
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        
        // Send welcome message
        sendWelcomeMessage(session);
        
        // Send current market overview
        sendMarketOverview(session);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        var sessionId = session.getId();
        var subscription = subscriptions.get(sessionId);
        
        if (subscription == null || !subscription.isActive()) {
            log.warn("Received message from inactive session: {}", sessionId);
            return;
        }
        
        try {
            var messageStr = message.getPayload().toString();
            var messageObj = objectMapper.readValue(messageStr, StreamingMessage.class);
            
            log.debug("Received message from session {}: {}", sessionId, messageObj.type());
            
            // Process message asynchronously
            messageProcessor.submit(() -> {
                try {
                    processClientMessage(session, subscription, messageObj);
                } catch (Exception e) {
                    log.error("Error processing message from session: " + sessionId, e);
                    sendErrorMessage(session, "Message processing failed", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error parsing message from session: " + sessionId, e);
            sendErrorMessage(session, "Invalid message format", e.getMessage());
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        var sessionId = session.getId();
        log.error("WebSocket transport error for session: " + sessionId, exception);
        
        // Update subscription status
        var subscription = subscriptions.get(sessionId);
        if (subscription != null) {
            subscription.setIsActive(false);
            subscription.setDisconnectedAt(Instant.now());
            subscription.setDisconnectReason("Transport error: " + exception.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        var sessionId = session.getId();
        
        log.info("WebSocket connection closed - Session: {} Status: {}", sessionId, closeStatus);
        
        // Clean up session
        sessions.remove(sessionId);
        var subscription = subscriptions.get(sessionId);
        
        if (subscription != null) {
            // Remove from symbol subscribers
            subscription.getSubscribedSymbols().keySet().forEach(symbol -> {
                var subscribers = symbolSubscribers.get(symbol);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        symbolSubscribers.remove(symbol);
                    }
                }
            });
            
            // Update subscription
            subscription.setIsActive(false);
            subscription.setDisconnectedAt(Instant.now());
            subscription.setDisconnectReason(closeStatus.toString());
        }
        
        activeConnections.decrementAndGet();
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void processClientMessage(WebSocketSession session, UserSubscription subscription, 
            StreamingMessage message) throws Exception {
        
        switch (message.type()) {
            case "SUBSCRIBE" -> handleSubscription(session, subscription, message);
            case "UNSUBSCRIBE" -> handleUnsubscription(session, subscription, message);
            case "PING" -> handlePing(session, message);
            case "GET_SNAPSHOT" -> handleSnapshotRequest(session, message);
            case "GET_HISTORY" -> handleHistoryRequest(session, message);
            case "UPDATE_PREFERENCES" -> handlePreferencesUpdate(session, subscription, message);
            default -> {
                log.warn("Unknown message type: {} from session: {}", message.type(), session.getId());
                sendErrorMessage(session, "Unknown message type", "Unsupported message type: " + message.type());
            }
        }
    }
    
    private void handleSubscription(WebSocketSession session, UserSubscription subscription, 
            StreamingMessage message) throws Exception {
        
        var data = message.data();
        var symbols = extractSymbolList(data.get("symbols"));
        var updateInterval = extractUpdateInterval(data.get("updateInterval"));
        
        log.info("Processing subscription for {} symbols from session: {}", 
            symbols.size(), session.getId());
        
        for (var symbol : symbols) {
            // Add to user subscription
            subscription.getSubscribedSymbols().put(symbol, SymbolSubscription.builder()
                .symbol(symbol)
                .subscribedAt(Instant.now())
                .updateInterval(updateInterval)
                .lastUpdateSent(Instant.now())
                .messageCount(0L)
                .build());
            
            // Add to symbol subscribers
            symbolSubscribers.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
        }
        
        // Send confirmation
        sendMessage(session, StreamingResponse.builder()
            .type("SUBSCRIPTION_CONFIRMED")
            .timestamp(Instant.now())
            .data(Map.of(
                "symbols", symbols,
                "updateInterval", updateInterval,
                "totalSubscriptions", subscription.getSubscribedSymbols().size()
            ))
            .build());
        
        // Send current data for subscribed symbols
        for (var symbol : symbols) {
            var snapshot = marketDataCache.get(symbol);
            if (snapshot != null) {
                sendMarketDataUpdate(session, snapshot);
            }
        }
    }
    
    private void handleUnsubscription(WebSocketSession session, UserSubscription subscription, 
            StreamingMessage message) throws Exception {
        
        var data = message.data();
        var symbols = extractSymbolList(data.get("symbols"));
        
        log.info("Processing unsubscription for {} symbols from session: {}", 
            symbols.size(), session.getId());
        
        for (var symbol : symbols) {
            // Remove from user subscription
            subscription.getSubscribedSymbols().remove(symbol);
            
            // Remove from symbol subscribers
            var subscribers = symbolSubscribers.get(symbol);
            if (subscribers != null) {
                subscribers.remove(session.getId());
                if (subscribers.isEmpty()) {
                    symbolSubscribers.remove(symbol);
                }
            }
        }
        
        // Send confirmation
        sendMessage(session, StreamingResponse.builder()
            .type("UNSUBSCRIPTION_CONFIRMED")
            .timestamp(Instant.now())
            .data(Map.of(
                "symbols", symbols,
                "remainingSubscriptions", subscription.getSubscribedSymbols().size()
            ))
            .build());
    }
    
    private void handlePing(WebSocketSession session, StreamingMessage message) throws Exception {
        // Send pong response
        sendMessage(session, StreamingResponse.builder()
            .type("PONG")
            .timestamp(Instant.now())
            .data(Map.of("originalTimestamp", message.timestamp()))
            .build());
    }
    
    private void handleSnapshotRequest(WebSocketSession session, StreamingMessage message) throws Exception {
        var symbols = extractSymbolList(message.data().get("symbols"));
        var snapshots = new ArrayList<MarketDataSnapshot>();
        
        for (var symbol : symbols) {
            var snapshot = marketDataCache.get(symbol);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        
        sendMessage(session, StreamingResponse.builder()
            .type("SNAPSHOTS")
            .timestamp(Instant.now())
            .data(Map.of("snapshots", snapshots))
            .build());
    }
    
    private void handleHistoryRequest(WebSocketSession session, StreamingMessage message) throws Exception {
        var data = message.data();
        var symbol = (String) data.get("symbol");
        var period = (String) data.getOrDefault("period", "1H");
        
        var history = priceHistories.get(symbol);
        if (history != null) {
            var filteredData = filterHistoryByPeriod(history, period);
            
            sendMessage(session, StreamingResponse.builder()
                .type("HISTORY")
                .timestamp(Instant.now())
                .data(Map.of(
                    "symbol", symbol,
                    "period", period,
                    "data", filteredData
                ))
                .build());
        } else {
            sendErrorMessage(session, "History not available", "No history data for symbol: " + symbol);
        }
    }
    
    private void handlePreferencesUpdate(WebSocketSession session, UserSubscription subscription, 
            StreamingMessage message) throws Exception {
        
        var data = message.data();
        var updateInterval = extractUpdateInterval(data.get("updateInterval"));
        var throttleUpdates = (Boolean) data.getOrDefault("throttleUpdates", true);
        
        // Update subscription preferences
        subscription.getSubscribedSymbols().values().forEach(symbolSub -> {
            symbolSub.setUpdateInterval(updateInterval);
            symbolSub.setThrottleUpdates(throttleUpdates);
        });
        
        sendMessage(session, StreamingResponse.builder()
            .type("PREFERENCES_UPDATED")
            .timestamp(Instant.now())
            .data(Map.of(
                "updateInterval", updateInterval,
                "throttleUpdates", throttleUpdates
            ))
            .build());
    }
    
    private void startMarketDataGeneration() {
        // Generate market data every 100ms
        scheduler.scheduleAtFixedRate(() -> {
            try {
                generateMarketData();
                broadcastMarketData();
            } catch (Exception e) {
                log.error("Error generating market data", e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    private void generateMarketData() {
        // Simulate market data for active symbols
        var activeSymbols = new HashSet<>(symbolSubscribers.keySet());
        
        // Add some default symbols if none are subscribed
        if (activeSymbols.isEmpty()) {
            activeSymbols.addAll(Set.of("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"));
        }
        
        for (var symbol : activeSymbols) {
            var currentSnapshot = marketDataCache.get(symbol);
            var newSnapshot = generateSnapshotForSymbol(symbol, currentSnapshot);
            
            marketDataCache.put(symbol, newSnapshot);
            updatePriceHistory(symbol, newSnapshot);
        }
    }
    
    private MarketDataSnapshot generateSnapshotForSymbol(String symbol, MarketDataSnapshot current) {
        var basePrice = current != null ? current.price() : BigDecimal.valueOf(100 + Math.random() * 400);
        
        // Generate realistic price movement
        var change = (Math.random() - 0.5) * 0.02; // ±1% change
        var newPrice = basePrice.multiply(BigDecimal.valueOf(1 + change))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        var volume = (long) (100000 + Math.random() * 1000000);
        var bid = newPrice.subtract(BigDecimal.valueOf(Math.random() * 0.10));
        var ask = newPrice.add(BigDecimal.valueOf(Math.random() * 0.10));
        
        return MarketDataSnapshot.builder()
            .symbol(symbol)
            .exchange("NASDAQ")
            .price(newPrice)
            .previousClose(current != null ? current.price() : newPrice)
            .change(current != null ? newPrice.subtract(current.price()) : BigDecimal.ZERO)
            .changePercent(current != null && current.price().compareTo(BigDecimal.ZERO) != 0 ? 
                newPrice.subtract(current.price())
                    .divide(current.price(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO)
            .volume(volume)
            .bid(bid)
            .ask(ask)
            .high(current != null ? current.high().max(newPrice) : newPrice)
            .low(current != null ? current.low().min(newPrice) : newPrice)
            .timestamp(Instant.now())
            .build();
    }
    
    private void updatePriceHistory(String symbol, MarketDataSnapshot snapshot) {
        priceHistories.computeIfAbsent(symbol, k -> new PriceHistory(symbol, new ArrayList<>()))
            .addDataPoint(PriceDataPoint.builder()
                .timestamp(snapshot.timestamp())
                .price(snapshot.price())
                .volume(snapshot.volume())
                .build());
        
        // Keep only last 1000 data points
        var history = priceHistories.get(symbol);
        if (history.getDataPoints().size() > 1000) {
            history.getDataPoints().remove(0);
        }
    }
    
    private void broadcastMarketData() {
        for (var entry : symbolSubscribers.entrySet()) {
            var symbol = entry.getKey();
            var subscribers = entry.getValue();
            var snapshot = marketDataCache.get(symbol);
            
            if (snapshot != null) {
                for (var sessionId : new HashSet<>(subscribers)) { // Copy to avoid concurrent modification
                    var session = sessions.get(sessionId);
                    var subscription = subscriptions.get(sessionId);
                    
                    if (session != null && subscription != null && subscription.isActive()) {
                        var symbolSub = subscription.getSubscribedSymbols().get(symbol);
                        if (symbolSub != null && shouldSendUpdate(symbolSub)) {
                            sendMarketDataUpdate(session, snapshot);
                            symbolSub.setLastUpdateSent(Instant.now());
                            symbolSub.setMessageCount(symbolSub.getMessageCount() + 1);
                        }
                    } else {
                        // Clean up inactive subscribers
                        subscribers.remove(sessionId);
                    }
                }
            }
        }
    }
    
    private boolean shouldSendUpdate(SymbolSubscription symbolSub) {
        var now = Instant.now();
        var timeSinceLastUpdate = now.toEpochMilli() - symbolSub.getLastUpdateSent().toEpochMilli();
        return timeSinceLastUpdate >= symbolSub.getUpdateInterval();
    }
    
    private void sendMarketDataUpdate(WebSocketSession session, MarketDataSnapshot snapshot) {
        try {
            sendMessage(session, StreamingResponse.builder()
                .type("MARKET_DATA")
                .timestamp(Instant.now())
                .data(Map.of("snapshot", snapshot))
                .build());
        } catch (Exception e) {
            log.error("Error sending market data update to session: " + session.getId(), e);
        }
    }
    
    private void startConnectionHealthCheck() {
        // Check connection health every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkConnectionHealth();
            } catch (Exception e) {
                log.error("Error during connection health check", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void checkConnectionHealth() {
        var now = Instant.now();
        var staleConnections = new ArrayList<String>();
        
        for (var entry : subscriptions.entrySet()) {
            var sessionId = entry.getKey();
            var subscription = entry.getValue();
            
            if (!subscription.isActive()) continue;
            
            var session = sessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                log.info("Marking stale connection as inactive: {}", sessionId);
                subscription.setIsActive(false);
                subscription.setDisconnectedAt(now);
                subscription.setDisconnectReason("Connection health check failed");
                staleConnections.add(sessionId);
            }
        }
        
        // Clean up stale connections
        for (var sessionId : staleConnections) {
            cleanupSession(sessionId);
        }
        
        log.debug("Connection health check completed. Active: {}, Cleaned up: {}", 
            activeConnections.get(), staleConnections.size());
    }
    
    private void startPerformanceMonitoring() {
        // Monitor performance every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logPerformanceMetrics();
            } catch (Exception e) {
                log.error("Error during performance monitoring", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
    
    private void logPerformanceMetrics() {
        var metrics = StreamingMetrics.builder()
            .totalConnections(totalConnections.get())
            .activeConnections(activeConnections.get())
            .messagesSent(messagesSent.get())
            .bytesTransferred(bytesTransferred.get())
            .activeSymbols(symbolSubscribers.size())
            .totalSubscriptions(subscriptions.values().stream()
                .mapToLong(sub -> sub.getSubscribedSymbols().size())
                .sum())
            .cacheSize(marketDataCache.size())
            .historySize(priceHistories.values().stream()
                .mapToLong(history -> history.getDataPoints().size())
                .sum())
            .timestamp(Instant.now())
            .build();
        
        log.info("Streaming Performance Metrics: {}", metrics);
    }
    
    private void sendWelcomeMessage(WebSocketSession session) {
        try {
            sendMessage(session, StreamingResponse.builder()
                .type("WELCOME")
                .timestamp(Instant.now())
                .data(Map.of(
                    "sessionId", session.getId(),
                    "serverTime", Instant.now(),
                    "features", List.of("REAL_TIME_QUOTES", "HISTORICAL_DATA", "ALERTS", "ANALYTICS"),
                    "updateIntervals", List.of(100, 250, 500, 1000, 5000),
                    "maxSymbols", 50
                ))
                .build());
        } catch (Exception e) {
            log.error("Error sending welcome message to session: " + session.getId(), e);
        }
    }
    
    private void sendMarketOverview(WebSocketSession session) {
        try {
            var overview = Map.of(
                "majorIndices", Map.of(
                    "S&P500", "4150.25",
                    "NASDAQ", "12800.50",
                    "DOW", "33500.75"
                ),
                "marketStatus", "OPEN",
                "tradingHours", "9:30 AM - 4:00 PM EST",
                "topMovers", List.of("AAPL", "TSLA", "GOOGL")
            );
            
            sendMessage(session, StreamingResponse.builder()
                .type("MARKET_OVERVIEW")
                .timestamp(Instant.now())
                .data(overview)
                .build());
        } catch (Exception e) {
            log.error("Error sending market overview to session: " + session.getId(), e);
        }
    }
    
    private void sendMessage(WebSocketSession session, StreamingResponse response) throws Exception {
        if (session.isOpen()) {
            var json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
            
            messagesSent.incrementAndGet();
            bytesTransferred.addAndGet(json.length());
            
            // Update subscription metrics
            var subscription = subscriptions.get(session.getId());
            if (subscription != null) {
                subscription.setMessagesSent(subscription.getMessagesSent() + 1);
                subscription.setBytesTransferred(subscription.getBytesTransferred() + json.length());
            }
        }
    }
    
    private void sendErrorMessage(WebSocketSession session, String error, String details) {
        try {
            sendMessage(session, StreamingResponse.builder()
                .type("ERROR")
                .timestamp(Instant.now())
                .data(Map.of(
                    "error", error,
                    "details", details
                ))
                .build());
        } catch (Exception e) {
            log.error("Error sending error message to session: " + session.getId(), e);
        }
    }
    
    private String extractUserId(WebSocketSession session) {
        // Extract user ID from session attributes or query parameters
        var userId = (String) session.getAttributes().get("userId");
        return userId != null ? userId : "anonymous_" + session.getId().substring(0, 8);
    }
    
    private List<String> extractSymbolList(Object symbolsObj) {
        if (symbolsObj instanceof List<?> list) {
            return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        }
        return List.of();
    }
    
    private int extractUpdateInterval(Object intervalObj) {
        if (intervalObj instanceof Number number) {
            return Math.max(100, Math.min(60000, number.intValue())); // 100ms to 60s
        }
        return 1000; // Default 1 second
    }
    
    private List<PriceDataPoint> filterHistoryByPeriod(PriceHistory history, String period) {
        var cutoff = switch (period) {
            case "1M" -> Instant.now().minusSeconds(60);
            case "5M" -> Instant.now().minusSeconds(300);
            case "1H" -> Instant.now().minusSeconds(3600);
            case "1D" -> Instant.now().minusSeconds(86400);
            default -> Instant.now().minusSeconds(3600); // Default 1 hour
        };
        
        return history.getDataPoints().stream()
            .filter(point -> point.timestamp().isAfter(cutoff))
            .toList();
    }
    
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        var subscription = subscriptions.get(sessionId);
        
        if (subscription != null) {
            subscription.getSubscribedSymbols().keySet().forEach(symbol -> {
                var subscribers = symbolSubscribers.get(symbol);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        symbolSubscribers.remove(symbol);
                    }
                }
            });
        }
    }
    
    // Data classes for streaming
    
    @Builder
    public record StreamingMessage(
        String type,
        Instant timestamp,
        Map<String, Object> data
    ) {}
    
    @Builder
    public record StreamingResponse(
        String type,
        Instant timestamp,
        Map<String, Object> data
    ) {}
    
    @Builder
    public record MarketDataSnapshot(
        String symbol,
        String exchange,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal change,
        BigDecimal changePercent,
        Long volume,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal high,
        BigDecimal low,
        Instant timestamp
    ) {}
    
    @Builder
    public record PriceDataPoint(
        Instant timestamp,
        BigDecimal price,
        Long volume
    ) {}
    
    public static class PriceHistory {
        private final String symbol;
        private final List<PriceDataPoint> dataPoints;
        
        public PriceHistory(String symbol, List<PriceDataPoint> dataPoints) {
            this.symbol = symbol;
            this.dataPoints = dataPoints;
        }
        
        public void addDataPoint(PriceDataPoint point) {
            dataPoints.add(point);
        }
        
        public String getSymbol() { return symbol; }
        public List<PriceDataPoint> getDataPoints() { return dataPoints; }
    }
    
    @Builder
    public static class UserSubscription {
        private String sessionId;
        private String userId;
        private Instant connectedAt;
        private Map<String, SymbolSubscription> subscribedSymbols;
        private Long messagesSent;
        private Long bytesTransferred;
        private Boolean isActive;
        private Instant disconnectedAt;
        private String disconnectReason;
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Instant getConnectedAt() { return connectedAt; }
        public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
        public Map<String, SymbolSubscription> getSubscribedSymbols() { return subscribedSymbols; }
        public void setSubscribedSymbols(Map<String, SymbolSubscription> subscribedSymbols) { this.subscribedSymbols = subscribedSymbols; }
        public Long getMessagesSent() { return messagesSent; }
        public void setMessagesSent(Long messagesSent) { this.messagesSent = messagesSent; }
        public Long getBytesTransferred() { return bytesTransferred; }
        public void setBytesTransferred(Long bytesTransferred) { this.bytesTransferred = bytesTransferred; }
        public Boolean isActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Instant getDisconnectedAt() { return disconnectedAt; }
        public void setDisconnectedAt(Instant disconnectedAt) { this.disconnectedAt = disconnectedAt; }
        public String getDisconnectReason() { return disconnectReason; }
        public void setDisconnectReason(String disconnectReason) { this.disconnectReason = disconnectReason; }
    }
    
    @Builder
    public static class SymbolSubscription {
        private String symbol;
        private Instant subscribedAt;
        private Integer updateInterval;
        private Instant lastUpdateSent;
        private Long messageCount;
        private Boolean throttleUpdates;
        
        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public Instant getSubscribedAt() { return subscribedAt; }
        public void setSubscribedAt(Instant subscribedAt) { this.subscribedAt = subscribedAt; }
        public Integer getUpdateInterval() { return updateInterval; }
        public void setUpdateInterval(Integer updateInterval) { this.updateInterval = updateInterval; }
        public Instant getLastUpdateSent() { return lastUpdateSent; }
        public void setLastUpdateSent(Instant lastUpdateSent) { this.lastUpdateSent = lastUpdateSent; }
        public Long getMessageCount() { return messageCount; }
        public void setMessageCount(Long messageCount) { this.messageCount = messageCount; }
        public Boolean getThrottleUpdates() { return throttleUpdates; }
        public void setThrottleUpdates(Boolean throttleUpdates) { this.throttleUpdates = throttleUpdates; }
    }
    
    @Builder
    public record StreamingMetrics(
        Long totalConnections,
        Long activeConnections,
        Long messagesSent,
        Long bytesTransferred,
        Integer activeSymbols,
        Long totalSubscriptions,
        Integer cacheSize,
        Long historySize,
        Instant timestamp
    ) {}
}