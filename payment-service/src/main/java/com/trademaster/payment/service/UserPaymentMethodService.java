package com.trademaster.payment.service;

import com.trademaster.payment.dto.PaymentMethodRequest;
import com.trademaster.payment.dto.PaymentMethodResponse;
import com.trademaster.payment.entity.UserPaymentMethod;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.exception.PaymentMethodNotFoundException;
import com.trademaster.payment.exception.PaymentServiceException;
import com.trademaster.payment.repository.UserPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Payment Method Service
 * 
 * Manages user payment methods with PCI compliance and security features.
 * Handles tokenization, verification, and lifecycle management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPaymentMethodService {

    private final UserPaymentMethodRepository paymentMethodRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    
    /**
     * Add new payment method for user
     */
    @Transactional
    public PaymentMethodResponse addPaymentMethod(UUID userId, PaymentMethodRequest request) {
        log.info("Adding payment method for user: {}", userId);
        
        try {
            // Validate request
            validatePaymentMethodRequest(request);
            
            // Check if user has reached maximum payment methods
            long activeMethodsCount = paymentMethodRepository.countByUserIdAndIsActiveTrue(userId);
            if (activeMethodsCount >= 10) { // Business rule: max 10 payment methods per user
                throw new PaymentServiceException("Maximum payment methods limit reached");
            }
            
            // Create payment method entity
            UserPaymentMethod paymentMethod = buildPaymentMethod(userId, request);
            
            // Save payment method
            UserPaymentMethod saved = paymentMethodRepository.save(paymentMethod);
            
            // Set as default if it's the first payment method
            if (activeMethodsCount == 0) {
                setAsDefault(userId, saved.getId());
            }
            
            // Audit log
            auditService.logPaymentMethodAdded(userId, saved.getId(), request.getPaymentMethodType());
            
            log.info("Payment method added successfully: {}", saved.getId());
            return mapToResponse(saved);
            
        } catch (Exception e) {
            log.error("Failed to add payment method for user: {}", userId, e);
            throw new PaymentServiceException("Failed to add payment method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all payment methods for user
     */
    public List<PaymentMethodResponse> getUserPaymentMethods(UUID userId) {
        List<UserPaymentMethod> paymentMethods = paymentMethodRepository.findByUserIdAndIsActiveTrue(userId);
        return paymentMethods.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get user's default payment method
     */
    public Optional<PaymentMethodResponse> getDefaultPaymentMethod(UUID userId) {
        Optional<UserPaymentMethod> defaultMethod = 
                paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
        return defaultMethod.map(this::mapToResponse);
    }
    
    /**
     * Set payment method as default
     */
    @Transactional
    public void setAsDefault(UUID userId, UUID paymentMethodId) {
        log.info("Setting payment method as default: {} for user: {}", paymentMethodId, userId);
        
        // Verify payment method belongs to user and is active
        UserPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new PaymentServiceException("Payment method does not belong to user");
        }
        
        if (!paymentMethod.getIsActive()) {
            throw new PaymentServiceException("Cannot set inactive payment method as default");
        }
        
        // Clear existing default
        paymentMethodRepository.clearDefaultMethods(userId);
        
        // Set new default
        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);
        
        // Audit log
        auditService.logPaymentMethodDefaultSet(userId, paymentMethodId);
        
        log.info("Payment method set as default successfully");
    }
    
    /**
     * Remove payment method
     */
    @Transactional
    public void removePaymentMethod(UUID userId, UUID paymentMethodId) {
        log.info("Removing payment method: {} for user: {}", paymentMethodId, userId);
        
        UserPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new PaymentServiceException("Payment method does not belong to user");
        }
        
        boolean wasDefault = paymentMethod.getIsDefault();
        
        // Deactivate payment method (soft delete for audit purposes)
        paymentMethodRepository.deactivatePaymentMethod(paymentMethodId);
        
        // If this was the default, set another active method as default
        if (wasDefault) {
            setNewDefaultIfNeeded(userId);
        }
        
        // Audit log
        auditService.logPaymentMethodRemoved(userId, paymentMethodId);
        
        log.info("Payment method removed successfully");
    }
    
    /**
     * Update last used timestamp
     */
    @Transactional
    public void updateLastUsed(UUID paymentMethodId) {
        paymentMethodRepository.updateLastUsedAt(paymentMethodId, Instant.now());
    }
    
    /**
     * Verify payment method
     */
    @Transactional
    public void verifyPaymentMethod(UUID userId, UUID paymentMethodId, String verificationData) {
        log.info("Verifying payment method: {} for user: {}", paymentMethodId, userId);
        
        UserPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new PaymentServiceException("Payment method does not belong to user");
        }
        
        // Perform verification based on payment method type
        boolean verificationResult = performVerification(paymentMethod, verificationData);
        
        if (verificationResult) {
            paymentMethod.setIsVerified(true);
            paymentMethod.setVerifiedAt(Instant.now());
            paymentMethodRepository.save(paymentMethod);
            
            auditService.logPaymentMethodVerified(userId, paymentMethodId);
            log.info("Payment method verified successfully");
        } else {
            throw new PaymentServiceException("Payment method verification failed");
        }
    }
    
    /**
     * Find expired payment methods
     */
    public List<UserPaymentMethod> findExpiredPaymentMethods() {
        Instant now = Instant.now();
        int currentYear = now.atZone(java.time.ZoneOffset.UTC).getYear();
        int currentMonth = now.atZone(java.time.ZoneOffset.UTC).getMonthValue();
        
        return paymentMethodRepository.findExpiredCardMethods(currentYear, currentMonth);
    }
    
    /**
     * Find unused payment methods
     */
    public List<UserPaymentMethod> findUnusedPaymentMethods(int daysUnused) {
        Instant cutoffDate = Instant.now().minus(daysUnused, ChronoUnit.DAYS);
        return paymentMethodRepository.findUnusedMethods(cutoffDate);
    }
    
    /**
     * Get payment method statistics for user
     */
    public List<Object[]> getUserPaymentMethodStats(UUID userId) {
        return paymentMethodRepository.getUserPaymentMethodStats(userId);
    }
    
    /**
     * Cleanup expired and unused payment methods
     */
    @Transactional
    public int cleanupPaymentMethods() {
        log.info("Starting payment method cleanup");
        
        int cleanedCount = 0;
        
        // Clean up expired cards
        List<UserPaymentMethod> expired = findExpiredPaymentMethods();
        for (UserPaymentMethod method : expired) {
            paymentMethodRepository.deactivatePaymentMethod(method.getId());
            auditService.logPaymentMethodExpired(method.getUserId(), method.getId());
            cleanedCount++;
        }
        
        // Clean up unused methods (6 months)
        List<UserPaymentMethod> unused = findUnusedPaymentMethods(180);
        for (UserPaymentMethod method : unused) {
            paymentMethodRepository.deactivatePaymentMethod(method.getId());
            auditService.logPaymentMethodInactive(method.getUserId(), method.getId());
            cleanedCount++;
        }
        
        log.info("Payment method cleanup completed. Cleaned up {} methods", cleanedCount);
        return cleanedCount;
    }
    
    private void validatePaymentMethodRequest(PaymentMethodRequest request) {
        if (request.getPaymentMethodType() == null) {
            throw new PaymentServiceException("Payment method type is required");
        }
        
        if (request.getGatewayProvider() == null) {
            throw new PaymentServiceException("Gateway provider is required");
        }
        
        if (request.getPaymentMethodToken() == null || request.getPaymentMethodToken().trim().isEmpty()) {
            throw new PaymentServiceException("Payment method token is required");
        }
        
        // Validate card-specific fields if applicable
        if (request.getPaymentMethodType() == PaymentMethod.CARD) {
            if (request.getExpiryMonth() == null || request.getExpiryYear() == null) {
                throw new PaymentServiceException("Expiry month and year are required for cards");
            }
            
            if (request.getExpiryMonth() < 1 || request.getExpiryMonth() > 12) {
                throw new PaymentServiceException("Invalid expiry month");
            }
            
            if (request.getExpiryYear() < Instant.now().atZone(java.time.ZoneOffset.UTC).getYear()) {
                throw new PaymentServiceException("Card has expired");
            }
        }
    }
    
    private UserPaymentMethod buildPaymentMethod(UUID userId, PaymentMethodRequest request) {
        return UserPaymentMethod.builder()
                .userId(userId)
                .paymentMethodType(request.getPaymentMethodType())
                .gatewayProvider(request.getGatewayProvider())
                .paymentMethodToken(request.getPaymentMethodToken())
                .lastFourDigits(request.getLastFourDigits())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cardBrand(request.getCardBrand())
                .cardholderName(encryptionService.encrypt(request.getCardholderName()))
                .billingAddress(encryptionService.encrypt(request.getBillingAddress()))
                .isActive(true)
                .isDefault(false)
                .isVerified(false)
                .build();
    }
    
    private PaymentMethodResponse mapToResponse(UserPaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .paymentMethodType(paymentMethod.getPaymentMethodType())
                .gatewayProvider(paymentMethod.getGatewayProvider())
                .lastFourDigits(paymentMethod.getLastFourDigits())
                .expiryMonth(paymentMethod.getExpiryMonth())
                .expiryYear(paymentMethod.getExpiryYear())
                .cardBrand(paymentMethod.getCardBrand())
                .cardholderName(encryptionService.decrypt(paymentMethod.getCardholderName()))
                .isDefault(paymentMethod.getIsDefault())
                .isVerified(paymentMethod.getIsVerified())
                .createdAt(paymentMethod.getCreatedAt())
                .lastUsedAt(paymentMethod.getLastUsedAt())
                .build();
    }
    
    private void setNewDefaultIfNeeded(UUID userId) {
        List<UserPaymentMethod> activeMethods = paymentMethodRepository.findByUserIdAndIsActiveTrue(userId);
        if (!activeMethods.isEmpty()) {
            UserPaymentMethod newDefault = activeMethods.get(0);
            newDefault.setIsDefault(true);
            paymentMethodRepository.save(newDefault);
        }
    }
    
    private boolean performVerification(UserPaymentMethod paymentMethod, String verificationData) {
        // Implementation would depend on payment method type and gateway
        // For cards: might verify through micro-transaction or CVV check
        // For bank accounts: might verify through micro-deposits
        // For UPI: might verify through UPI handle validation
        
        // Placeholder implementation
        return verificationData != null && !verificationData.trim().isEmpty();
    }
}