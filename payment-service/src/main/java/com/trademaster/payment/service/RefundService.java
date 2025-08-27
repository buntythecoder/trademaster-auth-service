package com.trademaster.payment.service;

import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.exception.PaymentNotFoundException;
import com.trademaster.payment.exception.RefundException;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Refund Service
 * 
 * Handles payment refunds with gateway integration and compliance tracking.
 * Supports full and partial refunds with proper audit trail.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RazorpayService razorpayService;
    private final StripeService stripeService;
    private final PaymentEventService paymentEventService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    
    /**
     * Process full refund
     */
    @Transactional
    public RefundResponse processFullRefund(UUID transactionId, RefundRequest refundRequest) {
        log.info("Processing full refund for transaction: {}", transactionId);
        
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        // Validate full refund eligibility
        validateFullRefundEligibility(transaction);
        
        // Calculate refund amount (full amount minus any existing refunds)
        BigDecimal refundAmount = calculateRefundableAmount(transaction);
        
        return processRefund(transaction, refundAmount, refundRequest, true);
    }
    
    /**
     * Process partial refund
     */
    @Transactional
    public RefundResponse processPartialRefund(UUID transactionId, BigDecimal refundAmount, RefundRequest refundRequest) {
        log.info("Processing partial refund of {} for transaction: {}", refundAmount, transactionId);
        
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        // Validate partial refund eligibility and amount
        validatePartialRefundEligibility(transaction, refundAmount);
        
        return processRefund(transaction, refundAmount, refundRequest, false);
    }
    
    /**
     * Get refund status
     */
    public RefundResponse getRefundStatus(UUID transactionId, String refundId) {
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        // Route to appropriate gateway for refund status
        return switch (transaction.getPaymentGateway()) {
            case RAZORPAY -> razorpayService.getRefundStatus(refundId);
            case STRIPE -> stripeService.getRefundStatus(refundId);
            default -> throw new RefundException("Refund status check not supported for gateway: " + 
                    transaction.getPaymentGateway());
        };
    }
    
    /**
     * Get all refunds for a transaction
     */
    public List<RefundResponse> getTransactionRefunds(UUID transactionId) {
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        return switch (transaction.getPaymentGateway()) {
            case RAZORPAY -> razorpayService.getTransactionRefunds(transaction.getGatewayTransactionId());
            case STRIPE -> stripeService.getTransactionRefunds(transaction.getGatewayTransactionId());
            default -> throw new RefundException("Refund listing not supported for gateway: " + 
                    transaction.getPaymentGateway());
        };
    }
    
    /**
     * Cancel pending refund
     */
    @Transactional
    public void cancelRefund(UUID transactionId, String refundId, String reason) {
        log.info("Cancelling refund {} for transaction: {}", refundId, transactionId);
        
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        // Route to appropriate gateway for refund cancellation
        boolean cancelled = switch (transaction.getPaymentGateway()) {
            case RAZORPAY -> razorpayService.cancelRefund(refundId);
            case STRIPE -> stripeService.cancelRefund(refundId);
            default -> throw new RefundException("Refund cancellation not supported for gateway: " + 
                    transaction.getPaymentGateway());
        };
        
        if (cancelled) {
            // Create audit event
            paymentEventService.createRefundCancelledEvent(transaction, refundId, reason);
            
            // Audit log
            auditService.logRefundCancelled(transaction.getUserId(), transactionId, refundId, reason);
            
            log.info("Refund cancelled successfully");
        } else {
            throw new RefundException("Failed to cancel refund");
        }
    }
    
    /**
     * Calculate total refunded amount for a transaction
     */
    public BigDecimal getTotalRefundedAmount(UUID transactionId) {
        PaymentTransaction transaction = getValidatedTransaction(transactionId);
        
        List<RefundResponse> refunds = getTransactionRefunds(transactionId);
        
        return refunds.stream()
                .filter(refund -> "succeeded".equalsIgnoreCase(refund.getStatus()) || 
                               "completed".equalsIgnoreCase(refund.getStatus()))
                .map(RefundResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate refundable amount for a transaction
     */
    public BigDecimal calculateRefundableAmount(PaymentTransaction transaction) {
        BigDecimal totalRefunded = getTotalRefundedAmount(transaction.getId());
        return transaction.getAmount().subtract(totalRefunded);
    }
    
    /**
     * Check if transaction is refundable
     */
    public boolean isRefundable(UUID transactionId) {
        try {
            PaymentTransaction transaction = getValidatedTransaction(transactionId);
            return isTransactionRefundable(transaction) && 
                   calculateRefundableAmount(transaction).compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            log.warn("Failed to check refund eligibility for transaction: {}", transactionId, e);
            return false;
        }
    }
    
    /**
     * Get refund statistics for a user
     */
    public Map<String, Object> getRefundStatistics(UUID userId, Instant fromDate, Instant toDate) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByUserIdAndCreatedAtBetween(userId, fromDate, toDate);
        
        long totalTransactions = transactions.size();
        long refundedTransactions = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        BigDecimal totalTransactionAmount = BigDecimal.ZERO;
        
        for (PaymentTransaction transaction : transactions) {
            totalTransactionAmount = totalTransactionAmount.add(transaction.getAmount());
            
            BigDecimal refundedAmount = getTotalRefundedAmount(transaction.getId());
            if (refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
                refundedTransactions++;
                totalRefunded = totalRefunded.add(refundedAmount);
            }
        }
        
        double refundRate = totalTransactions > 0 ? 
                (double) refundedTransactions / totalTransactions * 100 : 0;
        
        return Map.of(
                "totalTransactions", totalTransactions,
                "refundedTransactions", refundedTransactions,
                "refundRate", refundRate,
                "totalRefunded", totalRefunded,
                "totalTransactionAmount", totalTransactionAmount
        );
    }
    
    private RefundResponse processRefund(PaymentTransaction transaction, BigDecimal refundAmount, 
                                       RefundRequest refundRequest, boolean isFullRefund) {
        try {
            // Route to appropriate gateway
            RefundResponse gatewayResponse = switch (transaction.getPaymentGateway()) {
                case RAZORPAY -> razorpayService.processRefund(
                        transaction.getGatewayTransactionId(), refundAmount, refundRequest);
                case STRIPE -> stripeService.processRefund(
                        transaction.getGatewayTransactionId(), refundAmount, refundRequest);
                default -> throw new RefundException("Refund not supported for gateway: " + 
                        transaction.getPaymentGateway());
            };
            
            // Update transaction status if full refund
            if (isFullRefund && gatewayResponse.isSuccessful()) {
                transaction.setStatus(PaymentStatus.REFUNDED);
                transaction.setProcessedAt(Instant.now());
                paymentTransactionRepository.save(transaction);
            }
            
            // Create payment event
            if (isFullRefund) {
                paymentEventService.createRefundCompletedEvent(transaction, refundAmount);
            } else {
                paymentEventService.createPartialRefundEvent(transaction, refundAmount);
            }
            
            // Audit log
            auditService.logRefundProcessed(transaction.getUserId(), transaction.getId(), 
                    refundAmount, isFullRefund, gatewayResponse.getRefundId());
            
            // Send notification
            if (gatewayResponse.isSuccessful()) {
                notificationService.sendRefundConfirmation(transaction.getUserId(), 
                        refundAmount, transaction.getCurrency(), gatewayResponse.getRefundId());
            }
            
            log.info("Refund processed successfully: {}", gatewayResponse.getRefundId());
            return gatewayResponse;
            
        } catch (Exception e) {
            log.error("Failed to process refund for transaction: {}", transaction.getId(), e);
            
            // Audit error
            auditService.logRefundFailed(transaction.getUserId(), transaction.getId(), 
                    refundAmount, e.getMessage());
            
            throw new RefundException("Failed to process refund: " + e.getMessage(), e);
        }
    }
    
    private PaymentTransaction getValidatedTransaction(UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Transaction not found"));
    }
    
    private void validateFullRefundEligibility(PaymentTransaction transaction) {
        if (!isTransactionRefundable(transaction)) {
            throw new RefundException("Transaction is not eligible for refund");
        }
        
        BigDecimal refundableAmount = calculateRefundableAmount(transaction);
        if (refundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RefundException("No refundable amount available");
        }
    }
    
    private void validatePartialRefundEligibility(PaymentTransaction transaction, BigDecimal refundAmount) {
        if (!isTransactionRefundable(transaction)) {
            throw new RefundException("Transaction is not eligible for refund");
        }
        
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RefundException("Invalid refund amount");
        }
        
        BigDecimal refundableAmount = calculateRefundableAmount(transaction);
        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new RefundException("Refund amount exceeds refundable amount");
        }
        
        // Minimum refund amount check (business rule)
        if (refundAmount.compareTo(new BigDecimal("1.00")) < 0) {
            throw new RefundException("Minimum refund amount is ₹1.00");
        }
    }
    
    private boolean isTransactionRefundable(PaymentTransaction transaction) {
        // Check transaction status
        if (transaction.getStatus() != PaymentStatus.COMPLETED) {
            return false;
        }
        
        // Check refund time limit (90 days for most transactions)
        Instant refundDeadline = transaction.getCreatedAt().plus(90, java.time.temporal.ChronoUnit.DAYS);
        if (Instant.now().isAfter(refundDeadline)) {
            return false;
        }
        
        // Check if transaction has failed refunds that block further refunds
        // This would be implementation specific based on gateway rules
        
        return true;
    }
}