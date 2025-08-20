package com.trademaster.auth.repository;

import com.trademaster.auth.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserProfile entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Find profile by user ID
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * Find profiles by risk tolerance
     */
    List<UserProfile> findByRiskTolerance(UserProfile.RiskTolerance riskTolerance);

    /**
     * Find profiles by trading experience
     */
    List<UserProfile> findByTradingExperience(UserProfile.TradingExperience tradingExperience);

    /**
     * Find profiles by country code
     */
    List<UserProfile> findByCountryCode(String countryCode);

    /**
     * Check if profile exists for user
     */
    boolean existsByUserId(Long userId);

    /**
     * Count profiles by risk tolerance
     */
    @Query("SELECT p.riskTolerance, COUNT(p) FROM UserProfile p GROUP BY p.riskTolerance")
    List<Object[]> countProfilesByRiskTolerance();

    /**
     * Count profiles by trading experience
     */
    @Query("SELECT p.tradingExperience, COUNT(p) FROM UserProfile p GROUP BY p.tradingExperience")
    List<Object[]> countProfilesByTradingExperience();

    /**
     * Find profiles with incomplete KYC
     */
    @Query("SELECT p FROM UserProfile p WHERE p.firstName IS NULL OR p.lastName IS NULL OR p.dateOfBirth IS NULL")
    List<UserProfile> findIncompleteProfiles();
}