package com.trademaster.behavioralai.service;

import com.trademaster.behavioralai.domain.entity.CoachingInterventionEntity;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.CoachingIntervention;
import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import com.trademaster.behavioralai.repository.CoachingInterventionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Coaching Intervention Service
 * 
 * Advanced AI service for automated coaching interventions and behavioral modification.
 * Uses functional programming principles and virtual threads for real-time intervention delivery.
 * 
 * Features:
 * - Real-time intervention triggering based on behavioral patterns
 * - Personalized coaching message generation
 * - Intervention effectiveness tracking and optimization
 * - Rate limiting and user preference management
 */
@Service
@RequiredArgsConstructor

public final class CoachingInterventionService {
    private static final Logger log = LoggerFactory.getLogger(CoachingInterventionService.class);

    @Value("${behavioral-ai.coaching.max-interventions-per-hour:5}")
    private final Integer MAX_INTERVENTIONS_PER_HOUR;

    @Value("${behavioral-ai.coaching.effectiveness-threshold:0.6}")
    private final Double EFFECTIVENESS_THRESHOLD;

    @Value("${behavioral-ai.coaching.cool-down-minutes:15}")
    private final Integer COOL_DOWN_MINUTES;

    private final CoachingInterventionRepository interventionRepository;
    private final BehavioralPatternService behavioralPatternService;
    
    // User rate limiting cache
    private final Map<String, UserInterventionState> userStateCache = new ConcurrentHashMap<>();
    
    // Virtual thread executor for async intervention processing
    private static final java.util.concurrent.Executor VIRTUAL_EXECUTOR = 
        Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Evaluate and trigger coaching intervention based on behavioral pattern
     * 
     * @param userId User identifier
     * @param patternData Detected behavioral pattern
     * @return CompletableFuture with intervention result or error
     */
    public CompletableFuture<Result<CoachingIntervention, BehavioralAIError>> evaluateAndTriggerIntervention(
            String userId, BehavioralPatternData patternData) {
        
        return CompletableFuture
            .supplyAsync(() -> performInterventionEvaluation(userId, patternData), VIRTUAL_EXECUTOR)
            .exceptionally(this::handleInterventionException);
    }

    /**
     * Generate personalized coaching intervention
     * 
     * @param userId User identifier
     * @param triggerPattern Pattern that triggered the intervention
     * @param contextData Additional context for personalization
     * @return CompletableFuture with generated intervention
     */
    public CompletableFuture<Result<CoachingIntervention, BehavioralAIError>> generatePersonalizedIntervention(
            String userId, BehavioralPatternData.PatternType triggerPattern, 
            Map<String, Object> contextData) {
        
        return CompletableFuture
            .supplyAsync(() -> performInterventionGeneration(userId, triggerPattern, contextData), VIRTUAL_EXECUTOR);
    }

    /**
     * Process user response to coaching intervention
     * 
     * @param interventionId Intervention identifier
     * @param userResponse User response data
     * @return Result with updated intervention or error
     */
    @Transactional
    public Result<CoachingIntervention, BehavioralAIError> processUserResponse(
            String interventionId, CoachingIntervention.UserResponse userResponse) {
        
        return findInterventionById(interventionId)
            .flatMap(entity -> updateWithUserResponse(entity, userResponse))
            .flatMap(this::persistInterventionUpdate)
            .map(CoachingInterventionEntity::toDto);
    }

    /**
     * Get intervention effectiveness analytics
     * 
     * @param userId User identifier
     * @param timeRange Time range for analysis
     * @return CompletableFuture with effectiveness analytics
     */
    public CompletableFuture<Result<InterventionEffectivenessAnalytics, BehavioralAIError>> getEffectivenessAnalytics(
            String userId, EmotionDetectionService.TimeRange timeRange) {
        
        return CompletableFuture
            .supplyAsync(() -> performEffectivenessAnalysis(userId, timeRange), VIRTUAL_EXECUTOR);
    }

    /**
     * Get user's coaching preferences based on historical responses
     * 
     * @param userId User identifier
     * @return User coaching preferences
     */
    @Transactional(readOnly = true)
    public Result<UserCoachingPreferences, BehavioralAIError> getUserCoachingPreferences(String userId) {
        return Result.tryExecute(
            () -> {
                List<Object[]> preferences = interventionRepository.getUserInterventionPreferences(userId);
                return buildUserPreferences(userId, preferences);
            },
            ex -> BehavioralAIError.DataError.retrievalFailed("user_preferences", ex.getMessage()));
    }

    /**
     * Get urgent interventions requiring immediate attention
     * 
     * @return List of urgent interventions
     */
    @Transactional(readOnly = true)
    public List<CoachingIntervention> getUrgentInterventions() {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        return interventionRepository.findUrgentInterventionsWithoutResponse(since)
            .stream()
            .map(CoachingInterventionEntity::toDto)
            .toList();
    }

    // Private implementation methods

    private Result<CoachingIntervention, BehavioralAIError> performInterventionEvaluation(
            String userId, BehavioralPatternData patternData) {
        
        return checkInterventionEligibility(userId, patternData)
            .flatMap(eligiblePattern -> determineInterventionStrategy(userId, eligiblePattern))
            .flatMap(strategy -> generateAndTriggerIntervention(userId, strategy))
            .flatMap(this::saveIntervention);
    }

    private Result<CoachingIntervention, BehavioralAIError> performInterventionGeneration(
            String userId, BehavioralPatternData.PatternType triggerPattern, Map<String, Object> contextData) {
        
        return getUserCoachingPreferences(userId)
            .flatMap(preferences -> generateContextualMessage(userId, triggerPattern, preferences, contextData))
            .map(message -> buildInterventionFromMessage(userId, triggerPattern, message));
    }

    private Result<InterventionEffectivenessAnalytics, BehavioralAIError> performEffectivenessAnalysis(
            String userId, EmotionDetectionService.TimeRange timeRange) {
        
        return Result.tryExecute(
            () -> {
                Object[] metrics = interventionRepository.calculateInterventionMetrics(timeRange.start());
                List<Object[]> typeEffectiveness = interventionRepository.calculateAverageEffectivenessByType(timeRange.start());
                
                return buildEffectivenessAnalytics(userId, metrics, typeEffectiveness, timeRange);
            },
            ex -> BehavioralAIError.AnalysisError.patternDetectionFailed("effectiveness_analysis", ex.getMessage()));
    }

    private Result<BehavioralPatternData, BehavioralAIError> checkInterventionEligibility(
            String userId, BehavioralPatternData patternData) {
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            return Result.failure(BehavioralAIError.InterventionError.rateLimited(userId, MAX_INTERVENTIONS_PER_HOUR));
        }
        
        // Check cool-down period
        if (isInCoolDown(userId, patternData.patternType())) {
            return Result.failure(BehavioralAIError.InterventionError.rateLimited(userId, COOL_DOWN_MINUTES));
        }
        
        // Check if pattern requires intervention
        if (!patternData.requiresIntervention()) {
            return Result.failure(BehavioralAIError.InterventionError.interventionFailed(
                "pattern_threshold", "Pattern does not meet intervention threshold"));
        }
        
        return Result.success(patternData);
    }

    private Result<InterventionStrategy, BehavioralAIError> determineInterventionStrategy(
            String userId, BehavioralPatternData patternData) {
        
        return getUserCoachingPreferences(userId)
            .map(preferences -> calculateOptimalStrategy(patternData, preferences));
    }

    private Result<CoachingIntervention, BehavioralAIError> generateAndTriggerIntervention(
            String userId, InterventionStrategy strategy) {
        
        return Result.tryExecute(
            () -> {
                String interventionId = UUID.randomUUID().toString();
                String personalizedMessage = generatePersonalizedMessage(strategy);
                List<String> recommendations = generateRecommendations(strategy);
                Double expectedEffectiveness = calculateExpectedEffectiveness(strategy);
                
                return new CoachingIntervention(
                    interventionId,
                    userId,
                    strategy.interventionType(),
                    strategy.triggerPattern(),
                    personalizedMessage,
                    Instant.now(),
                    strategy.sessionId(),
                    strategy.priority(),
                    recommendations,
                    List.of(), // Educational resources would be populated separately
                    expectedEffectiveness,
                    null, // User response initially null
                    null, // Actual effectiveness calculated after response
                    Map.of("strategy", strategy.name(), "context", strategy.contextData())
                );
            },
            ex -> BehavioralAIError.InterventionError.interventionFailed("generation", ex.getMessage()));
    }

    private Result<CoachingIntervention, BehavioralAIError> saveIntervention(CoachingIntervention intervention) {
        return Result.tryExecute(
            () -> {
                CoachingInterventionEntity entity = CoachingInterventionEntity.fromDto(intervention);
                CoachingInterventionEntity saved = interventionRepository.save(entity);
                updateUserInterventionState(intervention.userId());
                return saved.toDto();
            },
            ex -> BehavioralAIError.DataError.storageFailed("save_intervention", ex.getMessage()));
    }

    private Result<CoachingInterventionEntity, BehavioralAIError> findInterventionById(String interventionId) {
        return interventionRepository.findByInterventionId(interventionId)
            .map(intervention -> Result.<CoachingInterventionEntity, BehavioralAIError>success(intervention))
            .orElse(Result.failure(BehavioralAIError.DataError.dataNotFound("intervention", interventionId)));
    }

    private Result<CoachingInterventionEntity, BehavioralAIError> updateWithUserResponse(
            CoachingInterventionEntity entity, CoachingIntervention.UserResponse userResponse) {
        
        return Result.tryExecute(
            () -> entity.withResponse(
                userResponse.responseType(),
                userResponse.feedback(),
                userResponse.rating(),
                userResponse.actionTaken()
            ),
            ex -> BehavioralAIError.InterventionError.interventionFailed("response_update", ex.getMessage()));
    }

    private Result<CoachingInterventionEntity, BehavioralAIError> persistInterventionUpdate(
            CoachingInterventionEntity updatedEntity) {
        
        return Result.tryExecute(
            () -> interventionRepository.save(updatedEntity),
            ex -> BehavioralAIError.DataError.storageFailed("update_intervention", ex.getMessage()));
    }

    // Helper methods

    private boolean isRateLimited(String userId) {
        UserInterventionState state = userStateCache.get(userId);
        if (state == null) return false;
        
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentInterventions = state.recentInterventions().stream()
            .filter(timestamp -> timestamp.isAfter(oneHourAgo))
            .count();
        
        return recentInterventions >= MAX_INTERVENTIONS_PER_HOUR;
    }

    private boolean isInCoolDown(String userId, BehavioralPatternData.PatternType patternType) {
        Instant coolDownThreshold = Instant.now().minus(COOL_DOWN_MINUTES, ChronoUnit.MINUTES);
        return interventionRepository.existsByUserIdAndTriggerPatternAndTriggeredAtAfter(
            userId, patternType, coolDownThreshold);
    }

    private InterventionStrategy calculateOptimalStrategy(BehavioralPatternData pattern, 
                                                        UserCoachingPreferences preferences) {
        
        CoachingIntervention.InterventionType type = selectInterventionType(pattern, preferences);
        CoachingIntervention.Priority priority = calculatePriority(pattern);
        
        return new InterventionStrategy(
            "personalized_" + pattern.patternType().name().toLowerCase(),
            type,
            pattern.patternType(),
            priority,
            pattern.sessionId(),
            Map.of(
                "pattern_confidence", pattern.confidence(),
                "user_preferences", preferences,
                "risk_level", pattern.getSeverity().name()
            )
        );
    }

    private CoachingIntervention.InterventionType selectInterventionType(
            BehavioralPatternData pattern, UserCoachingPreferences preferences) {
        
        return switch (pattern.getSeverity()) {
            case CRITICAL -> CoachingIntervention.InterventionType.REAL_TIME_ALERT;
            case HIGH -> preferences.prefersAlerts() ? 
                CoachingIntervention.InterventionType.PRE_TRADE_WARNING :
                CoachingIntervention.InterventionType.REAL_TIME_ALERT;
            case MEDIUM -> CoachingIntervention.InterventionType.POST_TRADE_ANALYSIS;
            case LOW -> CoachingIntervention.InterventionType.DAILY_INSIGHT;
        };
    }

    private CoachingIntervention.Priority calculatePriority(BehavioralPatternData pattern) {
        return switch (pattern.getSeverity()) {
            case CRITICAL -> CoachingIntervention.Priority.HIGH;
            case HIGH -> CoachingIntervention.Priority.HIGH;
            case MEDIUM -> CoachingIntervention.Priority.MEDIUM;
            case LOW -> CoachingIntervention.Priority.LOW;
        };
    }

    private String generatePersonalizedMessage(InterventionStrategy strategy) {
        return switch (strategy.triggerPattern()) {
            case IMPULSIVE_TRADING -> generateImpulsiveTradeMessage(strategy);
            case REVENGE_TRADING -> generateRevengeTradeMessage(strategy);
            case PANIC_SELLING -> generatePanicSellingMessage(strategy);
            case OVERCONFIDENCE_BIAS -> generateOverconfidenceMessage(strategy);
            case FEAR_OF_MISSING_OUT -> generateFOMOMessage(strategy);
            default -> generateGenericMessage(strategy);
        };
    }

    private List<String> generateRecommendations(InterventionStrategy strategy) {
        return switch (strategy.triggerPattern()) {
            case IMPULSIVE_TRADING -> List.of(
                "Take a 5-minute break before your next trade",
                "Review your trading plan and stick to it",
                "Consider setting automatic stop-losses"
            );
            case REVENGE_TRADING -> List.of(
                "Step away from trading for 30 minutes",
                "Review what triggered this emotional response",
                "Focus on your long-term trading goals"
            );
            case PANIC_SELLING -> List.of(
                "Review your risk management rules",
                "Consider your investment timeline",
                "Consult your trading mentor or advisor"
            );
            default -> List.of(
                "Take a moment to reflect on your current emotional state",
                "Review your recent trading decisions",
                "Consider practicing mindfulness techniques"
            );
        };
    }

    private Double calculateExpectedEffectiveness(InterventionStrategy strategy) {
        // Mock calculation based on strategy effectiveness
        return switch (strategy.interventionType()) {
            case REAL_TIME_ALERT -> 0.85;
            case PRE_TRADE_WARNING -> 0.78;
            case POST_TRADE_ANALYSIS -> 0.65;
            case DAILY_INSIGHT -> 0.55;
            default -> 0.60;
        };
    }

    // Message generation methods

    private String generateImpulsiveTradeMessage(InterventionStrategy strategy) {
        return "I've noticed some rapid trading activity. Taking a moment to breathe and reflect " +
               "on your trading strategy could help improve your outcomes. Consider reviewing your " +
               "planned trades before executing them.";
    }

    private String generateRevengeTradeMessage(InterventionStrategy strategy) {
        return "It looks like you might be trying to recover from a recent loss. Revenge trading " +
               "often leads to larger losses. Consider taking a short break to reset your mindset " +
               "and return to your systematic trading approach.";
    }

    private String generatePanicSellingMessage(InterventionStrategy strategy) {
        return "I see you're making quick exit decisions during market stress. Remember your " +
               "investment goals and time horizon. Panic selling often results in locking in losses " +
               "at the worst possible time.";
    }

    private String generateOverconfidenceMessage(InterventionStrategy strategy) {
        return "Your recent wins are great! However, I've noticed increasing position sizes and " +
               "risk-taking. Remember that markets can be unpredictable, and maintaining proper " +
               "risk management is crucial for long-term success.";
    }

    private String generateFOMOMessage(InterventionStrategy strategy) {
        return "I notice you're chasing market movements without your usual analysis. FOMO can " +
               "lead to poor entry points and increased risk. Consider waiting for your planned " +
               "setups rather than chasing momentum.";
    }

    private String generateGenericMessage(InterventionStrategy strategy) {
        return "I've detected some patterns in your trading behavior that might benefit from " +
               "reflection. Consider reviewing your recent decisions and ensuring they align " +
               "with your trading plan and risk management rules.";
    }

    private void updateUserInterventionState(String userId) {
        userStateCache.compute(userId, (key, state) -> {
            List<Instant> updatedInterventions = state != null ? 
                List.copyOf(state.recentInterventions()) : List.of();
            updatedInterventions = Stream.concat(
                updatedInterventions.stream(),
                Stream.of(Instant.now())
            ).toList();
            
            return new UserInterventionState(userId, updatedInterventions, Instant.now());
        });
    }

    private UserCoachingPreferences buildUserPreferences(String userId, List<Object[]> preferences) {
        // Build preferences from database query results
        Map<CoachingIntervention.InterventionType, Double> typePreferences = preferences.stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> CoachingIntervention.InterventionType.valueOf((String) row[0]),
                row -> (Double) row[1]
            ));
        
        return new UserCoachingPreferences(
            userId,
            typePreferences,
            true, // Default to alerts enabled
            EFFECTIVENESS_THRESHOLD,
            Instant.now()
        );
    }

    private InterventionEffectivenessAnalytics buildEffectivenessAnalytics(
            String userId, Object[] metrics, List<Object[]> typeEffectiveness, 
            EmotionDetectionService.TimeRange timeRange) {
        
        // Extract metrics from query results
        Long totalInterventions = (Long) metrics[0];
        Long acceptedInterventions = (Long) metrics[1];
        Long actionTaken = (Long) metrics[2];
        Double avgEffectiveness = (Double) metrics[3];
        Double avgResponseTime = (Double) metrics[4];
        
        Map<CoachingIntervention.InterventionType, Double> effectivenessByType = typeEffectiveness.stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> CoachingIntervention.InterventionType.valueOf((String) row[0]),
                row -> (Double) row[1]
            ));
        
        return new InterventionEffectivenessAnalytics(
            userId,
            totalInterventions,
            acceptedInterventions.doubleValue() / totalInterventions,
            actionTaken.doubleValue() / totalInterventions,
            avgEffectiveness,
            avgResponseTime,
            effectivenessByType,
            timeRange,
            Instant.now()
        );
    }

    private Result<String, BehavioralAIError> generateContextualMessage(
            String userId, BehavioralPatternData.PatternType triggerPattern, 
            UserCoachingPreferences preferences, Map<String, Object> contextData) {
        
        return Result.success(generatePersonalizedMessage(
            new InterventionStrategy(
                "contextual",
                CoachingIntervention.InterventionType.REAL_TIME_ALERT,
                triggerPattern,
                CoachingIntervention.Priority.MEDIUM,
                null,
                contextData
            )
        ));
    }

    private CoachingIntervention buildInterventionFromMessage(
            String userId, BehavioralPatternData.PatternType triggerPattern, String message) {
        
        return new CoachingIntervention(
            UUID.randomUUID().toString(),
            userId,
            CoachingIntervention.InterventionType.REAL_TIME_ALERT,
            triggerPattern,
            message,
            Instant.now(),
            null,
            CoachingIntervention.Priority.MEDIUM,
            List.of(),
            List.of(),
            0.75,
            null,
            null,
            Map.of()
        );
    }

    private Result<CoachingIntervention, BehavioralAIError> handleInterventionException(Throwable ex) {
        log.error("Intervention processing failed: {}", ex.getMessage(), ex);
        return Result.failure(BehavioralAIError.InterventionError.interventionFailed(
            "processing", ex.getMessage()));
    }

    // Supporting record types

    public record InterventionStrategy(
        String name,
        CoachingIntervention.InterventionType interventionType,
        BehavioralPatternData.PatternType triggerPattern,
        CoachingIntervention.Priority priority,
        String sessionId,
        Map<String, Object> contextData
    ) {}

    public record UserInterventionState(
        String userId,
        List<Instant> recentInterventions,
        Instant lastUpdated
    ) {}

    public record UserCoachingPreferences(
        String userId,
        Map<CoachingIntervention.InterventionType, Double> typePreferences,
        Boolean alertsEnabled,
        Double effectivenessThreshold,
        Instant lastUpdated
    ) {
        public boolean prefersAlerts() {
            return alertsEnabled && 
                   typePreferences.getOrDefault(CoachingIntervention.InterventionType.REAL_TIME_ALERT, 0.0) > 0.5;
        }
    }

    public record InterventionEffectivenessAnalytics(
        String userId,
        Long totalInterventions,
        Double acceptanceRate,
        Double actionTakenRate,
        Double avgEffectiveness,
        Double avgResponseTimeSeconds,
        Map<CoachingIntervention.InterventionType, Double> effectivenessByType,
        EmotionDetectionService.TimeRange analyzedPeriod,
        Instant analyzedAt
    ) {}
}