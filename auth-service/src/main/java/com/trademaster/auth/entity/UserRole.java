package com.trademaster.auth.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User Role entity for Role-Based Access Control (RBAC)
 * 
 * Defines roles with associated permissions for fine-grained access control.
 * Supports hierarchical permissions and dynamic role assignment.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_roles")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Role name is required")
    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonType.class)
    @Column(name = "permissions", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> permissions = Map.of();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRoleAssignment> assignments = new HashSet<>();

    // Business logic methods
    public boolean hasPermission(String domain, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> domainPermissions = (Map<String, Object>) permissions.get(domain);
        
        if (domainPermissions == null) {
            return false;
        }
        
        if (domainPermissions.containsKey("*")) {
            return true; // Wildcard permission
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<String> actions = (java.util.List<String>) domainPermissions.get("actions");
        
        return actions != null && actions.contains(action);
    }

    public void addPermission(String domain, String action) {
        if (permissions == null) {
            permissions = new java.util.HashMap<>();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> domainPerms = (Map<String, Object>) permissions.computeIfAbsent(domain, k -> new java.util.HashMap<>());
        
        @SuppressWarnings("unchecked")
        java.util.List<String> actions = (java.util.List<String>) domainPerms.computeIfAbsent("actions", k -> new java.util.ArrayList<>());
        
        if (!actions.contains(action)) {
            actions.add(action);
        }
    }

    public void removePermission(String domain, String action) {
        if (permissions == null || !permissions.containsKey(domain)) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> domainPerms = (Map<String, Object>) permissions.get(domain);
        
        @SuppressWarnings("unchecked")
        java.util.List<String> actions = (java.util.List<String>) domainPerms.get("actions");
        
        if (actions != null) {
            actions.remove(action);
            if (actions.isEmpty()) {
                permissions.remove(domain);
            }
        }
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserRole{id=%d, roleName='%s', isActive=%s}", 
                           id, roleName, isActive);
    }
}