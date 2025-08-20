package com.trademaster.auth.repository;

import com.trademaster.auth.entity.MfaConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MfaConfiguration entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface MfaConfigurationRepository extends JpaRepository<MfaConfiguration, Long> {

    /**
     * Find MFA configurations by user ID
     */
    List<MfaConfiguration> findByUserId(Long userId);

    /**
     * Find MFA configuration by user ID and type
     */
    Optional<MfaConfiguration> findByUserIdAndMfaType(Long userId, MfaConfiguration.MfaType mfaType);

    /**
     * Find enabled MFA configurations for user
     */
    List<MfaConfiguration> findByUserIdAndIsEnabled(Long userId, boolean isEnabled);

    /**
     * Find MFA configurations by type
     */
    List<MfaConfiguration> findByMfaType(MfaConfiguration.MfaType mfaType);

    /**
     * Check if user has any enabled MFA
     */
    @Query("SELECT COUNT(m) > 0 FROM MfaConfiguration m WHERE m.userId = :userId AND m.isEnabled = true")
    boolean hasEnabledMfa(@Param("userId") Long userId);

    /**
     * Count enabled MFA configurations for user
     */
    @Query("SELECT COUNT(m) FROM MfaConfiguration m WHERE m.userId = :userId AND m.isEnabled = true")
    long countEnabledMfaForUser(@Param("userId") Long userId);

    /**
     * Find all enabled MFA configurations
     */
    List<MfaConfiguration> findByIsEnabled(boolean isEnabled);

    /**
     * Count MFA configurations by type
     */
    @Query("SELECT m.mfaType, COUNT(m) FROM MfaConfiguration m WHERE m.isEnabled = true GROUP BY m.mfaType")
    List<Object[]> countEnabledMfaByType();
}