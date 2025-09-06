package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Authorization Service - Implements role-based access control (RBAC).
 * Handles permission checks and access control decisions.
 */
@Slf4j
@Service
public class AuthorizationService {
    
    // Role hierarchy: higher roles inherit permissions from lower roles
    private final Map<String, Set<String>> roleHierarchy = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();
    private final Map<String, Predicate<SecurityContext>> customPolicies = new ConcurrentHashMap<>();
    
    private final boolean strictMode;
    
    public AuthorizationService(@Value("${security.authorization.strict-mode:true}") boolean strictMode) {
        this.strictMode = strictMode;
        initializeRoleHierarchy();
        initializePermissions();
        initializeCustomPolicies();
    }
    
    /**
     * Authorize security context for general access.
     */
    public Result<SecurityContext, SecurityError> authorize(SecurityContext context) {
        log.debug("Authorizing context: userId={}, roles={}", 
            context.userId(), context.roles());
        
        // Check if user has any valid role
        if (context.roles().isEmpty()) {
            log.warn("No roles found for user: userId={}", context.userId());
            return Result.failure(SecurityError.authorizationDenied(
                "No roles assigned", context.correlationId()));
        }
        
        // Check if any role is active
        boolean hasActiveRole = context.roles().stream()
            .anyMatch(role -> rolePermissions.containsKey(role));
        
        if (!hasActiveRole && strictMode) {
            log.warn("No active roles found: userId={}, roles={}", 
                context.userId(), context.roles());
            return Result.failure(SecurityError.authorizationDenied(
                "No active roles", context.correlationId()));
        }
        
        return Result.success(context);
    }
    
    /**
     * Check if context has specific permission.
     */
    public Result<Boolean, SecurityError> hasPermission(
            SecurityContext context, 
            String permission) {
        
        log.debug("Checking permission: userId={}, permission={}", 
            context.userId(), permission);
        
        // Check direct permissions first
        if (context.permissions().contains(permission)) {
            return Result.success(true);
        }
        
        // Check role-based permissions
        Set<String> allPermissions = getAllPermissions(context.roles());
        boolean hasPermission = allPermissions.contains(permission) ||
            checkWildcardPermission(allPermissions, permission);
        
        if (!hasPermission) {
            log.debug("Permission denied: userId={}, permission={}", 
                context.userId(), permission);
            return strictMode 
                ? Result.failure(SecurityError.authorizationDenied(
                    "Permission denied: " + permission, context.correlationId()))
                : Result.success(false);
        }
        
        return Result.success(true);
    }
    
    /**
     * Check if context has all required permissions.
     */
    public Result<Boolean, SecurityError> hasAllPermissions(
            SecurityContext context,
            Set<String> requiredPermissions) {
        
        log.debug("Checking multiple permissions: userId={}, required={}", 
            context.userId(), requiredPermissions);
        
        Set<String> allPermissions = getAllPermissions(context.roles());
        allPermissions.addAll(context.permissions());
        
        boolean hasAll = requiredPermissions.stream()
            .allMatch(perm -> allPermissions.contains(perm) || 
                checkWildcardPermission(allPermissions, perm));
        
        if (!hasAll) {
            Set<String> missing = requiredPermissions.stream()
                .filter(perm -> !allPermissions.contains(perm) && 
                    !checkWildcardPermission(allPermissions, perm))
                .collect(java.util.stream.Collectors.toSet());
            
            log.debug("Missing permissions: userId={}, missing={}", 
                context.userId(), missing);
            
            return strictMode
                ? Result.failure(SecurityError.authorizationDenied(
                    "Missing permissions: " + missing, context.correlationId()))
                : Result.success(false);
        }
        
        return Result.success(true);
    }
    
    /**
     * Check if context has any of the required permissions.
     */
    public Result<Boolean, SecurityError> hasAnyPermission(
            SecurityContext context,
            Set<String> permissions) {
        
        log.debug("Checking any permission: userId={}, permissions={}", 
            context.userId(), permissions);
        
        Set<String> allPermissions = getAllPermissions(context.roles());
        allPermissions.addAll(context.permissions());
        
        boolean hasAny = permissions.stream()
            .anyMatch(perm -> allPermissions.contains(perm) || 
                checkWildcardPermission(allPermissions, perm));
        
        if (!hasAny && strictMode) {
            return Result.failure(SecurityError.authorizationDenied(
                "No matching permissions", context.correlationId()));
        }
        
        return Result.success(hasAny);
    }
    
    /**
     * Check if context has specific role.
     */
    public Result<Boolean, SecurityError> hasRole(
            SecurityContext context,
            String role) {
        
        log.debug("Checking role: userId={}, role={}", context.userId(), role);
        
        // Check direct role
        if (context.roles().contains(role)) {
            return Result.success(true);
        }
        
        // Check inherited roles
        boolean hasRole = context.roles().stream()
            .anyMatch(userRole -> {
                Set<String> inherited = roleHierarchy.get(userRole);
                return inherited != null && inherited.contains(role);
            });
        
        return Result.success(hasRole);
    }
    
    /**
     * Apply custom policy check.
     */
    public Result<Boolean, SecurityError> checkPolicy(
            SecurityContext context,
            String policyName) {
        
        log.debug("Checking policy: userId={}, policy={}", 
            context.userId(), policyName);
        
        Predicate<SecurityContext> policy = customPolicies.get(policyName);
        if (policy == null) {
            log.warn("Policy not found: {}", policyName);
            return Result.success(true); // Default allow if policy not found
        }
        
        try {
            boolean allowed = policy.test(context);
            if (!allowed) {
                log.debug("Policy check failed: userId={}, policy={}", 
                    context.userId(), policyName);
                return strictMode
                    ? Result.failure(SecurityError.authorizationDenied(
                        "Policy check failed: " + policyName, context.correlationId()))
                    : Result.success(false);
            }
            return Result.success(true);
        } catch (Exception e) {
            log.error("Policy check error: policy={}", policyName, e);
            return Result.failure(SecurityError.authorizationDenied(
                "Policy check error", context.correlationId()));
        }
    }
    
    /**
     * Get effective permissions for a set of roles.
     */
    public Set<String> getEffectivePermissions(Set<String> roles) {
        return getAllPermissions(roles);
    }
    
    // Private helper methods
    
    private void initializeRoleHierarchy() {
        // Admin inherits from all roles
        roleHierarchy.put("ADMIN", Set.of("USER", "TRADER", "ANALYST", "MANAGER"));
        
        // Manager inherits from trader and analyst
        roleHierarchy.put("MANAGER", Set.of("USER", "TRADER", "ANALYST"));
        
        // Trader and Analyst inherit from user
        roleHierarchy.put("TRADER", Set.of("USER"));
        roleHierarchy.put("ANALYST", Set.of("USER"));
        
        // Service accounts
        roleHierarchy.put("SERVICE", Set.of());
        roleHierarchy.put("AGENT", Set.of());
    }
    
    private void initializePermissions() {
        // Admin permissions
        rolePermissions.put("ADMIN", Set.of(
            "*", // All permissions
            "admin:*",
            "system:*"
        ));
        
        // Manager permissions
        rolePermissions.put("MANAGER", Set.of(
            "agent:create",
            "agent:read",
            "agent:update",
            "agent:delete",
            "agent:manage",
            "task:*",
            "report:*",
            "analytics:*"
        ));
        
        // Trader permissions
        rolePermissions.put("TRADER", Set.of(
            "agent:create",
            "agent:read",
            "agent:update",
            "task:create",
            "task:read",
            "task:update",
            "order:*",
            "portfolio:*"
        ));
        
        // Analyst permissions
        rolePermissions.put("ANALYST", Set.of(
            "agent:read",
            "task:read",
            "report:read",
            "report:create",
            "analytics:read",
            "data:read"
        ));
        
        // User permissions
        rolePermissions.put("USER", Set.of(
            "agent:read",
            "task:read",
            "profile:read",
            "profile:update"
        ));
        
        // Service permissions
        rolePermissions.put("SERVICE", Set.of(
            "service:*",
            "agent:*",
            "task:*",
            "internal:*"
        ));
        
        // Agent permissions
        rolePermissions.put("AGENT", Set.of(
            "agent:communicate",
            "task:execute",
            "data:read",
            "data:write"
        ));
    }
    
    private void initializeCustomPolicies() {
        // Time-based access policy
        customPolicies.put("business-hours", context -> {
            int hour = java.time.LocalTime.now().getHour();
            return hour >= 7 && hour <= 20; // 7 AM to 8 PM
        });
        
        // IP-based access policy
        customPolicies.put("internal-network", context -> {
            String ip = context.ipAddress();
            return ip != null && (ip.startsWith("10.") || 
                ip.startsWith("192.168.") || ip.equals("127.0.0.1"));
        });
        
        // Security level policy
        customPolicies.put("elevated-security", context -> 
            context.securityLevel().isHigherThan(SecurityContext.SecurityLevel.STANDARD)
        );
        
        // Risk-based policy
        customPolicies.put("low-risk", context -> 
            context.riskScore().score() < 0.3
        );
    }
    
    private Set<String> getAllPermissions(Set<String> roles) {
        Set<String> allPermissions = new HashSet<>();
        
        for (String role : roles) {
            // Add direct role permissions
            Set<String> directPerms = rolePermissions.get(role);
            if (directPerms != null) {
                allPermissions.addAll(directPerms);
            }
            
            // Add inherited role permissions
            Set<String> inherited = roleHierarchy.get(role);
            if (inherited != null) {
                for (String inheritedRole : inherited) {
                    Set<String> inheritedPerms = rolePermissions.get(inheritedRole);
                    if (inheritedPerms != null) {
                        allPermissions.addAll(inheritedPerms);
                    }
                }
            }
        }
        
        return allPermissions;
    }
    
    private boolean checkWildcardPermission(Set<String> permissions, String permission) {
        // Check for wildcard permissions
        if (permissions.contains("*")) {
            return true;
        }
        
        // Check for partial wildcards (e.g., "agent:*" matches "agent:create")
        String[] parts = permission.split(":");
        if (parts.length > 0) {
            String wildcardPerm = parts[0] + ":*";
            return permissions.contains(wildcardPerm);
        }
        
        return false;
    }
}