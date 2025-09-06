package com.trademaster.behavioralai.security;

import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Authorization Service
 * 
 * Handles authorization decisions for behavioral AI service access.
 * Implements fine-grained permission checking.
 */
@Service
@RequiredArgsConstructor

public final class AuthorizationService {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    // Endpoint to required permission mapping
    private static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
        "/api/v1/emotion/analyze", "behavioral_ai:read",
        "/api/v1/patterns/detect", "behavioral_ai:read", 
        "/api/v1/coaching/trigger", "behavioral_ai:write",
        "/api/v1/psychology-profile", "behavioral_ai:read",
        "/api/v1/admin/analytics", "behavioral_ai:admin"
    );

    /**
     * Authorize user access based on security context
     * 
     * @param context Security context with authentication info
     * @return Result with authorized context or error
     */
    public Result<SecurityContext, BehavioralAIError> authorize(SecurityContext context) {
        return checkUserPermissions(context)
            .flatMap(this::checkEndpointAccess)
            .flatMap(this::checkResourceAccess);
    }

    private Result<SecurityContext, BehavioralAIError> checkUserPermissions(SecurityContext context) {
        if (!context.isAuthenticated()) {
            return Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "authentication_required", "User must be authenticated"));
        }

        String requiredPermission = ENDPOINT_PERMISSIONS.get(context.endpoint());
        
        if (requiredPermission != null && !context.hasPermission(requiredPermission)) {
            return Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "insufficient_permissions", 
                "User lacks required permission: " + requiredPermission));
        }

        return Result.success(context);
    }

    private Result<SecurityContext, BehavioralAIError> checkEndpointAccess(SecurityContext context) {
        // Check if endpoint is accessible with current authentication level
        if (isAdminEndpoint(context.endpoint()) && !hasAdminRole(context)) {
            return Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "admin_required", "Admin role required for this endpoint"));
        }

        return Result.success(context);
    }

    private Result<SecurityContext, BehavioralAIError> checkResourceAccess(SecurityContext context) {
        // Check if user can access the specific resource (e.g., user's own data)
        String requestedUserId = (String) context.requestMetadata().get("requestedUserId");
        
        if (requestedUserId != null && !canAccessUserData(context, requestedUserId)) {
            return Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "resource_access_denied", "Cannot access other user's data"));
        }

        return Result.success(context);
    }

    private boolean isAdminEndpoint(String endpoint) {
        return endpoint.contains("/admin/") || endpoint.contains("/analytics");
    }

    private boolean hasAdminRole(SecurityContext context) {
        return context.authInfo() != null && 
               context.authInfo().roles().contains("ADMIN");
    }

    private boolean canAccessUserData(SecurityContext context, String requestedUserId) {
        // Users can access their own data, admins can access any data
        return context.userId().equals(requestedUserId) || hasAdminRole(context);
    }
}