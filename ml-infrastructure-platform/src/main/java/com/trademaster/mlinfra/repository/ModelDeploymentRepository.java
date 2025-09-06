package com.trademaster.mlinfra.repository;

import com.trademaster.mlinfra.domain.entity.ModelDeployment;
import com.trademaster.mlinfra.domain.dto.ModelDeploymentRequest;
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
 * Model Deployment Repository
 * 
 * Repository interface for model deployments following Spring Data JPA patterns.
 * Provides functional query methods for deployment management.
 */
@Repository
public interface ModelDeploymentRepository extends JpaRepository<ModelDeployment, Long> {

    /**
     * Find deployment by deployment name
     */
    Optional<ModelDeployment> findByDeploymentName(String deploymentName);

    /**
     * Find deployments by model name
     */
    List<ModelDeployment> findByModelNameOrderByDeployedAtDesc(String modelName);

    /**
     * Find deployments by model name and version
     */
    List<ModelDeployment> findByModelNameAndModelVersionOrderByDeployedAtDesc(String modelName, String modelVersion);

    /**
     * Find deployments by environment
     */
    List<ModelDeployment> findByEnvironmentOrderByDeployedAtDesc(ModelDeploymentRequest.Environment environment);

    /**
     * Find active deployments
     */
    @Query("SELECT d FROM ModelDeployment d WHERE d.status = 'RUNNING' ORDER BY d.deployedAt DESC")
    List<ModelDeployment> findActiveDeployments();

    /**
     * Find deployments by status
     */
    List<ModelDeployment> findByStatusOrderByDeployedAtDesc(String status);

    /**
     * Find production deployments
     */
    @Query("SELECT d FROM ModelDeployment d WHERE d.environment = 'PRODUCTION' AND d.status IN ('RUNNING', 'READY') ORDER BY d.deployedAt DESC")
    List<ModelDeployment> findProductionDeployments();

    /**
     * Find deployments requiring health check
     */
    @Query("SELECT d FROM ModelDeployment d WHERE d.status = 'RUNNING' AND d.lastHealthCheck IS NULL OR d.lastHealthCheck < :cutoff")
    List<ModelDeployment> findDeploymentsRequiringHealthCheck(@Param("cutoff") Instant cutoff);

    /**
     * Find deployments by endpoint
     */
    Optional<ModelDeployment> findByEndpoint(String endpoint);

    /**
     * Count deployments by environment
     */
    @Query("SELECT COUNT(d) FROM ModelDeployment d WHERE d.environment = :environment")
    Long countByEnvironment(@Param("environment") ModelDeploymentRequest.Environment environment);

    /**
     * Count active deployments
     */
    @Query("SELECT COUNT(d) FROM ModelDeployment d WHERE d.status IN ('RUNNING', 'READY')")
    Long countActiveDeployments();

    /**
     * Find deployments with pagination
     */
    Page<ModelDeployment> findByModelName(String modelName, Pageable pageable);

    /**
     * Find deployments by multiple criteria
     */
    @Query("SELECT d FROM ModelDeployment d WHERE " +
           "(:modelName IS NULL OR d.modelName = :modelName) AND " +
           "(:environment IS NULL OR d.environment = :environment) AND " +
           "(:status IS NULL OR d.status = :status) " +
           "ORDER BY d.deployedAt DESC")
    Page<ModelDeployment> findDeploymentsByCriteria(
        @Param("modelName") String modelName,
        @Param("environment") ModelDeploymentRequest.Environment environment,
        @Param("status") String status,
        Pageable pageable);

    /**
     * Find deployments with auto-scaling enabled
     */
    List<ModelDeployment> findByAutoScaleAndStatusOrderByDeployedAtDesc(Boolean autoScale, String status);

    /**
     * Delete old terminated deployments
     */
    void deleteByStatusAndDeployedAtBefore(String status, Instant cutoffDate);

    /**
     * Check if deployment name exists
     */
    boolean existsByDeploymentName(String deploymentName);

    /**
     * Find latest deployment for model
     */
    Optional<ModelDeployment> findFirstByModelNameAndModelVersionOrderByDeployedAtDesc(String modelName, String modelVersion);

    /**
     * Find deployment by model name, version and status
     */
    Optional<ModelDeployment> findByModelNameAndModelVersionAndStatus(String modelName, String modelVersion, String status);

    /**
     * Find deployment by deployment ID
     */
    Optional<ModelDeployment> findByDeploymentId(String deploymentId);

    /**
     * Count deployments by status
     */
    Long countByStatus(String status);
}