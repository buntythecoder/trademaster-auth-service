package com.trademaster.auth.repository;

import com.trademaster.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRole entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Find role by name
     */
    Optional<UserRole> findByRoleName(String roleName);

    /**
     * Find active roles
     */
    List<UserRole> findByIsActive(boolean isActive);

    /**
     * Check if role exists by name
     */
    boolean existsByRoleName(String roleName);
}