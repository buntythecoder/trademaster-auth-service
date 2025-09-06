package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Behavioral Pattern Data Record
 * 
 * Immutable data structure representing detected behavioral patterns in trading.
 * Follows functional programming principles with immutable data structures.
 */
@Schema(description = "Detected behavioral pattern with associated metrics and context")
public record BehavioralPatternData(
    @Schema(description = "Unique pattern identifier", example = "pattern-123")
    @JsonProperty("patternId")
    String patternId,
    
    @Schema(description = "User identifier", example = "user-456")
    @JsonProperty("userId")
    String userId,
    
    @Schema(description = "Type of behavioral pattern detected")
    @JsonProperty("patternType")
    PatternType patternType,
    
    @Schema(description = "Confidence score for pattern detection (0.0-1.0)", example = "0.92")
    @JsonProperty("confidence")
    Double confidence,
    
    @Schema(description = "Pattern detection timestamp")
    @JsonProperty("detectedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant detectedAt,
    
    @Schema(description = "Trading session identifier", example = "session-789")
    @JsonProperty("sessionId")
    String sessionId,
    
    @Schema(description = "Associated emotional state during pattern")
    @JsonProperty("emotionalState")
    EmotionAnalysisResult.EmotionType emotionalState,
    
    @Schema(description = "Risk score associated with pattern (0.0-1.0)", example = "0.75")
    @JsonProperty("riskScore")
    Double riskScore,
    
    @Schema(description = "Whether intervention was triggered")
    @JsonProperty("interventionTriggered")
    Boolean interventionTriggered,
    
    @Schema(description = "Pattern-specific metrics and indicators")
    @JsonProperty("patternMetrics")
    Map<String, Double> patternMetrics,
    
    @Schema(description = "Contributing behavioral indicators")
    @JsonProperty("indicators")
    List<BehavioralIndicator> indicators,
    
    @Schema(description = "Market context during pattern occurrence")
    @JsonProperty("marketContext")
    MarketContext marketContext
) {
    /**
     * Compact constructor with validation and immutability
     */
    public BehavioralPatternData {
        validateNonNull(patternId, "patternId");
        validateNonNull(userId, "userId");
        validateNonNull(patternType, "patternType");
        validateConfidence(confidence);
        validateNonNull(detectedAt, "detectedAt");
        
        // Ensure immutability of collections
        patternMetrics = patternMetrics != null ? Map.copyOf(patternMetrics) : Map.of();
        indicators = indicators != null ? List.copyOf(indicators) : List.of();
        interventionTriggered = interventionTriggered != null ? interventionTriggered : false;
        riskScore = riskScore != null ? riskScore : calculateRiskScore(patternType, confidence);
    }
    
    /**
     * Factory method for creating pattern with basic data
     */
    public static BehavioralPatternData create(String userId, PatternType type, 
                                             Double confidence, String sessionId) {
        return new BehavioralPatternData(
            generatePatternId(), userId, type, confidence, Instant.now(),
            sessionId, null, null, false, Map.of(), List.of(), null
        );
    }
    
    /**
     * Check if pattern requires immediate intervention
     */
    public boolean requiresIntervention() {
        return patternType.isHighRisk() && confidence > 0.8 && !interventionTriggered;
    }
    
    /**
     * Get pattern severity based on risk score and confidence
     */
    public PatternSeverity getSeverity() {
        return switch (riskScore) {
            case Double r when r >= 0.8 -> PatternSeverity.CRITICAL;
            case Double r when r >= 0.6 -> PatternSeverity.HIGH;
            case Double r when r >= 0.4 -> PatternSeverity.MEDIUM;
            default -> PatternSeverity.LOW;
        };
    }
    
    /**
     * Calculate pattern frequency within session
     */
    public int getFrequencyInSession() {
        return indicators.size();
    }
    
    private static void validateNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    private static void validateConfidence(Double confidence) {
        if (confidence == null || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
    
    private static Double calculateRiskScore(PatternType type, Double confidence) {
        return type.isHighRisk() ? confidence * 0.9 : confidence * 0.3;
    }
    
    private static String generatePatternId() {
        return "pattern-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * Behavioral pattern types with risk classification
     */
    public enum PatternType {
        IMPULSIVE_TRADING(true, "Rapid orders without analysis"),
        FEAR_OF_MISSING_OUT(true, "Chasing momentum trades"),
        LOSS_AVERSION(true, "Holding losers too long"),
        OVERCONFIDENCE_BIAS(true, "Oversized positions after wins"),
        REVENGE_TRADING(true, "Aggressive trading after losses"),
        ANALYSIS_PARALYSIS(false, "Excessive analysis without action"),
        HERD_MENTALITY(true, "Following crowd sentiment"),
        CONFIRMATION_BIAS(false, "Ignoring contrary signals"),
        ANCHORING_BIAS(false, "Fixation on specific prices"),
        PANIC_SELLING(true, "Emotional exits in stress");
        
        private final boolean highRisk;
        private final String description;
        
        PatternType(boolean highRisk, String description) {
            this.highRisk = highRisk;
            this.description = description;
        }
        
        public boolean isHighRisk() { return highRisk; }
        public String getDescription() { return description; }
    }
    
    /**
     * Pattern severity levels
     */
    public enum PatternSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Behavioral indicator record
     */
    public record BehavioralIndicator(
        String name,
        Double value,
        String description,
        Instant timestamp
    ) {
        public BehavioralIndicator {
            validateNonNull(name, "indicator name");
            validateNonNull(value, "indicator value");
            validateNonNull(timestamp, "indicator timestamp");
        }
    }
    
    /**
     * Market context record
     */
    public record MarketContext(
        String symbol,
        Double volatility,
        String trend,
        Double volume,
        Instant timestamp
    ) {
        public MarketContext {
            validateNonNull(timestamp, "market context timestamp");
        }
    }
}