package com.trademaster.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Security Facade - Entry point for all external access
 *
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Facade Pattern - Single entry point for external security
 * MANDATORY: Tiered Security - Full security stack for external access
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFacade {

    private final SecurityMediator securityMediator;

    /**
     * Secure external access point with full security pipeline
     *
     * MANDATORY: Zero Trust - Rule #6
     * All external access MUST go through this facade
     */
    public <T> CompletableFuture<SecurityResult<T>> secureAccess(
            SecurityContext context,
            Supplier<CompletableFuture<T>> operation) {

        log.debug("SecurityFacade: Processing secure access for correlation: {}",
            context.correlationId());

        return securityMediator.mediateAccess(context, operation);
    }

    /**
     * Synchronous secure access for simpler operations
     */
    public <T> SecurityResult<T> secureAccessSync(
            SecurityContext context,
            Function<SecurityContext, T> operation) {

        log.debug("SecurityFacade: Processing synchronous secure access for correlation: {}",
            context.correlationId());

        return securityMediator.mediateAccessSync(context, operation);
    }

    /**
     * Validate and create security context from request
     */
    public SecurityResult<SecurityContext> createSecurityContext(
            String correlationId,
            String userId,
            String ipAddress,
            String userAgent,
            String sessionId) {

        return SecurityContext.builder()
            .correlationId(correlationId)
            .userId(userId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .sessionId(sessionId)
            .timestamp(java.time.LocalDateTime.now())
            .build()
            .validate();
    }
}