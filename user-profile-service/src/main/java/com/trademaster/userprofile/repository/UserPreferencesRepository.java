package com.trademaster.userprofile.repository;

import com.trademaster.userprofile.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Preferences Repository with Query Support
 * 
 * MANDATORY: Immutability & Records Usage - Rule #9
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Pattern Matching Excellence - Rule #14
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    /**
     * Find preferences by user profile ID
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.userProfile.id = :userProfileId")
    Optional<UserPreferences> findByUserProfileId(@Param("userProfileId") UUID userProfileId);

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
    List<UserPreferences> findByTheme(@Param("theme") String theme);

    /**
     * Find all preferences with notifications enabled
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.emailNotifications = true OR up.smsNotifications = true OR up.pushNotifications = true")
    List<UserPreferences> findWithNotificationsEnabled();

    /**
     * Find all preferences with trading alerts enabled
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.tradingAlerts = true OR up.priceAlerts = true")
    List<UserPreferences> findWithTradingAlertsEnabled();
    
    /**
     * Find preferences by language
     */
    List<UserPreferences> findByLanguage(String language);
    
    /**
     * Find preferences with two-factor authentication enabled
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.twoFactorEnabled = true")
    List<UserPreferences> findWithTwoFactorEnabled();
    
    /**
     * Find preferences with short session timeout (high security users)
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.sessionTimeout <= :timeout")
    List<UserPreferences> findWithShortSessionTimeout(@Param("timeout") Integer timeout);
}