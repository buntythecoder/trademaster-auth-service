package com.trademaster.auth.entity;

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

    @Column(name = "permissions", columnDefinition = "text")
    @Builder.Default
    private String permissions = "";

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

        // Simple string-based permission check
        String permissionKey = domain + ":" + action;
        return permissions.contains(permissionKey) || permissions.contains(domain + ":*");
    }

    public void addPermission(String domain, String action) {
        permissions = Optional.ofNullable(permissions)
            .orElse("");

        String permissionKey = domain + ":" + action;
        permissions = Optional.of(permissions)
            .filter(perms -> !perms.contains(permissionKey))
            .map(perms -> perms.isEmpty() ? permissionKey : perms + ";" + permissionKey)
            .orElse(permissions);
    }

    public void removePermission(String domain, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        String permissionKey = domain + ":" + action;
        permissions = permissions.replace(permissionKey, "").replace(";;", ";").trim();
        if (permissions.startsWith(";")) {
            permissions = permissions.substring(1);
        }
        if (permissions.endsWith(";")) {
            permissions = permissions.substring(0, permissions.length() - 1);
        }
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserRole{id=%d, roleName='%s', isActive=%s}", 
                           id, roleName, isActive);
    }
}