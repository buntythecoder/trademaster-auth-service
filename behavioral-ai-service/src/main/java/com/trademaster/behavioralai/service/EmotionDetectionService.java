package com.trademaster.behavioralai.service;

import com.trademaster.behavioralai.dto.EmotionAnalysisResult;
import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Emotion Detection Service
 * 
 * Advanced AI service for real-time trading emotion analysis following TradeMaster standards.
 * Uses functional programming principles and virtual threads for high-performance analysis.
 * 
 * Features:
 * - Real-time emotion detection from trading behavior patterns
 * - Multi-modal analysis using ensemble ML models
 * - Confidence scoring with uncertainty quantification
 * - Virtual thread-based async processing for 10K+ concurrent users
 */
@Service
public final class EmotionDetectionService {
    
    private static final Logger log = LoggerFactory.getLogger(EmotionDetectionService.class);

    private final Double CONFIDENCE_THRESHOLD;
    private final Integer ANALYSIS_TIMEOUT_MS;
    private final Integer MIN_FEATURE_COUNT;
    private final MLModelService mlModelService;
    private final FeatureExtractionService featureExtractionService;
    
    // Constructor for Spring injection with @Value injection
    public EmotionDetectionService(@Value("${behavioral-ai.emotion.confidence-threshold:0.7}") Double confidenceThreshold,
                                 @Value("${behavioral-ai.emotion.analysis-timeout-ms:100}") Integer analysisTimeoutMs,
                                 @Value("${behavioral-ai.emotion.feature-count:20}") Integer minFeatureCount,
                                 MLModelService mlModelService,
                                 FeatureExtractionService featureExtractionService) {
        this.CONFIDENCE_THRESHOLD = confidenceThreshold;
        this.ANALYSIS_TIMEOUT_MS = analysisTimeoutMs;
        this.MIN_FEATURE_COUNT = minFeatureCount;
        this.mlModelService = mlModelService;
        this.featureExtractionService = featureExtractionService;
    }
    
    // Virtual thread executor for high-performance async processing
    private static final java.util.concurrent.Executor VIRTUAL_EXECUTOR = 
        Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Analyze emotional state from trading behavior data
     * 
     * @param tradingData Trading behavior data for analysis
     * @return CompletableFuture with emotion analysis result or error
     */
    public CompletableFuture<Result<EmotionAnalysisResult, BehavioralAIError>> analyzeEmotion(
            TradingBehaviorData tradingData) {
        
        return CompletableFuture
            .supplyAsync(() -> performEmotionAnalysis(tradingData), VIRTUAL_EXECUTOR)
            .orTimeout(ANALYSIS_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .exceptionally(this::handleAnalysisException);
    }

    /**
     * Batch emotion analysis for multiple trading sessions
     * 
     * @param behaviorDataList List of trading behavior data
     * @return CompletableFuture with list of emotion analysis results
     */
    public CompletableFuture<Result<List<EmotionAnalysisResult>, BehavioralAIError>> analyzeBatchEmotions(
            List<TradingBehaviorData> behaviorDataList) {
        
        return validateBatchInput(behaviorDataList)
            .map(validData -> processEmotionBatch(validData))
            .orElse(CompletableFuture.completedFuture(
                Result.failure(BehavioralAIError.ValidationError.invalidInput(
                    "behaviorDataList", "empty_or_null", "Input list cannot be empty"))));
    }

    /**
     * Get real-time emotional state for active trading session
     * 
     * @param userId User identifier
     * @param sessionId Trading session identifier
     * @return CompletableFuture with current emotional state
     */
    public CompletableFuture<Result<EmotionAnalysisResult, BehavioralAIError>> getCurrentEmotionalState(
            String userId, String sessionId) {
        
        return featureExtractionService.extractRealtimeFeatures(userId, sessionId)
            .thenCompose(featuresResult -> 
                featuresResult.isSuccess() ?
                    analyzeEmotionFromFeatures(userId, sessionId, featuresResult.getValue()) :
                    CompletableFuture.completedFuture(
                        Result.failure(featuresResult.getError())));
    }

    /**
     * Detect emotional patterns across time series data
     * 
     * @param userId User identifier
     * @param timeRange Time range for pattern analysis
     * @return CompletableFuture with emotion pattern analysis
     */
    public CompletableFuture<Result<EmotionPatternAnalysis, BehavioralAIError>> detectEmotionPatterns(
            String userId, TimeRange timeRange) {
        
        return featureExtractionService.extractTimeSeriesFeatures(userId, timeRange)
            .thenCompose(this::performPatternAnalysis);
    }

    // Private implementation methods following functional principles

    private Result<EmotionAnalysisResult, BehavioralAIError> performEmotionAnalysis(
            TradingBehaviorData tradingData) {
        
        return validateTradingData(tradingData)
            .flatMap(this::extractBehavioralFeatures)
            .flatMap(this::applyEmotionModels)
            .flatMap(this::calculateConfidenceScores)
            .map(this::buildEmotionResult);
    }

    private Result<TradingBehaviorData, BehavioralAIError> validateTradingData(
            TradingBehaviorData data) {
        
        // First check if data itself is null to avoid NPE
        if (data == null) {
            return Result.failure(BehavioralAIError.ValidationError.missingRequiredField("tradingData"));
        }
        
        // Validate userId
        if (data.userId() == null) {
            return Result.failure(BehavioralAIError.ValidationError.missingRequiredField("userId"));
        }
        
        // Validate time window
        if (data.timeWindow() == null || !data.timeWindow().isValid()) {
            return Result.failure(BehavioralAIError.ValidationError.invalidInput(
                "timeWindow", "invalid", "Time window must be valid"));
        }
        
        // Validate behavior metrics
        if (data.behaviorMetrics() == null || data.behaviorMetrics().isEmpty()) {
            return Result.failure(BehavioralAIError.ValidationError.missingRequiredField("behaviorMetrics"));
        }
        
        return Result.success(data);
    }

    private Result<FeatureVector, BehavioralAIError> extractBehavioralFeatures(
            TradingBehaviorData data) {
        
        return Result.tryExecute(
            () -> featureExtractionService.extractEmotionFeatures(data),
            ex -> BehavioralAIError.AnalysisError.featureExtractionFailed(
                "emotion_features", ex.getMessage()));
    }

    private Result<EmotionPrediction, BehavioralAIError> applyEmotionModels(
            FeatureVector features) {
        
        return features.size() >= MIN_FEATURE_COUNT ?
            mlModelService.predictEmotion(features) :
            Result.failure(BehavioralAIError.AnalysisError.insufficientData(
                "features", "minimum_" + MIN_FEATURE_COUNT + "_required"));
    }

    private Result<EmotionPrediction, BehavioralAIError> calculateConfidenceScores(
            EmotionPrediction prediction) {
        
        return prediction.confidence() >= CONFIDENCE_THRESHOLD ?
            Result.success(prediction) :
            Result.failure(BehavioralAIError.AnalysisError.processingTimeout(
                "low_confidence", CONFIDENCE_THRESHOLD.longValue()));
    }

    private EmotionAnalysisResult buildEmotionResult(EmotionPrediction prediction) {
        return EmotionAnalysisResult.success(
            UUID.randomUUID().toString(),
            prediction.userId(),
            prediction.primaryEmotion(),
            prediction.confidence(),
            prediction.emotionScores()
        );
    }

    private CompletableFuture<Result<List<EmotionAnalysisResult>, BehavioralAIError>> processEmotionBatch(
            List<TradingBehaviorData> validData) {
        
        List<CompletableFuture<Result<EmotionAnalysisResult, BehavioralAIError>>> futures =
            validData.stream()
                .map(data -> CompletableFuture.supplyAsync(() -> performEmotionAnalysis(data), VIRTUAL_EXECUTOR))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> collectBatchResults(futures));
    }

    private Result<List<EmotionAnalysisResult>, BehavioralAIError> collectBatchResults(
            List<CompletableFuture<Result<EmotionAnalysisResult, BehavioralAIError>>> futures) {
        
        List<EmotionAnalysisResult> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(Result::isSuccess)
            .map(Result::getValue)
            .toList();

        return results.isEmpty() ?
            Result.failure(BehavioralAIError.AnalysisError.processingTimeout("batch_analysis", 0L)) :
            Result.success(results);
    }

    private CompletableFuture<Result<EmotionAnalysisResult, BehavioralAIError>> analyzeEmotionFromFeatures(
            String userId, String sessionId, FeatureVector features) {
        
        return CompletableFuture.supplyAsync(() ->
            mlModelService.predictEmotion(features)
                .map(prediction -> EmotionAnalysisResult.success(
                    UUID.randomUUID().toString(),
                    userId,
                    prediction.primaryEmotion(),
                    prediction.confidence(),
                    prediction.emotionScores()
                )), VIRTUAL_EXECUTOR);
    }

    private CompletableFuture<Result<EmotionPatternAnalysis, BehavioralAIError>> performPatternAnalysis(
            Result<TimeSeriesFeatures, BehavioralAIError> featuresResult) {
        
        return featuresResult.isSuccess() ?
            CompletableFuture.supplyAsync(() ->
                analyzeEmotionPatterns(featuresResult.getValue()), VIRTUAL_EXECUTOR) :
            CompletableFuture.completedFuture(
                Result.failure(featuresResult.getError()));
    }

    private Result<EmotionPatternAnalysis, BehavioralAIError> analyzeEmotionPatterns(
            TimeSeriesFeatures features) {
        
        return mlModelService.detectEmotionPatterns(features)
            .map(patterns -> new EmotionPatternAnalysis(
                features.userId(),
                patterns,
                Instant.now(),
                calculatePatternConfidence(patterns)
            ));
    }

    // Validation helper methods

    private java.util.Optional<List<TradingBehaviorData>> validateBatchInput(
            List<TradingBehaviorData> input) {
        
        return java.util.Optional.ofNullable(input)
            .filter(list -> !list.isEmpty())
            .filter(list -> list.size() <= 1000); // Prevent resource exhaustion
    }

    private Double calculatePatternConfidence(List<EmotionPattern> patterns) {
        return patterns.isEmpty() ? 0.0 :
            patterns.stream()
                .mapToDouble(EmotionPattern::confidence)
                .average()
                .orElse(0.0);
    }

    private Result<EmotionAnalysisResult, BehavioralAIError> handleAnalysisException(Throwable ex) {
        log.error("Emotion analysis failed: {}", ex.getMessage(), ex);
        return Result.failure(switch (ex) {
            case java.util.concurrent.TimeoutException timeout ->
                BehavioralAIError.AnalysisError.processingTimeout("emotion_analysis", (long) ANALYSIS_TIMEOUT_MS);
            case IllegalArgumentException validation ->
                BehavioralAIError.ValidationError.invalidInput("input", validation.getMessage(), "Validation failed");
            default ->
                BehavioralAIError.AnalysisError.patternDetectionFailed("emotion_analysis", ex.getMessage());
        });
    }

    // Supporting record types for functional programming
    
    public record TradingBehaviorData(
        String userId,
        String sessionId,
        TimeWindow timeWindow,
        Map<String, Double> behaviorMetrics,
        List<TradingAction> tradingActions,
        Instant timestamp
    ) {}

    public record TimeRange(Instant start, Instant end) {
        public boolean isValid() {
            return start != null && end != null && start.isBefore(end);
        }
    }

    public record TimeWindow(Instant start, Instant end, Long durationMinutes) {
        public boolean isValid() {
            return start != null && end != null && durationMinutes > 0 &&
                   start.isBefore(end);
        }
    }

    public record TradingAction(
        String actionType,
        Instant timestamp,
        Map<String, Object> actionData
    ) {}

    public record FeatureVector(
        String userId,
        Map<String, Double> features,
        Instant extractedAt
    ) {
        public int size() {
            return features.size();
        }
    }

    public record TimeSeriesFeatures(
        String userId,
        Map<String, List<Double>> timeSeriesData,
        TimeRange timeRange,
        Instant extractedAt
    ) {}

    public record EmotionPrediction(
        String userId,
        EmotionAnalysisResult.EmotionType primaryEmotion,
        Map<EmotionAnalysisResult.EmotionType, Double> emotionScores,
        Double confidence,
        Instant predictedAt
    ) {}

    public record EmotionPattern(
        EmotionAnalysisResult.EmotionType emotion,
        String patternType,
        Double confidence,
        TimeRange timeRange
    ) {}

    public record EmotionPatternAnalysis(
        String userId,
        List<EmotionPattern> patterns,
        Instant analyzedAt,
        Double overallConfidence
    ) {}
}