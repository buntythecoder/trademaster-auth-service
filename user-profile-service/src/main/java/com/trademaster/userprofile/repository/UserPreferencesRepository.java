package com.trademaster.userprofile.repository;

import com.trademaster.userprofile.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreferences operations
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    /**
     * Find preferences by user profile ID
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.userProfile.id = :userProfileId")
    Optional<UserPreferences> findByUserProfileId(@Param("userProfileId") UUID userProfileId);

    /**
     * Find preferences by user email
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.userProfile.email = :email")
    Optional<UserPreferences> findByUserEmail(@Param("email") String email);

    /**
     * Check if preferences exist for user profile
     */
    @Query("SELECT COUNT(up) > 0 FROM UserPreferences up WHERE up.userProfile.id = :userProfileId")
    boolean existsByUserProfileId(@Param("userProfileId") UUID userProfileId);

    /**
     * Delete preferences by user profile ID
     */
    void deleteByUserProfileId(UUID userProfileId);

    /**
     * Find all preferences with specific theme
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.theme = :theme")
    java.util.List<UserPreferences> findByTheme(@Param("theme") String theme);

    /**
     * Find all preferences with notifications enabled
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.emailNotifications = true OR up.smsNotifications = true OR up.pushNotifications = true")
    java.util.List<UserPreferences> findWithNotificationsEnabled();

    /**
     * Find all preferences with trading alerts enabled
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.tradingAlerts = true OR up.priceAlerts = true")
    java.util.List<UserPreferences> findWithTradingAlertsEnabled();
}