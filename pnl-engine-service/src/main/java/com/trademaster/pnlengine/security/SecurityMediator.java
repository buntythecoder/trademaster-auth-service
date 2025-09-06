package com.trademaster.pnlengine.security;

import com.trademaster.pnlengine.common.functional.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Function;

/**
 * SecurityMediator for Zero Trust Architecture
 * 
 * MANDATORY: Java 24 + Zero Trust Security + Functional Programming
 * 
 * Coordinates all security components (authentication, authorization, 
 * risk assessment, audit) to provide comprehensive security validation
 * for all external operations.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Security Refactoring)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public final class SecurityMediator {
    
    @Value("${security.risk.threshold:0.8}")
    private double riskThreshold;
    
    @Value("${security.rate-limit.max-requests:100}")
    private int maxRequestsPerMinute;
    
    @Value("${security.session.max-duration-hours:8}")
    private int maxSessionDurationHours;
    
    // ============================================================================
    // CORE SECURITY MEDIATION
    // ============================================================================
    
    /**
     * Orchestrate complete security validation chain
     * Authentication → Authorization → Risk Assessment → Audit → Execute
     */
    public <T> Result<T, SecurityError> validateAndExecute(
            SecurityContext context,
            Function<SecurityContext, Result<T, SecurityError>> operation) {
        
        return authenticateContext(context)
            .flatMap(this::authorizeContext)
            .flatMap(this::assessRisk)
            .flatMap(this::checkRateLimit)
            .flatMap(validatedContext -> {
                // Execute operation with validated context
                var result = operation.apply(validatedContext);
                
                // Log operation result for audit
                logOperationResult(validatedContext, result);
                
                return result;
            });
    }
    
    // ============================================================================
    // AUTHENTICATION VALIDATION
    // ============================================================================
    
    private Result<SecurityContext, SecurityError> authenticateContext(SecurityContext context) {
        return switch (context.requestType()) {
            case API_CALL -> validateApiAuthentication(context);
            case INTERNAL -> validateInternalAuthentication(context);
            case BACKGROUND, SYSTEM -> validateSystemAuthentication(context);
        };
    }
    
    private Result<SecurityContext, SecurityError> validateApiAuthentication(SecurityContext context) {
        // Check session expiry
        return context.isExpired() ?
            Result.failure(new SecurityError.SessionExpired(
                context.sessionId(), context.userId(), context.expiresAt(), Instant.now())) :
            validateSessionDuration(context);
    }
    
    private Result<SecurityContext, SecurityError> validateInternalAuthentication(SecurityContext context) {
        // Internal services have relaxed authentication but must have system authority
        return context.hasAuthority("SYSTEM") ?
            Result.success(context) :
            Result.failure(new SecurityError.AuthenticationFailed(
                "Missing SYSTEM authority for internal service", 
                context.userId(), context.sessionId(), Instant.now()));
    }
    
    private Result<SecurityContext, SecurityError> validateSystemAuthentication(SecurityContext context) {
        // System operations must have system-level security
        return context.securityLevel() == SecurityContext.SecurityLevel.SYSTEM ?
            Result.success(context) :
            Result.failure(new SecurityError.AuthenticationFailed(
                "Invalid security level for system operation",
                context.userId(), context.sessionId(), Instant.now()));
    }
    
    private Result<SecurityContext, SecurityError> validateSessionDuration(SecurityContext context) {
        var sessionDuration = java.time.Duration.between(context.authenticatedAt(), Instant.now());
        var maxDuration = java.time.Duration.ofHours(maxSessionDurationHours);
        
        return sessionDuration.compareTo(maxDuration) <= 0 ?
            Result.success(context) :
            Result.failure(new SecurityError.SessionExpired(
                context.sessionId(), context.userId(), 
                context.authenticatedAt().plus(maxDuration), Instant.now()));
    }
    
    // ============================================================================
    // AUTHORIZATION VALIDATION
    // ============================================================================
    
    private Result<SecurityContext, SecurityError> authorizeContext(SecurityContext context) {
        // Check minimum security level
        var requiredLevel = determineRequiredSecurityLevel(context);
        
        return context.isValidFor(requiredLevel) ?
            validateSpecificAuthorizations(context) :
            Result.failure(new SecurityError.AuthorizationFailed(
                requiredLevel.name(), "security_level", context.userId(), 
                requiredLevel, Instant.now()));
    }
    
    private SecurityContext.SecurityLevel determineRequiredSecurityLevel(SecurityContext context) {
        return switch (context.requestType()) {
            case API_CALL -> SecurityContext.SecurityLevel.AUTHENTICATED;
            case INTERNAL -> SecurityContext.SecurityLevel.AUTHENTICATED;
            case BACKGROUND -> SecurityContext.SecurityLevel.SYSTEM;
            case SYSTEM -> SecurityContext.SecurityLevel.SYSTEM;
        };
    }
    
    private Result<SecurityContext, SecurityError> validateSpecificAuthorizations(SecurityContext context) {
        // For P&L operations, check specific scopes
        var requiredScopes = switch (context.requestType()) {
            case API_CALL -> java.util.Set.of("pnl:read", "portfolio:read");
            case INTERNAL -> java.util.Set.of("internal:pnl");
            case BACKGROUND, SYSTEM -> java.util.Set.of("system:all");
        };
        
        var hasAllScopes = requiredScopes.stream()
            .allMatch(scope -> context.hasScope(scope) || context.hasScope("all"));
        
        return hasAllScopes ?
            Result.success(context) :
            Result.failure(new SecurityError.AuthorizationFailed(
                "", String.join(",", requiredScopes), context.userId(), 
                context.securityLevel(), Instant.now()));
    }
    
    // ============================================================================
    // RISK ASSESSMENT
    // ============================================================================
    
    private Result<SecurityContext, SecurityError> assessRisk(SecurityContext context) {
        var riskScore = calculateRiskScore(context);
        
        return riskScore <= riskThreshold ?
            Result.success(context) :
            Result.failure(new SecurityError.RiskAssessmentFailed(
                context.userId(), "composite_risk", riskScore, riskThreshold,
                "Request blocked due to high risk score", Instant.now()));
    }
    
    private double calculateRiskScore(SecurityContext context) {
        var baseScore = 0.0;
        
        // IP-based risk factors
        baseScore += switch (context.clientIp()) {
            case "internal" -> 0.0;
            case String ip when ip.startsWith("192.168.") || ip.startsWith("10.") -> 0.1;
            default -> 0.3;
        };
        
        // Time-based risk factors
        var hour = java.time.LocalDateTime.now().getHour();
        baseScore += (hour >= 22 || hour <= 6) ? 0.2 : 0.0; // Night time access
        
        // User agent risk factors
        baseScore += switch (context.userAgent()) {
            case "service-to-service", "background-processor" -> 0.0;
            case String ua when ua.contains("bot") || ua.contains("curl") -> 0.4;
            default -> 0.1;
        };
        
        // Session age risk factors
        var sessionAge = java.time.Duration.between(context.authenticatedAt(), Instant.now());
        baseScore += sessionAge.toHours() > 4 ? 0.2 : 0.0;
        
        return Math.min(baseScore, 1.0);
    }
    
    // ============================================================================
    // RATE LIMITING
    // ============================================================================
    
    private Result<SecurityContext, SecurityError> checkRateLimit(SecurityContext context) {
        // For demonstration - in production this would use Redis or similar
        // Rate limiting is more complex and would track per user/endpoint
        
        return context.requestType() == SecurityContext.RequestType.SYSTEM ?
            Result.success(context) : // System requests bypass rate limiting
            checkApiRateLimit(context);
    }
    
    private Result<SecurityContext, SecurityError> checkApiRateLimit(SecurityContext context) {
        // Simplified rate limiting - in production would use sliding window
        var requestCount = getCurrentRequestCount(context.userId());
        
        return requestCount < maxRequestsPerMinute ?
            Result.success(context) :
            Result.failure(new SecurityError.RateLimitExceeded(
                context.userId(), "pnl-api", requestCount, maxRequestsPerMinute,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)));
    }
    
    private int getCurrentRequestCount(String userId) {
        // Placeholder - in production would query rate limiting store
        return 0;
    }
    
    // ============================================================================
    // AUDIT LOGGING
    // ============================================================================
    
    private <T> void logOperationResult(SecurityContext context, Result<T, SecurityError> result) {
        switch (result) {
            case Result.Success<T, SecurityError>(var value) -> 
                log.info("SECURITY_AUDIT: Operation successful for user: {}, correlation: {}, type: {}",
                    context.userId(), context.correlationId(), context.requestType());
                    
            case Result.Failure<T, SecurityError>(var error) ->
                log.warn("SECURITY_AUDIT: Operation failed for user: {}, correlation: {}, error: {}",
                    context.userId(), context.correlationId(), error.getAuditMessage());
        }
    }
}