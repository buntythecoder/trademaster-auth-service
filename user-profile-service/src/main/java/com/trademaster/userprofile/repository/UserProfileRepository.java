package com.trademaster.userprofile.repository;

import com.trademaster.userprofile.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
    /**
     * Find user profile by user ID
     */
    Optional<UserProfile> findByUserId(UUID userId);
    
    /**
     * Check if profile exists for user ID
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Find profiles by PAN number
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.personalInfo, '$.panNumber') = :panNumber")
    Optional<UserProfile> findByPanNumber(@Param("panNumber") String panNumber);
    
    /**
     * Find profiles by mobile number
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.personalInfo, '$.mobileNumber') = :mobileNumber")
    List<UserProfile> findByMobileNumber(@Param("mobileNumber") String mobileNumber);
    
    /**
     * Find profiles by email address
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.personalInfo, '$.emailAddress') = :email")
    List<UserProfile> findByEmailAddress(@Param("email") String email);
    
    /**
     * Find profiles by KYC status
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.kycInfo, '$.kycStatus') = :status")
    Page<UserProfile> findByKycStatus(@Param("status") String status, Pageable pageable);
    
    /**
     * Find profiles created between dates
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "up.createdAt BETWEEN :startDate AND :endDate")
    Page<UserProfile> findByCreatedAtBetween(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate, 
        Pageable pageable
    );
    
    /**
     * Find profiles by trading segment preference
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_CONTAINS(JSON_EXTRACT(up.tradingPreferences, '$.preferredSegments'), " +
           "JSON_QUOTE(:segment))")
    List<UserProfile> findByTradingSegment(@Param("segment") String segment);
    
    /**
     * Find profiles by risk level
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.tradingPreferences, '$.riskProfile.riskLevel') = :riskLevel")
    List<UserProfile> findByRiskLevel(@Param("riskLevel") String riskLevel);
    
    /**
     * Count profiles by KYC status
     */
    @Query("SELECT COUNT(up) FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.kycInfo, '$.kycStatus') = :status")
    long countByKycStatus(@Param("status") String status);
    
    /**
     * Find profiles that need KYC renewal (approaching expiry)
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.kycInfo, '$.kycStatus') = 'VERIFIED' AND " +
           "up.updatedAt < :cutoffDate")
    List<UserProfile> findProfilesNeedingKycRenewal(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find recently updated profiles
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "up.updatedAt > :since ORDER BY up.updatedAt DESC")
    List<UserProfile> findRecentlyUpdatedProfiles(@Param("since") Instant since);
    
    /**
     * Search profiles by name (first name or last name)
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "LOWER(JSON_EXTRACT(up.personalInfo, '$.firstName')) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(JSON_EXTRACT(up.personalInfo, '$.lastName')) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<UserProfile> searchByName(@Param("name") String name, Pageable pageable);
    
    /**
     * Find profiles with incomplete KYC
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.kycInfo, '$.kycStatus') IN ('NOT_STARTED', 'IN_PROGRESS', 'PENDING')")
    List<UserProfile> findProfilesWithIncompleteKyc();
    
    /**
     * Find profiles by city
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "LOWER(JSON_EXTRACT(up.personalInfo, '$.address.city')) = LOWER(:city)")
    Page<UserProfile> findByCity(@Param("city") String city, Pageable pageable);
    
    /**
     * Find profiles by state
     */
    @Query("SELECT up FROM UserProfile up WHERE " +
           "LOWER(JSON_EXTRACT(up.personalInfo, '$.address.state')) = LOWER(:state)")
    Page<UserProfile> findByState(@Param("state") String state, Pageable pageable);
    
    /**
     * Count total profiles
     */
    @Query("SELECT COUNT(up) FROM UserProfile up")
    long countTotalProfiles();
    
    /**
     * Count verified profiles
     */
    @Query("SELECT COUNT(up) FROM UserProfile up WHERE " +
           "JSON_EXTRACT(up.kycInfo, '$.kycStatus') = 'VERIFIED'")
    long countVerifiedProfiles();
    
    /**
     * Get profile completion statistics
     */
    @Query("SELECT " +
           "COUNT(up) as total, " +
           "SUM(CASE WHEN JSON_EXTRACT(up.kycInfo, '$.kycStatus') = 'VERIFIED' THEN 1 ELSE 0 END) as verified, " +
           "SUM(CASE WHEN JSON_EXTRACT(up.kycInfo, '$.kycStatus') IN ('IN_PROGRESS', 'PENDING') THEN 1 ELSE 0 END) as pending " +
           "FROM UserProfile up")
    Object[] getProfileStatistics();
}