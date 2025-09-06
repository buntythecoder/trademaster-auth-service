package com.trademaster.behavioralai.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.EmotionAnalysisResult;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Behavioral Pattern JPA Entity
 * 
 * Immutable entity for persisting behavioral patterns following TradeMaster standards.
 * Uses functional programming principles and proper encapsulation.
 */
@Entity
@Table(name = "behavioral_patterns", indexes = {
    @Index(name = "idx_behavioral_pattern_user_id", columnList = "user_id"),
    @Index(name = "idx_behavioral_pattern_detected_at", columnList = "detected_at"),
    @Index(name = "idx_behavioral_pattern_session", columnList = "trading_session_id")
})
@RequiredArgsConstructor

public final class BehavioralPattern {
    private static final Logger log = LoggerFactory.getLogger(BehavioralPattern.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private final String userId;

    @Column(name = "pattern_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private final BehavioralPatternData.PatternType patternType;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private final Double confidenceScore;

    @Column(name = "pattern_data", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private final Map<String, Object> patternData;

    @Column(name = "detected_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant detectedAt;

    @Column(name = "trading_session_id", length = 255)
    private final String tradingSessionId;

    @Column(name = "emotional_state", length = 50)
    @Enumerated(EnumType.STRING)
    private final EmotionAnalysisResult.EmotionType emotionalState;

    @Column(name = "risk_score", precision = 5, scale = 4)
    private final Double riskScore;

    @Column(name = "intervention_triggered", nullable = false)
    private final Boolean interventionTriggered;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant createdAt;

    /**
     * Protected constructor for JPA (required for entity loading)
     */
    protected BehavioralPattern() {
        this.id = null;
        this.userId = null;
        this.patternType = null;
        this.confidenceScore = null;
        this.patternData = null;
        this.detectedAt = null;
        this.tradingSessionId = null;
        this.emotionalState = null;
        this.riskScore = null;
        this.interventionTriggered = false;
        this.createdAt = Instant.now();
    }

    /**
     * Factory constructor following Builder pattern for controlled creation
     */
    public BehavioralPattern(String userId,
                           BehavioralPatternData.PatternType patternType,
                           Double confidenceScore,
                           Map<String, Object> patternData,
                           String tradingSessionId,
                           EmotionAnalysisResult.EmotionType emotionalState,
                           Double riskScore,
                           Boolean interventionTriggered) {
        this.id = null; // Let JPA handle ID generation
        this.userId = validateNonNull(userId, "userId");
        this.patternType = validateNonNull(patternType, "patternType");
        this.confidenceScore = validateConfidence(confidenceScore);
        this.patternData = patternData != null ? Map.copyOf(patternData) : Map.of();
        this.detectedAt = Instant.now();
        this.tradingSessionId = tradingSessionId;
        this.emotionalState = emotionalState;
        this.riskScore = riskScore != null ? riskScore : calculateDefaultRiskScore(patternType, confidenceScore);
        this.interventionTriggered = interventionTriggered != null ? interventionTriggered : false;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method for creating pattern from DTO
     */
    public static BehavioralPattern fromDto(BehavioralPatternData dto) {
        return new BehavioralPattern(
            dto.userId(),
            dto.patternType(),
            dto.confidence(),
            dto.patternMetrics() != null ? new HashMap<>(dto.patternMetrics()) : new HashMap<>(),
            dto.sessionId(),
            dto.emotionalState(),
            dto.riskScore(),
            dto.interventionTriggered()
        );
    }

    /**
     * Convert to DTO for service layer
     */
    public BehavioralPatternData toDto() {
        return new BehavioralPatternData(
            "pattern-" + getId(),
            getUserId(),
            getPatternType(),
            getConfidenceScore(),
            getDetectedAt(),
            getTradingSessionId(),
            getEmotionalState(),
            getRiskScore(),
            getInterventionTriggered(),
            getPatternData().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, 
                    entry -> ((Number) entry.getValue()).doubleValue())),
            null, // indicators populated by service layer
            null  // market context populated by service layer
        );
    }

    // Immutable getters (no setters - immutable entity)
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public BehavioralPatternData.PatternType getPatternType() { return patternType; }
    public Double getConfidenceScore() { return confidenceScore; }
    public Map<String, Object> getPatternData() { return patternData != null ? Map.copyOf(patternData) : Map.of(); }
    public Instant getDetectedAt() { return detectedAt; }
    public String getTradingSessionId() { return tradingSessionId; }
    public EmotionAnalysisResult.EmotionType getEmotionalState() { return emotionalState; }
    public Double getRiskScore() { return riskScore; }
    public Boolean getInterventionTriggered() { return interventionTriggered; }
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Business logic methods
     */
    public boolean isHighRisk() {
        return riskScore != null && riskScore > 0.7;
    }

    public boolean requiresIntervention() {
        return patternType.isHighRisk() && confidenceScore > 0.8 && !interventionTriggered;
    }

    // Private validation methods following functional principles
    private static String validateNonNull(String value, String fieldName) {
        return Optional.ofNullable(value)
            .filter(v -> !v.isBlank())
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " cannot be null or blank"));
    }

    private static <T> T validateNonNull(T value, String fieldName) {
        return Optional.ofNullable(value)
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " cannot be null"));
    }

    private static Double validateConfidence(Double confidence) {
        return Optional.ofNullable(confidence)
            .filter(c -> c >= 0.0 && c <= 1.0)
            .orElseThrow(() -> new IllegalArgumentException("Confidence must be between 0.0 and 1.0"));
    }

    private static Double calculateDefaultRiskScore(BehavioralPatternData.PatternType type, Double confidence) {
        return type.isHighRisk() ? confidence * 0.9 : confidence * 0.3;
    }

    /**
     * JPA Converter for JSONB column
     */
    @Converter
    public static class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final Logger log = LoggerFactory.getLogger(BehavioralPattern.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(Map<String, Object> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return "{}";
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                log.error("Error converting map to JSON: {}", e.getMessage());
                return "{}";
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(dbData, Map.class);
            } catch (JsonProcessingException e) {
                log.error("Error converting JSON to map: {}", e.getMessage());
                return Map.of();
            }
        }
    }
}