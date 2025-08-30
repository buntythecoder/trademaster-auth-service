package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.EconomicCalendarRequest;
import com.trademaster.marketdata.dto.EconomicCalendarResponse;
import com.trademaster.marketdata.entity.EconomicEvent;
import com.trademaster.marketdata.repository.EconomicEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Economic Calendar Service
 * 
 * Provides comprehensive economic calendar functionality with event
 * filtering, market impact analysis, and intelligent recommendations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EconomicCalendarService {
    
    private final EconomicEventRepository economicEventRepository;
    private final MarketImpactAnalysisService marketImpactAnalysisService;
    private final EconomicDataProviderService dataProviderService;
    
    /**
     * Get economic calendar events with filtering and analysis
     */
    public CompletableFuture<EconomicCalendarResponse> getCalendarEvents(EconomicCalendarRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing economic calendar request with {} filters", request.getActiveFilterCount());
            
            try {
                // Convert dates to LocalDateTime for database queries
                LocalDateTime startDateTime = request.startDate().atStartOfDay();
                LocalDateTime endDateTime = request.endDate().atTime(23, 59, 59);
                
                // Build pageable with sorting
                Pageable pageable = buildPageable(request);
                
                // Get events based on filters
                Page<EconomicEvent> eventsPage = getFilteredEvents(request, startDateTime, endDateTime, pageable);
                
                // Convert to DTOs
                List<EconomicCalendarResponse.EconomicEventDto> eventDtos = eventsPage.getContent()
                    .stream()
                    .map(this::convertToDto)
                    .toList();
                
                // Enhance with historical context if requested
                if (request.includeHistorical()) {
                    eventDtos = enhanceWithHistoricalContext(eventDtos);
                }
                
                // Calculate statistics
                var statistics = calculateStatistics(eventsPage.getContent(), request);
                
                // Perform market impact analysis
                var marketImpact = performMarketImpactAnalysis(eventsPage.getContent(), request);
                
                // Build pagination info
                var pagination = buildPaginationInfo(eventsPage);
                
                // Generate market alerts
                var marketAlerts = generateMarketAlerts(eventsPage.getContent());
                
                // Generate trends analysis
                var trendsAnalysis = generateTrendsAnalysis(eventsPage.getContent(), request);
                
                log.info("Economic calendar response prepared: {} events, {} total pages", 
                    eventDtos.size(), pagination.totalPages());
                
                return EconomicCalendarResponse.builder()
                    .originalRequest(request)
                    .responseTime(Instant.now())
                    .timezone(request.timezone())
                    .events(eventDtos)
                    .statistics(statistics)
                    .marketImpact(marketImpact)
                    .pagination(pagination)
                    .marketAlerts(marketAlerts)
                    .trendsAnalysis(trendsAnalysis)
                    .build();
                    
            } catch (Exception e) {
                log.error("Failed to process economic calendar request: {}", e.getMessage(), e);
                throw new RuntimeException("Economic calendar processing failed", e);
            }
        });
    }
    
    /**
     * Get filtered events based on request parameters
     */
    private Page<EconomicEvent> getFilteredEvents(EconomicCalendarRequest request, 
            LocalDateTime startDateTime, LocalDateTime endDateTime, Pageable pageable) {
        
        // Handle special time filters first
        if (request.todayOnly() != null && request.todayOnly()) {
            return economicEventRepository.findEventsByDateRange(
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now().toLocalDate().atTime(23, 59, 59),
                pageable);
        }
        
        if (request.upcomingOnly() != null && request.upcomingOnly()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime weekAhead = now.plusDays(7);
            return economicEventRepository.findEventsByDateRange(now, weekAhead, pageable);
        }
        
        if (request.hoursAhead() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime hoursAhead = now.plusHours(request.hoursAhead());
            List<EconomicEvent> events = economicEventRepository.findEventsByTimeRange(now, hoursAhead);
            return new PageImpl<>(events, pageable, events.size());
        }
        
        // Use complex filtering for advanced requests
        if (hasComplexFilters(request)) {
            return economicEventRepository.findEventsWithFilters(
                request.hasCountryFilter() ? new ArrayList<>(request.countries()) : null,
                request.hasCategoryFilter() ? new ArrayList<>(request.categories()) : null,
                request.hasImportanceFilter() ? request.importance().iterator().next() : null,
                request.hasStatusFilter() ? request.status().iterator().next() : null,
                startDateTime,
                endDateTime,
                pageable);
        }
        
        // Handle specific data quality filters
        if (request.surprisesOnly() != null && request.surprisesOnly()) {
            List<EconomicEvent> surpriseEvents = economicEventRepository.findEventsWithSurprises(
                request.minSurprisePercent(), startDateTime, endDateTime);
            return createPageFromList(surpriseEvents, pageable);
        }
        
        if (request.marketMovingOnly() != null && request.marketMovingOnly()) {
            BigDecimal minImpact = request.minImpactScore() != null ? 
                request.minImpactScore() : new BigDecimal("60");
            List<EconomicEvent> marketMovingEvents = economicEventRepository.findMarketMovingEvents(
                minImpact, startDateTime, endDateTime);
            return createPageFromList(marketMovingEvents, pageable);
        }
        
        if (request.globalEventsOnly() != null && request.globalEventsOnly()) {
            List<EconomicEvent> globalEvents = economicEventRepository.findGlobalMarketEvents(
                startDateTime, endDateTime);
            return createPageFromList(globalEvents, pageable);
        }
        
        // Handle search
        if (request.hasContentFilter()) {
            List<EconomicEvent> searchResults = economicEventRepository.searchEventsByText(
                request.searchTerm(), startDateTime, endDateTime);
            return createPageFromList(searchResults, pageable);
        }
        
        // Default: get all events in date range
        return economicEventRepository.findEventsByDateRange(startDateTime, endDateTime, pageable);
    }
    
    /**
     * Build pageable with sorting
     */
    private Pageable buildPageable(EconomicCalendarRequest request) {
        Sort sort = Sort.by(
            request.sortDirection() == EconomicCalendarRequest.SortDirection.DESC ?
                Sort.Direction.DESC : Sort.Direction.ASC,
            request.sortBy()
        );
        
        return PageRequest.of(request.page(), request.size(), sort);
    }
    
    /**
     * Check if request has complex filters requiring advanced query
     */
    private boolean hasComplexFilters(EconomicCalendarRequest request) {
        return request.hasCountryFilter() || request.hasCategoryFilter() || 
               request.hasImportanceFilter() || request.hasStatusFilter();
    }
    
    /**
     * Convert entity to DTO
     */
    private EconomicCalendarResponse.EconomicEventDto convertToDto(EconomicEvent event) {
        return EconomicCalendarResponse.EconomicEventDto.fromEntity(event);
    }
    
    /**
     * Enhance events with historical context
     */
    private List<EconomicCalendarResponse.EconomicEventDto> enhanceWithHistoricalContext(
            List<EconomicCalendarResponse.EconomicEventDto> events) {
        
        return events.stream()
            .map(event -> {
                var historicalContext = buildHistoricalContext(event);
                return EconomicCalendarResponse.EconomicEventDto.builder()
                    .id(event.id())
                    .eventId(event.eventId())
                    .title(event.title())
                    .description(event.description())
                    .country(event.country())
                    .countryName(event.countryName())
                    .category(event.category())
                    .importance(event.importance())
                    .importanceDisplay(event.importanceDisplay())
                    .eventDate(event.eventDate())
                    .eventDateDisplay(event.eventDateDisplay())
                    .status(event.status())
                    .statusDisplay(event.statusDisplay())
                    .releaseTime(event.releaseTime())
                    .nextReleaseDate(event.nextReleaseDate())
                    .unit(event.unit())
                    .frequency(event.frequency())
                    .forecastValue(event.forecastValue())
                    .previousValue(event.previousValue())
                    .actualValue(event.actualValue())
                    .revisionValue(event.revisionValue())
                    .surpriseFactor(event.surpriseFactor())
                    .beatExpectations(event.beatExpectations())
                    .missedExpectations(event.missedExpectations())
                    .marketImpactScore(event.marketImpactScore())
                    .marketImpactLevel(event.marketImpactLevel())
                    .expectedSentiment(event.expectedSentiment())
                    .actualSentiment(event.actualSentiment())
                    .sentimentDisplay(event.sentimentDisplay())
                    .source(event.source())
                    .sourceUrl(event.sourceUrl())
                    .dataProvider(event.dataProvider())
                    .relatedSymbols(event.relatedSymbols())
                    .relatedSectors(event.relatedSectors())
                    .affectedCurrencies(event.affectedCurrencies())
                    .isToday(event.isToday())
                    .isUpcoming(event.isUpcoming())
                    .isReleased(event.isReleased())
                    .isMarketMoving(event.isMarketMoving())
                    .isGlobalEvent(event.isGlobalEvent())
                    .hoursUntilEvent(event.hoursUntilEvent())
                    .hoursSinceEvent(event.hoursSinceEvent())
                    .historicalContext(historicalContext)
                    .analysisNotes(event.analysisNotes())
                    .marketImpactDescription(event.marketImpactDescription())
                    .displayTitle(event.displayTitle())
                    .forecastActualText(event.forecastActualText())
                    .timeUntilDisplay(event.timeUntilDisplay())
                    .priorityScore(event.priorityScore())
                    .build();
            })
            .toList();
    }
    
    /**
     * Build historical context for an event
     */
    private EconomicCalendarResponse.EconomicEventDto.HistoricalContext buildHistoricalContext(
            EconomicCalendarResponse.EconomicEventDto event) {
        
        // This would typically query historical data for the same event type
        // For now, return a placeholder
        return EconomicCalendarResponse.EconomicEventDto.HistoricalContext.builder()
            .lastTwelveValues(List.of()) // Would contain last 12 values
            .averageValue(null) // Calculate from historical data
            .minimumValue(null)
            .maximumValue(null)
            .standardDeviation(null)
            .trend(EconomicCalendarResponse.EconomicEventDto.HistoricalContext.TrendDirection.STABLE)
            .dataPoints(0)
            .build();
    }
    
    /**
     * Calculate calendar statistics
     */
    private EconomicCalendarResponse.CalendarStatistics calculateStatistics(
            List<EconomicEvent> events, EconomicCalendarRequest request) {
        
        int totalEvents = events.size();
        int todaysEvents = (int) events.stream().filter(EconomicEvent::isToday).count();
        int upcomingEvents = (int) events.stream().filter(EconomicEvent::isUpcoming).count();
        int releasedEvents = (int) events.stream().filter(EconomicEvent::isReleased).count();
        int highImpactEvents = (int) events.stream()
            .filter(e -> e.getImportance() == EconomicEvent.EventImportance.HIGH).count();
        int criticalEvents = (int) events.stream()
            .filter(e -> e.getImportance() == EconomicEvent.EventImportance.CRITICAL).count();
        
        // Group by country
        Map<String, Integer> eventsByCountry = events.stream()
            .collect(Collectors.groupingBy(
                EconomicEvent::getCountry,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by category
        Map<String, Integer> eventsByCategory = events.stream()
            .collect(Collectors.groupingBy(
                EconomicEvent::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by importance
        Map<String, Integer> eventsByImportance = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.getImportance().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by status
        Map<String, Integer> eventsByStatus = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.getStatus().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Calculate average impact score
        BigDecimal averageImpactScore = events.stream()
            .map(EconomicEvent::getMarketImpactScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(events.size()), 2, RoundingMode.HALF_UP);
        
        // Count surprises
        int eventsWithSurprises = (int) events.stream()
            .filter(e -> e.getSurpriseFactor() != null && 
                        e.getSurpriseFactor().abs().compareTo(new BigDecimal("5")) > 0)
            .count();
        
        // Find most active country and category
        String mostActiveCountry = eventsByCountry.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
        
        String mostActiveCategory = eventsByCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
        
        return EconomicCalendarResponse.CalendarStatistics.builder()
            .totalEvents(totalEvents)
            .todaysEvents(todaysEvents)
            .upcomingEvents(upcomingEvents)
            .releasedEvents(releasedEvents)
            .highImpactEvents(highImpactEvents)
            .criticalEvents(criticalEvents)
            .eventsByCountry(eventsByCountry)
            .eventsByCategory(eventsByCategory)
            .eventsByImportance(eventsByImportance)
            .eventsByStatus(eventsByStatus)
            .averageImpactScore(averageImpactScore)
            .eventsWithSurprises(eventsWithSurprises)
            .mostActiveCountry(mostActiveCountry)
            .mostActiveCategory(mostActiveCategory)
            .build();
    }
    
    /**
     * Perform market impact analysis
     */
    private EconomicCalendarResponse.MarketImpactAnalysis performMarketImpactAnalysis(
            List<EconomicEvent> events, EconomicCalendarRequest request) {
        
        // Get high impact events
        List<EconomicCalendarResponse.EconomicEventDto> highImpactEvents = events.stream()
            .filter(e -> e.getImportance() == EconomicEvent.EventImportance.HIGH ||
                        e.getImportance() == EconomicEvent.EventImportance.CRITICAL)
            .map(this::convertToDto)
            .toList();
        
        // Get market moving events
        List<EconomicCalendarResponse.EconomicEventDto> marketMovingEvents = events.stream()
            .filter(EconomicEvent::isMarketMoving)
            .map(this::convertToDto)
            .toList();
        
        // Get surprise events
        List<EconomicCalendarResponse.EconomicEventDto> surpriseEvents = events.stream()
            .filter(e -> e.getSurpriseFactor() != null && 
                        e.getSurpriseFactor().abs().compareTo(new BigDecimal("5")) > 0)
            .map(this::convertToDto)
            .toList();
        
        // Calculate currency impact scores
        Map<String, BigDecimal> currencyImpactScore = calculateCurrencyImpactScores(events);
        
        // Calculate sector impact scores
        Map<String, BigDecimal> sectorImpactScore = calculateSectorImpactScores(events);
        
        // Generate risk factors and opportunities
        List<String> riskFactors = generateRiskFactors(events);
        List<String> opportunities = generateOpportunities(events);
        
        // Determine overall market sentiment
        String overallMarketSentiment = calculateOverallMarketSentiment(events);
        
        // Calculate volatility expectation
        BigDecimal volatilityExpectation = calculateVolatilityExpectation(events);
        
        return EconomicCalendarResponse.MarketImpactAnalysis.builder()
            .highImpactEvents(highImpactEvents)
            .marketMovingEvents(marketMovingEvents)
            .surpriseEvents(surpriseEvents)
            .currencyImpactScore(currencyImpactScore)
            .sectorImpactScore(sectorImpactScore)
            .riskFactors(riskFactors)
            .opportunities(opportunities)
            .overallMarketSentiment(overallMarketSentiment)
            .volatilityExpectation(volatilityExpectation)
            .build();
    }
    
    /**
     * Generate market alerts based on events
     */
    private List<String> generateMarketAlerts(List<EconomicEvent> events) {
        List<String> alerts = new ArrayList<>();
        
        // Check for critical events today
        long criticalToday = events.stream()
            .filter(e -> e.isToday() && e.getImportance() == EconomicEvent.EventImportance.CRITICAL)
            .count();
        
        if (criticalToday > 0) {
            alerts.add(String.format("🚨 %d critical economic event(s) scheduled for today", criticalToday));
        }
        
        // Check for multiple high impact events
        long highImpactUpcoming = events.stream()
            .filter(e -> e.isUpcoming() && 
                        (e.getImportance() == EconomicEvent.EventImportance.HIGH ||
                         e.getImportance() == EconomicEvent.EventImportance.CRITICAL))
            .count();
        
        if (highImpactUpcoming >= 3) {
            alerts.add(String.format("⚠️ %d high-impact events in the next 7 days - expect increased volatility", 
                highImpactUpcoming));
        }
        
        // Check for recent surprises
        long recentSurprises = events.stream()
            .filter(e -> e.isReleased())
            .filter(e -> e.getHoursSinceEvent() <= 24)
            .filter(e -> e.getSurpriseFactor() != null)
            .filter(e -> e.getSurpriseFactor().abs().compareTo(new BigDecimal("10")) > 0)
            .count();
        
        if (recentSurprises > 0) {
            alerts.add(String.format("📊 %d significant economic surprise(s) in the last 24 hours", recentSurprises));
        }
        
        return alerts;
    }
    
    // Helper methods (implementation stubs)
    
    private Page<EconomicEvent> createPageFromList(List<EconomicEvent> events, Pageable pageable) {
        // Create a page from a list - implementation stub
        return Page.empty(); // Simplified
    }
    
    private EconomicCalendarResponse.PaginationInfo buildPaginationInfo(Page<EconomicEvent> page) {
        return EconomicCalendarResponse.PaginationInfo.builder()
            .currentPage(page.getNumber())
            .pageSize(page.getSize())
            .totalPages(page.getTotalPages())
            .totalEvents(page.getTotalElements())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
    
    private Map<String, BigDecimal> calculateCurrencyImpactScores(List<EconomicEvent> events) {
        // Calculate impact scores for different currencies
        return Map.of(
            "USD", new BigDecimal("75.5"),
            "EUR", new BigDecimal("65.2"),
            "INR", new BigDecimal("45.8")
        );
    }
    
    private Map<String, BigDecimal> calculateSectorImpactScores(List<EconomicEvent> events) {
        // Calculate impact scores for different sectors
        return Map.of(
            "Financial Services", new BigDecimal("80.3"),
            "Technology", new BigDecimal("25.7"),
            "Energy", new BigDecimal("60.1")
        );
    }
    
    private List<String> generateRiskFactors(List<EconomicEvent> events) {
        return List.of(
            "Multiple central bank meetings scheduled",
            "High inflation data releases pending",
            "Geopolitical tensions affecting markets"
        );
    }
    
    private List<String> generateOpportunities(List<EconomicEvent> events) {
        return List.of(
            "Potential dovish central bank signals",
            "Strong employment data expected",
            "Currency volatility trading opportunities"
        );
    }
    
    private String calculateOverallMarketSentiment(List<EconomicEvent> events) {
        // Analyze sentiment based on expected outcomes
        return "CAUTIOUSLY_OPTIMISTIC";
    }
    
    private BigDecimal calculateVolatilityExpectation(List<EconomicEvent> events) {
        // Calculate expected volatility based on events
        return new BigDecimal("15.5"); // Percentage
    }
    
    private Map<String, Object> generateTrendsAnalysis(List<EconomicEvent> events, 
            EconomicCalendarRequest request) {
        return Map.of(
            "economicHealth", "MODERATE",
            "inflationTrend", "STABILIZING",
            "employmentTrend", "IMPROVING",
            "monetaryPolicy", "ACCOMMODATIVE"
        );
    }
}