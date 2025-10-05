package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User Role Assignment entity for mapping users to roles
 * 
 * Supports:
 * - Temporary role assignments with expiration
 * - Assignment tracking and audit trail
 * - Active/inactive status management
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_role_assignments", 
       indexes = {
           @Index(name = "idx_user_role_assignments_user_id", columnList = "userId"),
           @Index(name = "idx_user_role_assignments_role_id", columnList = "roleId"),
           @Index(name = "idx_user_role_assignments_active", columnList = "isActive")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "role_id"})
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @CreatedDate
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by", length = 100)
    @Builder.Default
    private String assignedBy = "system";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", referencedColumnName = "id", insertable = false, updatable = false)
    private UserRole role;

    // Business logic methods
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void extend(int days) {
        if (expiresAt != null) {
            this.expiresAt = expiresAt.plusDays(days);
        } else {
            this.expiresAt = LocalDateTime.now().plusDays(days);
        }
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserRoleAssignment{id=%d, userId=%d, roleId=%d, roleName='%s', isActive=%s, expiresAt=%s}", 
                           id, userId, roleId, 
                           role != null ? role.getRoleName() : "unknown", 
                           isActive, expiresAt);
    }
}