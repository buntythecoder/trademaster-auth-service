package com.trademaster.behavioralai.controller;

import com.trademaster.behavioralai.dto.BehavioralAIResponse;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.CoachingIntervention;
import com.trademaster.behavioralai.dto.EmotionAnalysisResult;
import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import com.trademaster.behavioralai.security.SecurityContext;
import com.trademaster.behavioralai.security.SecurityFacade;
import com.trademaster.behavioralai.service.BehavioralPatternService;
import com.trademaster.behavioralai.service.CoachingInterventionService;
import com.trademaster.behavioralai.service.EmotionDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Behavioral AI REST Controller
 * 
 * Main REST API controller for behavioral AI services following TradeMaster standards.
 * Uses SecurityFacade for Zero Trust security and functional programming patterns.
 */
@RestController
@RequestMapping("/api/v1/behavioral-ai")
@RequiredArgsConstructor
@Tag(name = "Behavioral AI", description = "AI-powered behavioral analysis and coaching")
public final class BehavioralAIController {
    
    private static final Logger log = LoggerFactory.getLogger(BehavioralAIController.class);

    private final SecurityFacade securityFacade;
    private final EmotionDetectionService emotionDetectionService;
    private final BehavioralPatternService behavioralPatternService;
    private final CoachingInterventionService coachingInterventionService;

    /**
     * Analyze emotional state from trading behavior
     */
    @PostMapping("/emotion/analyze")
    @Operation(summary = "Analyze trading emotions", 
               description = "Analyze emotional state from trading behavior data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Emotion analysis completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid trading data provided"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<EmotionAnalysisResult>>> analyzeEmotion(
            @Parameter(description = "Trading behavior data for analysis")
            @RequestBody EmotionDetectionService.TradingBehaviorData tradingData,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(tradingData.userId(), "/api/v1/behavioral-ai/emotion/analyze", "POST", request),
            context -> emotionDetectionService.analyzeEmotion(tradingData)
                .thenApply(result -> buildResponse(result, "Emotion analysis completed"))
        );
    }

    /**
     * Get current emotional state for active trading session
     */
    @GetMapping("/emotion/current/{userId}")
    @Operation(summary = "Get current emotional state", 
               description = "Get real-time emotional state for active trading session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Current emotional state retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User session not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<EmotionAnalysisResult>>> getCurrentEmotionalState(
            @Parameter(description = "User identifier") @PathVariable String userId,
            @Parameter(description = "Trading session ID") @RequestParam String sessionId,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(userId, "/api/v1/behavioral-ai/emotion/current/" + userId, "GET", request),
            context -> emotionDetectionService.getCurrentEmotionalState(userId, sessionId)
                .thenApply(result -> buildResponse(result, "Current emotional state retrieved"))
        );
    }

    /**
     * Detect behavioral patterns from trading activity
     */
    @PostMapping("/patterns/detect")
    @Operation(summary = "Detect behavioral patterns", 
               description = "Detect behavioral patterns from trading activity data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern detection completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid trading data provided"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<List<BehavioralPatternData>>>> detectPatterns(
            @Parameter(description = "Trading behavior data")
            @RequestBody EmotionDetectionService.TradingBehaviorData tradingData,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(tradingData.userId(), "/api/v1/behavioral-ai/patterns/detect", "POST", request),
            context -> behavioralPatternService.detectPatterns(tradingData)
                .thenApply(result -> buildResponse(result, "Pattern detection completed"))
        );
    }

    /**
     * Get user's behavioral patterns with pagination
     */
    @GetMapping("/patterns/user/{userId}")
    @Operation(summary = "Get user behavioral patterns", 
               description = "Retrieve user's behavioral patterns with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User patterns retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<BehavioralAIResponse<Page<BehavioralPatternData>>> getUserPatterns(
            @Parameter(description = "User identifier") @PathVariable String userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        return secureOperation(
            createSecurityContext(userId, "/api/v1/behavioral-ai/patterns/user/" + userId, "GET", request),
            context -> {
                Page<BehavioralPatternData> patterns = behavioralPatternService.getUserPatterns(
                    userId, PageRequest.of(page, size));
                return Result.success(patterns);
            },
            "User patterns retrieved successfully"
        );
    }

    /**
     * Analyze behavioral pattern trends
     */
    @GetMapping("/patterns/trends/{userId}")
    @Operation(summary = "Analyze pattern trends", 
               description = "Analyze behavioral pattern trends over time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern trend analysis completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid time range provided"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<BehavioralPatternService.PatternTrendAnalysis>>> analyzePatternTrends(
            @Parameter(description = "User identifier") @PathVariable String userId,
            @Parameter(description = "Start time (ISO timestamp)") @RequestParam String startTime,
            @Parameter(description = "End time (ISO timestamp)") @RequestParam String endTime,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(userId, "/api/v1/behavioral-ai/patterns/trends/" + userId, "GET", request),
            context -> {
                EmotionDetectionService.TimeRange timeRange = new EmotionDetectionService.TimeRange(
                    java.time.Instant.parse(startTime), java.time.Instant.parse(endTime));
                return behavioralPatternService.analyzePatternTrends(userId, timeRange)
                    .thenApply(result -> buildResponse(result, "Pattern trend analysis completed"));
            }
        );
    }

    /**
     * Trigger coaching intervention
     */
    @PostMapping("/coaching/trigger")
    @Operation(summary = "Trigger coaching intervention", 
               description = "Evaluate and trigger coaching intervention based on behavioral pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coaching intervention processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pattern data provided"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<CoachingIntervention>>> triggerCoachingIntervention(
            @Parameter(description = "User identifier") @RequestParam String userId,
            @Parameter(description = "Behavioral pattern data") @RequestBody BehavioralPatternData patternData,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(userId, "/api/v1/behavioral-ai/coaching/trigger", "POST", request),
            context -> coachingInterventionService.evaluateAndTriggerIntervention(userId, patternData)
                .thenApply(result -> buildResponse(result, "Coaching intervention processed"))
        );
    }

    /**
     * Process user response to coaching intervention
     */
    @PostMapping("/coaching/response/{interventionId}")
    @Operation(summary = "Process coaching response", 
               description = "Process user response to coaching intervention")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User response processed successfully"),
        @ApiResponse(responseCode = "404", description = "Intervention not found"),
        @ApiResponse(responseCode = "400", description = "Invalid response data")
    })
    public ResponseEntity<BehavioralAIResponse<CoachingIntervention>> processCoachingResponse(
            @Parameter(description = "Intervention identifier") @PathVariable String interventionId,
            @Parameter(description = "User response data") @RequestBody CoachingIntervention.UserResponse userResponse,
            HttpServletRequest request) {
        
        return secureOperation(
            createSecurityContext("system", "/api/v1/behavioral-ai/coaching/response/" + interventionId, "POST", request),
            context -> coachingInterventionService.processUserResponse(interventionId, userResponse),
            "User response processed successfully"
        );
    }

    /**
     * Get coaching effectiveness analytics
     */
    @GetMapping("/coaching/analytics/{userId}")
    @Operation(summary = "Get coaching effectiveness", 
               description = "Get intervention effectiveness analytics for user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid time range provided"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<BehavioralAIResponse<CoachingInterventionService.InterventionEffectivenessAnalytics>>> getCoachingAnalytics(
            @Parameter(description = "User identifier") @PathVariable String userId,
            @Parameter(description = "Analysis start time") @RequestParam String startTime,
            @Parameter(description = "Analysis end time") @RequestParam String endTime,
            HttpServletRequest request) {
        
        return secureAsyncOperation(
            createSecurityContext(userId, "/api/v1/behavioral-ai/coaching/analytics/" + userId, "GET", request),
            context -> {
                EmotionDetectionService.TimeRange timeRange = new EmotionDetectionService.TimeRange(
                    java.time.Instant.parse(startTime), java.time.Instant.parse(endTime));
                return coachingInterventionService.getEffectivenessAnalytics(userId, timeRange)
                    .thenApply(result -> buildResponse(result, "Analytics retrieved successfully"));
            }
        );
    }

    /**
     * Get urgent interventions requiring attention
     */
    @GetMapping("/coaching/urgent")
    @Operation(summary = "Get urgent interventions", 
               description = "Get list of urgent interventions requiring immediate attention")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Urgent interventions retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<BehavioralAIResponse<List<CoachingIntervention>>> getUrgentInterventions(
            HttpServletRequest request) {
        
        return secureOperation(
            createSecurityContext("system", "/api/v1/behavioral-ai/coaching/urgent", "GET", request),
            context -> Result.success(coachingInterventionService.getUrgentInterventions()),
            "Urgent interventions retrieved successfully"
        );
    }

    // Security and utility methods

    private SecurityContext createSecurityContext(String userId, String endpoint, String method, HttpServletRequest request) {
        Map<String, Object> requestMetadata = Map.of(
            "Authorization", request.getHeader("Authorization") != null ? request.getHeader("Authorization") : "",
            "requestedUserId", userId,
            "userAgent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "",
            "remoteAddr", request.getRemoteAddr()
        );
        
        return new SecurityContext(
            userId,
            endpoint,
            method,
            new SecurityContext.ClientInfo(
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("X-Device-Fingerprint"),
                request.getHeader("X-User-Location"),
                java.time.Instant.now()
            ),
            null, // Will be set during authentication
            List.of(),
            requestMetadata,
            java.time.Instant.now()
        );
    }

    private <T> ResponseEntity<BehavioralAIResponse<T>> secureOperation(
            SecurityContext context,
            java.util.function.Function<SecurityContext, Result<T, BehavioralAIError>> operation,
            String successMessage) {
        
        String correlationId = generateCorrelationId();
        return securityFacade.secureAccess(context, ctx -> operation.apply(ctx))
            .fold(
                error -> ResponseEntity.status(mapErrorToHttpStatus(error))
                    .body(BehavioralAIResponse.error(error.getErrorMessage(), correlationId, 
                        BehavioralAIResponse.ErrorDetails.create(error.getCode(), error.getClass().getSimpleName(), error.getErrorMessage()))),
                result -> result.fold(
                    error -> ResponseEntity.status(mapErrorToHttpStatus(error))
                        .body(BehavioralAIResponse.error(error.getErrorMessage(), correlationId,
                            BehavioralAIResponse.ErrorDetails.create(error.getCode(), error.getClass().getSimpleName(), error.getErrorMessage()))),
                    data -> ResponseEntity.ok(BehavioralAIResponse.success(data, successMessage, correlationId))
                )
            );
    }

    private <T> CompletableFuture<ResponseEntity<BehavioralAIResponse<T>>> secureAsyncOperation(
            SecurityContext context,
            java.util.function.Function<SecurityContext, CompletableFuture<ResponseEntity<BehavioralAIResponse<T>>>> operation) {
        
        String correlationId = generateCorrelationId();
        return securityFacade.secureAccess(context, operation)
            .fold(
                error -> CompletableFuture.completedFuture(
                    ResponseEntity.status(mapErrorToHttpStatus(error))
                        .body(BehavioralAIResponse.error(error.getErrorMessage(), correlationId,
                            BehavioralAIResponse.ErrorDetails.create(error.getCode(), error.getClass().getSimpleName(), error.getErrorMessage())))),
                java.util.function.Function.identity()
            );
    }

    private <T> ResponseEntity<BehavioralAIResponse<T>> buildResponse(Result<T, BehavioralAIError> result, String successMessage) {
        String correlationId = generateCorrelationId();
        return result.fold(
            error -> ResponseEntity.status(mapErrorToHttpStatus(error))
                .body(BehavioralAIResponse.error(error.getErrorMessage(), correlationId,
                    BehavioralAIResponse.ErrorDetails.create(error.getCode(), error.getClass().getSimpleName(), error.getErrorMessage()))),
            data -> ResponseEntity.ok(BehavioralAIResponse.success(data, successMessage, correlationId))
        );
    }

    private int mapErrorToHttpStatus(BehavioralAIError error) {
        return switch (error) {
            case BehavioralAIError.ValidationError validationError -> 400;
            case BehavioralAIError.DataError dataError -> 404;
            case BehavioralAIError.AnalysisError analysisError -> 422;
            case BehavioralAIError.ModelError modelError -> 503;
            case BehavioralAIError.InterventionError interventionError -> 429;
            default -> 500;
        };
    }

    private String generateCorrelationId() {
        return "ctrl-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}