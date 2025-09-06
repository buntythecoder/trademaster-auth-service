package com.trademaster.agentos.security.mediator;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import com.trademaster.agentos.security.service.*;
import com.trademaster.agentos.security.validator.InputValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Security Mediator - Coordinates all security services.
 * Implements the Mediator pattern to orchestrate authentication,
 * authorization, risk assessment, and audit logging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityMediator {
    
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final RiskAssessmentService riskAssessmentService;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;
    private final SessionService sessionService;
    private final InputValidator inputValidator;
    
    /**
     * Mediate synchronous access with full security stack.
     * Follows Zero Trust principle: verify everything, trust nothing.
     */
    public <T> Result<T, SecurityError> mediateAccess(
            SecurityContext context,
            Supplier<T> operation) {
        
        String correlationId = context.correlationId();
        log.debug("Mediating access: correlationId={}", correlationId);
        
        // Security pipeline: authenticate -> check rate limit -> authorize -> assess risk -> execute -> audit
        return authenticate(context)
            .flatMap(this::checkRateLimit)
            .flatMap(this::authorize)
            .flatMap(this::assessRisk)
            .flatMap(validContext -> executeOperation(validContext, operation))
            .onSuccess(result -> auditSuccess(context, "ACCESS_GRANTED", result))
            .onFailure(error -> auditFailure(context, "ACCESS_DENIED", error));
    }
    
    /**
     * Mediate access with input validation and transformation.
     */
    public <T, R> Result<R, SecurityError> mediateAccessWithInput(
            SecurityContext context,
            T input,
            Function<T, R> operation) {
        
        String correlationId = context.correlationId();
        log.debug("Mediating access with input: correlationId={}", correlationId);
        
        // Validate input first
        return validateInput(input, context)
            .flatMap(validInput -> mediateAccess(context, () -> operation.apply(validInput)));
    }
    
    /**
     * Mediate asynchronous access using virtual threads.
     */
    public <T> CompletableFuture<Result<T, SecurityError>> mediateAsyncAccess(
            SecurityContext context,
            Supplier<CompletableFuture<T>> asyncOperation) {
        
        String correlationId = context.correlationId();
        log.debug("Mediating async access: correlationId={}", correlationId);
        
        // Run security checks first, then execute async operation
        Result<SecurityContext, SecurityError> securityResult = authenticate(context)
            .flatMap(this::checkRateLimit)
            .flatMap(this::authorize)
            .flatMap(this::assessRisk);
        
        return switch (securityResult) {
            case Result.Success(SecurityContext validContext) -> {
                auditService.logSecurityEvent(validContext, "ASYNC_ACCESS_GRANTED", null);
                yield asyncOperation.get()
                    .thenApply(Result::<T, SecurityError>success)
                    .exceptionally(ex -> {
                        log.error("Async operation failed: correlationId={}", correlationId, ex);
                        auditFailure(context, "ASYNC_OPERATION_FAILED", 
                            SecurityError.authorizationDenied(ex.getMessage(), correlationId));
                        return Result.failure(SecurityError.authorizationDenied(
                            "Operation failed", correlationId));
                    });
            }
            case Result.Failure(SecurityError error) -> {
                auditFailure(context, "ASYNC_ACCESS_DENIED", error);
                yield CompletableFuture.completedFuture(Result.failure(error));
            }
        };
    }
    
    /**
     * Validate security context.
     */
    public Result<SecurityContext, SecurityError> validateContext(SecurityContext context) {
        log.debug("Validating context: correlationId={}", context.correlationId());
        
        return authenticate(context)
            .flatMap(this::validateSession)
            .flatMap(this::checkRateLimit);
    }
    
    /**
     * Check specific permission.
     */
    public Result<Boolean, SecurityError> checkPermission(
            SecurityContext context,
            String permission) {
        
        log.debug("Checking permission: correlationId={}, permission={}", 
                context.correlationId(), permission);
        
        return authenticate(context)
            .flatMap(validContext -> authorizationService.hasPermission(validContext, permission));
    }
    
    /**
     * Validate security token.
     */
    public Result<SecurityContext, SecurityError> validateToken(String token) {
        log.debug("Validating token");
        
        return authenticationService.validateToken(token);
    }
    
    /**
     * Refresh security context.
     */
    public Result<SecurityContext, SecurityError> refreshContext(
            SecurityContext context,
            String refreshToken) {
        
        log.debug("Refreshing context: correlationId={}", context.correlationId());
        
        return authenticationService.refreshToken(context, refreshToken)
            .flatMap(newToken -> authenticationService.extractContextFromToken(newToken))
            .onSuccess(newContext -> auditService.logSecurityEvent(
                newContext, "TOKEN_REFRESHED", null));
    }
    
    /**
     * Revoke security context.
     */
    public Result<Boolean, SecurityError> revokeContext(SecurityContext context) {
        log.debug("Revoking context: correlationId={}", context.correlationId());
        
        return authenticate(context)
            .flatMap(validContext -> {
                sessionService.invalidateSession(validContext.sessionId());
                authenticationService.revokeToken(validContext.token());
                auditService.logSecurityEvent(validContext, "CONTEXT_REVOKED", null);
                return Result.<Boolean, SecurityError>success(true);
            });
    }
    
    // Private helper methods
    
    private Result<SecurityContext, SecurityError> authenticate(SecurityContext context) {
        return authenticationService.authenticate(context)
            .onSuccess(ctx -> log.debug("Authentication successful: userId={}", ctx.userId()))
            .onFailure(error -> log.warn("Authentication failed: {}", error.message()));
    }
    
    private Result<SecurityContext, SecurityError> checkRateLimit(SecurityContext context) {
        return rateLimitService.checkLimit(context)
            .onSuccess(ctx -> log.debug("Rate limit check passed: userId={}", ctx.userId()))
            .onFailure(error -> log.warn("Rate limit exceeded: {}", error.message()));
    }
    
    private Result<SecurityContext, SecurityError> authorize(SecurityContext context) {
        return authorizationService.authorize(context)
            .onSuccess(ctx -> log.debug("Authorization successful: userId={}", ctx.userId()))
            .onFailure(error -> log.warn("Authorization failed: {}", error.message()));
    }
    
    private Result<SecurityContext, SecurityError> assessRisk(SecurityContext context) {
        return riskAssessmentService.assessRisk(context)
            .flatMap(riskContext -> {
                var riskScore = riskContext.riskScore().score();
                return switch (riskContext.riskScore().level()) {
                    case CRITICAL -> Result.failure(SecurityError.riskThresholdExceeded(
                        riskScore, context.correlationId()));
                    case HIGH -> {
                        log.warn("High risk detected: score={}, correlationId={}", 
                            riskScore, context.correlationId());
                        yield Result.success(riskContext);
                    }
                    default -> Result.success(riskContext);
                };
            });
    }
    
    private Result<SecurityContext, SecurityError> validateSession(SecurityContext context) {
        return sessionService.validateSession(context)
            .onSuccess(ctx -> log.debug("Session valid: sessionId={}", ctx.sessionId()))
            .onFailure(error -> log.warn("Session validation failed: {}", error.message()));
    }
    
    private <T> Result<T, SecurityError> validateInput(T input, SecurityContext context) {
        return inputValidator.validate(input, context)
            .onSuccess(validInput -> log.debug("Input validation passed"))
            .onFailure(error -> log.warn("Input validation failed: {}", error.message()));
    }
    
    private <T> Result<T, SecurityError> executeOperation(
            SecurityContext context,
            Supplier<T> operation) {
        
        try {
            T result = operation.get();
            log.debug("Operation executed successfully: correlationId={}", 
                context.correlationId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Operation execution failed: correlationId={}", 
                context.correlationId(), e);
            return Result.failure(SecurityError.authorizationDenied(
                "Operation failed: " + e.getMessage(), context.correlationId()));
        }
    }
    
    
    private void auditSuccess(SecurityContext context, String event, Object result) {
        auditService.logSecurityEvent(context, event, result);
    }
    
    private void auditFailure(SecurityContext context, String event, SecurityError error) {
        auditService.logSecurityFailure(context, event, error);
    }
}