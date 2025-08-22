package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.EconomicEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Economic Calendar Request DTO
 * 
 * Request parameters for querying economic calendar events
 * with filtering and pagination support.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record EconomicCalendarRequest(
    
    // Date filtering
    @NotNull(message = "Start date is required")
    LocalDate startDate,
    
    @NotNull(message = "End date is required")
    LocalDate endDate,
    
    // Geographic filtering
    Set<String> countries, // ISO 3-letter codes
    
    // Event filtering
    Set<String> categories,
    Set<EconomicEvent.EventImportance> importance,
    Set<EconomicEvent.EventStatus> status,
    
    // Impact filtering
    java.math.BigDecimal minImpactScore,
    Boolean marketMovingOnly,
    Boolean globalEventsOnly,
    
    // Content filtering
    String searchTerm,
    Boolean includeRevisions,
    Boolean includePreliminary,
    
    // Time-based filtering
    Boolean todayOnly,
    Boolean upcomingOnly, // Next 7 days
    Integer hoursAhead, // Events in next X hours
    
    // Data quality filtering
    Boolean releasedOnly,
    Boolean withActualValues,
    Boolean surprisesOnly, // Events that beat/missed forecasts
    java.math.BigDecimal minSurprisePercent,
    
    // Sorting and pagination
    String sortBy,
    SortDirection sortDirection,
    Integer page,
    Integer size,
    
    // Output options
    Boolean includeAnalysis,
    Boolean includeHistorical,
    Boolean includeForecast,
    String timezone // For time display conversion
    
) {
    
    public EconomicCalendarRequest {
        // Set defaults
        if (page == null) page = 0;
        if (size == null) size = 50;
        if (sortBy == null) sortBy = "eventDate";
        if (sortDirection == null) sortDirection = SortDirection.ASC;
        if (marketMovingOnly == null) marketMovingOnly = false;
        if (globalEventsOnly == null) globalEventsOnly = false;
        if (includeRevisions == null) includeRevisions = true;
        if (includePreliminary == null) includePreliminary = true;
        if (releasedOnly == null) releasedOnly = false;
        if (withActualValues == null) withActualValues = false;
        if (surprisesOnly == null) surprisesOnly = false;
        if (includeAnalysis == null) includeAnalysis = false;
        if (includeHistorical == null) includeHistorical = false;
        if (includeForecast == null) includeForecast = false;
        if (timezone == null) timezone = "UTC";
        if (minSurprisePercent == null) minSurprisePercent = new java.math.BigDecimal("0.05"); // 5%
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Validation methods
     */
    public boolean isValid() {
        return startDate != null && endDate != null && 
               !endDate.isBefore(startDate) &&
               page != null && page >= 0 &&
               size != null && size > 0 && size <= 1000;
    }
    
    public boolean hasCountryFilter() {
        return countries != null && !countries.isEmpty();
    }
    
    public boolean hasCategoryFilter() {
        return categories != null && !categories.isEmpty();
    }
    
    public boolean hasImportanceFilter() {
        return importance != null && !importance.isEmpty();
    }
    
    public boolean hasStatusFilter() {
        return status != null && !status.isEmpty();
    }
    
    public boolean hasImpactFilter() {
        return minImpactScore != null || marketMovingOnly || globalEventsOnly;
    }
    
    public boolean hasContentFilter() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }
    
    public boolean hasTimeFilter() {
        return todayOnly != null && todayOnly ||
               upcomingOnly != null && upcomingOnly ||
               hoursAhead != null;
    }
    
    public boolean hasDataQualityFilter() {
        return releasedOnly || withActualValues || surprisesOnly;
    }
    
    /**
     * Get total number of active filters
     */
    public int getActiveFilterCount() {
        int count = 0;
        
        if (hasCountryFilter()) count++;
        if (hasCategoryFilter()) count++;
        if (hasImportanceFilter()) count++;
        if (hasStatusFilter()) count++;
        if (hasImpactFilter()) count++;
        if (hasContentFilter()) count++;
        if (hasTimeFilter()) count++;
        if (hasDataQualityFilter()) count++;
        
        return count;
    }
    
    /**
     * Get date range in days
     */
    public long getDateRangeDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Check if request spans multiple months
     */
    public boolean isMultiMonth() {
        return startDate.getMonth() != endDate.getMonth() || 
               startDate.getYear() != endDate.getYear();
    }
    
    /**
     * Get expected result complexity
     */
    public String getComplexity() {
        long days = getDateRangeDays();
        int filters = getActiveFilterCount();
        
        if (days > 365 || filters > 5) {
            return "HIGH";
        } else if (days > 90 || filters > 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Create preset requests
     */
    public static EconomicCalendarRequest todaysEvents() {
        LocalDate today = LocalDate.now();
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(today)
            .todayOnly(true)
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest thisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(weekEnd)
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest upcomingHighImpact() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextWeek)
            .importance(Set.of(EconomicEvent.EventImportance.HIGH, EconomicEvent.EventImportance.CRITICAL))
            .upcomingOnly(true)
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest marketMovingEvents() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextMonth)
            .marketMovingOnly(true)
            .sortBy("marketImpactScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static EconomicCalendarRequest globalEvents() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextWeek)
            .countries(Set.of("USA", "CHN", "EUR", "GBR", "JPN"))
            .globalEventsOnly(true)
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest indiaEvents() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextMonth)
            .countries(Set.of("IND"))
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest recentSurprises() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(startDate)
            .endDate(endDate)
            .surprisesOnly(true)
            .releasedOnly(true)
            .withActualValues(true)
            .sortBy("eventDate")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static EconomicCalendarRequest centralBankEvents() {
        LocalDate today = LocalDate.now();
        LocalDate nextQuarter = today.plusDays(90);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextQuarter)
            .categories(Set.of("Interest Rate", "Monetary Policy", "Central Bank"))
            .importance(Set.of(EconomicEvent.EventImportance.HIGH, EconomicEvent.EventImportance.CRITICAL))
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest employmentData() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextMonth)
            .categories(Set.of("Employment", "Labor Market", "Jobs"))
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static EconomicCalendarRequest inflationData() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextMonth)
            .categories(Set.of("Inflation", "CPI", "PPI", "Core Inflation"))
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    /**
     * Create request for specific analysis needs
     */
    public static EconomicCalendarRequest forMarketAnalysis() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextWeek)
            .marketMovingOnly(true)
            .includeAnalysis(true)
            .includeHistorical(true)
            .includeForecast(true)
            .sortBy("marketImpactScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static EconomicCalendarRequest forRiskManagement() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusDays(30);
        return EconomicCalendarRequest.builder()
            .startDate(today)
            .endDate(nextMonth)
            .importance(Set.of(EconomicEvent.EventImportance.HIGH, EconomicEvent.EventImportance.CRITICAL))
            .globalEventsOnly(true)
            .includeAnalysis(true)
            .sortBy("eventDate")
            .sortDirection(SortDirection.ASC)
            .build();
    }
}