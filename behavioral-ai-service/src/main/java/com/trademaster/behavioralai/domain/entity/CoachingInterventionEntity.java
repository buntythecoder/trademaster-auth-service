package com.trademaster.behavioralai.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.CoachingIntervention;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Coaching Intervention JPA Entity
 * 
 * Immutable entity for persisting coaching interventions following TradeMaster standards.
 * Tracks AI-generated interventions and user responses.
 */
@Entity
@Table(name = "coaching_interventions", indexes = {
    @Index(name = "idx_coaching_intervention_user_id", columnList = "user_id"),
    @Index(name = "idx_coaching_intervention_triggered", columnList = "triggered_at"),
    @Index(name = "idx_coaching_intervention_type", columnList = "intervention_type"),
    @Index(name = "idx_coaching_intervention_session", columnList = "trading_session_id")
})
@RequiredArgsConstructor

public final class CoachingInterventionEntity {
    private static final Logger log = LoggerFactory.getLogger(CoachingInterventionEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "intervention_id", nullable = false, unique = true, length = 255)
    private final String interventionId;

    @Column(name = "user_id", nullable = false, length = 255)
    private final String userId;

    @Column(name = "intervention_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private final CoachingIntervention.InterventionType interventionType;

    @Column(name = "trigger_pattern", length = 50)
    @Enumerated(EnumType.STRING)
    private final BehavioralPatternData.PatternType triggerPattern;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private final String message;

    @Column(name = "triggered_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant triggeredAt;

    @Column(name = "trading_session_id", length = 255)
    private final String tradingSessionId;

    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private final CoachingIntervention.Priority priority;

    @Column(name = "expected_effectiveness", precision = 3, scale = 2)
    private final Double expectedEffectiveness;

    @Column(name = "user_response", length = 50)
    @Enumerated(EnumType.STRING)
    private final CoachingIntervention.UserResponse.ResponseType userResponse;

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private final String userFeedback;

    @Column(name = "user_rating")
    private final Integer userRating;

    @Column(name = "responded_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant respondedAt;

    @Column(name = "actual_effectiveness", precision = 3, scale = 2)
    private final Double actualEffectiveness;

    @Column(name = "action_taken", nullable = false)
    private final Boolean actionTaken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant createdAt;

    /**
     * Protected constructor for JPA
     */
    protected CoachingInterventionEntity() {
        this.id = null;
        this.interventionId = null;
        this.userId = null;
        this.interventionType = null;
        this.triggerPattern = null;
        this.message = null;
        this.triggeredAt = null;
        this.tradingSessionId = null;
        this.priority = CoachingIntervention.Priority.MEDIUM;
        this.expectedEffectiveness = null;
        this.userResponse = null;
        this.userFeedback = null;
        this.userRating = null;
        this.respondedAt = null;
        this.actualEffectiveness = null;
        this.actionTaken = false;
        this.createdAt = Instant.now();
    }

    /**
     * Factory constructor for creating intervention
     */
    public CoachingInterventionEntity(String interventionId,
                                    String userId,
                                    CoachingIntervention.InterventionType interventionType,
                                    BehavioralPatternData.PatternType triggerPattern,
                                    String message,
                                    String tradingSessionId,
                                    CoachingIntervention.Priority priority,
                                    Double expectedEffectiveness) {
        this.id = null;
        this.interventionId = validateNonNull(interventionId, "interventionId");
        this.userId = validateNonNull(userId, "userId");
        this.interventionType = validateNonNull(interventionType, "interventionType");
        this.triggerPattern = triggerPattern;
        this.message = validateNonNull(message, "message");
        this.triggeredAt = Instant.now();
        this.tradingSessionId = tradingSessionId;
        this.priority = priority != null ? priority : CoachingIntervention.Priority.MEDIUM;
        this.expectedEffectiveness = validateEffectiveness(expectedEffectiveness);
        this.userResponse = null;
        this.userFeedback = null;
        this.userRating = null;
        this.respondedAt = null;
        this.actualEffectiveness = null;
        this.actionTaken = false;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method for creating intervention from DTO
     */
    public static CoachingInterventionEntity fromDto(CoachingIntervention dto) {
        return new CoachingInterventionEntity(
            dto.interventionId(),
            dto.userId(),
            dto.interventionType(),
            dto.triggerPattern(),
            dto.message(),
            dto.sessionId(),
            dto.priority(),
            dto.expectedEffectiveness()
        );
    }

    /**
     * Create entity with user response
     */
    public CoachingInterventionEntity withResponse(CoachingIntervention.UserResponse.ResponseType response,
                                                 String feedback,
                                                 Integer rating,
                                                 Boolean actionTaken) {
        return new CoachingInterventionEntity(
            this.interventionId, this.userId, this.interventionType, this.triggerPattern,
            this.message, this.tradingSessionId, this.priority, this.expectedEffectiveness,
            response, feedback, rating, Instant.now(), 
            calculateActualEffectiveness(response, rating), actionTaken
        );
    }

    /**
     * Private constructor for response updates
     */
    private CoachingInterventionEntity(String interventionId, String userId,
                                     CoachingIntervention.InterventionType interventionType,
                                     BehavioralPatternData.PatternType triggerPattern,
                                     String message, String tradingSessionId,
                                     CoachingIntervention.Priority priority,
                                     Double expectedEffectiveness,
                                     CoachingIntervention.UserResponse.ResponseType userResponse,
                                     String userFeedback, Integer userRating,
                                     Instant respondedAt, Double actualEffectiveness,
                                     Boolean actionTaken) {
        this.id = null;
        this.interventionId = interventionId;
        this.userId = userId;
        this.interventionType = interventionType;
        this.triggerPattern = triggerPattern;
        this.message = message;
        this.triggeredAt = Instant.now();
        this.tradingSessionId = tradingSessionId;
        this.priority = priority;
        this.expectedEffectiveness = expectedEffectiveness;
        this.userResponse = userResponse;
        this.userFeedback = userFeedback;
        this.userRating = userRating;
        this.respondedAt = respondedAt;
        this.actualEffectiveness = actualEffectiveness;
        this.actionTaken = actionTaken != null ? actionTaken : false;
        this.createdAt = Instant.now();
    }

    /**
     * Convert to DTO
     */
    public CoachingIntervention toDto() {
        CoachingIntervention.UserResponse userResponseDto = userResponse != null ?
            new CoachingIntervention.UserResponse(userResponse, userFeedback, 
                                                userRating, respondedAt, actionTaken) : null;

        return new CoachingIntervention(
            getInterventionId(),
            getUserId(),
            getInterventionType(),
            getTriggerPattern(),
            getMessage(),
            getTriggeredAt(),
            getTradingSessionId(),
            getPriority(),
            null, // recommendations populated by service
            null, // educational resources populated by service
            getExpectedEffectiveness(),
            userResponseDto,
            getActualEffectiveness(),
            null  // context populated by service
        );
    }

    // Immutable getters
    public Long getId() { return id; }
    public String getInterventionId() { return interventionId; }
    public String getUserId() { return userId; }
    public CoachingIntervention.InterventionType getInterventionType() { return interventionType; }
    public BehavioralPatternData.PatternType getTriggerPattern() { return triggerPattern; }
    public String getMessage() { return message; }
    public Instant getTriggeredAt() { return triggeredAt; }
    public String getTradingSessionId() { return tradingSessionId; }
    public CoachingIntervention.Priority getPriority() { return priority; }
    public Double getExpectedEffectiveness() { return expectedEffectiveness; }
    public CoachingIntervention.UserResponse.ResponseType getUserResponse() { return userResponse; }
    public String getUserFeedback() { return userFeedback; }
    public Integer getUserRating() { return userRating; }
    public Instant getRespondedAt() { return respondedAt; }
    public Double getActualEffectiveness() { return actualEffectiveness; }
    public Boolean getActionTaken() { return actionTaken; }
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Business logic methods
     */
    public boolean isUrgent() {
        return priority == CoachingIntervention.Priority.HIGH;
    }

    public boolean hasUserResponse() {
        return userResponse != null;
    }

    public boolean isEffective() {
        return actualEffectiveness != null && actualEffectiveness > 0.6;
    }

    public Long getResponseTimeSeconds() {
        return respondedAt != null ? 
            respondedAt.getEpochSecond() - triggeredAt.getEpochSecond() : null;
    }

    // Private methods
    private static String validateNonNull(String value, String fieldName) {
        return Optional.ofNullable(value)
            .filter(v -> !v.isBlank())
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " cannot be null or blank"));
    }

    private static <T> T validateNonNull(T value, String fieldName) {
        return Optional.ofNullable(value)
            .orElseThrow(() -> new IllegalArgumentException(fieldName + " cannot be null"));
    }

    private static Double validateEffectiveness(Double effectiveness) {
        return Optional.ofNullable(effectiveness)
            .filter(e -> e >= 0.0 && e <= 1.0)
            .orElse(null);
    }

    private Double calculateActualEffectiveness(CoachingIntervention.UserResponse.ResponseType response, 
                                              Integer rating) {
        return Optional.ofNullable(response)
            .map(r -> switch (r) {
                case ACCEPTED -> expectedEffectiveness != null ? expectedEffectiveness : 0.8;
                case DISMISSED -> 0.3;
                case IGNORED -> 0.1;
                case FEEDBACK_PROVIDED -> rating != null ? rating / 5.0 : 0.7;
            })
            .orElse(null);
    }
}