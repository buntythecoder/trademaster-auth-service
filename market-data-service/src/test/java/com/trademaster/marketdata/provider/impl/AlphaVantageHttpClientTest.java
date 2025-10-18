package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.functional.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import com.trademaster.marketdata.resilience.CircuitBreakerService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AlphaVantageHttpClient
 *
 * Tests HTTP client functionality, error handling, and URL building
 * Target: >90% code coverage (small, focused class)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlphaVantageHttpClient Unit Tests")
class AlphaVantageHttpClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    private AlphaVantageHttpClient httpClient;

    private static final String TEST_API_KEY = "test-api-key-12345";
    private static final String TEST_SYMBOL = "AAPL";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    @BeforeEach
    void setUp() {
        // Setup RestTemplateBuilder mock to support method chaining
        // requestFactory(Supplier) returns the builder itself for chaining
        when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        // Setup CircuitBreakerService mock to execute the supplier directly
        // This simulates circuit breaker in closed state (allowing calls through)
        when(circuitBreakerService.executeAlphaVantageCall(any()))
            .thenAnswer(invocation -> {
                Supplier<ResponseEntity<Map>> supplier = invocation.getArgument(0);
                return CompletableFuture.completedFuture(supplier.get());
            });

        // Create instance with properly mocked builder chain and circuit breaker
        httpClient = new AlphaVantageHttpClient(restTemplateBuilder, circuitBreakerService);
    }

    @Nested
    @DisplayName("Connection Testing")
    class ConnectionTestingTests {

        @Test
        @DisplayName("Should test connection successfully with valid API key")
        void testConnectionSuccess() {
            // Given
            Map<String, Object> responseBody = Map.of(
                "Global Quote", Map.of(
                    "01. symbol", "IBM",
                    "05. price", "150.50"
                )
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.testConnection(TEST_API_KEY);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.get().getBody()).isEqualTo(responseBody);

            // Verify the URL contains required parameters
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate).getForEntity(urlCaptor.capture(), eq(Map.class));

            String capturedUrl = urlCaptor.getValue();
            assertThat(capturedUrl).contains("function=GLOBAL_QUOTE");
            assertThat(capturedUrl).contains("symbol=IBM");
            assertThat(capturedUrl).contains("apikey=" + TEST_API_KEY);
        }

        @Test
        @DisplayName("Should fail connection test with invalid status code")
        void testConnectionFailureInvalidStatus() {
            // Given
            ResponseEntity<Map> response = new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.testConnection(TEST_API_KEY);

            // Then
            assertThat(result.isFailure()).isTrue();
            // Verify the failure contains appropriate error information
            assertThatThrownBy(() -> result.get())
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should handle connection test network errors")
        void testConnectionNetworkError() {
            // Given
            when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("Network timeout"));

            // When
            Try<ResponseEntity<Map>> result = httpClient.testConnection(TEST_API_KEY);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .hasRootCauseInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should handle null response from API")
        void testConnectionNullResponse() {
            // Given
            when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(null);

            // When
            Try<ResponseEntity<Map>> result = httpClient.testConnection(TEST_API_KEY);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    @DisplayName("HTTP Request Execution")
    class HttpRequestExecutionTests {

        @Test
        @DisplayName("Should execute GET request successfully")
        void testExecuteRequestSuccess() {
            // Given
            String testUrl = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=" + TEST_API_KEY;
            Map<String, Object> responseBody = Map.of(
                "Meta Data", Map.of("Symbol", "AAPL"),
                "Time Series (Daily)", Map.of()
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.getForEntity(testUrl, Map.class)).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.get().getBody()).isEqualTo(responseBody);
            assertThat(result.get().getBody()).isNotNull();

            verify(restTemplate).getForEntity(testUrl, Map.class);
        }

        @Test
        @DisplayName("Should fail request with non-OK status")
        void testExecuteRequestNonOkStatus() {
            // Given
            String testUrl = BASE_URL + "?function=GLOBAL_QUOTE&symbol=INVALID";
            ResponseEntity<Map> response = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

            when(restTemplate.getForEntity(testUrl, Map.class)).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should fail request with null response body")
        void testExecuteRequestNullBody() {
            // Given
            String testUrl = BASE_URL + "?function=GLOBAL_QUOTE&symbol=AAPL";
            ResponseEntity<Map> response = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.getForEntity(testUrl, Map.class)).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should handle REST client exceptions")
        void testExecuteRequestRestClientException() {
            // Given
            String testUrl = BASE_URL + "?function=GLOBAL_QUOTE";
            when(restTemplate.getForEntity(testUrl, Map.class))
                .thenThrow(new RestClientException("Connection timeout"));

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .hasRootCauseInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should handle network timeouts")
        void testExecuteRequestTimeout() {
            // Given
            String testUrl = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=AAPL";
            when(restTemplate.getForEntity(testUrl, Map.class))
                .thenThrow(new RestClientException("Read timed out"));

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .hasRootCauseInstanceOf(RestClientException.class);
        }
    }

    @Nested
    @DisplayName("URL Building Tests")
    class UrlBuildingTests {

        @Test
        @DisplayName("Should build historical data URL correctly")
        void testBuildHistoricalDataUrl() {
            // When
            String url = httpClient.buildHistoricalDataUrl(TEST_SYMBOL, TEST_API_KEY);

            // Then
            assertThat(url).isNotNull();
            assertThat(url).startsWith(BASE_URL);
            assertThat(url).contains("function=TIME_SERIES_DAILY");
            assertThat(url).contains("symbol=" + TEST_SYMBOL);
            assertThat(url).contains("apikey=" + TEST_API_KEY);
            assertThat(url).contains("outputsize=full");
        }

        @Test
        @DisplayName("Should build current price URL correctly")
        void testBuildCurrentPriceUrl() {
            // When
            String url = httpClient.buildCurrentPriceUrl(TEST_SYMBOL, TEST_API_KEY);

            // Then
            assertThat(url).isNotNull();
            assertThat(url).startsWith(BASE_URL);
            assertThat(url).contains("function=GLOBAL_QUOTE");
            assertThat(url).contains("symbol=" + TEST_SYMBOL);
            assertThat(url).contains("apikey=" + TEST_API_KEY);
        }

        @Test
        @DisplayName("Should build URL with special characters in symbol")
        void testBuildUrlWithSpecialCharacters() {
            // Given
            String symbolWithDash = "BRK-B";

            // When
            String url = httpClient.buildCurrentPriceUrl(symbolWithDash, TEST_API_KEY);

            // Then
            assertThat(url).contains("symbol=BRK-B");
        }

        @Test
        @DisplayName("Should build URL with different symbols")
        void testBuildUrlDifferentSymbols() {
            // Given
            String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA"};

            // When & Then
            for (String symbol : symbols) {
                String url = httpClient.buildHistoricalDataUrl(symbol, TEST_API_KEY);
                assertThat(url).contains("symbol=" + symbol);
            }
        }

        @Test
        @DisplayName("Should include base URL in all generated URLs")
        void testAllUrlsIncludeBaseUrl() {
            // When
            String historicalUrl = httpClient.buildHistoricalDataUrl(TEST_SYMBOL, TEST_API_KEY);
            String currentPriceUrl = httpClient.buildCurrentPriceUrl(TEST_SYMBOL, TEST_API_KEY);

            // Then
            assertThat(historicalUrl).startsWith(BASE_URL);
            assertThat(currentPriceUrl).startsWith(BASE_URL);
        }
    }

    @Nested
    @DisplayName("API Key Handling")
    class ApiKeyHandlingTests {

        @Test
        @DisplayName("Should include API key in all requests")
        void testApiKeyInAllRequests() {
            // When
            String historicalUrl = httpClient.buildHistoricalDataUrl(TEST_SYMBOL, TEST_API_KEY);
            String currentPriceUrl = httpClient.buildCurrentPriceUrl(TEST_SYMBOL, TEST_API_KEY);

            // Then
            assertThat(historicalUrl).contains("apikey=" + TEST_API_KEY);
            assertThat(currentPriceUrl).contains("apikey=" + TEST_API_KEY);
        }

        @Test
        @DisplayName("Should handle different API key formats")
        void testDifferentApiKeyFormats() {
            // Given
            String[] apiKeys = {"DEMO", "ABC123", "test-key-with-dashes", "KEY_WITH_UNDERSCORES"};

            // When & Then
            for (String apiKey : apiKeys) {
                String url = httpClient.buildCurrentPriceUrl(TEST_SYMBOL, apiKey);
                assertThat(url).contains("apikey=" + apiKey);
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete full request lifecycle successfully")
        void testFullRequestLifecycle() {
            // Given
            String url = httpClient.buildHistoricalDataUrl(TEST_SYMBOL, TEST_API_KEY);
            Map<String, Object> responseBody = Map.of(
                "Meta Data", Map.of("Symbol", TEST_SYMBOL),
                "Time Series (Daily)", Map.of("2024-01-01", Map.of("close", "150.00"))
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.getForEntity(url, Map.class)).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(url);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().getBody()).containsKey("Meta Data");
            assertThat(result.get().getBody()).containsKey("Time Series (Daily)");
        }

        @Test
        @DisplayName("Should build and execute current price request")
        void testBuildAndExecuteCurrentPrice() {
            // Given
            String url = httpClient.buildCurrentPriceUrl(TEST_SYMBOL, TEST_API_KEY);
            Map<String, Object> responseBody = Map.of(
                "Global Quote", Map.of(
                    "01. symbol", TEST_SYMBOL,
                    "05. price", "150.50",
                    "06. volume", "10000000"
                )
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.getForEntity(url, Map.class)).thenReturn(response);

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(url);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().getBody()).containsKey("Global Quote");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle unexpected runtime exceptions")
        void testUnexpectedRuntimeException() {
            // Given
            String testUrl = BASE_URL + "?function=INVALID";
            when(restTemplate.getForEntity(testUrl, Map.class))
                .thenThrow(new RuntimeException("Unexpected error"));

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .hasRootCauseInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void testIllegalArgumentException() {
            // Given
            String testUrl = "invalid-url-format";
            when(restTemplate.getForEntity(testUrl, Map.class))
                .thenThrow(new IllegalArgumentException("Invalid URL"));

            // When
            Try<ResponseEntity<Map>> result = httpClient.executeRequest(testUrl);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(() -> result.get())
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        }
    }
}
