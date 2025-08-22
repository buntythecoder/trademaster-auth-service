package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.EconomicEvent;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Economic Calendar Response DTO
 * 
 * Comprehensive response containing economic events with analysis,
 * statistics, and market impact assessment.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record EconomicCalendarResponse(
    
    // Request metadata
    EconomicCalendarRequest originalRequest,
    Instant responseTime,
    String timezone,
    
    // Events data
    List<EconomicEventDto> events,
    
    // Statistics and analysis
    CalendarStatistics statistics,
    MarketImpactAnalysis marketImpact,
    
    // Pagination
    PaginationInfo pagination,
    
    // Additional insights
    List<String> marketAlerts,
    Map<String, Object> trendsAnalysis
    
) {
    
    /**
     * Economic Event DTO for API responses
     */
    @Builder
    public record EconomicEventDto(
        
        // Basic event information
        Long id,
        String eventId,
        String title,
        String description,
        String country,
        String countryName,
        String category,
        EconomicEvent.EventImportance importance,
        String importanceDisplay,
        
        // Timing
        LocalDateTime eventDate,
        String eventDateDisplay,
        EconomicEvent.EventStatus status,
        String statusDisplay,
        LocalDateTime releaseTime,
        LocalDateTime nextReleaseDate,
        
        // Values and forecast
        String unit,
        String frequency,
        BigDecimal forecastValue,
        BigDecimal previousValue,
        BigDecimal actualValue,
        BigDecimal revisionValue,
        
        // Analysis
        BigDecimal surpriseFactor, // % deviation from forecast
        Boolean beatExpectations,
        Boolean missedExpectations,
        BigDecimal marketImpactScore,
        String marketImpactLevel,
        
        // Sentiment
        EconomicEvent.MarketSentiment expectedSentiment,
        EconomicEvent.MarketSentiment actualSentiment,
        String sentimentDisplay,
        
        // Source information
        String source,
        String sourceUrl,
        String dataProvider,
        
        // Related data
        List<String> relatedSymbols,
        List<String> relatedSectors,
        List<String> affectedCurrencies,
        
        // Time-based flags
        Boolean isToday,
        Boolean isUpcoming,
        Boolean isReleased,
        Boolean isMarketMoving,
        Boolean isGlobalEvent,
        Long hoursUntilEvent,
        Long hoursSinceEvent,
        
        // Historical context (optional)
        HistoricalContext historicalContext,
        
        // Analysis notes
        String analysisNotes,
        String marketImpactDescription,
        
        // Display formatting
        String displayTitle,
        String forecastActualText,
        String timeUntilDisplay,
        Integer priorityScore
        
    ) {
        
        /**
         * Historical context for events
         */
        @Builder
        public record HistoricalContext(
            List<BigDecimal> lastTwelveValues,
            BigDecimal averageValue,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            BigDecimal standardDeviation,
            TrendDirection trend,
            Integer dataPoints
        ) {
            
            public enum TrendDirection {
                RISING, FALLING, STABLE, VOLATILE
            }
        }
        
        /**
         * Factory method to create DTO from entity
         */
        public static EconomicEventDto fromEntity(EconomicEvent event) {
            return EconomicEventDto.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .title(event.getTitle())
                .description(event.getDescription())
                .country(event.getCountry())
                .countryName(getCountryName(event.getCountry()))
                .category(event.getCategory())
                .importance(event.getImportance())
                .importanceDisplay(event.getImportance().getDescription())
                .eventDate(event.getEventDate())
                .eventDateDisplay(formatEventDate(event.getEventDate()))
                .status(event.getStatus())
                .statusDisplay(event.getStatus().getDescription())
                .releaseTime(event.getReleaseTime())
                .nextReleaseDate(event.getNextReleaseDate())
                .unit(event.getUnit())
                .frequency(event.getFrequency())
                .forecastValue(event.getForecastValue())
                .previousValue(event.getPreviousValue())
                .actualValue(event.getActualValue())
                .revisionValue(event.getRevisionValue())
                .surpriseFactor(event.getSurpriseFactor())
                .beatExpectations(event.isBeatExpectations())
                .missedExpectations(event.isMissedExpectations())
                .marketImpactScore(event.getMarketImpactScore())
                .marketImpactLevel(event.getMarketImpactLevel())
                .expectedSentiment(event.getExpectedSentiment())
                .actualSentiment(event.getActualSentiment())
                .sentimentDisplay(formatSentiment(event.getActualSentiment(), event.getExpectedSentiment()))
                .source(event.getSource())
                .sourceUrl(event.getSourceUrl())
                .dataProvider(event.getDataProvider())
                .relatedSymbols(parseJsonArray(event.getRelatedSymbols()))
                .relatedSectors(parseJsonArray(event.getRelatedSectors()))
                .affectedCurrencies(getAffectedCurrencies(event.getCountry()))
                .isToday(event.isToday())
                .isUpcoming(event.isUpcoming())
                .isReleased(event.isReleased())
                .isMarketMoving(event.isMarketMoving())
                .isGlobalEvent(event.isGlobalEvent())
                .hoursUntilEvent(event.getHoursUntilEvent())
                .hoursSinceEvent(event.getHoursSinceEvent())
                .analysisNotes(event.getAnalysisNotes())
                .marketImpactDescription(getMarketImpactDescription(event))
                .displayTitle(event.getDisplayTitle())
                .forecastActualText(event.getForecastActualText())
                .timeUntilDisplay(formatTimeUntil(event.getHoursUntilEvent()))
                .priorityScore(event.getPriorityScore())
                .build();
        }
        
        // Helper methods
        private static String getCountryName(String countryCode) {
            return switch (countryCode) {
                case "IND" -> "India";
                case "USA" -> "United States";
                case "CHN" -> "China";
                case "EUR" -> "Eurozone";
                case "GBR" -> "United Kingdom";
                case "JPN" -> "Japan";
                case "GER" -> "Germany";
                case "FRA" -> "France";
                case "CAN" -> "Canada";
                case "AUS" -> "Australia";
                default -> countryCode;
            };
        }
        
        private static String formatEventDate(LocalDateTime date) {
            if (date == null) return null;
            return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
        
        private static String formatSentiment(EconomicEvent.MarketSentiment actual, 
                EconomicEvent.MarketSentiment expected) {
            if (actual != null) {
                return actual.getDescription();
            }
            if (expected != null) {
                return "Expected: " + expected.getDescription();
            }
            return "Neutral";
        }
        
        private static List<String> parseJsonArray(String jsonArray) {
            // Simplified JSON parsing - in production use proper JSON library
            if (jsonArray == null || jsonArray.trim().isEmpty()) {
                return List.of();
            }
            return List.of(); // Stub
        }
        
        private static List<String> getAffectedCurrencies(String country) {
            return switch (country) {
                case "IND" -> List.of("INR", "USD/INR");
                case "USA" -> List.of("USD", "DXY", "EUR/USD", "GBP/USD", "USD/JPY");
                case "EUR" -> List.of("EUR", "EUR/USD", "EUR/GBP", "EUR/JPY");
                case "GBR" -> List.of("GBP", "GBP/USD", "EUR/GBP", "GBP/JPY");
                case "JPN" -> List.of("JPY", "USD/JPY", "EUR/JPY", "GBP/JPY");
                case "CHN" -> List.of("CNY", "USD/CNY", "EUR/CNY");
                default -> List.of();
            };
        }
        
        private static String getMarketImpactDescription(EconomicEvent event) {
            if (event.getMarketImpactScore() == null) {
                return "Impact assessment pending";
            }
            
            String level = event.getMarketImpactLevel();
            return switch (level) {
                case "VERY_HIGH" -> "Major market moving event - expect significant volatility";
                case "HIGH" -> "High impact event - moderate to significant market reaction expected";
                case "MEDIUM" -> "Moderate impact - some market reaction likely";
                case "LOW" -> "Low impact - minimal market reaction expected";
                case "MINIMAL" -> "Minimal impact - little to no market reaction expected";
                default -> "Impact level unknown";
            };
        }
        
        private static String formatTimeUntil(Long hours) {
            if (hours == null || hours < 0) {
                return "Past event";
            }
            
            if (hours == 0) {
                return "Now";
            } else if (hours < 24) {
                return String.format("%d hours", hours);
            } else {
                long days = hours / 24;
                long remainingHours = hours % 24;
                if (remainingHours == 0) {
                    return String.format("%d days", days);
                } else {
                    return String.format("%d days, %d hours", days, remainingHours);
                }
            }
        }
    }
    
    /**
     * Calendar statistics
     */
    @Builder
    public record CalendarStatistics(
        Integer totalEvents,
        Integer todaysEvents,
        Integer upcomingEvents,
        Integer releasedEvents,
        Integer highImpactEvents,
        Integer criticalEvents,
        Map<String, Integer> eventsByCountry,
        Map<String, Integer> eventsByCategory,
        Map<String, Integer> eventsByImportance,
        Map<String, Integer> eventsByStatus,
        BigDecimal averageImpactScore,
        Integer eventsWithSurprises,
        String mostActiveCountry,
        String mostActiveCategory
    ) {}
    
    /**
     * Market impact analysis
     */
    @Builder
    public record MarketImpactAnalysis(
        List<EconomicEventDto> highImpactEvents,
        List<EconomicEventDto> marketMovingEvents,
        List<EconomicEventDto> surpriseEvents,
        Map<String, BigDecimal> currencyImpactScore,
        Map<String, BigDecimal> sectorImpactScore,
        List<String> riskFactors,
        List<String> opportunities,
        String overallMarketSentiment,
        BigDecimal volatilityExpectation
    ) {}
    
    /**
     * Pagination information
     */
    @Builder
    public record PaginationInfo(
        Integer currentPage,
        Integer pageSize,
        Integer totalPages,
        Long totalEvents,
        Boolean hasNext,
        Boolean hasPrevious
    ) {}
    
    /**
     * Get events by importance level
     */
    public List<EconomicEventDto> getEventsByImportance(EconomicEvent.EventImportance importance) {
        return events.stream()
            .filter(event -> event.importance() == importance)
            .toList();
    }
    
    /**
     * Get events by country
     */
    public List<EconomicEventDto> getEventsByCountry(String country) {
        return events.stream()
            .filter(event -> country.equals(event.country()))
            .toList();
    }
    
    /**
     * Get events by category
     */
    public List<EconomicEventDto> getEventsByCategory(String category) {
        return events.stream()
            .filter(event -> category.equals(event.category()))
            .toList();
    }
    
    /**
     * Get today's events
     */
    public List<EconomicEventDto> getTodaysEvents() {
        return events.stream()
            .filter(event -> Boolean.TRUE.equals(event.isToday()))
            .toList();
    }
    
    /**
     * Get upcoming events
     */
    public List<EconomicEventDto> getUpcomingEvents() {
        return events.stream()
            .filter(event -> Boolean.TRUE.equals(event.isUpcoming()))
            .toList();
    }
    
    /**
     * Get market moving events
     */
    public List<EconomicEventDto> getMarketMovingEvents() {
        return events.stream()
            .filter(event -> Boolean.TRUE.equals(event.isMarketMoving()))
            .toList();
    }
    
    /**
     * Get events with surprises
     */
    public List<EconomicEventDto> getSurpriseEvents() {
        return events.stream()
            .filter(event -> (Boolean.TRUE.equals(event.beatExpectations()) || 
                             Boolean.TRUE.equals(event.missedExpectations())) &&
                             event.surpriseFactor() != null)
            .toList();
    }
    
    /**
     * Get response summary
     */
    public String getSummary() {
        if (statistics == null) {
            return "No statistics available";
        }
        
        return String.format(
            "Found %d events (%d today, %d upcoming, %d high impact)",
            statistics.totalEvents(),
            statistics.todaysEvents(),
            statistics.upcomingEvents(),
            statistics.highImpactEvents()
        );
    }
    
    /**
     * Check if response contains significant events
     */
    public boolean hasSignificantEvents() {
        return statistics != null && 
               (statistics.criticalEvents() > 0 || statistics.highImpactEvents() > 0);
    }
    
    /**
     * Get risk level assessment
     */
    public String getRiskLevel() {
        if (statistics == null) {
            return "UNKNOWN";
        }
        
        if (statistics.criticalEvents() > 0) {
            return "HIGH";
        } else if (statistics.highImpactEvents() > 2) {
            return "MEDIUM";
        } else if (statistics.highImpactEvents() > 0) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }
}