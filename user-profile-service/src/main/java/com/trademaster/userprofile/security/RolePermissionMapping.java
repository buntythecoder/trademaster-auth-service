package com.trademaster.userprofile.security;

import com.trademaster.userprofile.entity.Permission;
import com.trademaster.userprofile.entity.Role;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines the mapping between roles and their granted permissions.
 * This centralized mapping ensures consistent permission assignment across the system.
 */
public final class RolePermissionMapping {
    
    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(Role.class);
    
    static {
        // USER role permissions - basic self-service operations
        ROLE_PERMISSIONS.put(Role.USER, Set.of(
            Permission.PROFILE_READ_OWN,
            Permission.PROFILE_WRITE_OWN,
            Permission.PROFILE_DELETE_OWN,
            Permission.KYC_READ_OWN,
            Permission.KYC_WRITE_OWN,
            Permission.DOCUMENT_READ_OWN,
            Permission.DOCUMENT_UPLOAD_OWN,
            Permission.DOCUMENT_DELETE_OWN,
            Permission.TRADING_BASIC
        ));
        
        // PREMIUM_USER role permissions - includes USER permissions plus premium features
        ROLE_PERMISSIONS.put(Role.PREMIUM_USER, Set.of(
            Permission.PROFILE_READ_OWN,
            Permission.PROFILE_WRITE_OWN,
            Permission.PROFILE_DELETE_OWN,
            Permission.KYC_READ_OWN,
            Permission.KYC_WRITE_OWN,
            Permission.DOCUMENT_READ_OWN,
            Permission.DOCUMENT_UPLOAD_OWN,
            Permission.DOCUMENT_DELETE_OWN,
            Permission.TRADING_BASIC,
            Permission.TRADING_ADVANCED,
            Permission.TRADING_MARGIN,
            Permission.TRADING_DERIVATIVES
        ));
        
        // SUPPORT_AGENT role permissions - can read user data for assistance
        ROLE_PERMISSIONS.put(Role.SUPPORT_AGENT, Set.of(
            Permission.PROFILE_READ_OWN,
            Permission.PROFILE_WRITE_OWN,
            Permission.PROFILE_DELETE_OWN,
            Permission.KYC_READ_OWN,
            Permission.KYC_WRITE_OWN,
            Permission.DOCUMENT_READ_OWN,
            Permission.DOCUMENT_UPLOAD_OWN,
            Permission.DOCUMENT_DELETE_OWN,
            Permission.TRADING_BASIC,
            Permission.PROFILE_READ_ANY,
            Permission.KYC_READ_ANY,
            Permission.DOCUMENT_READ_ANY,
            Permission.USER_SEARCH
        ));
        
        // COMPLIANCE_OFFICER role permissions - compliance and KYC management
        ROLE_PERMISSIONS.put(Role.COMPLIANCE_OFFICER, Set.of(
            Permission.PROFILE_READ_OWN,
            Permission.PROFILE_WRITE_OWN,
            Permission.PROFILE_DELETE_OWN,
            Permission.KYC_READ_OWN,
            Permission.KYC_WRITE_OWN,
            Permission.DOCUMENT_READ_OWN,
            Permission.DOCUMENT_UPLOAD_OWN,
            Permission.DOCUMENT_DELETE_OWN,
            Permission.TRADING_BASIC,
            Permission.PROFILE_READ_ANY,
            Permission.KYC_READ_ANY,
            Permission.KYC_VERIFY,
            Permission.KYC_APPROVE,
            Permission.KYC_REJECT,
            Permission.DOCUMENT_READ_ANY,
            Permission.DOCUMENT_VERIFY,
            Permission.USER_SEARCH,
            Permission.USER_STATISTICS,
            Permission.COMPLIANCE_REPORTS,
            Permission.COMPLIANCE_AUDIT,
            Permission.COMPLIANCE_INVESTIGATION
        ));
        
        // ADMIN role permissions - full system access
        ROLE_PERMISSIONS.put(Role.ADMIN, Set.of(
            // All permissions
            Permission.values()
        ));
    }
    
    /**
     * Get all permissions granted to a specific role
     */
    public static Set<Permission> getPermissionsForRole(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
    
    /**
     * Check if a role has a specific permission
     */
    public static boolean roleHasPermission(Role role, Permission permission) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission);
    }
    
    /**
     * Check if any of the given roles has the specified permission
     */
    public static boolean hasPermission(Set<Role> roles, Permission permission) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        
        return roles.stream()
            .anyMatch(role -> roleHasPermission(role, permission));
    }
    
    /**
     * Get all permissions for a set of roles (union of all permissions)
     */
    public static Set<Permission> getAllPermissions(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        
        return roles.stream()
            .flatMap(role -> getPermissionsForRole(role).stream())
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Check if roles include admin privilege
     */
    public static boolean hasAdminPrivilege(Set<Role> roles) {
        return roles != null && roles.contains(Role.ADMIN);
    }
    
    /**
     * Check if roles include compliance privilege
     */
    public static boolean hasCompliancePrivilege(Set<Role> roles) {
        return roles != null && (roles.contains(Role.COMPLIANCE_OFFICER) || roles.contains(Role.ADMIN));
    }
    
    /**
     * Check if roles include support privilege
     */
    public static boolean hasSupportPrivilege(Set<Role> roles) {
        return roles != null && (roles.contains(Role.SUPPORT_AGENT) || 
                                roles.contains(Role.COMPLIANCE_OFFICER) || 
                                roles.contains(Role.ADMIN));
    }
    
    private RolePermissionMapping() {
        // Utility class - prevent instantiation
    }
}