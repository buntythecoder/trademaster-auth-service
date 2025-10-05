package com.trademaster.auth.repository;

import com.trademaster.auth.entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRoleAssignment entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {

    /**
     * Find assignments by user ID
     */
    List<UserRoleAssignment> findByUserId(Long userId);

    /**
     * Find active assignments by user ID
     */
    List<UserRoleAssignment> findByUserIdAndIsActive(Long userId, boolean isActive);

    /**
     * Find assignment by user ID and role ID
     */
    Optional<UserRoleAssignment> findByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Find assignments by role ID
     */
    List<UserRoleAssignment> findByRoleId(Long roleId);

    /**
     * Check if user has specific role
     */
    @Query("SELECT COUNT(ura) > 0 FROM UserRoleAssignment ura JOIN ura.role r WHERE ura.userId = :userId AND r.roleName = :roleName AND ura.isActive = true")
    boolean hasRole(@Param("userId") Long userId, @Param("roleName") String roleName);
}