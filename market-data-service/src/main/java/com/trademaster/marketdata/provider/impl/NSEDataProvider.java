package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.provider.ExchangeDataProvider;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * NSE Real Data Provider Implementation with Circuit Breaker Protection
 *
 * Uses Java 24 Virtual Threads with OkHttp for optimal performance.
 * Implements Resilience4j circuit breaker pattern for fault tolerance (Rule #25)
 *
 * IMPORTANT: This requires:
 * 1. NSE data vendor license agreement
 * 2. API credentials and endpoints
 * 3. Compliance with NSE data redistribution policies
 * 4. Payment of data subscription fees
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Circuit Breaker)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trademaster.exchanges.nse.enabled", havingValue = "true")
public class NSEDataProvider implements ExchangeDataProvider {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerService circuitBreakerService;
    
    // NSE API Configuration (would be externalized)
    private static final String NSE_BASE_URL = "https://www.nseindia.com/api/";
    private static final String QUOTE_ENDPOINT = "quote-equity";
    private static final String MARKET_DATA_ENDPOINT = "market-data-pre-open";

    @Override
    public String getExchangeName() {
        return "NSE";
    }

    @Override
    public CompletableFuture<MarketDataPoint> getCurrentPrice(String symbol) {
        // Wrap NSE API call with circuit breaker protection
        return circuitBreakerService.executeNSECallWithFallback(
            () -> {
                try {
                    return fetchNSEQuote(symbol);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to fetch NSE quote for " + symbol, e);
                }
            },
            () -> null  // Fallback returns null if circuit is open
        ).exceptionally(ex -> {
            log.error("Failed to fetch NSE data for symbol {}: {}", symbol, ex.getMessage());
            return null;
        });
    }

    /**
     * Internal method to fetch quote from NSE API
     * Follows Rule #11 (Functional Error Handling) - throws exceptions instead of catching
     */
    private MarketDataPoint fetchNSEQuote(String symbol) throws Exception {
        String url = NSE_BASE_URL + QUOTE_ENDPOINT + "?symbol=" + symbol;

        // NOTE: NSE has anti-scraping measures and requires proper headers
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; TradeMaster/1.0)")
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "gzip, deflate, br")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                NSEQuoteResponse nseResponse = objectMapper.readValue(responseBody, NSEQuoteResponse.class);

                if (nseResponse != null && nseResponse.data() != null) {
                    return convertToMarketDataPoint(nseResponse.data(), symbol);
                } else {
                    log.warn("No data received from NSE for symbol: {}", symbol);
                    return null;
                }
            } else {
                log.warn("NSE API returned error: {} for symbol: {}", response.code(), symbol);
                throw new RuntimeException("NSE API error: " + response.code());
            }
        }
    }

    @Override
    public CompletableFuture<List<MarketDataPoint>> getBulkPrices(List<String> symbols) {
        // NSE doesn't provide bulk API, so we make individual calls
        List<CompletableFuture<MarketDataPoint>> futures = symbols.stream()
            .map(this::getCurrentPrice)
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(data -> data != null)
                .toList());
    }

    @Override
    public boolean isMarketOpen() {
        // Wrap NSE market status check with circuit breaker protection
        return circuitBreakerService.executeNSECallWithFallback(
            () -> {
                try {
                    return checkNSEMarketStatus();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to check NSE market status", e);
                }
            },
            () -> false  // Fallback assumes market is closed if circuit is open
        ).exceptionally(ex -> {
            log.error("Failed to check NSE market status: {}", ex.getMessage());
            return false;
        }).join();  // Block for this synchronous method
    }

    /**
     * Internal method to check NSE market status
     * Follows Rule #11 (Functional Error Handling) - throws exceptions instead of catching
     */
    private boolean checkNSEMarketStatus() throws Exception {
        String url = NSE_BASE_URL + "market-status";

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; TradeMaster/1.0)")
            .header("Accept", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                NSEMarketStatusResponse statusResponse = objectMapper.readValue(responseBody, NSEMarketStatusResponse.class);

                return statusResponse != null && "OPEN".equals(statusResponse.marketStatus());
            } else {
                log.warn("NSE market status API returned error: {}", response.code());
                throw new RuntimeException("NSE API error: " + response.code());
            }
        }
    }

    private MarketDataPoint convertToMarketDataPoint(NSEQuoteData data, String symbol) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange("NSE")
            .dataType("QUOTE")
            .source("NSE_API")
            .price(new BigDecimal(data.lastPrice()))
            .volume(data.totalTradedVolume())
            .open(new BigDecimal(data.open()))
            .high(new BigDecimal(data.dayHigh()))
            .low(new BigDecimal(data.dayLow()))
            .previousClose(new BigDecimal(data.previousClose()))
            .change(new BigDecimal(data.change()))
            .changePercent(new BigDecimal(data.pChange()))
            .timestamp(Instant.now())
            .qualityScore(1.0)
            .build();
    }

    // NSE API Response DTOs
    public record NSEQuoteResponse(
        NSEQuoteData data,
        String status
    ) {}

    public record NSEQuoteData(
        String symbol,
        String companyName,
        String lastPrice,
        String change,
        String pChange,
        String previousClose,
        String open,
        String close,
        String dayHigh,
        String dayLow,
        Long totalTradedVolume,
        Long totalTradedValue,
        String lastUpdateTime,
        String yearHigh,
        String yearLow
    ) {}

    public record NSEMarketStatusResponse(
        String marketStatus,
        String tradeDate,
        String index,
        String last,
        String variation,
        String percentChange
    ) {}
}

/**
 * Configuration for NSE HTTP Client using OkHttp
 */
@Configuration
@ConditionalOnProperty(name = "trademaster.exchanges.nse.enabled", havingValue = "true")
class NSEHttpClientConfig {

    @Bean
    public OkHttpClient nseHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    @Bean 
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}