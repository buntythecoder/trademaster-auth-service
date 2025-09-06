package com.trademaster.behavioralai.service;

import com.trademaster.behavioralai.domain.entity.BehavioralPattern;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import com.trademaster.behavioralai.repository.BehavioralPatternRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Behavioral Pattern Recognition Service
 * 
 * Advanced service for detecting, analyzing, and managing trading behavioral patterns.
 * Uses functional programming principles and virtual threads for high-performance pattern recognition.
 * 
 * Features:
 * - Real-time pattern detection from trading behavior
 * - Pattern frequency analysis and trending
 * - Risk assessment based on behavioral patterns
 * - Pattern correlation and clustering analysis
 */
@Service
@RequiredArgsConstructor

public final class BehavioralPatternService {
    private static final Logger log = LoggerFactory.getLogger(BehavioralPatternService.class);

    @Value("${behavioral-ai.patterns.detection-threshold:0.75}")
    private final Double DETECTION_THRESHOLD;

    @Value("${behavioral-ai.patterns.max-patterns-per-session:10}")
    private final Integer MAX_PATTERNS_PER_SESSION;

    @Value("${behavioral-ai.patterns.analysis-window-hours:24}")
    private final Integer ANALYSIS_WINDOW_HOURS;

    private final BehavioralPatternRepository patternRepository;
    private final MLModelService mlModelService;
    private final EmotionDetectionService emotionDetectionService;

    // Virtual thread executor for high-performance async processing
    private static final java.util.concurrent.Executor VIRTUAL_EXECUTOR = 
        Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Detect behavioral patterns from trading activity data
     * 
     * @param tradingData Trading behavior data for pattern analysis
     * @return CompletableFuture with detected patterns or error
     */
    public CompletableFuture<Result<List<BehavioralPatternData>, BehavioralAIError>> detectPatterns(
            EmotionDetectionService.TradingBehaviorData tradingData) {
        
        return CompletableFuture
            .supplyAsync(() -> performPatternDetection(tradingData), VIRTUAL_EXECUTOR)
            .exceptionally(this::handleDetectionException);
    }

    /**
     * Analyze pattern trends for a user over time period
     * 
     * @param userId User identifier
     * @param timeRange Time range for trend analysis
     * @return CompletableFuture with pattern trend analysis
     */
    public CompletableFuture<Result<PatternTrendAnalysis, BehavioralAIError>> analyzePatternTrends(
            String userId, EmotionDetectionService.TimeRange timeRange) {
        
        return CompletableFuture
            .supplyAsync(() -> performTrendAnalysis(userId, timeRange), VIRTUAL_EXECUTOR);
    }

    /**
     * Get risk assessment based on user's behavioral patterns
     * 
     * @param userId User identifier
     * @return CompletableFuture with behavioral risk assessment
     */
    public CompletableFuture<Result<BehavioralRiskAssessment, BehavioralAIError>> assessBehavioralRisk(
            String userId) {
        
        return CompletableFuture
            .supplyAsync(() -> performRiskAssessment(userId), VIRTUAL_EXECUTOR);
    }

    /**
     * Find similar behavioral patterns across users
     * 
     * @param patternData Reference pattern for similarity search
     * @param maxResults Maximum number of similar patterns to return
     * @return CompletableFuture with similar patterns
     */
    public CompletableFuture<Result<List<BehavioralPatternData>, BehavioralAIError>> findSimilarPatterns(
            BehavioralPatternData patternData, Integer maxResults) {
        
        return CompletableFuture
            .supplyAsync(() -> performSimilaritySearch(patternData, maxResults), VIRTUAL_EXECUTOR);
    }

    /**
     * Save detected behavioral pattern
     * 
     * @param patternData Behavioral pattern to save
     * @return Result with saved pattern or error
     */
    @Transactional
    public Result<BehavioralPatternData, BehavioralAIError> savePattern(
            BehavioralPatternData patternData) {
        
        return validatePattern(patternData)
            .flatMap(this::checkPatternDuplication)
            .flatMap(this::persistPattern)
            .map(BehavioralPattern::toDto);
    }

    /**
     * Get user's behavioral patterns with pagination
     * 
     * @param userId User identifier
     * @param pageRequest Pagination parameters
     * @return Page of behavioral patterns
     */
    @Transactional(readOnly = true)
    public Page<BehavioralPatternData> getUserPatterns(String userId, PageRequest pageRequest) {
        return patternRepository.findByUserId(userId, pageRequest)
            .map(BehavioralPattern::toDto);
    }

    /**
     * Get patterns requiring immediate intervention
     * 
     * @param userId User identifier
     * @return List of high-risk patterns needing intervention
     */
    @Transactional(readOnly = true)
    public List<BehavioralPatternData> getPatternsRequiringIntervention(String userId) {
        List<BehavioralPatternData.PatternType> highRiskPatterns = getHighRiskPatternTypes();
        
        return patternRepository.findPatternsRequiringIntervention(
                userId, DETECTION_THRESHOLD, highRiskPatterns)
            .stream()
            .map(BehavioralPattern::toDto)
            .toList();
    }

    // Private implementation methods following functional principles

    private Result<List<BehavioralPatternData>, BehavioralAIError> performPatternDetection(
            EmotionDetectionService.TradingBehaviorData tradingData) {
        
        return validateTradingData(tradingData)
            .flatMap(this::extractPatternFeatures)
            .flatMap(this::applyPatternDetectionModels)
            .flatMap(this::filterSignificantPatterns)
            .map(this::buildPatternResults);
    }

    private Result<PatternTrendAnalysis, BehavioralAIError> performTrendAnalysis(
            String userId, EmotionDetectionService.TimeRange timeRange) {
        
        return Result.tryExecute(
            () -> {
                List<BehavioralPattern> historicalPatterns = 
                    patternRepository.findByUserIdAndDetectedAtBetween(
                        userId, timeRange.start(), timeRange.end());
                
                return analyzeTrends(historicalPatterns, timeRange);
            },
            ex -> BehavioralAIError.AnalysisError.patternDetectionFailed("trend_analysis", ex.getMessage()));
    }

    private Result<BehavioralRiskAssessment, BehavioralAIError> performRiskAssessment(String userId) {
        return Result.tryExecute(
            () -> {
                Instant since = Instant.now().minus(ANALYSIS_WINDOW_HOURS, ChronoUnit.HOURS);
                List<BehavioralPattern> recentPatterns = 
                    patternRepository.findByUserIdAndDetectedAtAfterOrderByDetectedAtDesc(userId, since);
                
                return calculateRiskAssessment(recentPatterns);
            },
            ex -> BehavioralAIError.AnalysisError.patternDetectionFailed("risk_assessment", ex.getMessage()));
    }

    private Result<List<BehavioralPatternData>, BehavioralAIError> performSimilaritySearch(
            BehavioralPatternData patternData, Integer maxResults) {
        
        return mlModelService.findSimilarPatterns(patternData, maxResults)
            .map(patterns -> patterns.stream()
                .map(this::convertToPatternData)
                .toList());
    }

    private Result<EmotionDetectionService.TradingBehaviorData, BehavioralAIError> validateTradingData(
            EmotionDetectionService.TradingBehaviorData data) {
        
        return data != null && data.userId() != null && !data.behaviorMetrics().isEmpty() ?
            Result.success(data) :
            Result.failure(BehavioralAIError.ValidationError.invalidInput(
                "tradingData", "incomplete", "Trading data must contain userId and behavior metrics"));
    }

    private Result<PatternFeatureVector, BehavioralAIError> extractPatternFeatures(
            EmotionDetectionService.TradingBehaviorData data) {
        
        return Result.tryExecute(
            () -> new PatternFeatureVector(
                data.userId(),
                extractBehavioralFeatures(data),
                Instant.now()
            ),
            ex -> BehavioralAIError.AnalysisError.featureExtractionFailed("pattern_features", ex.getMessage()));
    }

    private Result<List<PatternDetection>, BehavioralAIError> applyPatternDetectionModels(
            PatternFeatureVector features) {
        
        return mlModelService.detectBehavioralPatterns(features);
    }

    private Result<List<PatternDetection>, BehavioralAIError> filterSignificantPatterns(
            List<PatternDetection> detections) {
        
        List<PatternDetection> significantPatterns = detections.stream()
            .filter(detection -> detection.confidence() >= DETECTION_THRESHOLD)
            .limit(MAX_PATTERNS_PER_SESSION)
            .toList();
            
        return Result.success(significantPatterns);
    }

    private List<BehavioralPatternData> buildPatternResults(List<PatternDetection> detections) {
        return detections.stream()
            .map(this::convertDetectionToPatternData)
            .toList();
    }

    private Result<BehavioralPatternData, BehavioralAIError> validatePattern(
            BehavioralPatternData pattern) {
        
        return pattern.patternType() != null && 
               pattern.confidence() >= 0.0 && pattern.confidence() <= 1.0 &&
               pattern.userId() != null ?
            Result.success(pattern) :
            Result.failure(BehavioralAIError.ValidationError.invalidInput(
                "pattern", "validation_failed", "Pattern validation failed"));
    }

    private Result<BehavioralPatternData, BehavioralAIError> checkPatternDuplication(
            BehavioralPatternData pattern) {
        
        Instant recentThreshold = Instant.now().minus(1, ChronoUnit.HOURS);
        boolean isDuplicate = patternRepository.existsByUserIdAndPatternTypeAndDetectedAtAfter(
            pattern.userId(), pattern.patternType(), recentThreshold);
            
        return isDuplicate ?
            Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "duplicate_pattern", "Pattern already detected recently")) :
            Result.success(pattern);
    }

    private Result<BehavioralPattern, BehavioralAIError> persistPattern(
            BehavioralPatternData patternData) {
        
        return Result.tryExecute(
            () -> {
                BehavioralPattern entity = BehavioralPattern.fromDto(patternData);
                return patternRepository.save(entity);
            },
            ex -> BehavioralAIError.DataError.storageFailed("save_pattern", ex.getMessage()));
    }

    // Helper methods for pattern analysis

    private Map<String, Double> extractBehavioralFeatures(EmotionDetectionService.TradingBehaviorData data) {
        return Map.of(
            "trade_frequency", calculateTradeFrequency(data),
            "position_volatility", calculatePositionVolatility(data),
            "decision_speed", calculateDecisionSpeed(data),
            "risk_pattern", calculateRiskPattern(data),
            "timing_pattern", calculateTimingPattern(data)
        );
    }

    private PatternTrendAnalysis analyzeTrends(List<BehavioralPattern> patterns, 
                                             EmotionDetectionService.TimeRange timeRange) {
        
        Map<BehavioralPatternData.PatternType, Long> patternCounts = patterns.stream()
            .collect(Collectors.groupingBy(BehavioralPattern::getPatternType, Collectors.counting()));
        
        List<BehavioralPatternData.PatternType> trendingPatterns = identifyTrendingPatterns(patterns);
        Double overallRiskTrend = calculateOverallRiskTrend(patterns);
        
        return new PatternTrendAnalysis(
            patterns.get(0).getUserId(),
            patternCounts,
            trendingPatterns,
            overallRiskTrend,
            timeRange,
            Instant.now()
        );
    }

    private BehavioralRiskAssessment calculateRiskAssessment(List<BehavioralPattern> patterns) {
        Double averageRiskScore = patterns.stream()
            .mapToDouble(p -> p.getRiskScore() != null ? p.getRiskScore() : 0.0)
            .average()
            .orElse(0.0);
        
        Map<BehavioralPatternData.PatternType, Integer> patternFrequency = patterns.stream()
            .collect(Collectors.groupingBy(
                BehavioralPattern::getPatternType,
                Collectors.summingInt(p -> 1)
            ));
        
        RiskLevel riskLevel = calculateRiskLevel(averageRiskScore, patternFrequency);
        List<String> recommendations = generateRiskRecommendations(riskLevel, patternFrequency);
        
        return new BehavioralRiskAssessment(
            patterns.get(0).getUserId(),
            averageRiskScore,
            riskLevel,
            patternFrequency,
            recommendations,
            Instant.now()
        );
    }

    // Calculation methods

    private Double calculateTradeFrequency(EmotionDetectionService.TradingBehaviorData data) {
        return data.tradingActions().size() / 
               Math.max(1.0, data.timeWindow().durationMinutes() / 60.0);
    }

    private Double calculatePositionVolatility(EmotionDetectionService.TradingBehaviorData data) {
        return data.behaviorMetrics().getOrDefault("position_variance", 0.0);
    }

    private Double calculateDecisionSpeed(EmotionDetectionService.TradingBehaviorData data) {
        return data.behaviorMetrics().getOrDefault("decision_speed", 0.0);
    }

    private Double calculateRiskPattern(EmotionDetectionService.TradingBehaviorData data) {
        return data.behaviorMetrics().getOrDefault("risk_score", 0.0);
    }

    private Double calculateTimingPattern(EmotionDetectionService.TradingBehaviorData data) {
        return data.behaviorMetrics().getOrDefault("timing_score", 0.0);
    }

    private List<BehavioralPatternData.PatternType> identifyTrendingPatterns(List<BehavioralPattern> patterns) {
        // Implementation would analyze pattern frequency changes over time
        return patterns.stream()
            .collect(Collectors.groupingBy(BehavioralPattern::getPatternType, Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 2) // Patterns with high frequency
            .map(Map.Entry::getKey)
            .toList();
    }

    private Double calculateOverallRiskTrend(List<BehavioralPattern> patterns) {
        return patterns.stream()
            .filter(p -> p.getRiskScore() != null)
            .mapToDouble(BehavioralPattern::getRiskScore)
            .average()
            .orElse(0.0);
    }

    private RiskLevel calculateRiskLevel(Double averageRiskScore, 
                                       Map<BehavioralPatternData.PatternType, Integer> patternFreq) {
        
        long highRiskPatterns = patternFreq.entrySet().stream()
            .filter(entry -> entry.getKey().isHighRisk())
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        return switch (averageRiskScore) {
            case Double r when r > 0.8 || highRiskPatterns > 5 -> RiskLevel.CRITICAL;
            case Double r when r > 0.6 || highRiskPatterns > 3 -> RiskLevel.HIGH;
            case Double r when r > 0.4 || highRiskPatterns > 1 -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }

    private List<String> generateRiskRecommendations(RiskLevel riskLevel, 
                                                   Map<BehavioralPatternData.PatternType, Integer> patterns) {
        
        return switch (riskLevel) {
            case CRITICAL -> List.of(
                "Immediate intervention required",
                "Consider reducing position sizes",
                "Implement strict stop-loss rules",
                "Take a trading break"
            );
            case HIGH -> List.of(
                "Monitor trading behavior closely",
                "Review recent trading decisions",
                "Consider mindfulness exercises"
            );
            case MEDIUM -> List.of(
                "Be aware of behavioral patterns",
                "Maintain trading discipline"
            );
            case LOW -> List.of("Continue current trading approach");
        };
    }

    private List<BehavioralPatternData.PatternType> getHighRiskPatternTypes() {
        return List.of(
            BehavioralPatternData.PatternType.IMPULSIVE_TRADING,
            BehavioralPatternData.PatternType.REVENGE_TRADING,
            BehavioralPatternData.PatternType.PANIC_SELLING,
            BehavioralPatternData.PatternType.OVERCONFIDENCE_BIAS,
            BehavioralPatternData.PatternType.FEAR_OF_MISSING_OUT
        );
    }

    private BehavioralPatternData convertDetectionToPatternData(PatternDetection detection) {
        return BehavioralPatternData.create(
            detection.userId(),
            detection.patternType(),
            detection.confidence(),
            detection.sessionId()
        );
    }

    private BehavioralPatternData convertToPatternData(Object pattern) {
        // Implementation would convert from ML model result to DTO
        // For now, return a placeholder
        return BehavioralPatternData.create("unknown", 
            BehavioralPatternData.PatternType.IMPULSIVE_TRADING, 0.5, "unknown");
    }

    private Result<List<BehavioralPatternData>, BehavioralAIError> handleDetectionException(Throwable ex) {
        log.error("Pattern detection failed: {}", ex.getMessage(), ex);
        return Result.failure(BehavioralAIError.AnalysisError.patternDetectionFailed(
            "pattern_detection", ex.getMessage()));
    }

    // Supporting record types

    public record PatternFeatureVector(
        String userId,
        Map<String, Double> features,
        Instant extractedAt
    ) {}

    public record PatternDetection(
        String userId,
        BehavioralPatternData.PatternType patternType,
        Double confidence,
        String sessionId,
        Map<String, Object> evidence
    ) {}

    public record PatternTrendAnalysis(
        String userId,
        Map<BehavioralPatternData.PatternType, Long> patternCounts,
        List<BehavioralPatternData.PatternType> trendingPatterns,
        Double overallRiskTrend,
        EmotionDetectionService.TimeRange timeRange,
        Instant analyzedAt
    ) {}

    public record BehavioralRiskAssessment(
        String userId,
        Double averageRiskScore,
        RiskLevel riskLevel,
        Map<BehavioralPatternData.PatternType, Integer> patternFrequency,
        List<String> recommendations,
        Instant assessedAt
    ) {}

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}