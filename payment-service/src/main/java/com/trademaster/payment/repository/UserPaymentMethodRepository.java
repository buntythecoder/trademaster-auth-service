package com.trademaster.payment.repository;

import com.trademaster.payment.entity.UserPaymentMethod;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Payment Method Repository
 * 
 * Data access layer for managing user payment methods with
 * security and compliance features.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UserPaymentMethodRepository extends JpaRepository<UserPaymentMethod, UUID> {

    /**
     * Find all active payment methods for a user
     */
    List<UserPaymentMethod> findByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find all payment methods for a user (including inactive)
     */
    List<UserPaymentMethod> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find user's default payment method
     */
    Optional<UserPaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);
    
    /**
     * Find payment method by token and user
     */
    Optional<UserPaymentMethod> findByUserIdAndPaymentMethodToken(UUID userId, String token);
    
    /**
     * Find payment methods by type for a user
     */
    List<UserPaymentMethod> findByUserIdAndPaymentMethodTypeAndIsActiveTrue(
            UUID userId, PaymentMethod paymentMethodType);
    
    /**
     * Find payment methods by gateway for a user
     */
    List<UserPaymentMethod> findByUserIdAndGatewayProviderAndIsActiveTrue(
            UUID userId, PaymentGateway gatewayProvider);
    
    /**
     * Count active payment methods for a user
     */
    long countByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find expired payment methods
     */
    @Query("SELECT pm FROM UserPaymentMethod pm WHERE " +
           "pm.isActive = true AND pm.paymentMethodType = 'CARD' AND " +
           "(pm.expiryYear < :currentYear OR " +
           "(pm.expiryYear = :currentYear AND pm.expiryMonth < :currentMonth))")
    List<UserPaymentMethod> findExpiredCardMethods(
            @Param("currentYear") int currentYear,
            @Param("currentMonth") int currentMonth);
    
    /**
     * Find unused payment methods older than specified duration
     */
    @Query("SELECT pm FROM UserPaymentMethod pm WHERE " +
           "pm.isActive = true AND " +
           "(pm.lastUsedAt IS NULL OR pm.lastUsedAt < :cutoffDate)")
    List<UserPaymentMethod> findUnusedMethods(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE UserPaymentMethod pm SET pm.lastUsedAt = :lastUsedAt WHERE pm.id = :id")
    void updateLastUsedAt(@Param("id") UUID id, @Param("lastUsedAt") Instant lastUsedAt);
    
    /**
     * Set all user payment methods as non-default
     */
    @Modifying
    @Query("UPDATE UserPaymentMethod pm SET pm.isDefault = false WHERE pm.userId = :userId")
    void clearDefaultMethods(@Param("userId") UUID userId);
    
    /**
     * Deactivate payment method
     */
    @Modifying
    @Query("UPDATE UserPaymentMethod pm SET pm.isActive = false WHERE pm.id = :id")
    void deactivatePaymentMethod(@Param("id") UUID id);
    
    /**
     * Bulk deactivate payment methods for a user
     */
    @Modifying
    @Query("UPDATE UserPaymentMethod pm SET pm.isActive = false WHERE pm.userId = :userId")
    void deactivateAllUserMethods(@Param("userId") UUID userId);
    
    /**
     * Find methods that need verification
     */
    @Query("SELECT pm FROM UserPaymentMethod pm WHERE " +
           "pm.isActive = true AND pm.isVerified = false AND " +
           "pm.createdAt < :verificationCutoff")
    List<UserPaymentMethod> findMethodsRequiringVerification(
            @Param("verificationCutoff") Instant verificationCutoff);
    
    /**
     * Get payment method statistics for a user
     */
    @Query("SELECT " +
           "pm.paymentMethodType, " +
           "COUNT(pm) as methodCount, " +
           "MAX(pm.lastUsedAt) as lastUsed " +
           "FROM UserPaymentMethod pm " +
           "WHERE pm.userId = :userId AND pm.isActive = true " +
           "GROUP BY pm.paymentMethodType")
    List<Object[]> getUserPaymentMethodStats(@Param("userId") UUID userId);
}