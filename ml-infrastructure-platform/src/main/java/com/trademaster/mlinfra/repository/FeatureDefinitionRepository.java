package com.trademaster.mlinfra.repository;

import com.trademaster.mlinfra.domain.entity.FeatureDefinition;
import com.trademaster.mlinfra.domain.dto.FeatureDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Feature Definition Repository
 * 
 * Repository interface for feature definitions following Spring Data JPA patterns.
 * Provides functional query methods for feature management.
 */
@Repository
public interface FeatureDefinitionRepository extends JpaRepository<FeatureDefinition, Long> {

    /**
     * Find feature by feature ID
     */
    Optional<FeatureDefinition> findByFeatureId(String featureId);

    /**
     * Find feature by name and version
     */
    Optional<FeatureDefinition> findByFeatureNameAndFeatureVersion(String featureName, String featureVersion);

    /**
     * Find features by name pattern
     */
    List<FeatureDefinition> findByNameContainingIgnoreCase(String namePattern);

    /**
     * Find features by active status
     */
    List<FeatureDefinition> findByIsActiveOrderByCreatedAtDesc(boolean isActive);

    /**
     * Find features by owner
     */
    List<FeatureDefinition> findByOwnerOrderByCreatedAtDesc(String owner);

    /**
     * Find features by type
     */
    List<FeatureDefinition> findByType(FeatureDto.FeatureType type);

    /**
     * Find features by status
     */
    List<FeatureDefinition> findByStatusOrderByUpdatedAtDesc(FeatureDto.FeatureStatus status);

    /**
     * Find active features
     */
    @Query("SELECT f FROM FeatureDefinition f WHERE f.status = 'ACTIVE' ORDER BY f.updatedAt DESC")
    List<FeatureDefinition> findActiveFeatures();

    /**
     * Find features by owner and status
     */
    List<FeatureDefinition> findByOwnerAndStatus(String owner, FeatureDto.FeatureStatus status);

    /**
     * Find features created after date
     */
    List<FeatureDefinition> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since);

    /**
     * Find features by data source
     */
    List<FeatureDefinition> findByDataSourceOrderByUpdatedAtDesc(String dataSource);

    /**
     * Count features by owner
     */
    @Query("SELECT COUNT(f) FROM FeatureDefinition f WHERE f.owner = :owner")
    Long countByOwner(@Param("owner") String owner);

    /**
     * Count active features
     */
    @Query("SELECT COUNT(f) FROM FeatureDefinition f WHERE f.status = 'ACTIVE'")
    Long countActiveFeatures();

    /**
     * Find features with pagination
     */
    Page<FeatureDefinition> findByOwner(String owner, Pageable pageable);

    /**
     * Find features by multiple criteria
     */
    @Query("SELECT f FROM FeatureDefinition f WHERE " +
           "(:owner IS NULL OR f.owner = :owner) AND " +
           "(:type IS NULL OR f.type = :type) AND " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:dataSource IS NULL OR f.dataSource = :dataSource) " +
           "ORDER BY f.updatedAt DESC")
    Page<FeatureDefinition> findFeaturesByCriteria(
        @Param("owner") String owner,
        @Param("type") FeatureDto.FeatureType type,
        @Param("status") FeatureDto.FeatureStatus status,
        @Param("dataSource") String dataSource,
        Pageable pageable);

    /**
     * Find features requiring computation
     */
    @Query("SELECT f FROM FeatureDefinition f WHERE f.status = 'ACTIVE' AND f.computeConfig IS NOT NULL")
    List<FeatureDefinition> findFeaturesRequiringComputation();

    /**
     * Delete old archived features
     */
    void deleteByStatusAndUpdatedAtBefore(FeatureDto.FeatureStatus status, Instant cutoffDate);

    /**
     * Check if feature name exists for owner
     */
    boolean existsByNameAndOwner(String name, String owner);

    /**
     * Find feature by name and active status
     */
    Optional<FeatureDefinition> findByFeatureNameAndIsActive(String featureName, boolean isActive);
}