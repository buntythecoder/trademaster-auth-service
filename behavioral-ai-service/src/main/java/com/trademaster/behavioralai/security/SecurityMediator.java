package com.trademaster.behavioralai.security;

import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Function;

/**
 * Security Mediator
 * 
 * Coordinates all security components for Zero Trust architecture.
 * Mediates authentication, authorization, risk assessment, and audit logging.
 */
@Component
@RequiredArgsConstructor

public final class SecurityMediator {
    private static final Logger log = LoggerFactory.getLogger(SecurityMediator.class);

    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final RiskAssessmentService riskAssessmentService;
    private final AuditService auditService;

    /**
     * Mediate secure access through complete security pipeline
     * 
     * @param context Security context
     * @param operation Operation to execute after security validation
     * @return Result with operation result or security error
     */
    public <T> Result<T, BehavioralAIError> mediateAccess(
            SecurityContext context,
            Function<SecurityContext, T> operation) {
        
        String correlationId = generateCorrelationId();
        Instant startTime = Instant.now();
        
        return authenticateUser(context, correlationId)
            .flatMap(authContext -> authorizeAccess(authContext, correlationId))
            .flatMap(authzContext -> assessRisk(authzContext, correlationId))
            .flatMap(validatedContext -> executeSecureOperation(validatedContext, operation, correlationId))
            .onSuccess(result -> auditSuccess(context, correlationId, startTime))
            .onFailure(error -> auditFailure(context, error, correlationId, startTime));
    }

    private Result<SecurityContext, BehavioralAIError> authenticateUser(
            SecurityContext context, String correlationId) {
        
        log.debug("Authenticating user request - correlationId: {}", correlationId);
        return authenticationService.authenticate(context);
    }

    private Result<SecurityContext, BehavioralAIError> authorizeAccess(
            SecurityContext context, String correlationId) {
        
        log.debug("Authorizing user access - correlationId: {}", correlationId);
        return authorizationService.authorize(context);
    }

    private Result<SecurityContext, BehavioralAIError> assessRisk(
            SecurityContext context, String correlationId) {
        
        log.debug("Assessing security risk - correlationId: {}", correlationId);
        return riskAssessmentService.assessRisk(context);
    }

    private <T> Result<T, BehavioralAIError> executeSecureOperation(
            SecurityContext context, Function<SecurityContext, T> operation, String correlationId) {
        
        return Result.tryExecute(
            () -> {
                log.debug("Executing secure operation - correlationId: {}", correlationId);
                return operation.apply(context);
            },
            ex -> {
                log.error("Secure operation failed - correlationId: {}, error: {}", correlationId, ex.getMessage());
                return BehavioralAIError.ValidationError.businessRuleViolation("operation_failed", ex.getMessage());
            }
        );
    }

    private void auditSuccess(SecurityContext context, String correlationId, Instant startTime) {
        auditService.logSecurityEvent(
            SecurityAuditEvent.success(
                correlationId,
                context.userId(),
                context.endpoint(),
                context.clientInfo(),
                startTime
            )
        );
    }

    private void auditFailure(SecurityContext context, BehavioralAIError error, 
                            String correlationId, Instant startTime) {
        auditService.logSecurityEvent(
            SecurityAuditEvent.failure(
                correlationId,
                context.userId(),
                context.endpoint(),
                error.getCode(),
                error.getErrorMessage(),
                context.clientInfo(),
                startTime
            )
        );
    }

    private String generateCorrelationId() {
        return "sec-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}