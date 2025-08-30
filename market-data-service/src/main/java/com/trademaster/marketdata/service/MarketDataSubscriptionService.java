package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.SubscriptionRequest;
import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;

/**
 * Market Data Subscription Service
 * 
 * Features:
 * - WebSocket subscription management
 * - Symbol-based subscription filtering
 * - Real-time data broadcasting
 * - Subscription lifecycle management
 * - Integration with data sources
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataSubscriptionService {

    private final MarketDataService marketDataService;
    private final MarketDataCacheService cacheService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Active subscriptions: sessionId -> SubscriptionInfo
    private final Map<String, SubscriptionInfo> activeSubscriptions = new ConcurrentHashMap<>();
    
    // Symbol subscriptions: symbol:exchange -> Set<sessionId>
    private final Map<String, Set<String>> symbolSubscriptions = new ConcurrentHashMap<>();

    /**
     * Subscribe to market data for specific symbols
     */
    public boolean subscribe(String sessionId, SubscriptionRequest request) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            log.info("Processing subscription for session {}: symbols={}, dataTypes={}", 
                sessionId, request.getSymbols(), request.getDataTypes());
            
            var subscriptionTask = scope.fork(() -> {
                // Create subscription info
                SubscriptionInfo subscription = new SubscriptionInfo(
                    sessionId,
                    new ArrayList<>(request.getSymbols()),
                    request.getDataTypes().stream().map(Enum::name).toList(),
                    request.getExchange(),
                    System.currentTimeMillis()
                );
                
                // Store subscription
                activeSubscriptions.put(sessionId, subscription);
                
                // Add to symbol-based index
                for (String symbol : request.getSymbols()) {
                    String symbolKey = symbol + ":" + request.getExchange();
                    symbolSubscriptions.computeIfAbsent(symbolKey, k -> ConcurrentHashMap.newKeySet())
                        .add(sessionId);
                }
                
                // Send initial snapshot data
                sendSnapshotData(sessionId, request);
                
                log.info("Subscription created for session {}: {} symbols on {}", 
                    sessionId, request.getSymbols().size(), request.getExchange());
                
                return true;
            });
            
            scope.join();
            scope.throwIfFailed();
            
            return subscriptionTask.get();
            
        } catch (Exception e) {
            log.error("Failed to create subscription for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Unsubscribe from specific symbols
     */
    public boolean unsubscribe(String sessionId, List<String> symbols) {
        try {
            SubscriptionInfo subscription = activeSubscriptions.get(sessionId);
            if (subscription == null) {
                log.warn("No active subscription found for session {}", sessionId);
                return false;
            }
            
            // Remove symbols from subscription
            subscription.symbols.removeAll(symbols);
            
            // Remove from symbol index
            for (String symbol : symbols) {
                String symbolKey = symbol + ":" + subscription.exchange;
                Set<String> sessions = symbolSubscriptions.get(symbolKey);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        symbolSubscriptions.remove(symbolKey);
                    }
                }
            }
            
            log.info("Unsubscribed session {} from {} symbols", sessionId, symbols.size());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to unsubscribe session {} from symbols: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Remove all subscriptions for a session
     */
    public void removeAllSubscriptions(String sessionId) {
        try {
            SubscriptionInfo subscription = activeSubscriptions.remove(sessionId);
            if (subscription != null) {
                // Remove from symbol index
                for (String symbol : subscription.symbols) {
                    String symbolKey = symbol + ":" + subscription.exchange;
                    Set<String> sessions = symbolSubscriptions.get(symbolKey);
                    if (sessions != null) {
                        sessions.remove(sessionId);
                        if (sessions.isEmpty()) {
                            symbolSubscriptions.remove(symbolKey);
                        }
                    }
                }
                
                log.info("Removed all subscriptions for session {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error removing subscriptions for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Get sessions subscribed to a symbol
     */
    public Set<String> getSubscribedSessions(String symbol, String exchange) {
        String symbolKey = symbol + ":" + exchange;
        return symbolSubscriptions.getOrDefault(symbolKey, Set.of());
    }

    /**
     * Get supported symbols (from cache or data source)
     */
    public List<String> getSupportedSymbols() {
        try {
            // Get active symbols from market data service
            return marketDataService.getActiveSymbols("NSE", 60).join();
            
        } catch (Exception e) {
            log.error("Failed to get supported symbols: {}", e.getMessage());
            return getDefaultSymbols();
        }
    }

    /**
     * Get subscription ID for a session
     */
    public String getSubscriptionId(String sessionId) {
        SubscriptionInfo subscription = activeSubscriptions.get(sessionId);
        return subscription != null ? "sub_" + sessionId + "_" + subscription.createdAt : null;
    }

    /**
     * Get snapshot data for symbols
     */
    public Map<String, Object> getSnapshot(List<String> symbols) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            Map<String, Object> snapshot = new ConcurrentHashMap<>();
            
            var snapshotTask = scope.fork(() -> {
                symbols.parallelStream().forEach(symbol -> {
                    try {
                        // Try cache first for fast response
                        var cachedPrice = cacheService.getCurrentPrice(symbol, "NSE");
                        if (cachedPrice.isPresent()) {
                            var price = cachedPrice.get();
                            snapshot.put(symbol, Map.of(
                                "symbol", price.symbol(),
                                "price", price.price(),
                                "volume", price.volume(),
                                "change", price.change(),
                                "changePercent", price.changePercent(),
                                "timestamp", price.marketTime()
                            ));
                        } else {
                            // Fallback to service
                            marketDataService.getCurrentPrice(symbol, "NSE")
                                .thenAccept(dataPoint -> {
                                    if (dataPoint.isPresent()) {
                                        var point = dataPoint.get();
                                        snapshot.put(symbol, Map.of(
                                            "symbol", point.symbol(),
                                            "price", point.price(),
                                            "volume", point.volume(),
                                            "timestamp", point.timestamp()
                                        ));
                                    }
                                });
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get snapshot for symbol {}: {}", symbol, e.getMessage());
                    }
                });
                
                return snapshot;
            });
            
            scope.join();
            scope.throwIfFailed();
            
            return snapshotTask.get();
            
        } catch (Exception e) {
            log.error("Failed to get snapshot data: {}", e.getMessage());
            return Map.of("error", "Failed to retrieve snapshot data");
        }
    }

    /**
     * Broadcast market data update to subscribed sessions
     */
    public void broadcastMarketDataUpdate(MarketDataPoint data) {
        try {
            String symbolKey = data.symbol() + ":" + data.exchange();
            Set<String> subscribedSessions = symbolSubscriptions.get(symbolKey);
            
            if (subscribedSessions != null && !subscribedSessions.isEmpty()) {
                // This would be handled by the WebSocket handler
                // For now, we'll use Kafka to notify the WebSocket service
                kafkaTemplate.send("websocket-broadcasts", symbolKey, data);
                
                log.trace("Broadcasted {} update to {} sessions", 
                    symbolKey, subscribedSessions.size());
            }
            
        } catch (Exception e) {
            log.error("Error broadcasting market data update: {}", e.getMessage());
        }
    }

    /**
     * Send initial snapshot data to new subscription
     */
    private void sendSnapshotData(String sessionId, SubscriptionRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> snapshot = getSnapshot(request.getSymbols());
                
                // Send snapshot via WebSocket
                // This would integrate with the WebSocket handler
                kafkaTemplate.send("websocket-snapshots", sessionId, snapshot);
                
                log.debug("Sent snapshot data to session {} for {} symbols", 
                    sessionId, request.getSymbols().size());
                
            } catch (Exception e) {
                log.error("Failed to send snapshot data to session {}: {}", sessionId, e.getMessage());
            }
        });
    }

    /**
     * Get default supported symbols
     */
    private List<String> getDefaultSymbols() {
        return List.of(
            "RELIANCE", "TCS", "INFY", "HDFC", "ICICIBANK", 
            "SBIN", "BAJFINANCE", "BHARTIARTL", "ITC", "KOTAKBANK",
            "LT", "HDFCBANK", "MARUTI", "ASIANPAINT", "NESTLEIND"
        );
    }

    /**
     * Get subscription statistics
     */
    public SubscriptionStats getSubscriptionStats() {
        int totalSymbols = symbolSubscriptions.size();
        long totalSessions = activeSubscriptions.size();
        
        Map<String, Long> symbolCounts = new ConcurrentHashMap<>();
        symbolSubscriptions.forEach((symbol, sessions) -> {
            symbolCounts.put(symbol, (long) sessions.size());
        });
        
        return new SubscriptionStats(
            totalSessions,
            totalSymbols,
            symbolCounts,
            System.currentTimeMillis()
        );
    }

    /**
     * Subscription information
     */
    private static class SubscriptionInfo {
        final String sessionId;
        final Set<String> symbols;
        final List<String> dataTypes;
        final String exchange;
        final long createdAt;

        SubscriptionInfo(String sessionId, List<String> symbols, List<String> dataTypes, 
                        String exchange, long createdAt) {
            this.sessionId = sessionId;
            this.symbols = ConcurrentHashMap.newKeySet();
            this.symbols.addAll(symbols);
            this.dataTypes = dataTypes;
            this.exchange = exchange;
            this.createdAt = createdAt;
        }
    }

    /**
     * Subscription statistics
     */
    public record SubscriptionStats(
        long totalSessions,
        long totalSymbols,
        Map<String, Long> symbolSubscriptionCounts,
        long timestamp
    ) {}
}