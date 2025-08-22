package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.MarketNewsRequest;
import com.trademaster.marketdata.dto.MarketNewsResponse;
import com.trademaster.marketdata.service.MarketNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Market News REST API Controller
 * 
 * Provides comprehensive market news with sentiment analysis,
 * filtering, and real-time insights.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market-news")
@RequiredArgsConstructor
@Tag(name = "Market News", description = "Market news with sentiment analysis API")
public class MarketNewsController {
    
    private final MarketNewsService marketNewsService;
    
    /**
     * Get market news with comprehensive filtering
     */
    @PostMapping("/search")
    @Operation(summary = "Search market news", 
               description = "Get market news with comprehensive filtering and sentiment analysis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "News retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> searchNews(
            @RequestBody @Valid MarketNewsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("News search request by user: {} with {} active filters", 
            userDetails.getUsername(), request.getActiveFilterCount());
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response))
            .exceptionally(throwable -> {
                log.error("Error searching market news", throwable);
                return ResponseEntity.internalServerError()
                    .body(MarketNewsResponse.error("Failed to retrieve market news"));
            });
    }
    
    /**
     * Get breaking news
     */
    @GetMapping("/breaking")
    @Operation(summary = "Get breaking news", description = "Retrieve latest breaking market news")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getBreakingNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Breaking news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.breakingNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get trending news
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending news", description = "Retrieve trending market news")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getTrendingNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Trending news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.trendingNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get market moving news
     */
    @GetMapping("/market-moving")
    @Operation(summary = "Get market moving news", description = "Retrieve news with high market impact")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getMarketMovingNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Market moving news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.marketMovingNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get news for a specific symbol
     */
    @GetMapping("/symbol/{symbol}")
    @Operation(summary = "Get symbol news", description = "Retrieve news for a specific symbol")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getSymbolNews(
            @Parameter(description = "Trading symbol", example = "AAPL")
            @PathVariable String symbol,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Symbol news request for {} by user: {}", symbol, userDetails.getUsername());
        
        var request = MarketNewsRequest.symbolNews(symbol);
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get news for a specific sector
     */
    @GetMapping("/sector/{sector}")
    @Operation(summary = "Get sector news", description = "Retrieve news for a specific sector")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getSectorNews(
            @Parameter(description = "Sector name", example = "Technology")
            @PathVariable String sector,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Sector news request for {} by user: {}", sector, userDetails.getUsername());
        
        var request = MarketNewsRequest.sectorNews(sector);
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get sentiment analysis summary
     */
    @PostMapping("/sentiment")
    @Operation(summary = "Get sentiment analysis", 
               description = "Get comprehensive sentiment analysis for market news")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getSentimentAnalysis(
            @RequestBody @Valid MarketNewsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Sentiment analysis request by user: {}", userDetails.getUsername());
        
        var sentimentRequest = MarketNewsRequest.forSentimentAnalysis();
        
        return marketNewsService.getMarketNews(sentimentRequest)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get positive sentiment news
     */
    @GetMapping("/positive")
    @Operation(summary = "Get positive news", description = "Retrieve news with positive sentiment")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getPositiveNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Positive news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.positiveNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get negative sentiment news
     */
    @GetMapping("/negative")
    @Operation(summary = "Get negative news", description = "Retrieve news with negative sentiment")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getNegativeNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Negative news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.negativeNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get high impact news
     */
    @GetMapping("/high-impact")
    @Operation(summary = "Get high impact news", description = "Retrieve news with high market impact")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getHighImpactNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("High impact news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.highImpactNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get news by category
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get category news", description = "Retrieve news for a specific category")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getCategoryNews(
            @Parameter(description = "News category", example = "earnings")
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Category news request for {} by user: {}", category, userDetails.getUsername());
        
        var request = MarketNewsRequest.categoryNews(category);
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get news from specific source
     */
    @GetMapping("/source/{source}")
    @Operation(summary = "Get source news", description = "Retrieve news from a specific source")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getSourceNews(
            @Parameter(description = "News source", example = "Reuters")
            @PathVariable String source,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Source news request for {} by user: {}", source, userDetails.getUsername());
        
        var request = MarketNewsRequest.sourceNews(source);
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Search news with text query
     */
    @GetMapping("/search")
    @Operation(summary = "Search news by text", description = "Search news using text query")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> searchNewsByText(
            @Parameter(description = "Search query", example = "inflation rate")
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Text search request for '{}' by user: {}", q, userDetails.getUsername());
        
        var request = MarketNewsRequest.searchNews(q);
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get high quality news
     */
    @GetMapping("/quality")
    @Operation(summary = "Get quality news", description = "Retrieve high quality, verified news")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getQualityNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Quality news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.qualityNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
    
    /**
     * Get high engagement news
     */
    @GetMapping("/popular")
    @Operation(summary = "Get popular news", description = "Retrieve news with high engagement")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<MarketNewsResponse>> getPopularNews(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Popular news request by user: {}", userDetails.getUsername());
        
        var request = MarketNewsRequest.highEngagementNews();
        
        return marketNewsService.getMarketNews(request)
            .thenApply(response -> ResponseEntity.ok(response));
    }
}