package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.controller.MarketDataConstants.*;
import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.security.SubscriptionTierValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data REST API Controller
 * 
 * Features:
 * - Subscription tier-based access control
 * - Real-time and historical market data
 * - Performance optimized with caching
 * - Rate limiting based on user tiers
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Real-time and historical market data API")
public class MarketDataController {

    private final MarketDataRequestHandler requestHandler;
    private final MarketDataResponseMapper responseMapper;
    private final MarketDataCacheService cacheService;
    private final SubscriptionTierValidator tierValidator;

    /**
     * Get current price for a symbol
     * Free Tier: 15-minute delay, Premium: Real-time
     */
    @GetMapping("/price/{symbol}")
    @Operation(summary = "Get current price", description = "Retrieve current price data for a symbol")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Price data retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Subscription tier insufficient"),
        @ApiResponse(responseCode = "404", description = "Symbol not found")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MarketDataResponse> getCurrentPrice(
            @Parameter(description = "Trading symbol", example = "RELIANCE")
            @PathVariable @NotBlank String symbol,

            @Parameter(description = "Exchange", example = "NSE")
            @RequestParam(defaultValue = "NSE") String exchange,

            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Price request for {}:{} by user {}", symbol, exchange, userDetails.getUsername());

        return tierValidator.validateAndExecute(userDetails,
            SubscriptionTierValidator.DataAccess.CURRENT_PRICE,
            () -> requestHandler.handlePriceRequest(symbol, exchange, userDetails));
    }

    /**
     * Get historical OHLC data
     * Subscription tier determines data range and frequency
     */
    @GetMapping("/history/{symbol}")
    @Operation(summary = "Get historical data", description = "Retrieve historical OHLC data")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketDataResponse>> getHistoricalData(
            @Parameter(description = "Trading symbol")
            @PathVariable @NotBlank String symbol,

            @RequestParam(defaultValue = "NSE") String exchange,

            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @Parameter(description = "Data interval", example = "1m")
            @RequestParam(defaultValue = "1m")
            @Pattern(regexp = "^(1m|5m|15m|1h|1d)$") String interval,

            @AuthenticationPrincipal UserDetails userDetails) {

        return tierValidator.validateAndExecuteAsync(userDetails,
            SubscriptionTierValidator.DataAccess.HISTORICAL_DATA,
            () -> requestHandler.handleHistoricalDataRequest(symbol, exchange, from, to, interval, userDetails));
    }

    /**
     * Get market overview for multiple symbols
     */
    @GetMapping("/overview")
    @Operation(summary = "Get market overview", description = "Retrieve market overview for multiple symbols")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketDataResponse>> getMarketOverview(
            @Parameter(description = "Comma-separated symbols")
            @RequestParam String symbols,

            @RequestParam(defaultValue = "NSE") String exchange,

            @AuthenticationPrincipal UserDetails userDetails) {

        return tierValidator.validateAndExecuteAsync(userDetails,
            SubscriptionTierValidator.DataAccess.BULK_DATA,
            () -> {
                List<String> symbolList = List.of(symbols.split(","));
                return requestHandler.handleBulkPriceRequest(symbolList, exchange, userDetails);
            });
    }

    /**
     * Get order book data (Premium feature)
     */
    @GetMapping("/orderbook/{symbol}")
    @Operation(summary = "Get order book", description = "Retrieve real-time order book data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MarketDataResponse> getOrderBook(
            @PathVariable @NotBlank String symbol,
            @RequestParam(defaultValue = "NSE") String exchange,
            @AuthenticationPrincipal UserDetails userDetails) {

        return tierValidator.validateAndExecute(userDetails,
            SubscriptionTierValidator.DataAccess.ORDER_BOOK,
            () -> requestHandler.handleOrderBookRequest(symbol, exchange));
    }

    /**
     * Get active symbols for an exchange
     */
    @GetMapping("/symbols")
    @Operation(summary = "Get active symbols", description = "Retrieve list of active trading symbols")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketDataResponse>> getActiveSymbols(
            @RequestParam(defaultValue = "NSE") String exchange,
            @AuthenticationPrincipal UserDetails userDetails) {

        return tierValidator.validateAndExecuteAsync(userDetails,
            SubscriptionTierValidator.DataAccess.SYMBOL_LIST,
            () -> requestHandler.handleActiveSymbolsRequest(exchange, TimeWindows.ACTIVE_SYMBOLS_WINDOW_MINUTES));
    }

    /**
     * Get market statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get market statistics", description = "Retrieve market statistics and metrics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MarketDataResponse> getMarketStats(
            @RequestParam(defaultValue = "NSE") String exchange,
            @AuthenticationPrincipal UserDetails userDetails) {

        return tierValidator.validateAndExecute(userDetails,
            SubscriptionTierValidator.DataAccess.MARKET_STATS,
            () -> responseMapper.buildMarketStatsResponse(exchange, cacheService.getMetrics()));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check market data service health")
    public ResponseEntity<MarketDataResponse> healthCheck() {
        return responseMapper.buildHealthCheckResponse(cacheService.getMetrics());
    }
}