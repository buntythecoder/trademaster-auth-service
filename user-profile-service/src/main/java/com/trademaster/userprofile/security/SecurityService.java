package com.trademaster.userprofile.security;

import com.trademaster.userprofile.entity.Permission;
import com.trademaster.userprofile.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Central security service for authorization logic and security context management.
 * Provides methods for checking permissions, roles, and user identity.
 */
@Service
@Slf4j
public class SecurityService {
    
    /**
     * Get the currently authenticated user details
     */
    public JwtUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof JwtUserDetails)) {
            throw new SecurityException("No authenticated user found");
        }
        
        return (JwtUserDetails) authentication.getPrincipal();
    }
    
    /**
     * Get the current user's ID
     */
    public UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
    
    /**
     * Get the current user's roles
     */
    public Set<Role> getCurrentUserRoles() {
        JwtUserDetails user = getCurrentUser();
        return user.getRoles().stream()
            .map(Role::valueOf)
            .collect(Collectors.toSet());
    }
    
    /**
     * Check if the current user has a specific role
     */
    public boolean hasRole(Role role) {
        return getCurrentUserRoles().contains(role);
    }
    
    /**
     * Check if the current user has any of the specified roles
     */
    public boolean hasAnyRole(Role... roles) {
        Set<Role> userRoles = getCurrentUserRoles();
        return Set.of(roles).stream().anyMatch(userRoles::contains);
    }
    
    /**
     * Check if the current user has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        Set<Role> userRoles = getCurrentUserRoles();
        return RolePermissionMapping.hasPermission(userRoles, permission);
    }
    
    /**
     * Check if the current user has any of the specified permissions
     */
    public boolean hasAnyPermission(Permission... permissions) {
        return Set.of(permissions).stream().anyMatch(this::hasPermission);
    }
    
    /**
     * Check if the current user can access another user's data
     */
    public boolean canAccessUserData(UUID targetUserId) {
        UUID currentUserId = getCurrentUserId();
        
        // Users can always access their own data
        if (currentUserId.equals(targetUserId)) {
            return true;
        }
        
        // Check if user has administrative privileges
        return hasPermission(Permission.PROFILE_READ_ANY);
    }
    
    /**
     * Check if the current user can modify another user's data
     */
    public boolean canModifyUserData(UUID targetUserId) {
        UUID currentUserId = getCurrentUserId();
        
        // Users can always modify their own data
        if (currentUserId.equals(targetUserId)) {
            return hasPermission(Permission.PROFILE_WRITE_OWN);
        }
        
        // Check if user has administrative privileges
        return hasPermission(Permission.PROFILE_WRITE_ANY);
    }
    
    /**
     * Check if the current user can delete another user's data
     */
    public boolean canDeleteUserData(UUID targetUserId) {
        UUID currentUserId = getCurrentUserId();
        
        // Users can delete their own data (subject to business rules)
        if (currentUserId.equals(targetUserId)) {
            return hasPermission(Permission.PROFILE_DELETE_OWN);
        }
        
        // Only admins can delete other users' data
        return hasPermission(Permission.PROFILE_DELETE_ANY);
    }
    
    /**
     * Check if the current user can perform KYC verification
     */
    public boolean canVerifyKyc() {
        return hasPermission(Permission.KYC_VERIFY);
    }
    
    /**
     * Check if the current user can approve KYC applications
     */
    public boolean canApproveKyc() {
        return hasPermission(Permission.KYC_APPROVE);
    }
    
    /**
     * Check if the current user can access compliance features
     */
    public boolean canAccessCompliance() {
        return RolePermissionMapping.hasCompliancePrivilege(getCurrentUserRoles());
    }
    
    /**
     * Check if the current user is an administrator
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
    
    /**
     * Check if the current user has support privileges
     */
    public boolean hasSupportPrivileges() {
        return RolePermissionMapping.hasSupportPrivilege(getCurrentUserRoles());
    }
    
    /**
     * Enforce that the current user can access the target user's data
     */
    public void enforceUserDataAccess(UUID targetUserId) {
        if (!canAccessUserData(targetUserId)) {
            log.warn("User {} attempted to access data for user {} without permission", 
                    getCurrentUserId(), targetUserId);
            throw new SecurityException("Insufficient privileges to access this user's data");
        }
    }
    
    /**
     * Enforce that the current user can modify the target user's data
     */
    public void enforceUserDataModification(UUID targetUserId) {
        if (!canModifyUserData(targetUserId)) {
            log.warn("User {} attempted to modify data for user {} without permission", 
                    getCurrentUserId(), targetUserId);
            throw new SecurityException("Insufficient privileges to modify this user's data");
        }
    }
    
    /**
     * Enforce that the current user has a specific permission
     */
    public void enforcePermission(Permission permission) {
        if (!hasPermission(permission)) {
            log.warn("User {} attempted to perform action requiring {} permission", 
                    getCurrentUserId(), permission);
            throw new SecurityException("Insufficient privileges: " + permission.getDescription());
        }
    }
    
    /**
     * Enforce that the current user has a specific role
     */
    public void enforceRole(Role role) {
        if (!hasRole(role)) {
            log.warn("User {} attempted to perform action requiring {} role", 
                    getCurrentUserId(), role);
            throw new SecurityException("Insufficient privileges: " + role.getDescription() + " role required");
        }
    }
    
    /**
     * Get debug information about the current security context
     */
    public String getSecurityContextInfo() {
        try {
            JwtUserDetails user = getCurrentUser();
            return String.format("User: %s, Roles: %s, Session: %s", 
                    user.getUserId(), user.getRoles(), user.getSessionId());
        } catch (Exception e) {
            return "No authenticated user";
        }
    }
}