package com.trademaster.payment.entity;

import com.trademaster.payment.enums.BillingCycle;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription Plan Entity
 * 
 * Represents different subscription tiers available to users with their
 * pricing, features, and limitations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;
    
    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Pricing Information
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;
    
    // Features and Limits (stored as JSON)
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> features;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false) 
    private Map<String, Object> limits;
    
    // Status flags
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;
    
    // Trial configuration
    @Min(0)
    @Max(365)
    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 0;
    
    // Additional metadata
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Helper methods
    public boolean hasTrial() {
        return trialDays != null && trialDays > 0;
    }
    
    public boolean isFree() {
        return price != null && price.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public String getDisplayPrice() {
        if (isFree()) {
            return "Free";
        }
        return String.format("%s %.2f/%s", currency, price, billingCycle.name().toLowerCase());
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String featureName, Class<T> type) {
        if (features == null || !features.containsKey(featureName)) {
            return null;
        }
        Object value = features.get(featureName);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getLimit(String limitName, Class<T> type) {
        if (limits == null || !limits.containsKey(limitName)) {
            return null;
        }
        Object value = limits.get(limitName);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    public boolean hasFeature(String featureName) {
        Boolean feature = getFeature(featureName, Boolean.class);
        return feature != null && feature;
    }
    
    public Integer getLimitValue(String limitName) {
        Integer limit = getLimit(limitName, Integer.class);
        return limit != null ? limit : 0;
    }
}