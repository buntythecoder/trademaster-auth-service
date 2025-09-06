package com.trademaster.agentos.security.facade;

import com.trademaster.agentos.security.mediator.SecurityMediator;
import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Security Facade - Single entry point for all external access.
 * Implements Zero Trust architecture with default-deny policy.
 * 
 * This is the MANDATORY security boundary for all external API calls.
 * Internal service-to-service calls should NOT use this facade.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFacade {
    
    private final SecurityMediator securityMediator;
    
    /**
     * Secure synchronous access with full security stack.
     * 
     * @param context Security context with authentication/authorization info
     * @param operation The protected operation to execute
     * @return Result containing either the operation result or security error
     */
    public <T> Result<T, SecurityError> secureAccess(
            SecurityContext context,
            Supplier<T> operation) {
        
        log.debug("Security facade processing request: correlationId={}, userId={}", 
                context.correlationId(), context.userId());
        
        // Delegate to mediator for coordinated security checks
        return securityMediator.mediateAccess(context, operation);
    }
    
    /**
     * Secure access with transformation.
     * 
     * @param context Security context
     * @param input Input data for the operation
     * @param operation Protected operation with input
     * @return Result containing either the operation result or security error
     */
    public <T, R> Result<R, SecurityError> secureAccessWithInput(
            SecurityContext context,
            T input,
            Function<T, R> operation) {
        
        log.debug("Security facade processing request with input: correlationId={}", 
                context.correlationId());
        
        // Delegate to mediator with input transformation
        return securityMediator.mediateAccessWithInput(context, input, operation);
    }
    
    /**
     * Secure asynchronous access using virtual threads.
     * 
     * @param context Security context
     * @param asyncOperation Async protected operation
     * @return CompletableFuture with security result
     */
    public <T> CompletableFuture<Result<T, SecurityError>> secureAsyncAccess(
            SecurityContext context,
            Supplier<CompletableFuture<T>> asyncOperation) {
        
        log.debug("Security facade processing async request: correlationId={}", 
                context.correlationId());
        
        // Use virtual threads for async security processing
        return CompletableFuture.supplyAsync(() -> 
            securityMediator.mediateAsyncAccess(context, asyncOperation)
        ).thenCompose(Function.identity());
    }
    
    /**
     * Secure batch access with structured concurrency.
     * Processes multiple operations with shared security context.
     * 
     * @param context Security context
     * @param operations Multiple operations to execute
     * @return Result containing all operation results or security error
     */
    public <T> Result<T, SecurityError> secureBatchAccess(
            SecurityContext context,
            BatchOperations<T> operations) {
        
        log.debug("Security facade processing batch request: correlationId={}, operations={}", 
                context.correlationId(), operations.count());
        
        // Validate security context first
        var validationResult = securityMediator.validateContext(context);
        
        return validationResult.flatMap(validContext -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                var results = operations.execute(scope, validContext);
                
                scope.join();
                scope.throwIfFailed();
                
                return Result.success(results);
            } catch (Exception e) {
                log.error("Batch operation failed: correlationId={}", context.correlationId(), e);
                return Result.failure(SecurityError.authorizationDenied(
                    "Batch operation failed", context.correlationId()));
            }
        });
    }
    
    /**
     * Check if a security context has specific permission.
     * 
     * @param context Security context
     * @param permission Required permission
     * @return Result indicating if permission is granted
     */
    public Result<Boolean, SecurityError> checkPermission(
            SecurityContext context,
            String permission) {
        
        log.debug("Checking permission: correlationId={}, permission={}", 
                context.correlationId(), permission);
        
        return securityMediator.checkPermission(context, permission);
    }
    
    /**
     * Validate security token without executing operation.
     * 
     * @param token Security token to validate
     * @return Result containing security context or error
     */
    public Result<SecurityContext, SecurityError> validateToken(String token) {
        log.debug("Validating security token");
        
        return securityMediator.validateToken(token);
    }
    
    /**
     * Refresh security context with new token.
     * 
     * @param context Current security context
     * @param refreshToken Refresh token
     * @return Result containing new security context or error
     */
    public Result<SecurityContext, SecurityError> refreshContext(
            SecurityContext context,
            String refreshToken) {
        
        log.debug("Refreshing security context: correlationId={}", context.correlationId());
        
        return securityMediator.refreshContext(context, refreshToken);
    }
    
    /**
     * Revoke security context (logout).
     * 
     * @param context Security context to revoke
     * @return Result indicating success or error
     */
    public Result<Boolean, SecurityError> revokeContext(SecurityContext context) {
        log.debug("Revoking security context: correlationId={}, userId={}", 
                context.correlationId(), context.userId());
        
        return securityMediator.revokeContext(context);
    }
    
    /**
     * Interface for batch operations execution.
     */
    @FunctionalInterface
    public interface BatchOperations<T> {
        T execute(StructuredTaskScope<Object> scope, SecurityContext context);
        
        default int count() {
            return 1;
        }
    }
}