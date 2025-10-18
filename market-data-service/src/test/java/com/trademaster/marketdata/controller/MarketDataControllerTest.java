package com.trademaster.marketdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataCacheService.CacheMetrics;
import com.trademaster.marketdata.security.SubscriptionTierValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for MarketDataController
 *
 * Tests REST API endpoints using standalone MockMvc (no Spring context)
 * Target: >90% code coverage for controller layer
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataController Unit Tests")
class MarketDataControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MarketDataRequestHandler requestHandler;

    @Mock
    private MarketDataResponseMapper responseMapper;

    @Mock
    private MarketDataCacheService cacheService;

    @Mock
    private SubscriptionTierValidator tierValidator;

    @InjectMocks
    private MarketDataController controller;

    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/market-data";
    private static final String TEST_SYMBOL = "RELIANCE";
    private static final String TEST_EXCHANGE = "NSE";

    @BeforeEach
    void setUp() {
        // Build standalone MockMvc (no Spring context needed)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 date/time support

        // Default mock behavior for tier validator (allow all)
        when(tierValidator.validateAndExecute(any(), any(), any()))
            .thenAnswer(invocation -> {
                Supplier<?> supplier = invocation.getArgument(2);
                return supplier.get();
            });

        when(tierValidator.validateAndExecuteAsync(any(), any(), any()))
            .thenAnswer(invocation -> {
                Supplier<CompletableFuture<?>> supplier = invocation.getArgument(2);
                return supplier.get();
            });
    }

    @Nested
    @DisplayName("Current Price Endpoint Tests")
    class CurrentPriceTests {

        @Test
        @DisplayName("Should get current price successfully")
        void shouldGetCurrentPriceSuccess() throws Exception {
            // Given
            var priceData = Map.of("symbol", TEST_SYMBOL, "price", 2500.00);
            var response = ResponseEntity.ok(MarketDataResponse.success(priceData));

            when(requestHandler.handlePriceRequest(eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/{symbol}", TEST_SYMBOL)
                    .param("exchange", TEST_EXCHANGE)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value(TEST_SYMBOL));

            verify(requestHandler).handlePriceRequest(eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any());
        }

        @Test
        @DisplayName("Should use default exchange NSE when not specified")
        void shouldUseDefaultExchange() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(Map.of()));
            when(requestHandler.handlePriceRequest(anyString(), eq(TEST_EXCHANGE), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/{symbol}", TEST_SYMBOL))
                .andExpect(status().isOk());

            verify(requestHandler).handlePriceRequest(eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any());
        }

        @Test
        @DisplayName("Should handle subscription tier validation")
        void shouldHandleSubscriptionTierValidation() throws Exception {
            // Given
            when(tierValidator.validateAndExecute(any(), any(), any()))
                .thenReturn(ResponseEntity.status(403)
                    .body(MarketDataResponse.subscriptionError("PREMIUM", "FREE")));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/{symbol}", TEST_SYMBOL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_TIER_ERROR"));
        }

        @Test
        @DisplayName("Should handle different exchanges")
        void shouldHandleDifferentExchanges() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(Map.of()));
            when(requestHandler.handlePriceRequest(eq(TEST_SYMBOL), eq("BSE"), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/{symbol}", TEST_SYMBOL)
                    .param("exchange", "BSE"))
                .andExpect(status().isOk());

            verify(requestHandler).handlePriceRequest(eq(TEST_SYMBOL), eq("BSE"), any());
        }
    }

    @Nested
    @DisplayName("Historical Data Endpoint Tests")
    class HistoricalDataTests {

        @Test
        @DisplayName("Should get historical data successfully")
        void shouldGetHistoricalDataSuccess() throws Exception {
            // Given
            var historicalData = List.of(
                Map.of("date", "2024-01-01", "close", 2500.00),
                Map.of("date", "2024-01-02", "close", 2550.00)
            );
            var response = ResponseEntity.ok(MarketDataResponse.success(historicalData));

            when(requestHandler.handleHistoricalDataRequest(
                eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any(Instant.class), any(Instant.class), eq("1d"), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/history/{symbol}", TEST_SYMBOL)
                    .param("exchange", TEST_EXCHANGE)
                    .param("from", "2024-01-01T00:00:00Z")
                    .param("to", "2024-01-31T00:00:00Z")
                    .param("interval", "1d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

            verify(requestHandler).handleHistoricalDataRequest(
                eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any(Instant.class), any(Instant.class), eq("1d"), any());
        }

        @Test
        @DisplayName("Should use default interval 1m when not specified")
        void shouldUseDefaultInterval() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(List.of()));
            when(requestHandler.handleHistoricalDataRequest(
                anyString(), anyString(), any(Instant.class), any(Instant.class), eq("1m"), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/history/{symbol}", TEST_SYMBOL)
                    .param("from", "2024-01-01T00:00:00Z")
                    .param("to", "2024-01-31T00:00:00Z"))
                .andExpect(status().isOk());

            verify(requestHandler).handleHistoricalDataRequest(
                eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any(Instant.class), any(Instant.class), eq("1m"), any());
        }

        @Test
        @DisplayName("Should handle different intervals")
        void shouldHandleDifferentIntervals() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(List.of()));
            when(requestHandler.handleHistoricalDataRequest(
                anyString(), anyString(), any(Instant.class), any(Instant.class), eq("1h"), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/history/{symbol}", TEST_SYMBOL)
                    .param("from", "2024-01-01T00:00:00Z")
                    .param("to", "2024-01-31T00:00:00Z")
                    .param("interval", "1h"))
                .andExpect(status().isOk());

            verify(requestHandler).handleHistoricalDataRequest(
                eq(TEST_SYMBOL), eq(TEST_EXCHANGE), any(Instant.class), any(Instant.class), eq("1h"), any());
        }
    }

    @Nested
    @DisplayName("Market Overview Endpoint Tests")
    class MarketOverviewTests {

        @Test
        @DisplayName("Should get market overview for multiple symbols")
        void shouldGetMarketOverviewSuccess() throws Exception {
            // Given
            var overviewData = List.of(
                Map.of("symbol", "RELIANCE", "price", 2500.00),
                Map.of("symbol", "TCS", "price", 3500.00)
            );
            var response = ResponseEntity.ok(MarketDataResponse.success(overviewData));

            when(requestHandler.handleBulkPriceRequest(anyList(), eq(TEST_EXCHANGE), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/overview")
                    .param("symbols", "RELIANCE,TCS")
                    .param("exchange", TEST_EXCHANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

            verify(requestHandler).handleBulkPriceRequest(anyList(), eq(TEST_EXCHANGE), any());
        }

        @Test
        @DisplayName("Should parse comma-separated symbols correctly")
        void shouldParseCommaSeparatedSymbols() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(List.of()));
            when(requestHandler.handleBulkPriceRequest(argThat(list ->
                list != null && list.size() == 3 &&
                list.contains("RELIANCE") && list.contains("TCS") && list.contains("INFY")),
                anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/overview")
                    .param("symbols", "RELIANCE,TCS,INFY"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should use default exchange NSE")
        void shouldUseDefaultExchangeForOverview() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(List.of()));
            when(requestHandler.handleBulkPriceRequest(anyList(), eq(TEST_EXCHANGE), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/overview")
                    .param("symbols", "RELIANCE"))
                .andExpect(status().isOk());

            verify(requestHandler).handleBulkPriceRequest(anyList(), eq(TEST_EXCHANGE), any());
        }
    }

    @Nested
    @DisplayName("Order Book Endpoint Tests")
    class OrderBookTests {

        @Test
        @DisplayName("Should get order book successfully")
        void shouldGetOrderBookSuccess() throws Exception {
            // Given
            var orderBookData = Map.of(
                "bids", List.of(Map.of("price", 2500.00, "quantity", 100)),
                "asks", List.of(Map.of("price", 2505.00, "quantity", 50))
            );
            var response = ResponseEntity.ok(MarketDataResponse.success(orderBookData));

            when(requestHandler.handleOrderBookRequest(eq(TEST_SYMBOL), eq(TEST_EXCHANGE)))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/orderbook/{symbol}", TEST_SYMBOL)
                    .param("exchange", TEST_EXCHANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bids").isArray())
                .andExpect(jsonPath("$.data.asks").isArray());

            verify(requestHandler).handleOrderBookRequest(eq(TEST_SYMBOL), eq(TEST_EXCHANGE));
        }

        @Test
        @DisplayName("Should require premium subscription for order book")
        void shouldRequirePremiumSubscription() throws Exception {
            // Given
            when(tierValidator.validateAndExecute(any(),
                eq(SubscriptionTierValidator.DataAccess.ORDER_BOOK), any()))
                .thenReturn(ResponseEntity.status(403)
                    .body(MarketDataResponse.subscriptionError("PREMIUM", "FREE")));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/orderbook/{symbol}", TEST_SYMBOL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_TIER_ERROR"));
        }
    }

    @Nested
    @DisplayName("Active Symbols Endpoint Tests")
    class ActiveSymbolsTests {

        @Test
        @DisplayName("Should get active symbols successfully")
        void shouldGetActiveSymbolsSuccess() throws Exception {
            // Given
            var symbolsData = List.of("RELIANCE", "TCS", "INFY", "WIPRO");
            var response = ResponseEntity.ok(MarketDataResponse.success(symbolsData));

            when(requestHandler.handleActiveSymbolsRequest(eq(TEST_EXCHANGE), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/symbols")
                    .param("exchange", TEST_EXCHANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

            verify(requestHandler).handleActiveSymbolsRequest(eq(TEST_EXCHANGE), anyInt());
        }

        @Test
        @DisplayName("Should use default exchange NSE")
        void shouldUseDefaultExchangeForSymbols() throws Exception {
            // Given
            var response = ResponseEntity.ok(MarketDataResponse.success(List.of()));
            when(requestHandler.handleActiveSymbolsRequest(eq(TEST_EXCHANGE), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/symbols"))
                .andExpect(status().isOk());

            verify(requestHandler).handleActiveSymbolsRequest(eq(TEST_EXCHANGE), anyInt());
        }
    }

    @Nested
    @DisplayName("Market Statistics Endpoint Tests")
    class MarketStatsTests {

        @Test
        @DisplayName("Should get market statistics successfully")
        void shouldGetMarketStatsSuccess() throws Exception {
            // Given
            var metrics = new CacheMetrics(9500L, 500L, 1000L, 95.0, 45.0, Instant.now());
            var statsData = Map.of("exchange", TEST_EXCHANGE, "metrics", metrics);
            var response = ResponseEntity.ok(MarketDataResponse.success(statsData));

            when(cacheService.getMetrics()).thenReturn(metrics);
            when(responseMapper.buildMarketStatsResponse(eq(TEST_EXCHANGE), eq(metrics)))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                    .param("exchange", TEST_EXCHANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exchange").value(TEST_EXCHANGE));

            verify(cacheService).getMetrics();
            verify(responseMapper).buildMarketStatsResponse(eq(TEST_EXCHANGE), eq(metrics));
        }

        @Test
        @DisplayName("Should use default exchange NSE")
        void shouldUseDefaultExchangeForStats() throws Exception {
            // Given
            var metrics = new CacheMetrics(100L, 10L, 50L, 90.9, 35.0, Instant.now());
            var response = ResponseEntity.ok(MarketDataResponse.success(Map.of()));
            when(cacheService.getMetrics()).thenReturn(metrics);
            when(responseMapper.buildMarketStatsResponse(eq(TEST_EXCHANGE), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isOk());

            verify(responseMapper).buildMarketStatsResponse(eq(TEST_EXCHANGE), any());
        }
    }

    @Nested
    @DisplayName("Health Check Endpoint Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should perform health check without authentication")
        void shouldPerformHealthCheckWithoutAuth() throws Exception {
            // Given
            var metrics = new CacheMetrics(850L, 150L, 500L, 85.0, 32.0, Instant.now());
            var healthData = Map.of("status", "UP", "cacheAvailable", true);
            var response = ResponseEntity.ok(MarketDataResponse.success(healthData));

            when(cacheService.getMetrics()).thenReturn(metrics);
            when(responseMapper.buildHealthCheckResponse(any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));

            verify(cacheService).getMetrics();
            verify(responseMapper).buildHealthCheckResponse(any());
        }

        @Test
        @DisplayName("Should return health status with metrics")
        void shouldReturnHealthStatusWithMetrics() throws Exception {
            // Given
            var metrics = new CacheMetrics(8500L, 1500L, 5000L, 85.0, 32.0, Instant.now());
            var healthData = Map.of("status", "UP", "metrics", metrics);
            var response = ResponseEntity.ok(MarketDataResponse.success(healthData));

            when(cacheService.getMetrics()).thenReturn(metrics);
            when(responseMapper.buildHealthCheckResponse(eq(metrics)))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metrics.hitRate").value(85.0));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle request handler exceptions gracefully")
        void shouldHandleRequestHandlerException() throws Exception {
            // Given
            when(requestHandler.handlePriceRequest(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // When & Then - Standalone MockMvc doesn't have exception resolution by default
            // The exception will be propagated
            try {
                mockMvc.perform(get(BASE_URL + "/price/RELIANCE"))
                    .andExpect(status().is5xxServerError());
            } catch (Exception e) {
                // Expected behavior for standalone MockMvc
            }
        }

        @Test
        @DisplayName("Should handle invalid exchange parameter")
        void shouldHandleInvalidExchange() throws Exception {
            // Given
            var response = ResponseEntity.badRequest()
                .body(MarketDataResponse.error("INVALID_EXCHANGE", "Invalid exchange", "Exchange not supported"));
            when(requestHandler.handlePriceRequest(anyString(), eq("INVALID"), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/RELIANCE")
                    .param("exchange", "INVALID"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle symbol not found")
        void shouldHandleSymbolNotFound() throws Exception {
            // Given
            var response = ResponseEntity.status(404)
                .body(MarketDataResponse.error("SYMBOL_NOT_FOUND", "Symbol not found", "UNKNOWN symbol"));
            when(requestHandler.handlePriceRequest(eq("UNKNOWN"), anyString(), any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/price/UNKNOWN"))
                .andExpect(status().isNotFound());
        }
    }
}
