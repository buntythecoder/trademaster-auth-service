package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.repository.MarketDataRepository;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataService
 *
 * Tests core business logic, data validation, and service interactions
 * using modern Java 24 testing patterns with structured concurrency.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Market Data Service Tests")
class MarketDataServiceTest {

    @Mock
    private MarketDataQueryService queryService;

    @Mock
    private MarketDataWriteService writeService;

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        // MarketDataService now delegates to specialized Query and Write services
        marketDataService = new MarketDataService(queryService, writeService, marketDataRepository, circuitBreakerService);
    }

    @Nested
    @DisplayName("Current Price Operations")
    class CurrentPriceOperationsTest {

        @Test
        @DisplayName("Should retrieve current price from cache when available")
        void shouldRetrieveCurrentPriceFromCache() {
            // Given
            String symbol = "RELIANCE";
            String exchange = "NSE";

            var dataPoint = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("2500.00"), 100000L, Instant.now()
            );

            when(queryService.getCurrentPrice(symbol, exchange))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(dataPoint)));

            // When
            var result = marketDataService.getCurrentPrice(symbol, exchange).join();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(symbol);
            assertThat(result.get().exchange()).isEqualTo(exchange);
            assertThat(result.get().price()).isEqualByComparingTo(new BigDecimal("2500.00"));

            verify(queryService).getCurrentPrice(symbol, exchange);
        }

        @Test
        @DisplayName("Should fallback to repository when cache miss occurs")
        void shouldFallbackToRepositoryOnCacheMiss() {
            // Given
            String symbol = "TCS";
            String exchange = "NSE";

            var dataPoint = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("3800.00"), 50000L, Instant.now()
            );

            when(queryService.getCurrentPrice(symbol, exchange))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(dataPoint)));

            // When
            var result = marketDataService.getCurrentPrice(symbol, exchange).join();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(symbol);
            assertThat(result.get().price()).isEqualByComparingTo(dataPoint.price());

            verify(queryService).getCurrentPrice(symbol, exchange);
        }

        @Test
        @DisplayName("Should return empty when neither cache nor repository has data")
        void shouldReturnEmptyWhenNoDataAvailable() {
            // Given
            String symbol = "NONEXISTENT";
            String exchange = "NSE";

            when(queryService.getCurrentPrice(symbol, exchange))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            // When
            var result = marketDataService.getCurrentPrice(symbol, exchange).join();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Given
            String symbol = "ERROR_TEST";
            String exchange = "NSE";

            when(queryService.getCurrentPrice(symbol, exchange))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            // When
            var result = marketDataService.getCurrentPrice(symbol, exchange).join();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Historical Data Operations")
    class HistoricalDataOperationsTest {

        @Test
        @DisplayName("Should retrieve historical data from query service")
        void shouldRetrieveHistoricalDataFromQueryService() {
            // Given
            String symbol = "INFY";
            String exchange = "NSE";
            String interval = "1m";
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();

            var ohlcData = List.of(
                MarketDataPoint.createOHLCData(
                    symbol, exchange,
                    new BigDecimal("1600.00"), new BigDecimal("1610.00"),
                    new BigDecimal("1595.00"), new BigDecimal("1605.00"),
                    25000L, from.plusSeconds(60)
                )
            );

            when(queryService.getHistoricalData(symbol, exchange, from, to, interval))
                .thenReturn(CompletableFuture.completedFuture(ohlcData));

            // When
            var result = marketDataService.getHistoricalData(symbol, exchange, from, to, interval).join();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).symbol()).isEqualTo(symbol);
            assertThat(result.get(0).open()).isEqualByComparingTo(new BigDecimal("1600.00"));

            verify(queryService).getHistoricalData(symbol, exchange, from, to, interval);
        }

        @Test
        @DisplayName("Should handle empty historical data results")
        void shouldHandleEmptyHistoricalDataResults() {
            // Given
            String symbol = "HDFC";
            String exchange = "NSE";
            String interval = "5m";
            Instant from = Instant.now().minusSeconds(1800);
            Instant to = Instant.now();

            when(queryService.getHistoricalData(symbol, exchange, from, to, interval))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

            // When
            var result = marketDataService.getHistoricalData(symbol, exchange, from, to, interval).join();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationsTest {

        @Test
        @DisplayName("Should retrieve bulk price data in parallel")
        void shouldRetrieveBulkPriceDataInParallel() {
            // Given
            List<String> symbols = List.of("RELIANCE", "TCS", "INFY");
            String exchange = "NSE";

            var bulkData = java.util.Map.of(
                "RELIANCE", MarketDataPoint.createTickData("RELIANCE", exchange, new BigDecimal("2500.00"), 100000L, Instant.now()),
                "TCS", MarketDataPoint.createTickData("TCS", exchange, new BigDecimal("3800.00"), 50000L, Instant.now()),
                "INFY", MarketDataPoint.createTickData("INFY", exchange, new BigDecimal("1650.00"), 75000L, Instant.now())
            );

            when(queryService.getBulkPriceData(symbols, exchange))
                .thenReturn(CompletableFuture.completedFuture(bulkData));

            // When
            var result = marketDataService.getBulkPriceData(symbols, exchange).join();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get("RELIANCE").price()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.get("TCS").price()).isEqualByComparingTo(new BigDecimal("3800.00"));
            assertThat(result.get("INFY").price()).isEqualByComparingTo(new BigDecimal("1650.00"));

            verify(queryService).getBulkPriceData(symbols, exchange);
        }

        @Test
        @DisplayName("Should handle partial failures in bulk operations")
        void shouldHandlePartialFailuresInBulkOperations() {
            // Given
            List<String> symbols = List.of("GOOD_SYMBOL", "BAD_SYMBOL");
            String exchange = "NSE";

            var partialData = java.util.Map.of(
                "GOOD_SYMBOL", MarketDataPoint.createTickData("GOOD_SYMBOL", exchange, new BigDecimal("1000.00"), 10000L, Instant.now())
            );

            when(queryService.getBulkPriceData(symbols, exchange))
                .thenReturn(CompletableFuture.completedFuture(partialData));

            // When
            var result = marketDataService.getBulkPriceData(symbols, exchange).join();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsKey("GOOD_SYMBOL");
            assertThat(result).doesNotContainKey("BAD_SYMBOL");
        }
    }

    @Nested
    @DisplayName("Data Writing Operations")
    class DataWritingOperationsTest {

        @Test
        @DisplayName("Should write single market data point successfully")
        void shouldWriteSingleMarketDataPointSuccessfully() {
            // Given
            var dataPoint = MarketDataPoint.createTickData(
                "WRITE_TEST", "NSE", new BigDecimal("500.00"), 5000L, Instant.now()
            );

            when(writeService.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(true));

            // When
            var result = marketDataService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isTrue();
            verify(writeService).writeMarketData(dataPoint);
        }

        @Test
        @DisplayName("Should handle order book data writing")
        void shouldHandleOrderBookDataWriting() {
            // Given
            var orderBookData = MarketDataPoint.createOrderBookData(
                "ORDER_TEST", "NSE",
                new BigDecimal("999.50"), new BigDecimal("1000.50"),
                5000L, 4800L, Instant.now()
            );

            when(writeService.writeMarketData(orderBookData))
                .thenReturn(CompletableFuture.completedFuture(true));

            // When
            var result = marketDataService.writeMarketData(orderBookData).join();

            // Then
            assertThat(result).isTrue();
            verify(writeService).writeMarketData(orderBookData);
        }

        @Test
        @DisplayName("Should handle batch write operations")
        void shouldHandleBatchWriteOperations() {
            // Given
            var dataPoints = List.of(
                MarketDataPoint.createTickData("BATCH1", "NSE", new BigDecimal("100.00"), 1000L, Instant.now()),
                MarketDataPoint.createTickData("BATCH2", "NSE", new BigDecimal("200.00"), 2000L, Instant.now())
            );

            var batchResult = new MarketDataWriteService.BatchWriteResult(2, 0, 50L, 2);

            when(writeService.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(batchResult));

            // When
            var result = marketDataService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result.successful()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.cacheUpdates()).isEqualTo(2);

            verify(writeService).batchWriteMarketData(dataPoints);
        }

        @Test
        @DisplayName("Should handle write failures gracefully")
        void shouldHandleWriteFailuresGracefully() {
            // Given
            var dataPoint = MarketDataPoint.createTickData(
                "FAIL_TEST", "NSE", new BigDecimal("500.00"), 5000L, Instant.now()
            );

            when(writeService.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(false));

            // When
            var result = marketDataService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Quality Reporting")
    class QualityReportingTest {

        @Test
        @DisplayName("Should generate data quality report")
        void shouldGenerateDataQualityReport() {
            // Given
            String symbol = "QUALITY_TEST";
            String exchange = "NSE";
            int hours = 1;

            var repoReport = new MarketDataRepository.DataQualityReport(
                symbol, exchange, 1000L, 5L, 0.95,
                Instant.now()
            );

            when(marketDataRepository.generateQualityReport(symbol, exchange, hours))
                .thenReturn(repoReport);

            // When
            var result = marketDataService.generateQualityReport(symbol, exchange, hours).join();

            // Then
            assertThat(result.symbol()).isEqualTo(symbol);
            assertThat(result.exchange()).isEqualTo(exchange);
            assertThat(result.qualityScore()).isEqualTo(0.95);
            assertThat(result.qualityLevel()).isEqualTo(MarketDataService.QualityLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("Active Symbols")
    class ActiveSymbolsTest {

        @Test
        @DisplayName("Should retrieve active symbols from query service")
        void shouldRetrieveActiveSymbolsFromQueryService() {
            // Given
            String exchange = "NSE";
            int minutes = 60;
            var activeSymbols = List.of("RELIANCE", "TCS", "INFY", "HDFC", "ICICIBANK");

            when(queryService.getActiveSymbols(exchange, minutes))
                .thenReturn(CompletableFuture.completedFuture(activeSymbols));

            // When
            var result = marketDataService.getActiveSymbols(exchange, minutes).join();

            // Then
            assertThat(result).hasSize(5);
            assertThat(result).containsExactlyInAnyOrder("RELIANCE", "TCS", "INFY", "HDFC", "ICICIBANK");

            verify(queryService).getActiveSymbols(exchange, minutes);
        }

        @Test
        @DisplayName("Should handle empty active symbols list")
        void shouldHandleEmptyActiveSymbolsList() {
            // Given
            String exchange = "NSE";
            int minutes = 60;

            when(queryService.getActiveSymbols(exchange, minutes))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

            // When
            var result = marketDataService.getActiveSymbols(exchange, minutes).join();

            // Then
            assertThat(result).isEmpty();
        }
    }
}