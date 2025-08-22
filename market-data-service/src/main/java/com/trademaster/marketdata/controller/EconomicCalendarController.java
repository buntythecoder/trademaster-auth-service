package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.EconomicCalendarRequest;
import com.trademaster.marketdata.dto.EconomicCalendarResponse;
import com.trademaster.marketdata.entity.EconomicEvent;
import com.trademaster.marketdata.service.EconomicCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Economic Calendar REST Controller
 * 
 * Provides endpoints for economic calendar events with comprehensive
 * filtering, market impact analysis, and intelligent recommendations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/economic-calendar")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Economic Calendar", description = "Economic events and market impact analysis")
public class EconomicCalendarController {
    
    private final EconomicCalendarService economicCalendarService;
    
    @PostMapping("/events")
    @Operation(
        summary = "Get Economic Calendar Events",
        description = "Get economic calendar events with comprehensive filtering and analysis",
        responses = {
            @ApiResponse(responseCode = "200", description = "Calendar events retrieved successfully",
                content = @Content(schema = @Schema(implementation = EconomicCalendarResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Calendar service error")
        }
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getCalendarEvents(
            @Valid @RequestBody EconomicCalendarRequest request) {
        
        log.info("Processing economic calendar request for {} to {} with {} filters",
            request.startDate(), request.endDate(), request.getActiveFilterCount());
        
        return economicCalendarService.getCalendarEvents(request)
            .thenApply(response -> {
                log.info("Economic calendar response: {} events, risk level: {}",
                    response.statistics().totalEvents(), response.getRiskLevel());
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                log.error("Economic calendar request failed: {}", throwable.getMessage(), throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/today")
    @Operation(
        summary = "Today's Economic Events",
        description = "Get all economic events scheduled for today"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getTodaysEvents(
            @Parameter(description = "Countries to include (ISO 3-letter codes)")
            @RequestParam(required = false) Set<String> countries,
            
            @Parameter(description = "Event categories to include")
            @RequestParam(required = false) Set<String> categories,
            
            @Parameter(description = "Minimum importance level")
            @RequestParam(required = false) EconomicEvent.EventImportance minImportance,
            
            @Parameter(description = "Include market analysis")
            @RequestParam(defaultValue = "true") Boolean includeAnalysis,
            
            @Parameter(description = "Timezone for display")
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        var requestBuilder = EconomicCalendarRequest.todaysEvents().toBuilder()
            .includeAnalysis(includeAnalysis)
            .timezone(timezone);
        
        if (countries != null && !countries.isEmpty()) {
            requestBuilder.countries(countries);
        }
        
        if (categories != null && !categories.isEmpty()) {
            requestBuilder.categories(categories);
        }
        
        if (minImportance != null) {
            requestBuilder.importance(Set.of(minImportance, EconomicEvent.EventImportance.CRITICAL));
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/week")
    @Operation(
        summary = "This Week's Economic Events",
        description = "Get economic events for the current week"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getWeekEvents(
            @Parameter(description = "High impact events only")
            @RequestParam(defaultValue = "false") Boolean highImpactOnly,
            
            @Parameter(description = "Include upcoming analysis")
            @RequestParam(defaultValue = "true") Boolean includeAnalysis) {
        
        var requestBuilder = EconomicCalendarRequest.thisWeek().toBuilder()
            .includeAnalysis(includeAnalysis);
        
        if (highImpactOnly) {
            requestBuilder.importance(Set.of(
                EconomicEvent.EventImportance.HIGH, 
                EconomicEvent.EventImportance.CRITICAL
            ));
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/upcoming")
    @Operation(
        summary = "Upcoming High Impact Events",
        description = "Get upcoming high impact economic events (next 7 days)"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getUpcomingHighImpact(
            @Parameter(description = "Number of days ahead to look", example = "7")
            @RequestParam(defaultValue = "7") Integer daysAhead,
            
            @Parameter(description = "Market moving events only")
            @RequestParam(defaultValue = "true") Boolean marketMovingOnly) {
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        
        var requestBuilder = EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(endDate)
            .upcomingOnly(true)
            .includeAnalysis(true)
            .includeForecast(true)
            .sortBy("eventDate")
            .sortDirection(EconomicCalendarRequest.SortDirection.ASC);
        
        if (marketMovingOnly) {
            requestBuilder.marketMovingOnly(true);
        } else {
            requestBuilder.importance(Set.of(
                EconomicEvent.EventImportance.HIGH,
                EconomicEvent.EventImportance.CRITICAL
            ));
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/market-moving")
    @Operation(
        summary = "Market Moving Events",
        description = "Get events with significant market impact potential"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getMarketMovingEvents(
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Minimum impact score (0-100)", example = "60")
            @RequestParam(defaultValue = "60") java.math.BigDecimal minImpactScore,
            
            @Parameter(description = "Global events only")
            @RequestParam(defaultValue = "false") Boolean globalOnly) {
        
        var requestBuilder = EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .marketMovingOnly(true)
            .minImpactScore(minImpactScore)
            .includeAnalysis(true)
            .sortBy("marketImpactScore")
            .sortDirection(EconomicCalendarRequest.SortDirection.DESC);
        
        if (globalOnly) {
            requestBuilder.globalEventsOnly(true);
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/country/{countryCode}")
    @Operation(
        summary = "Events by Country",
        description = "Get economic events for a specific country"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getEventsByCountry(
            @Parameter(description = "ISO 3-letter country code", example = "IND")
            @PathVariable String countryCode,
            
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Event categories")
            @RequestParam(required = false) Set<String> categories,
            
            @Parameter(description = "Include analysis")
            @RequestParam(defaultValue = "true") Boolean includeAnalysis) {
        
        var requestBuilder = EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .countries(Set.of(countryCode.toUpperCase()))
            .includeAnalysis(includeAnalysis)
            .sortBy("eventDate")
            .sortDirection(EconomicCalendarRequest.SortDirection.ASC);
        
        if (categories != null && !categories.isEmpty()) {
            requestBuilder.categories(categories);
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/category/{categoryName}")
    @Operation(
        summary = "Events by Category",
        description = "Get economic events for a specific category"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getEventsByCategory(
            @Parameter(description = "Event category", example = "Inflation")
            @PathVariable String categoryName,
            
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Countries to include")
            @RequestParam(required = false) Set<String> countries) {
        
        var requestBuilder = EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .categories(Set.of(categoryName))
            .includeAnalysis(true)
            .includeHistorical(true)
            .sortBy("eventDate")
            .sortDirection(EconomicCalendarRequest.SortDirection.ASC);
        
        if (countries != null && !countries.isEmpty()) {
            requestBuilder.countries(countries);
        }
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/surprises")
    @Operation(
        summary = "Recent Economic Surprises",
        description = "Get recent events that significantly beat or missed expectations"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getRecentSurprises(
            @Parameter(description = "Days back to look", example = "30")
            @RequestParam(defaultValue = "30") Integer daysBack,
            
            @Parameter(description = "Minimum surprise percentage", example = "5.0")
            @RequestParam(defaultValue = "5.0") java.math.BigDecimal minSurprisePercent) {
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(daysBack);
        
        EconomicCalendarRequest request = EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .surprisesOnly(true)
            .minSurprisePercent(minSurprisePercent.divide(new java.math.BigDecimal("100")))
            .releasedOnly(true)
            .withActualValues(true)
            .includeAnalysis(true)
            .sortBy("eventDate")
            .sortDirection(EconomicCalendarRequest.SortDirection.DESC)
            .build();
        
        return economicCalendarService.getCalendarEvents(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/central-bank")
    @Operation(
        summary = "Central Bank Events",
        description = "Get central bank meetings and monetary policy events"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> getCentralBankEvents(
            @Parameter(description = "Days ahead to look", example = "90")
            @RequestParam(defaultValue = "90") Integer daysAhead,
            
            @Parameter(description = "Countries to include")
            @RequestParam(required = false) Set<String> countries) {
        
        var requestBuilder = EconomicCalendarRequest.centralBankEvents().toBuilder();
        
        if (countries != null && !countries.isEmpty()) {
            requestBuilder.countries(countries);
        }
        
        LocalDate today = LocalDate.now();
        requestBuilder.startDate(today).endDate(today.plusDays(daysAhead));
        
        return economicCalendarService.getCalendarEvents(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/search")
    @Operation(
        summary = "Search Economic Events",
        description = "Search economic events by title or description"
    )
    public CompletableFuture<ResponseEntity<EconomicCalendarResponse>> searchEvents(
            @Parameter(description = "Search term")
            @RequestParam String query,
            
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        
        EconomicCalendarRequest request = EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .searchTerm(query)
            .size(size)
            .includeAnalysis(true)
            .sortBy("eventDate")
            .sortDirection(EconomicCalendarRequest.SortDirection.ASC)
            .build();
        
        return economicCalendarService.getCalendarEvents(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/presets")
    @Operation(
        summary = "Get Calendar Presets",
        description = "Get available preset calendar configurations"
    )
    public ResponseEntity<Map<String, Object>> getCalendarPresets() {
        return ResponseEntity.ok(Map.of(
            "today", Map.of(
                "name", "Today's Events",
                "description", "All economic events scheduled for today",
                "endpoint", "/today"
            ),
            "thisWeek", Map.of(
                "name", "This Week",
                "description", "Economic events for the current week",
                "endpoint", "/week"
            ),
            "upcomingHighImpact", Map.of(
                "name", "Upcoming High Impact",
                "description", "High impact events in the next 7 days",
                "endpoint", "/upcoming"
            ),
            "marketMoving", Map.of(
                "name", "Market Moving Events",
                "description", "Events with significant market impact potential",
                "endpoint", "/market-moving"
            ),
            "centralBank", Map.of(
                "name", "Central Bank Events",
                "description", "Central bank meetings and monetary policy events",
                "endpoint", "/central-bank"
            ),
            "recentSurprises", Map.of(
                "name", "Recent Surprises",
                "description", "Events that recently beat or missed expectations",
                "endpoint", "/surprises"
            ),
            "indiaEvents", Map.of(
                "name", "India Events",
                "description", "Economic events specific to India",
                "endpoint", "/country/IND"
            ),
            "globalEvents", Map.of(
                "name", "Global Events",
                "description", "Events affecting global markets",
                "filters", Map.of("globalEventsOnly", true)
            )
        ));
    }
    
    @GetMapping("/filters/available")
    @Operation(
        summary = "Get Available Filters",
        description = "Get all available filter options for economic calendar"
    )
    public ResponseEntity<Map<String, Object>> getAvailableFilters() {
        return ResponseEntity.ok(Map.of(
            "countries", Map.of(
                "IND", "India",
                "USA", "United States", 
                "EUR", "Eurozone",
                "GBR", "United Kingdom",
                "JPN", "Japan",
                "CHN", "China",
                "GER", "Germany",
                "FRA", "France",
                "CAN", "Canada",
                "AUS", "Australia"
            ),
            "categories", List.of(
                "Interest Rate", "Inflation", "Employment", "GDP", "Manufacturing",
                "Consumer Confidence", "Trade", "Government Budget", "Monetary Policy",
                "Central Bank", "Labor Market", "Housing", "Retail Sales", "Industrial Production"
            ),
            "importance", Map.of(
                "LOW", "Low impact - minor market effect",
                "MEDIUM", "Medium impact - moderate market effect", 
                "HIGH", "High impact - significant market effect",
                "CRITICAL", "Critical impact - major market moving event"
            ),
            "status", Map.of(
                "SCHEDULED", "Scheduled for release",
                "RELEASED", "Data released",
                "DELAYED", "Release delayed",
                "CANCELLED", "Event cancelled",
                "REVISED", "Data revised",
                "PRELIMINARY", "Preliminary data"
            ),
            "timePeriods", Map.of(
                "today", "Today's events only",
                "thisWeek", "Current week events",
                "nextWeek", "Next week events",
                "thisMonth", "Current month events",
                "nextMonth", "Next month events"
            ),
            "dataQuality", Map.of(
                "releasedOnly", "Only events with released data",
                "withActualValues", "Only events with actual values",
                "surprisesOnly", "Only events with significant surprises",
                "marketMovingOnly", "Only market moving events",
                "globalEventsOnly", "Only global impact events"
            )
        ));
    }
}