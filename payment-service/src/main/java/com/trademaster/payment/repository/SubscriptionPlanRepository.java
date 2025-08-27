package com.trademaster.payment.repository;

import com.trademaster.payment.entity.SubscriptionPlan;
import com.trademaster.payment.enums.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Subscription Plan Repository
 * 
 * Data access layer for subscription plan management and pricing queries.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    /**
     * Find all active subscription plans
     */
    List<SubscriptionPlan> findByIsActiveTrueOrderByPriceAsc();
    
    /**
     * Find active plans by billing cycle
     */
    List<SubscriptionPlan> findByIsActiveTrueAndBillingCycleOrderByPriceAsc(BillingCycle billingCycle);
    
    /**
     * Find featured plans
     */
    List<SubscriptionPlan> findByIsActiveTrueAndIsFeaturedTrueOrderByPriceAsc();
    
    /**
     * Find free plans
     */
    List<SubscriptionPlan> findByIsActiveTrueAndPriceOrderByCreatedAtAsc(BigDecimal price);
    
    /**
     * Find plan by name (case-insensitive)
     */
    Optional<SubscriptionPlan> findByNameIgnoreCaseAndIsActiveTrue(String name);
    
    /**
     * Find plans within price range
     */
    List<SubscriptionPlan> findByIsActiveTrueAndPriceBetweenOrderByPriceAsc(
            BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Find plans with trial
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true AND sp.trialDays > 0 ORDER BY sp.price")
    List<SubscriptionPlan> findPlansWithTrial();
    
    /**
     * Get plan pricing statistics
     */
    @Query("SELECT " +
           "sp.billingCycle, " +
           "MIN(sp.price) as minPrice, " +
           "MAX(sp.price) as maxPrice, " +
           "AVG(sp.price) as avgPrice, " +
           "COUNT(sp) as planCount " +
           "FROM SubscriptionPlan sp " +
           "WHERE sp.isActive = true " +
           "GROUP BY sp.billingCycle")
    List<Object[]> getPricingStatistics();
    
    /**
     * Find plans by feature availability
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE " +
           "sp.isActive = true AND " +
           "JSON_EXTRACT(sp.features, :featurePath) = true " +
           "ORDER BY sp.price")
    List<SubscriptionPlan> findPlansByFeature(@Param("featurePath") String featurePath);
    
    /**
     * Find cheapest plan with specific feature
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE " +
           "sp.isActive = true AND " +
           "JSON_EXTRACT(sp.features, :featurePath) = true " +
           "ORDER BY sp.price LIMIT 1")
    Optional<SubscriptionPlan> findCheapestPlanWithFeature(@Param("featurePath") String featurePath);
    
    /**
     * Search plans by name or description
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE " +
           "sp.isActive = true AND " +
           "(LOWER(sp.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sp.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY sp.price")
    List<SubscriptionPlan> searchPlans(@Param("searchTerm") String searchTerm);
}