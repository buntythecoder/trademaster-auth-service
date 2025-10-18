package com.trademaster.marketdata.security;

import com.trademaster.common.functional.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Security Facade for Zero Trust external access
 *
 * Single entry point for all REST API security validation.
 * Delegates to SecurityMediator for coordinated security checks.
 *
 * Usage:
 * <pre>{@code
 * // In REST Controller
 * public ResponseEntity<PriceAlertResponse> createAlert(
 *         @RequestBody PriceAlertRequest request,
 *         @AuthenticationPrincipal UserDetails userDetails,
 *         HttpServletRequest httpRequest) {
 *
 *     SecurityContext context = SecurityContext.fromUserDetails(
 *         userDetails, httpRequest.getRemoteAddr());
 *
 *     return securityFacade.secureAccess(
 *         context,
 *         () -> priceAlertService.createAlert(request, userDetails.getUsername())
 *     ).fold(
 *         response -> ResponseEntity.ok(response),
 *         error -> ResponseEntity.status(HttpStatus.FORBIDDEN)
 *             .body(PriceAlertResponse.error(error.getMessage()))
 *     );
 * }
 * }</pre>
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFacade {

    private final SecurityMediator mediator;

    /**
     * Execute operation with full zero-trust security validation
     *
     * @param context Security context with user, roles, IP, timestamp
     * @param operation The business operation to execute
     * @return Result containing either the operation result or a security error
     */
    public <T> Result<T, SecurityError> secureAccess(
            SecurityContext context,
            Supplier<T> operation) {

        log.debug("SecurityFacade: Processing secure access request for user: {}",
            context != null ? context.userId() : "null");

        return mediator.mediateAccess(context, operation);
    }

    /**
     * Execute operation with specific role requirement
     *
     * @param context Security context
     * @param requiredRole Role required for this operation
     * @param operation The business operation to execute
     * @return Result containing either the operation result or a security error
     */
    public <T> Result<T, SecurityError> secureAccessWithRole(
            SecurityContext context,
            String requiredRole,
            Supplier<T> operation) {

        log.debug("SecurityFacade: Processing secure access request with role {} for user: {}",
            requiredRole, context != null ? context.userId() : "null");

        return mediator.mediateAccessWithRole(context, requiredRole, operation);
    }

    /**
     * Execute operation with multiple role requirements (user must have ANY of the roles)
     *
     * @param context Security context
     * @param requiredRoles Roles where user must have at least one
     * @param operation The business operation to execute
     * @return Result containing either the operation result or a security error
     */
    public <T> Result<T, SecurityError> secureAccessWithAnyRole(
            SecurityContext context,
            String[] requiredRoles,
            Supplier<T> operation) {

        log.debug("SecurityFacade: Processing secure access request with roles {} for user: {}",
            java.util.Arrays.toString(requiredRoles),
            context != null ? context.userId() : "null");

        // Check if user has any of the required roles
        boolean hasRole = context != null && context.hasAnyRole(requiredRoles);

        if (!hasRole) {
            log.warn("Authorization failed: user {} lacks any of required roles {}",
                context != null ? context.userId() : "null",
                java.util.Arrays.toString(requiredRoles));
            return Result.failure(SecurityError.AUTHORIZATION_FAILED);
        }

        return mediator.mediateAccess(context, operation);
    }

    /**
     * Block an IP address from accessing the system
     */
    public void blockIpAddress(String ipAddress) {
        log.warn("SecurityFacade: Blocking IP address: {}", ipAddress);
        mediator.blockIp(ipAddress);
    }

    /**
     * Unblock a previously blocked IP address
     */
    public void unblockIpAddress(String ipAddress) {
        log.info("SecurityFacade: Unblocking IP address: {}", ipAddress);
        mediator.unblockIp(ipAddress);
    }
}
