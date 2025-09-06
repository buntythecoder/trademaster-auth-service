package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Emotion Analysis Result Record
 * 
 * Immutable data structure representing the result of emotion detection analysis.
 * Uses Java 24 Records for immutability and functional programming compliance.
 */
@Schema(description = "Result of emotional state analysis from trading behavior")
public record EmotionAnalysisResult(
    @Schema(description = "Unique correlation ID for tracking", example = "uuid-123")
    @JsonProperty("correlationId")
    String correlationId,
    
    @Schema(description = "User identifier", example = "user-456")
    @JsonProperty("userId")
    String userId,
    
    @Schema(description = "Primary detected emotional state")
    @JsonProperty("primaryEmotion")
    EmotionType primaryEmotion,
    
    @Schema(description = "Confidence score for primary emotion (0.0-1.0)", example = "0.85")
    @JsonProperty("primaryConfidence")
    Double primaryConfidence,
    
    @Schema(description = "Map of all detected emotions with confidence scores")
    @JsonProperty("emotionScores")
    Map<EmotionType, Double> emotionScores,
    
    @Schema(description = "Trading session identifier", example = "session-789")
    @JsonProperty("sessionId")
    String sessionId,
    
    @Schema(description = "Analysis timestamp")
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant timestamp,
    
    @Schema(description = "Risk level indicator based on emotional state")
    @JsonProperty("riskLevel")
    RiskLevel riskLevel,
    
    @Schema(description = "Additional behavioral indicators")
    @JsonProperty("behavioralIndicators")
    Map<String, Object> behavioralIndicators
) {
    /**
     * Compact constructor with validation
     */
    public EmotionAnalysisResult {
        // Validate required fields using functional approach
        validateNonNull(correlationId, "correlationId");
        validateNonNull(userId, "userId");
        validateNonNull(primaryEmotion, "primaryEmotion");
        validateConfidence(primaryConfidence);
        validateNonNull(timestamp, "timestamp");
        
        // Default values for optional fields
        emotionScores = emotionScores != null ? Map.copyOf(emotionScores) : Map.of();
        riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        behavioralIndicators = behavioralIndicators != null ? Map.copyOf(behavioralIndicators) : Map.of();
    }
    
    /**
     * Factory method for creating successful analysis result
     */
    public static EmotionAnalysisResult success(String correlationId, String userId, 
                                              EmotionType emotion, Double confidence,
                                              Map<EmotionType, Double> scores) {
        return new EmotionAnalysisResult(
            correlationId, userId, emotion, confidence, scores,
            null, Instant.now(), calculateRiskLevel(emotion, confidence), Map.of()
        );
    }
    
    /**
     * Check if emotion indicates high risk state
     */
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || 
               primaryEmotion.isHighRisk() ||
               primaryConfidence > 0.8;
    }
    
    /**
     * Get emotion intensity based on confidence score
     */
    public EmotionIntensity getIntensity() {
        return switch (primaryConfidence) {
            case Double c when c >= 0.8 -> EmotionIntensity.HIGH;
            case Double c when c >= 0.6 -> EmotionIntensity.MEDIUM;
            default -> EmotionIntensity.LOW;
        };
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
    
    private static RiskLevel calculateRiskLevel(EmotionType emotion, Double confidence) {
        return emotion.isHighRisk() && confidence > 0.7 ? RiskLevel.HIGH : RiskLevel.LOW;
    }
    
    /**
     * Supported emotion types for detection
     */
    public enum EmotionType {
        CALM(false, "Stable emotional state"),
        EXCITED(true, "High energy positive state"),
        ANXIOUS(true, "Worried or nervous state"),
        FEARFUL(true, "Risk-averse fear state"),
        CONFIDENT(false, "Positive assertive state"),
        FRUSTRATED(true, "Negative agitated state"),
        EUPHORIC(true, "Extreme positive state"),
        PANICKED(true, "Extreme fear state"),
        GREEDY(true, "Risk-seeking state"),
        REGRETFUL(false, "Post-decision negative state");
        
        private final boolean highRisk;
        private final String description;
        
        EmotionType(boolean highRisk, String description) {
            this.highRisk = highRisk;
            this.description = description;
        }
        
        public boolean isHighRisk() { return highRisk; }
        public String getDescription() { return description; }
    }
    
    /**
     * Risk levels based on emotional analysis
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
    
    /**
     * Emotion intensity levels
     */
    public enum EmotionIntensity {
        LOW, MEDIUM, HIGH
    }
}