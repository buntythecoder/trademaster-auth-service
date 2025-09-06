package com.trademaster.behavioralai.security;

import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Security Facade
 * 
 * Zero Trust security facade following TradeMaster standards.
 * Provides secure access control for external API requests.
 */
@Component
@RequiredArgsConstructor

public final class SecurityFacade {
    private static final Logger log = LoggerFactory.getLogger(SecurityFacade.class);

    private final SecurityMediator securityMediator;

    /**
     * Secure access control for external API operations
     * 
     * @param context Security context containing authentication/authorization info
     * @param operation Function to execute after security validation
     * @return Result with operation result or security error
     */
    public <T> Result<T, BehavioralAIError> secureAccess(
            SecurityContext context,
            Function<SecurityContext, T> operation) {
        
        return securityMediator.mediateAccess(context, operation);
    }

    /**
     * Secure access with supplier operation (no context needed)
     * 
     * @param context Security context
     * @param operation Supplier operation to execute
     * @return Result with operation result or security error
     */
    public <T> Result<T, BehavioralAIError> secureAccess(
            SecurityContext context,
            Supplier<T> operation) {
        
        return securityMediator.mediateAccess(context, ctx -> operation.get());
    }
}