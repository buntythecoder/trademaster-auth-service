package com.trademaster.pnlengine.security;

import com.trademaster.pnlengine.common.functional.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SecurityFacade for Zero Trust Architecture
 * 
 * MANDATORY: Java 24 + Zero Trust Security + Functional Programming
 * 
 * Single entry point for all external security operations following
 * Zero Trust principles. All external requests must go through this facade.
 * 
 * Security Layers:
 * 1. Authentication validation
 * 2. Authorization checking  
 * 3. Risk assessment
 * 4. Audit logging
 * 5. Operation execution
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Security Refactoring)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public final class SecurityFacade {
    
    private final SecurityMediator securityMediator;
    
    // ============================================================================
    // EXTERNAL ACCESS METHODS (ZERO TRUST ENFORCEMENT)
    // ============================================================================
    
    /**
     * Execute operation with full security validation
     * MANDATORY for ALL external API calls
     */
    public <T> Result<T, SecurityError> executeSecurely(
            SecurityContext context,
            Function<SecurityContext, Result<T, SecurityError>> operation) {
        
        return securityMediator.validateAndExecute(context, operation);
    }
    
    /**
     * Execute operation with specific security level requirement
     */
    public <T> Result<T, SecurityError> executeWithLevel(
            SecurityContext context,
            SecurityContext.SecurityLevel requiredLevel,
            Function<SecurityContext, Result<T, SecurityError>> operation) {
        
        return context.isValidFor(requiredLevel) ?
            securityMediator.validateAndExecute(context, operation) :
            Result.failure(new SecurityError.AuthorizationFailed(
                requiredLevel.name(), "level_access", context.userId(), requiredLevel, 
                java.time.Instant.now()));
    }
    
    /**
     * Execute operation with authority requirement
     */
    public <T> Result<T, SecurityError> executeWithAuthority(
            SecurityContext context,
            String requiredAuthority,
            Function<SecurityContext, Result<T, SecurityError>> operation) {
        
        return context.hasAuthority(requiredAuthority) ?
            securityMediator.validateAndExecute(context, operation) :
            Result.failure(new SecurityError.AuthorizationFailed(
                requiredAuthority, "", context.userId(), context.securityLevel(),
                java.time.Instant.now()));
    }
    
    /**
     * Execute operation with scope requirement
     */
    public <T> Result<T, SecurityError> executeWithScope(
            SecurityContext context,
            String requiredScope,
            Function<SecurityContext, Result<T, SecurityError>> operation) {
        
        return context.hasScope(requiredScope) ?
            securityMediator.validateAndExecute(context, operation) :
            Result.failure(new SecurityError.AuthorizationFailed(
                "", requiredScope, context.userId(), context.securityLevel(),
                java.time.Instant.now()));
    }
    
    /**
     * Execute privileged operation (requires elevated permissions)
     */
    public <T> Result<T, SecurityError> executePrivileged(
            SecurityContext context,
            String operation,
            Function<SecurityContext, Result<T, SecurityError>> privilegedOperation) {
        
        return executeWithLevel(context, SecurityContext.SecurityLevel.PRIVILEGED, 
            validatedContext -> {
                log.info("Executing privileged operation '{}' for user: {}", operation, context.userId());
                return privilegedOperation.apply(validatedContext);
            });
    }
    
    /**
     * Execute system-level operation (highest security level)
     */
    public <T> Result<T, SecurityError> executeSystem(
            SecurityContext context,
            String operation,
            Function<SecurityContext, Result<T, SecurityError>> systemOperation) {
        
        return executeWithLevel(context, SecurityContext.SecurityLevel.SYSTEM,
            validatedContext -> {
                log.info("Executing system operation '{}' for context: {}", operation, context.correlationId());
                return systemOperation.apply(validatedContext);
            });
    }
    
    // ============================================================================
    // CONVENIENCE METHODS FOR COMMON PATTERNS
    // ============================================================================
    
    /**
     * Execute operation with automatic error handling
     */
    public <T> Result<T, SecurityError> executeWithHandling(
            SecurityContext context,
            Supplier<T> operation) {
        
        return executeSecurely(context, ctx -> {
            try {
                var result = operation.get();
                return Result.success(result);
            } catch (Exception e) {
                log.error("Operation failed for user: {}", ctx.userId(), e);
                return Result.failure(new SecurityError.SystemSecurityError(
                    "operation_execution", "EXECUTION_ERROR", e.getMessage(), 
                    java.time.Instant.now()));
            }
        });
    }
    
    /**
     * Execute operation with result transformation
     */
    public <T, U> Result<U, SecurityError> executeAndTransform(
            SecurityContext context,
            Supplier<T> operation,
            Function<T, U> transformer) {
        
        return executeWithHandling(context, operation)
            .map(transformer);
    }
    
    /**
     * Execute operation with validation
     */
    public <T> Result<T, SecurityError> executeWithValidation(
            SecurityContext context,
            Supplier<T> operation,
            Function<T, Boolean> validator,
            String validationError) {
        
        return executeWithHandling(context, operation)
            .flatMap(result -> validator.apply(result) ?
                Result.success(result) :
                Result.failure(new SecurityError.InputValidationFailed(
                    "operation_result", validationError, "N/A", java.time.Instant.now())));
    }
}