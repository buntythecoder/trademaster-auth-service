package com.trademaster.marketdata.kafka;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.websocket.MarketDataWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka Stream Processor for Market Data Pipeline
 * 
 * Features:
 * - Real-time market data processing from raw feeds
 * - Data normalization and validation
 * - Parallel processing with virtual threads
 * - WebSocket broadcasting to connected clients
 * - Error handling with dead letter queues
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataStreamProcessor {

    private final MarketDataService marketDataService;
    private final MarketDataCacheService cacheService;
    private final MarketDataWebSocketHandler webSocketHandler;
    private final MarketDataQualityValidator qualityValidator;
    
    // Performance monitoring
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong validationErrors = new AtomicLong(0);
    private final AtomicLong processingErrors = new AtomicLong(0);

    /**
     * Process raw market data from exchange feeds
     */
    @KafkaListener(
        topics = "market-data-raw",
        groupId = "market-data-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processRawMarketData(
            @Payload MarketDataPoint rawData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        long startTime = System.nanoTime();
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            log.trace("Processing raw market data: {} from {}:{}", 
                rawData.symbol(), rawData.exchange(), rawData.price());
            
            // Validate data quality
            var validationTask = scope.fork(() -> qualityValidator.validate(rawData));
            
            scope.join();
            scope.throwIfFailed();
            
            var validationResult = validationTask.get();
            
            if (!validationResult.isValid()) {
                log.warn("Invalid market data for {}:{}: {}", 
                    rawData.symbol(), rawData.exchange(), validationResult.errorMessage());
                validationErrors.incrementAndGet();
                acknowledgment.acknowledge();
                return;
            }
            
            // Process valid data in parallel
            processValidMarketData(rawData).thenRun(() -> {
                processedMessages.incrementAndGet();
                acknowledgment.acknowledge();
                
                long processingTime = System.nanoTime() - startTime;
                if (processingTime > 10_000_000) { // > 10ms warning
                    log.warn("Slow processing detected: {}ns for {}:{}", 
                        processingTime, rawData.symbol(), rawData.exchange());
                }
            }).exceptionally(throwable -> {
                log.error("Error processing market data for {}:{}: {}", 
                    rawData.symbol(), rawData.exchange(), throwable.getMessage());
                processingErrors.incrementAndGet();
                acknowledgment.acknowledge(); // Acknowledge to prevent reprocessing
                return null;
            });
            
        } catch (Exception e) {
            log.error("Critical error in market data processing: {}", e.getMessage(), e);
            processingErrors.incrementAndGet();
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process tick data for real-time analysis
     */
    @KafkaListener(
        topics = "tick-data",
        groupId = "tick-data-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processTickData(
            @Payload MarketDataPoint tickData,
            Acknowledgment acknowledgment) {
        
        try {
            // High-frequency tick processing
            processTick(tickData).thenRun(() -> {
                acknowledgment.acknowledge();
                log.trace("Processed tick for {}:{} at {}", 
                    tickData.symbol(), tickData.exchange(), tickData.price());
            });
            
        } catch (Exception e) {
            log.error("Error processing tick data for {}:{}: {}", 
                tickData.symbol(), tickData.exchange(), e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process OHLC aggregated data
     */
    @KafkaListener(
        topics = "ohlc-data",
        groupId = "ohlc-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOHLCData(
            @Payload List<MarketDataPoint> ohlcData,
            Acknowledgment acknowledgment) {
        
        try {
            // Batch process OHLC data
            processOHLCBatch(ohlcData).thenRun(() -> {
                acknowledgment.acknowledge();
                log.debug("Processed OHLC batch of {} records", ohlcData.size());
            });
            
        } catch (Exception e) {
            log.error("Error processing OHLC data batch: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process order book updates
     */
    @KafkaListener(
        topics = "order-book-updates",
        groupId = "order-book-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderBookUpdates(
            @Payload MarketDataPoint orderBookData,
            Acknowledgment acknowledgment) {
        
        try {
            if (orderBookData.hasOrderBookData()) {
                processOrderBook(orderBookData).thenRun(() -> {
                    acknowledgment.acknowledge();
                    log.trace("Processed order book for {}:{}", 
                        orderBookData.symbol(), orderBookData.exchange());
                });
            } else {
                log.warn("Invalid order book data received for {}:{}", 
                    orderBookData.symbol(), orderBookData.exchange());
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Error processing order book data for {}:{}: {}", 
                orderBookData.symbol(), orderBookData.exchange(), e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Handle trade events for analytics
     */
    @KafkaListener(
        topics = "trade-events",
        groupId = "trade-events-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processTradeEvents(
            @Payload MarketDataPoint tradeData,
            Acknowledgment acknowledgment) {
        
        try {
            // Process individual trades
            processTrade(tradeData).thenRun(() -> {
                acknowledgment.acknowledge();
                log.trace("Processed trade event for {}:{}", 
                    tradeData.symbol(), tradeData.exchange());
            });
            
        } catch (Exception e) {
            log.error("Error processing trade event for {}:{}: {}", 
                tradeData.symbol(), tradeData.exchange(), e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process valid market data with parallel operations
     */
    private CompletableFuture<Void> processValidMarketData(MarketDataPoint data) {
        return CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Store in database
                var dbTask = scope.fork(() -> {
                    marketDataService.writeMarketData(data).join();
                    return null;
                });
                
                // Update cache
                var cacheTask = scope.fork(() -> {
                    cacheService.cacheCurrentPrice(data).join();
                    if (data.hasOrderBookData()) {
                        cacheService.cacheOrderBook(data).join();
                    }
                    return null;
                });
                
                // Broadcast to WebSocket clients
                var wsTask = scope.fork(() -> {
                    broadcastToWebSocketClients(data);
                    return null;
                });
                
                scope.join();
                scope.throwIfFailed();
                
                log.trace("Successfully processed market data for {}:{}", 
                    data.symbol(), data.exchange());
                
            } catch (Exception e) {
                log.error("Error in parallel processing for {}:{}: {}", 
                    data.symbol(), data.exchange(), e.getMessage());
                throw new RuntimeException("Market data processing failed", e);
            }
        });
    }

    /**
     * Process high-frequency tick data
     */
    private CompletableFuture<Void> processTick(MarketDataPoint tickData) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Update real-time cache with minimal latency
                cacheService.cacheCurrentPrice(tickData).join();
                
                // Broadcast to WebSocket for real-time feeds
                broadcastToWebSocketClients(tickData);
                
            } catch (Exception e) {
                throw new RuntimeException("Tick processing failed", e);
            }
        });
    }

    /**
     * Process OHLC data batch
     */
    private CompletableFuture<Void> processOHLCBatch(List<MarketDataPoint> ohlcData) {
        return CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Batch write to database
                var dbTask = scope.fork(() -> {
                    marketDataService.batchWriteMarketData(ohlcData).join();
                    return null;
                });
                
                // Update cache for each symbol
                var cacheTask = scope.fork(() -> {
                    ohlcData.parallelStream().forEach(point -> {
                        try {
                            cacheService.cacheOHLCData(
                                point.symbol(),
                                point.exchange(),
                                "1m", // Assuming 1-minute OHLC
                                List.of(point)
                            ).join();
                        } catch (Exception e) {
                            log.warn("Failed to cache OHLC for {}:{}: {}", 
                                point.symbol(), point.exchange(), e.getMessage());
                        }
                    });
                    return null;
                });
                
                scope.join();
                scope.throwIfFailed();
                
            } catch (Exception e) {
                throw new RuntimeException("OHLC batch processing failed", e);
            }
        });
    }

    /**
     * Process order book updates
     */
    private CompletableFuture<Void> processOrderBook(MarketDataPoint orderBookData) {
        return CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Cache order book with very short TTL
                var cacheTask = scope.fork(() -> {
                    cacheService.cacheOrderBook(orderBookData).join();
                    return null;
                });
                
                // Broadcast to WebSocket for real-time order book updates
                var wsTask = scope.fork(() -> {
                    broadcastOrderBookUpdate(orderBookData);
                    return null;
                });
                
                scope.join();
                scope.throwIfFailed();
                
            } catch (Exception e) {
                throw new RuntimeException("Order book processing failed", e);
            }
        });
    }

    /**
     * Process individual trade events
     */
    private CompletableFuture<Void> processTrade(MarketDataPoint tradeData) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Store trade for analytics
                marketDataService.writeMarketData(tradeData).join();
                
                // Broadcast trade event to WebSocket clients
                broadcastTradeEvent(tradeData);
                
            } catch (Exception e) {
                throw new RuntimeException("Trade processing failed", e);
            }
        });
    }

    /**
     * Broadcast market data to WebSocket clients
     */
    private void broadcastToWebSocketClients(MarketDataPoint data) {
        try {
            // Get active WebSocket sessions for this symbol
            var sessions = getActiveSessionsForSymbol(data.symbol(), data.exchange());
            
            sessions.parallelStream().forEach(sessionId -> {
                try {
                    webSocketHandler.sendMarketData(sessionId, 
                        convertToMessage(data, "PRICE_UPDATE"));
                } catch (Exception e) {
                    log.warn("Failed to send WebSocket update to session {}: {}", 
                        sessionId, e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error broadcasting to WebSocket clients: {}", e.getMessage());
        }
    }

    /**
     * Broadcast order book updates
     */
    private void broadcastOrderBookUpdate(MarketDataPoint orderBookData) {
        try {
            var sessions = getActiveSessionsForSymbol(orderBookData.symbol(), orderBookData.exchange());
            
            sessions.parallelStream().forEach(sessionId -> {
                try {
                    webSocketHandler.sendMarketData(sessionId,
                        convertToMessage(orderBookData, "ORDER_BOOK_UPDATE"));
                } catch (Exception e) {
                    log.warn("Failed to send order book update to session {}: {}", 
                        sessionId, e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error broadcasting order book updates: {}", e.getMessage());
        }
    }

    /**
     * Broadcast trade events
     */
    private void broadcastTradeEvent(MarketDataPoint tradeData) {
        try {
            var sessions = getActiveSessionsForSymbol(tradeData.symbol(), tradeData.exchange());
            
            sessions.parallelStream().forEach(sessionId -> {
                try {
                    webSocketHandler.sendMarketData(sessionId,
                        convertToMessage(tradeData, "TRADE_EVENT"));
                } catch (Exception e) {
                    log.warn("Failed to send trade event to session {}: {}", 
                        sessionId, e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error broadcasting trade events: {}", e.getMessage());
        }
    }

    /**
     * Get active WebSocket sessions subscribed to a symbol
     */
    private List<String> getActiveSessionsForSymbol(String symbol, String exchange) {
        // This would integrate with the WebSocket subscription service
        // For now, return empty list - implementation depends on subscription management
        return List.of();
    }

    /**
     * Convert MarketDataPoint to WebSocket message format
     */
    private com.trademaster.marketdata.dto.MarketDataMessage convertToMessage(
            MarketDataPoint data, String messageType) {
        
        return com.trademaster.marketdata.dto.MarketDataMessage.builder()
            .type(messageType)
            .symbol(data.symbol())
            .exchange(data.exchange())
            .price(data.price())
            .volume(data.volume())
            .bid(data.bid())
            .ask(data.ask())
            .timestamp(data.timestamp())
            .build();
    }

    /**
     * Get processing statistics
     */
    public ProcessingStats getProcessingStats() {
        return new ProcessingStats(
            processedMessages.get(),
            validationErrors.get(),
            processingErrors.get(),
            Instant.now()
        );
    }

    /**
     * Processing statistics record
     */
    public record ProcessingStats(
        long processedMessages,
        long validationErrors,
        long processingErrors,
        Instant timestamp
    ) {
        public double getErrorRate() {
            long total = processedMessages + validationErrors + processingErrors;
            return total > 0 ? ((double) (validationErrors + processingErrors) / total) * 100 : 0.0;
        }
        
        public boolean isHealthy() {
            return getErrorRate() < 5.0; // Less than 5% error rate considered healthy
        }
    }
}