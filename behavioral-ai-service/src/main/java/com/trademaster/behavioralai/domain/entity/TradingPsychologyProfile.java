package com.trademaster.behavioralai.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Trading Psychology Profile JPA Entity
 * 
 * Immutable entity representing comprehensive psychological profile of a trader.
 * Follows TradeMaster standards with functional programming principles.
 */
@Entity
@Table(name = "trading_psychology_profiles", indexes = {
    @Index(name = "idx_psychology_profile_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_psychology_profile_updated", columnList = "last_updated")
})
@RequiredArgsConstructor

public final class TradingPsychologyProfile {
    private static final Logger log = LoggerFactory.getLogger(TradingPsychologyProfile.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 255)
    private final String userId;

    @Column(name = "risk_tolerance_score", nullable = false, precision = 5, scale = 4)
    private final Double riskToleranceScore;

    @Column(name = "emotional_stability_score", nullable = false, precision = 5, scale = 4)
    private final Double emotionalStabilityScore;

    @Column(name = "impulsivity_score", nullable = false, precision = 5, scale = 4)
    private final Double impulsivityScore;

    @Column(name = "overconfidence_score", nullable = false, precision = 5, scale = 4)
    private final Double overconfidenceScore;

    @Column(name = "loss_aversion_score", nullable = false, precision = 5, scale = 4)
    private final Double lossAversionScore;

    @Column(name = "dominant_pattern", length = 50)
    @Enumerated(EnumType.STRING)
    private final BehavioralPatternData.PatternType dominantPattern;

    @Column(name = "trader_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private final TraderType traderType;

    @Column(name = "confidence_level", precision = 5, scale = 4)
    private final Double confidenceLevel;

    @Column(name = "last_updated", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant createdAt;

    /**
     * Protected constructor for JPA
     */
    protected TradingPsychologyProfile() {
        this.id = null;
        this.userId = null;
        this.riskToleranceScore = 0.5;
        this.emotionalStabilityScore = 0.5;
        this.impulsivityScore = 0.5;
        this.overconfidenceScore = 0.5;
        this.lossAversionScore = 0.5;
        this.dominantPattern = null;
        this.traderType = TraderType.BALANCED;
        this.confidenceLevel = 0.0;
        this.lastUpdated = Instant.now();
        this.createdAt = Instant.now();
    }

    /**
     * Factory constructor for creating psychology profile
     */
    public TradingPsychologyProfile(String userId,
                                   Double riskTolerance,
                                   Double emotionalStability,
                                   Double impulsivity,
                                   Double overconfidence,
                                   Double lossAversion) {
        this.id = null;
        this.userId = validateNonNull(userId, "userId");
        this.riskToleranceScore = validateScore(riskTolerance, "riskToleranceScore");
        this.emotionalStabilityScore = validateScore(emotionalStability, "emotionalStabilityScore");
        this.impulsivityScore = validateScore(impulsivity, "impulsivityScore");
        this.overconfidenceScore = validateScore(overconfidence, "overconfidenceScore");
        this.lossAversionScore = validateScore(lossAversion, "lossAversionScore");
        this.dominantPattern = calculateDominantPattern();
        this.traderType = calculateTraderType();
        this.confidenceLevel = calculateConfidenceLevel();
        this.lastUpdated = Instant.now();
        this.createdAt = Instant.now();
    }

    /**
     * Factory method for creating initial profile with default scores
     */
    public static TradingPsychologyProfile createDefault(String userId) {
        return new TradingPsychologyProfile(userId, 0.5, 0.5, 0.5, 0.5, 0.5);
    }

    /**
     * Create updated profile with new scores
     */
    public TradingPsychologyProfile updateScores(Double riskTolerance,
                                                Double emotionalStability,
                                                Double impulsivity,
                                                Double overconfidence,
                                                Double lossAversion) {
        return new TradingPsychologyProfile(
            this.userId,
            riskTolerance != null ? riskTolerance : this.riskToleranceScore,
            emotionalStability != null ? emotionalStability : this.emotionalStabilityScore,
            impulsivity != null ? impulsivity : this.impulsivityScore,
            overconfidence != null ? overconfidence : this.overconfidenceScore,
            lossAversion != null ? lossAversion : this.lossAversionScore
        );
    }

    // Immutable getters
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Double getRiskToleranceScore() { return riskToleranceScore; }
    public Double getEmotionalStabilityScore() { return emotionalStabilityScore; }
    public Double getImpulsivityScore() { return impulsivityScore; }
    public Double getOverconfidenceScore() { return overconfidenceScore; }
    public Double getLossAversionScore() { return lossAversionScore; }
    public BehavioralPatternData.PatternType getDominantPattern() { return dominantPattern; }
    public TraderType getTraderType() { return traderType; }
    public Double getConfidenceLevel() { return confidenceLevel; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Business logic methods
     */
    public boolean isHighRiskTrader() {
        return riskToleranceScore > 0.7 && impulsivityScore > 0.6;
    }

    public boolean isEmotionallyStable() {
        return emotionalStabilityScore > 0.7 && impulsivityScore < 0.4;
    }

    public boolean requiresIntensiveCoaching() {
        return impulsivityScore > 0.8 || 
               (overconfidenceScore > 0.8 && emotionalStabilityScore < 0.4) ||
               (lossAversionScore > 0.8 && riskToleranceScore < 0.3);
    }

    public Double getOverallRiskScore() {
        return (impulsivityScore * 0.3 + 
                overconfidenceScore * 0.25 + 
                (1.0 - emotionalStabilityScore) * 0.25 +
                lossAversionScore * 0.2);
    }

    // Private calculation methods
    private BehavioralPatternData.PatternType calculateDominantPattern() {
        return switch (getHighestScoreCategory()) {
            case "impulsivity" -> BehavioralPatternData.PatternType.IMPULSIVE_TRADING;
            case "overconfidence" -> BehavioralPatternData.PatternType.OVERCONFIDENCE_BIAS;
            case "lossAversion" -> BehavioralPatternData.PatternType.LOSS_AVERSION;
            default -> null;
        };
    }

    private TraderType calculateTraderType() {
        Double riskScore = getOverallRiskScore();
        return switch (riskScore) {
            case Double r when r > 0.7 -> TraderType.AGGRESSIVE;
            case Double r when r > 0.4 -> TraderType.MODERATE;
            case Double r when r > 0.2 -> TraderType.CONSERVATIVE;
            default -> TraderType.BALANCED;
        };
    }

    private Double calculateConfidenceLevel() {
        // Confidence based on data consistency and stability
        return emotionalStabilityScore * 0.6 + (1.0 - impulsivityScore) * 0.4;
    }

    private String getHighestScoreCategory() {
        Double maxScore = Math.max(impulsivityScore, 
                          Math.max(overconfidenceScore, lossAversionScore));
        
        return switch (maxScore) {
            case Double s when s.equals(impulsivityScore) -> "impulsivity";
            case Double s when s.equals(overconfidenceScore) -> "overconfidence";
            case Double s when s.equals(lossAversionScore) -> "lossAversion";
            default -> "balanced";
        };
    }

    // Validation methods
    private static String validateNonNull(String value, String fieldName) {
        return Optional.ofNullable(value)
            .filter(v -> !v.isBlank())
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " cannot be null or blank"));
    }

    private static Double validateScore(Double score, String fieldName) {
        return Optional.ofNullable(score)
            .filter(s -> s >= 0.0 && s <= 1.0)
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0"));
    }

    /**
     * Trader type classification
     */
    public enum TraderType {
        CONSERVATIVE("Low risk, systematic approach"),
        MODERATE("Balanced risk-reward approach"),
        AGGRESSIVE("High risk, high reward seeking"),
        BALANCED("Well-rounded trading style");

        private final String description;

        TraderType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }
}