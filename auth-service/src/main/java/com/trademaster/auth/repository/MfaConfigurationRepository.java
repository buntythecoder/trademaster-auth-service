package com.trademaster.auth.repository;

import com.trademaster.auth.entity.MfaConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for MfaConfiguration entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface MfaConfigurationRepository extends JpaRepository<MfaConfiguration, UUID> {

    /**
     * Find MFA configurations by user ID
     */
    List<MfaConfiguration> findByUserId(String userId);

    /**
     * Find MFA configuration by user ID and type
     */
    Optional<MfaConfiguration> findByUserIdAndMfaType(String userId, MfaConfiguration.MfaType mfaType);

    /**
     * Find enabled MFA configurations for user
     */
    List<MfaConfiguration> findByUserIdAndEnabled(String userId, boolean enabled);

    /**
     * Find MFA configurations by type
     */
    List<MfaConfiguration> findByMfaType(MfaConfiguration.MfaType mfaType);

    /**
     * Check if user has any enabled MFA
     */
    boolean existsByUserIdAndMfaTypeAndEnabled(String userId, MfaConfiguration.MfaType mfaType, boolean enabled);

    /**
     * Find all enabled MFA configurations
     */
    List<MfaConfiguration> findByEnabledTrue();

    /**
     * Find enabled configurations for user
     */
    @Query("SELECT m FROM MfaConfiguration m WHERE m.userId = :userId AND m.enabled = true")
    List<MfaConfiguration> findEnabledConfigurationsForUser(@Param("userId") String userId);

    /**
     * Find locked configurations (failed attempts >= 3)
     */
    @Query("SELECT m FROM MfaConfiguration m WHERE m.failedAttempts >= 3")
    List<MfaConfiguration> findLockedConfigurations();

    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE MfaConfiguration m SET m.lastUsed = :timestamp WHERE m.id = :id")
    void updateLastUsed(@Param("id") UUID id, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Reset failed attempts
     */
    @Modifying
    @Query("UPDATE MfaConfiguration m SET m.failedAttempts = 0 WHERE m.id = :id")
    void resetFailedAttempts(@Param("id") UUID id);

    /**
     * Increment failed attempts
     */
    @Modifying
    @Query("UPDATE MfaConfiguration m SET m.failedAttempts = m.failedAttempts + 1 WHERE m.id = :id")
    void incrementFailedAttempts(@Param("id") UUID id);

    /**
     * Count enabled MFA configurations for user
     */
    @Query("SELECT COUNT(m) FROM MfaConfiguration m WHERE m.userId = :userId AND m.enabled = true")
    long countEnabledConfigurationsForUser(@Param("userId") String userId);

    /**
     * Find stale configurations (not used recently)
     */
    @Query("SELECT m FROM MfaConfiguration m WHERE m.lastUsed < :cutoffDate AND m.enabled = true")
    List<MfaConfiguration> findStaleConfigurations(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete all configurations for user
     */
    void deleteByUserId(String userId);
}