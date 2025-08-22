package com.trademaster.marketdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Economic Event Entity
 * 
 * Represents economic calendar events that can impact market movements.
 * Includes event details, forecasts, actual values, and market impact.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "economic_events", indexes = {
    @Index(name = "idx_economic_events_date", columnList = "eventDate"),
    @Index(name = "idx_economic_events_country", columnList = "country"),
    @Index(name = "idx_economic_events_importance", columnList = "importance"),
    @Index(name = "idx_economic_events_category", columnList = "category"),
    @Index(name = "idx_economic_events_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EconomicEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String eventId; // External provider event ID
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, length = 3)
    private String country; // ISO 3-letter country code (IND, USA, etc.)
    
    @Column(nullable = false, length = 100)
    private String category; // GDP, Inflation, Employment, etc.
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventImportance importance;
    
    @Column(nullable = false)
    private LocalDateTime eventDate;
    
    @Column(length = 50)
    private String frequency; // Monthly, Quarterly, Annual, etc.
    
    @Column(length = 100)
    private String unit; // %, billions, millions, index, etc.
    
    // Forecast and actual values
    @Column(precision = 15, scale = 4)
    private BigDecimal forecastValue;
    
    @Column(precision = 15, scale = 4)
    private BigDecimal previousValue;
    
    @Column(precision = 15, scale = 4)
    private BigDecimal actualValue;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal revisionValue; // Revision to previous value
    
    // Market impact analysis
    @Column(precision = 5, scale = 2)
    private BigDecimal marketImpactScore; // 0-100 impact score
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private MarketSentiment expectedSentiment;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private MarketSentiment actualSentiment;
    
    // Status and timing
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    
    @Column
    private LocalDateTime releaseTime;
    
    @Column
    private LocalDateTime nextReleaseDate;
    
    // Data source information
    @Column(length = 100)
    private String source; // Central Bank, Government Agency, etc.
    
    @Column(length = 100)
    private String sourceUrl;
    
    @Column(length = 50)
    private String dataProvider; // TradingEconomics, Alpha Vantage, etc.
    
    // Additional metadata
    @Column(columnDefinition = "JSON")
    private String relatedSymbols; // JSON array of affected symbols
    
    @Column(columnDefinition = "JSON")
    private String relatedSectors; // JSON array of affected sectors
    
    @Column(columnDefinition = "JSON")
    private String historicalData; // JSON with historical values
    
    @Column(columnDefinition = "TEXT")
    private String analysisNotes;
    
    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    /**
     * Event importance levels
     */
    public enum EventImportance {
        LOW("Low impact - minor market effect"),
        MEDIUM("Medium impact - moderate market effect"),
        HIGH("High impact - significant market effect"),
        CRITICAL("Critical impact - major market moving event");
        
        private final String description;
        
        EventImportance(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getWeight() {
            return switch (this) {
                case LOW -> 1;
                case MEDIUM -> 2;
                case HIGH -> 3;
                case CRITICAL -> 4;
            };
        }
    }
    
    /**
     * Market sentiment directions
     */
    public enum MarketSentiment {
        VERY_BEARISH("Very Bearish"),
        BEARISH("Bearish"),
        NEUTRAL("Neutral"),
        BULLISH("Bullish"),
        VERY_BULLISH("Very Bullish");
        
        private final String description;
        
        MarketSentiment(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getScore() {
            return switch (this) {
                case VERY_BEARISH -> -2;
                case BEARISH -> -1;
                case NEUTRAL -> 0;
                case BULLISH -> 1;
                case VERY_BULLISH -> 2;
            };
        }
    }
    
    /**
     * Event status
     */
    public enum EventStatus {
        SCHEDULED("Scheduled for release"),
        RELEASED("Data released"),
        DELAYED("Release delayed"),
        CANCELLED("Event cancelled"),
        REVISED("Data revised"),
        PRELIMINARY("Preliminary data");
        
        private final String description;
        
        EventStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Business logic methods
     */
    
    /**
     * Check if event has been released
     */
    public boolean isReleased() {
        return status == EventStatus.RELEASED || status == EventStatus.REVISED;
    }
    
    /**
     * Check if event is upcoming (within next 7 days)
     */
    public boolean isUpcoming() {
        return eventDate.isAfter(LocalDateTime.now()) && 
               eventDate.isBefore(LocalDateTime.now().plusDays(7));
    }
    
    /**
     * Check if event is today
     */
    public boolean isToday() {
        LocalDateTime now = LocalDateTime.now();
        return eventDate.toLocalDate().equals(now.toLocalDate());
    }
    
    /**
     * Get surprise factor (actual vs forecast)
     */
    public BigDecimal getSurpriseFactor() {
        if (actualValue == null || forecastValue == null || 
            forecastValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return actualValue.subtract(forecastValue)
            .divide(forecastValue.abs(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Check if actual value surprised to the upside
     */
    public boolean isBeatExpectations() {
        BigDecimal surprise = getSurpriseFactor();
        return surprise != null && surprise.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if actual value missed expectations
     */
    public boolean isMissedExpectations() {
        BigDecimal surprise = getSurpriseFactor();
        return surprise != null && surprise.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get market impact classification
     */
    public String getMarketImpactLevel() {
        if (marketImpactScore == null) {
            return "UNKNOWN";
        }
        
        BigDecimal score = marketImpactScore;
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "VERY_HIGH";
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "HIGH";
        } else if (score.compareTo(new BigDecimal("40")) >= 0) {
            return "MEDIUM";
        } else if (score.compareTo(new BigDecimal("20")) >= 0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    /**
     * Check if event is market moving
     */
    public boolean isMarketMoving() {
        return importance == EventImportance.HIGH || importance == EventImportance.CRITICAL ||
               (marketImpactScore != null && marketImpactScore.compareTo(new BigDecimal("60")) >= 0);
    }
    
    /**
     * Get time until event (in hours)
     */
    public long getHoursUntilEvent() {
        return java.time.Duration.between(LocalDateTime.now(), eventDate).toHours();
    }
    
    /**
     * Get time since event (in hours)
     */
    public long getHoursSinceEvent() {
        return java.time.Duration.between(eventDate, LocalDateTime.now()).toHours();
    }
    
    /**
     * Check if event affects specific country
     */
    public boolean affectsCountry(String countryCode) {
        return country.equalsIgnoreCase(countryCode);
    }
    
    /**
     * Check if event affects global markets
     */
    public boolean isGlobalEvent() {
        return "USA".equals(country) || "CHN".equals(country) || "EUR".equals(country) ||
               importance == EventImportance.CRITICAL;
    }
    
    /**
     * Get event priority score for sorting
     */
    public int getPriorityScore() {
        int score = importance.getWeight() * 25;
        
        if (marketImpactScore != null) {
            score += marketImpactScore.intValue();
        }
        
        // Boost score for upcoming events
        if (isUpcoming()) {
            score += 20;
        }
        
        // Boost score for today's events
        if (isToday()) {
            score += 30;
        }
        
        return score;
    }
    
    /**
     * Format event for display
     */
    public String getDisplayTitle() {
        StringBuilder title = new StringBuilder();
        title.append(country).append(" - ").append(this.title);
        
        if (frequency != null) {
            title.append(" (").append(frequency).append(")");
        }
        
        return title.toString();
    }
    
    /**
     * Get forecast vs actual comparison text
     */
    public String getForecastActualText() {
        if (forecastValue == null) {
            return "No forecast available";
        }
        
        if (actualValue == null) {
            return String.format("Forecast: %s %s", forecastValue, unit != null ? unit : "");
        }
        
        return String.format("Forecast: %s %s | Actual: %s %s", 
            forecastValue, unit != null ? unit : "",
            actualValue, unit != null ? unit : "");
    }
}