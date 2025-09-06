package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Coaching Intervention Record
 * 
 * Immutable data structure representing AI-generated coaching interventions
 * for behavioral pattern modification and trading improvement.
 */
@Schema(description = "AI-generated coaching intervention with personalized recommendations")
public record CoachingIntervention(
    @Schema(description = "Unique intervention identifier", example = "intervention-123")
    @JsonProperty("interventionId")
    String interventionId,
    
    @Schema(description = "User identifier", example = "user-456")
    @JsonProperty("userId")
    String userId,
    
    @Schema(description = "Type of coaching intervention")
    @JsonProperty("interventionType")
    InterventionType interventionType,
    
    @Schema(description = "Pattern that triggered the intervention")
    @JsonProperty("triggerPattern")
    BehavioralPatternData.PatternType triggerPattern,
    
    @Schema(description = "Primary coaching message")
    @JsonProperty("message")
    String message,
    
    @Schema(description = "Intervention trigger timestamp")
    @JsonProperty("triggeredAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant triggeredAt,
    
    @Schema(description = "Trading session identifier")
    @JsonProperty("sessionId")
    String sessionId,
    
    @Schema(description = "Intervention priority level")
    @JsonProperty("priority")
    Priority priority,
    
    @Schema(description = "Personalized recommendations")
    @JsonProperty("recommendations")
    List<String> recommendations,
    
    @Schema(description = "Educational resources")
    @JsonProperty("educationalResources")
    List<EducationalResource> educationalResources,
    
    @Schema(description = "Expected effectiveness score (0.0-1.0)")
    @JsonProperty("expectedEffectiveness")
    Double expectedEffectiveness,
    
    @Schema(description = "User response to intervention")
    @JsonProperty("userResponse")
    UserResponse userResponse,
    
    @Schema(description = "Actual effectiveness after user feedback")
    @JsonProperty("actualEffectiveness")
    Double actualEffectiveness,
    
    @Schema(description = "Additional intervention context")
    @JsonProperty("context")
    Map<String, Object> context
) {
    /**
     * Compact constructor with validation
     */
    public CoachingIntervention {
        validateNonNull(interventionId, "interventionId");
        validateNonNull(userId, "userId");
        validateNonNull(interventionType, "interventionType");
        validateNonNull(message, "message");
        validateNonNull(triggeredAt, "triggeredAt");
        validateEffectiveness(expectedEffectiveness);
        
        // Immutable collections and defaults
        recommendations = recommendations != null ? List.copyOf(recommendations) : List.of();
        educationalResources = educationalResources != null ? 
            List.copyOf(educationalResources) : List.of();
        priority = priority != null ? priority : Priority.MEDIUM;
        context = context != null ? Map.copyOf(context) : Map.of();
    }
    
    /**
     * Factory method for creating urgent intervention
     */
    public static CoachingIntervention urgent(String userId, 
                                            BehavioralPatternData.PatternType pattern,
                                            String message, String sessionId) {
        return new CoachingIntervention(
            generateInterventionId(), userId, InterventionType.REAL_TIME_ALERT,
            pattern, message, Instant.now(), sessionId, Priority.HIGH,
            List.of(), List.of(), 0.8, null, null, Map.of()
        );
    }
    
    /**
     * Factory method for educational intervention
     */
    public static CoachingIntervention educational(String userId, String message,
                                                 List<EducationalResource> resources) {
        return new CoachingIntervention(
            generateInterventionId(), userId, InterventionType.EDUCATIONAL,
            null, message, Instant.now(), null, Priority.LOW,
            List.of(), resources, 0.6, null, null, Map.of()
        );
    }
    
    /**
     * Check if intervention requires immediate user attention
     */
    public boolean isUrgent() {
        return priority == Priority.HIGH && 
               (interventionType == InterventionType.REAL_TIME_ALERT ||
                interventionType == InterventionType.PRE_TRADE_WARNING);
    }
    
    /**
     * Calculate intervention success rate based on response
     */
    public Double getSuccessRate() {
        return userResponse != null ? 
            userResponse.calculateEffectiveness(expectedEffectiveness) :
            expectedEffectiveness;
    }
    
    /**
     * Get time since intervention was triggered
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - triggeredAt.getEpochSecond();
    }
    
    private static void validateNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    private static void validateEffectiveness(Double effectiveness) {
        if (effectiveness != null && (effectiveness < 0.0 || effectiveness > 1.0)) {
            throw new IllegalArgumentException("Effectiveness must be between 0.0 and 1.0");
        }
    }
    
    private static String generateInterventionId() {
        return "intervention-" + System.currentTimeMillis() + "-" +
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * Types of coaching interventions
     */
    public enum InterventionType {
        PRE_TRADE_WARNING("Warning before trade execution"),
        REAL_TIME_ALERT("Immediate notification during trading"),
        POST_TRADE_ANALYSIS("Reflection after trade completion"),
        DAILY_INSIGHT("Daily behavioral summary"),
        WEEKLY_REPORT("Comprehensive weekly analysis"),
        EDUCATIONAL("Educational content delivery"),
        MINDFULNESS("Emotional regulation guidance");
        
        private final String description;
        
        InterventionType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Intervention priority levels
     */
    public enum Priority {
        LOW(1, "Informational"),
        MEDIUM(2, "Recommended action"),
        HIGH(3, "Urgent attention required");
        
        private final int level;
        private final String description;
        
        Priority(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
    
    /**
     * Educational resource record
     */
    public record EducationalResource(
        String title,
        String description,
        String url,
        ResourceType type,
        Integer estimatedMinutes
    ) {
        public EducationalResource {
            validateNonNull(title, "resource title");
            validateNonNull(type, "resource type");
        }
        
        public enum ResourceType {
            ARTICLE, VIDEO, INFOGRAPHIC, QUIZ, SIMULATION
        }
    }
    
    /**
     * User response to intervention
     */
    public record UserResponse(
        ResponseType responseType,
        String feedback,
        Integer rating,
        Instant respondedAt,
        Boolean actionTaken
    ) {
        public UserResponse {
            validateNonNull(responseType, "response type");
            validateNonNull(respondedAt, "response timestamp");
        }
        
        public Double calculateEffectiveness(Double expected) {
            return switch (responseType) {
                case ACCEPTED -> expected * 1.0;
                case DISMISSED -> expected * 0.3;
                case IGNORED -> expected * 0.1;
                case FEEDBACK_PROVIDED -> expected * (rating != null ? rating / 5.0 : 0.8);
            };
        }
        
        public enum ResponseType {
            ACCEPTED, DISMISSED, IGNORED, FEEDBACK_PROVIDED
        }
    }
}