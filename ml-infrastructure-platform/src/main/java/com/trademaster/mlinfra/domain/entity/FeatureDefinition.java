package com.trademaster.mlinfra.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Feature Definition Entity
 * 
 * Represents feature definitions in the feature store:
 * - Feature computation logic and metadata
 * - Data type and validation rules
 * - Versioning and lineage tracking
 * - Computation type (real-time vs batch)
 */
@Entity
@Table(name = "feature_store",
    uniqueConstraints = @UniqueConstraint(columnNames = {"feature_name", "feature_version"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_name", nullable = false, length = 255)
    private String featureName;

    @Column(name = "feature_version", nullable = false, length = 50)
    private String featureVersion;

    @Column(name = "feature_definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "feature_type", nullable = false, length = 50)
    private String featureType;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "computation_type", nullable = false, length = 20)
    private String computationType; // real_time, batch

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dependencies", columnDefinition = "jsonb")
    private java.util.List<String> dependencies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if feature supports real-time computation
     */
    @JsonIgnore
    public boolean isRealTime() {
        return "real_time".equalsIgnoreCase(computationType);
    }

    /**
     * Check if feature is batch-computed
     */
    @JsonIgnore
    public boolean isBatch() {
        return "batch".equalsIgnoreCase(computationType);
    }

    /**
     * Get feature identifier
     */
    @JsonIgnore
    public String getFeatureIdentifier() {
        return featureName + ":" + featureVersion;
    }

    /**
     * Check if feature has dependencies
     */
    @JsonIgnore
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }

    /**
     * Check if feature has validation rules
     */
    @JsonIgnore
    public boolean hasValidationRules() {
        return validationRules != null && !validationRules.isEmpty();
    }

    /**
     * Add dependency
     */
    public FeatureDefinition withDependency(String dependency) {
        if (this.dependencies == null) {
            this.dependencies = new java.util.ArrayList<>();
        }
        if (!this.dependencies.contains(dependency)) {
            this.dependencies.add(dependency);
        }
        return this;
    }

    /**
     * Add validation rule
     */
    public FeatureDefinition withValidationRule(String ruleName, Object ruleValue) {
        if (this.validationRules == null) {
            this.validationRules = new java.util.HashMap<>();
        }
        this.validationRules.put(ruleName, ruleValue);
        return this;
    }

    /**
     * Add metadata
     */
    public FeatureDefinition withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Deactivate feature
     */
    public FeatureDefinition deactivate() {
        this.isActive = false;
        return this;
    }

    /**
     * Activate feature
     */
    public FeatureDefinition activate() {
        this.isActive = true;
        return this;
    }

    /**
     * Get age in days
     */
    @JsonIgnore
    public long getAgeInDays() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }

    /**
     * Get days since last update
     */
    @JsonIgnore
    public long getDaysSinceUpdate() {
        return java.time.Duration.between(lastUpdated, LocalDateTime.now()).toDays();
    }

    /**
     * Check if feature is stale
     */
    @JsonIgnore
    public boolean isStale(int staleDays) {
        return getDaysSinceUpdate() > staleDays;
    }

    /**
     * Create builder with defaults
     */
}