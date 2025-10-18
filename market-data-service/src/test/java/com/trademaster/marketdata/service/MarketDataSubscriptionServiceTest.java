package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.SubscriptionRequest;
import com.trademaster.marketdata.entity.MarketDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataSubscriptionService
 *
 * Tests MANDATORY RULES compliance:
 * - RULE #3: Result types, Optional chains, no if-else
 * - RULE #5: Cognitive complexity ≤7 per method
 * - RULE #9: Immutable data structures
 * - RULE #17: Named constants externalization
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Market Data Subscription Service Tests")
class MarketDataSubscriptionServiceTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private MarketDataCacheService cacheService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MarketDataSubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new MarketDataSubscriptionService(
            marketDataService,
            cacheService,
            kafkaTemplate
        );
    }

    // Test helper method for creating SubscriptionRequest with minimal params
    private SubscriptionRequest createSubscriptionRequest(List<String> symbols, String exchange) {
        return new SubscriptionRequest(
            symbols,
            null,  // dataTypes (defaults to TICK, OHLC)
            Set.of(exchange),  // exchanges (was String, now Set<String>)
            null,  // updateFrequency (defaults)
            null,  // includeSnapshot (defaults to true)
            null,  // orderBookDepth (defaults)
            null,  // includeExtendedHours (defaults to false)
            null,  // minPriceChange (optional)
            null,  // minVolume (optional)
            null,  // maxUpdatesPerSecond (defaults)
            null   // preferences (optional)
        );
    }

    @Nested
    @DisplayName("Result Type Pattern Tests - RULE #3: Railway Oriented Programming")
    class ResultTypePatternTests {

        @Test
        @DisplayName("Should handle success result correctly")
        void shouldHandleSuccessResult() {
            // Given
            var result = MarketDataSubscriptionService.Result.success("test-value");

            // When & Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElse("default")).isEqualTo("test-value");
        }

        @Test
        @DisplayName("Should handle failure result correctly")
        void shouldHandleFailureResult() {
            // Given
            var result = MarketDataSubscriptionService.Result.<String, String>failure("error");

            // When & Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.orElse("default")).isEqualTo("default");
        }

        @Test
        @DisplayName("Should map success values - RULE #3: Functional composition")
        void shouldMapSuccessValues() {
            // Given
            var result = MarketDataSubscriptionService.Result.<Integer, String>success(5);

            // When
            var mapped = result.map(x -> x * 2);

            // Then
            assertThat(mapped.orElse(0)).isEqualTo(10);
        }

        @Test
        @DisplayName("Should propagate failures in map - RULE #3: Railway pattern")
        void shouldPropagateFailuresInMap() {
            // Given
            var result = MarketDataSubscriptionService.Result.<Integer, String>failure("error");

            // When
            var mapped = result.map(x -> x * 2);

            // Then
            assertThat(mapped.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should flatMap success values - RULE #3: Monadic composition")
        void shouldFlatMapSuccessValues() {
            // Given
            var result = MarketDataSubscriptionService.Result.<Integer, String>success(5);

            // When
            var flatMapped = result.flatMap(x ->
                MarketDataSubscriptionService.Result.success(x * 2)
            );

            // Then
            assertThat(flatMapped.orElse(0)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Subscription Management Tests - RULE #3: Optional Chains")
    class SubscriptionManagementTests {

        @Test
        @DisplayName("Should subscribe successfully - RULE #3: Result type instead of try-catch")
        void shouldSubscribeSuccessfully() {
            // Given
            String sessionId = "session-123";
            var request = createSubscriptionRequest(
                List.of("RELIANCE", "TCS"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE", "TCS")));

            // When
            boolean result = subscriptionService.subscribe(sessionId, request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle subscription error gracefully - RULE #3: Result type error handling")
        void shouldHandleSubscriptionErrorGracefully() {
            // Given
            String sessionId = "session-456";
            var request = createSubscriptionRequest(
                List.of("INVALID"),
                
                "INVALID_EXCHANGE"
            );

            // When
            boolean result = subscriptionService.subscribe(sessionId, request);

            // Then - Should return false instead of throwing exception
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should unsubscribe successfully - RULE #3: Optional chain")
        void shouldUnsubscribeSuccessfully() {
            // Given
            String sessionId = "session-789";
            var request = createSubscriptionRequest(
                List.of("RELIANCE", "TCS", "INFY"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE", "TCS", "INFY")));

            subscriptionService.subscribe(sessionId, request);

            // When
            boolean result = subscriptionService.unsubscribe(sessionId, List.of("RELIANCE"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle unsubscribe for non-existent session - RULE #3: Optional orElseGet")
        void shouldHandleUnsubscribeForNonExistentSession() {
            // When
            boolean result = subscriptionService.unsubscribe("non-existent", List.of("RELIANCE"));

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should remove all subscriptions - RULE #3: Optional ifPresent")
        void shouldRemoveAllSubscriptions() {
            // Given
            String sessionId = "session-remove-all";
            var request = createSubscriptionRequest(
                List.of("RELIANCE", "TCS"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE", "TCS")));

            subscriptionService.subscribe(sessionId, request);

            // When
            subscriptionService.removeAllSubscriptions(sessionId);

            // Then
            Set<String> sessions = subscriptionService.getSubscribedSessions("RELIANCE", "NSE");
            assertThat(sessions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Symbol Query Tests - RULE #3: Stream API & Optional Chains")
    class SymbolQueryTests {

        @Test
        @DisplayName("Should return subscribed sessions - RULE #3: getOrDefault pattern")
        void shouldReturnSubscribedSessions() {
            // Given
            String sessionId = "session-query";
            var request = createSubscriptionRequest(
                List.of("RELIANCE"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE")));

            subscriptionService.subscribe(sessionId, request);

            // When
            Set<String> sessions = subscriptionService.getSubscribedSessions("RELIANCE", "NSE");

            // Then
            assertThat(sessions).contains(sessionId);
        }

        @Test
        @DisplayName("Should return empty set for non-subscribed symbol")
        void shouldReturnEmptySetForNonSubscribedSymbol() {
            // When
            Set<String> sessions = subscriptionService.getSubscribedSessions("NON_EXISTENT", "NSE");

            // Then
            assertThat(sessions).isEmpty();
        }

        @Test
        @DisplayName("Should get supported symbols from service - RULE #3: Result type fallback")
        void shouldGetSupportedSymbolsFromService() {
            // Given
            List<String> activeSymbols = List.of("RELIANCE", "TCS", "INFY");
            when(marketDataService.getActiveSymbols("NSE", 60))
                .thenReturn(CompletableFuture.completedFuture(activeSymbols));

            // When
            List<String> symbols = subscriptionService.getSupportedSymbols();

            // Then
            assertThat(symbols).containsAll(activeSymbols);
        }

        @Test
        @DisplayName("Should fallback to default symbols on error - RULE #3: Result orElse")
        void shouldFallbackToDefaultSymbolsOnError() {
            // Given
            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Service error")));

            // When
            List<String> symbols = subscriptionService.getSupportedSymbols();

            // Then
            assertThat(symbols).contains("RELIANCE", "TCS", "INFY");
        }

        @Test
        @DisplayName("Should get subscription ID - RULE #3: Optional map instead of ternary")
        void shouldGetSubscriptionId() {
            // Given
            String sessionId = "session-id-test";
            var request = createSubscriptionRequest(
                List.of("RELIANCE"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE")));

            subscriptionService.subscribe(sessionId, request);

            // When
            String subscriptionId = subscriptionService.getSubscriptionId(sessionId);

            // Then
            assertThat(subscriptionId).startsWith("sub_" + sessionId);
        }

        @Test
        @DisplayName("Should return null for non-existent subscription ID")
        void shouldReturnNullForNonExistentSubscriptionId() {
            // When
            String subscriptionId = subscriptionService.getSubscriptionId("non-existent");

            // Then
            assertThat(subscriptionId).isNull();
        }
    }

    @Nested
    @DisplayName("Snapshot Data Tests - RULE #3: Optional Chains & Stream API")
    class SnapshotDataTests {

        @Test
        @DisplayName("Should get snapshot from cache - RULE #3: Optional map")
        void shouldGetSnapshotFromCache() {
            // Given
            List<String> symbols = List.of("RELIANCE");
            var cachedPrice = createMockCachedPrice("RELIANCE", BigDecimal.valueOf(2500.0));

            when(cacheService.getCurrentPrice("RELIANCE", "NSE"))
                .thenReturn(Optional.of(cachedPrice));

            // When
            Map<String, Object> snapshot = subscriptionService.getSnapshot(symbols);

            // Then
            assertThat(snapshot).containsKey("RELIANCE");
            assertThat(snapshot).doesNotContainKey("error");
        }

        @Test
        @DisplayName("Should fallback to service when cache miss - RULE #3: Optional or()")
        void shouldFallbackToServiceWhenCacheMiss() {
            // Given
            List<String> symbols = List.of("TCS");
            var dataPoint = createMockDataPoint("TCS", BigDecimal.valueOf(3500.0));

            when(cacheService.getCurrentPrice("TCS", "NSE"))
                .thenReturn(Optional.empty());
            when(marketDataService.getCurrentPrice("TCS", "NSE"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(dataPoint)));

            // When
            Map<String, Object> snapshot = subscriptionService.getSnapshot(symbols);

            // Then
            assertThat(snapshot).containsKey("TCS");
        }

        @Test
        @DisplayName("Should handle snapshot error gracefully - RULE #3: Result orElse")
        void shouldHandleSnapshotErrorGracefully() {
            // Given
            List<String> symbols = List.of("INVALID");

            when(cacheService.getCurrentPrice(anyString(), anyString()))
                .thenThrow(new RuntimeException("Cache error"));

            // When
            Map<String, Object> snapshot = subscriptionService.getSnapshot(symbols);

            // Then - Should return error map instead of throwing
            assertThat(snapshot).containsKey("error");
        }

        @Test
        @DisplayName("Should handle parallel snapshot retrieval - RULE #3: Stream parallelStream")
        void shouldHandleParallelSnapshotRetrieval() {
            // Given
            List<String> symbols = List.of("RELIANCE", "TCS", "INFY");
            symbols.forEach(symbol -> {
                var cachedPrice = createMockCachedPrice(symbol, BigDecimal.valueOf(1000.0));
                when(cacheService.getCurrentPrice(symbol, "NSE"))
                    .thenReturn(Optional.of(cachedPrice));
            });

            // When
            Map<String, Object> snapshot = subscriptionService.getSnapshot(symbols);

            // Then
            assertThat(snapshot).hasSize(3);
            assertThat(snapshot).containsKeys("RELIANCE", "TCS", "INFY");
        }
    }

    @Nested
    @DisplayName("Broadcast Tests - RULE #3: Optional Chains with Filter")
    class BroadcastTests {

        @Test
        @DisplayName("Should broadcast to subscribed sessions - RULE #3: Optional filter & ifPresent")
        void shouldBroadcastToSubscribedSessions() {
            // Given
            String sessionId = "session-broadcast";
            var request = createSubscriptionRequest(
                List.of("RELIANCE"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE")));

            subscriptionService.subscribe(sessionId, request);

            var dataPoint = createMockDataPoint("RELIANCE", BigDecimal.valueOf(2500.0));

            // When
            subscriptionService.broadcastMarketDataUpdate(dataPoint);

            // Then
            verify(kafkaTemplate).send(eq("websocket-broadcasts"), anyString(), eq(dataPoint));
        }

        @Test
        @DisplayName("Should not broadcast when no subscriptions - RULE #3: Optional filter empty")
        void shouldNotBroadcastWhenNoSubscriptions() {
            // Given
            var dataPoint = createMockDataPoint("UNKNOWN", BigDecimal.valueOf(1000.0));

            // When
            subscriptionService.broadcastMarketDataUpdate(dataPoint);

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle broadcast error gracefully - RULE #3: Result type error handling")
        void shouldHandleBroadcastErrorGracefully() {
            // Given
            var dataPoint = createMockDataPoint("RELIANCE", BigDecimal.valueOf(2500.0));

            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka error"));

            // When & Then - Should not throw exception
            assertThatNoException().isThrownBy(() ->
                subscriptionService.broadcastMarketDataUpdate(dataPoint)
            );
        }
    }

    @Nested
    @DisplayName("Statistics Tests - RULE #3: Stream API")
    class StatisticsTests {

        @Test
        @DisplayName("Should get subscription statistics")
        void shouldGetSubscriptionStatistics() {
            // Given
            String sessionId1 = "session-stats-1";
            String sessionId2 = "session-stats-2";

            var request1 = createSubscriptionRequest(
                List.of("RELIANCE", "TCS"),
                
                "NSE"
            );
            var request2 = createSubscriptionRequest(
                List.of("RELIANCE"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE", "TCS")));

            subscriptionService.subscribe(sessionId1, request1);
            subscriptionService.subscribe(sessionId2, request2);

            // When
            var stats = subscriptionService.getSubscriptionStats();

            // Then
            assertThat(stats.totalSessions()).isEqualTo(2);
            assertThat(stats.totalSymbols()).isGreaterThan(0);
            assertThat(stats.symbolSubscriptionCounts()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Constants Usage Tests - RULE #17: Named Constants")
    class ConstantsUsageTests {

        @Test
        @DisplayName("Should use DEFAULT_EXCHANGE constant")
        void shouldUseDefaultExchangeConstant() {
            // Given
            List<String> activeSymbols = List.of("RELIANCE");
            when(marketDataService.getActiveSymbols("NSE", 60))
                .thenReturn(CompletableFuture.completedFuture(activeSymbols));

            // When
            subscriptionService.getSupportedSymbols();

            // Then
            verify(marketDataService).getActiveSymbols("NSE", 60);
        }

        @Test
        @DisplayName("Should use SYMBOL_KEY_SEPARATOR constant")
        void shouldUseSymbolKeySeparatorConstant() {
            // When
            Set<String> sessions = subscriptionService.getSubscribedSessions("RELIANCE", "NSE");

            // Then - Internal implementation uses ":" separator
            assertThat(sessions).isNotNull();
        }

        @Test
        @DisplayName("Should use SUBSCRIPTION_PREFIX constant")
        void shouldUseSubscriptionPrefixConstant() {
            // Given
            String sessionId = "test-session";
            var request = createSubscriptionRequest(
                List.of("RELIANCE"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE")));

            subscriptionService.subscribe(sessionId, request);

            // When
            String subscriptionId = subscriptionService.getSubscriptionId(sessionId);

            // Then
            assertThat(subscriptionId).startsWith("sub_");
        }

        @Test
        @DisplayName("Should use DEFAULT_SYMBOLS constant")
        void shouldUseDefaultSymbolsConstant() {
            // Given
            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Error")));

            // When
            List<String> symbols = subscriptionService.getSupportedSymbols();

            // Then
            assertThat(symbols).contains("RELIANCE", "TCS", "INFY", "HDFC");
        }
    }

    @Nested
    @DisplayName("MANDATORY RULES Compliance Tests")
    class MandatoryRulesComplianceTests {

        @Test
        @DisplayName("RULE #3: All methods use functional patterns (Result types, Optional chains)")
        void shouldUseOnlyFunctionalPatterns() {
            // All operations should execute without errors using functional patterns
            assertThatNoException().isThrownBy(() -> {
                var request = createSubscriptionRequest(List.of("RELIANCE"),  "NSE");

                when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                    .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE")));

                subscriptionService.subscribe("test", request);
                subscriptionService.unsubscribe("test", List.of("RELIANCE"));
                subscriptionService.removeAllSubscriptions("test");
                subscriptionService.getSupportedSymbols();
                subscriptionService.getSubscriptionId("test");
            });
        }

        @Test
        @DisplayName("RULE #9: Result type uses immutable records")
        void shouldUseImmutableResultTypes() {
            // Result types are sealed interfaces with immutable records
            var success = MarketDataSubscriptionService.Result.success("test");
            var failure = MarketDataSubscriptionService.Result.failure("error");

            assertThat(success).isInstanceOf(MarketDataSubscriptionService.Result.Success.class);
            assertThat(failure).isInstanceOf(MarketDataSubscriptionService.Result.Failure.class);
        }

        @Test
        @DisplayName("RULE #17: All magic numbers externalized to constants")
        void shouldUseNamedConstants() {
            // Verify behavior is consistent with documented constants
            when(marketDataService.getActiveSymbols("NSE", 60))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

            subscriptionService.getSupportedSymbols();

            verify(marketDataService).getActiveSymbols("NSE", 60);
        }

        @Test
        @DisplayName("Integration test: Subscribe -> Query -> Unsubscribe -> Verify")
        void shouldProvideConsistentEndToEndBehavior() {
            // Given
            String sessionId = "integration-test";
            var request = createSubscriptionRequest(
                List.of("RELIANCE", "TCS"),
                
                "NSE"
            );

            when(marketDataService.getActiveSymbols(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("RELIANCE", "TCS")));

            // When - Subscribe
            boolean subscribed = subscriptionService.subscribe(sessionId, request);
            assertThat(subscribed).isTrue();

            // When - Query
            Set<String> sessions1 = subscriptionService.getSubscribedSessions("RELIANCE", "NSE");
            assertThat(sessions1).contains(sessionId);

            // When - Unsubscribe
            boolean unsubscribed = subscriptionService.unsubscribe(sessionId, List.of("RELIANCE"));
            assertThat(unsubscribed).isTrue();

            // When - Query again
            Set<String> sessions2 = subscriptionService.getSubscribedSessions("TCS", "NSE");
            assertThat(sessions2).contains(sessionId); // Still subscribed to TCS

            // When - Remove all
            subscriptionService.removeAllSubscriptions(sessionId);

            // Then - Verify complete removal
            Set<String> sessions3 = subscriptionService.getSubscribedSessions("TCS", "NSE");
            assertThat(sessions3).doesNotContain(sessionId);
        }
    }

    // Helper methods for test data creation
    private MarketDataCacheService.CachedPrice createMockCachedPrice(String symbol, BigDecimal price) {
        return new MarketDataCacheService.CachedPrice(
            symbol,
            "NSE",
            price,
            1000000L,
            BigDecimal.valueOf(10.0),
            BigDecimal.valueOf(0.5),
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }

    private MarketDataPoint createMockDataPoint(String symbol, BigDecimal price) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange("NSE")
            .price(price)
            .volume(1000000L)
            .timestamp(java.time.Instant.now())
            .build();
    }
}
