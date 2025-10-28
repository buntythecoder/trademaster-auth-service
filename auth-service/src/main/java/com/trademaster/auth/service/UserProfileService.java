package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * User Profile Service - SOLID Single Responsibility Principle
 *
 * Responsibilities:
 * - KYC (Know Your Customer) status management
 * - KYC document validation and verification
 * - Subscription tier management
 * - Profile updates and notifications
 *
 * This service is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching, switch expressions)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    // ========== KYC Management ==========

    /**
     * Update KYC status with comprehensive validation
     *
     * @param userId User identifier
     * @param newStatus New KYC status
     * @param kycDocuments KYC documents (optional)
     * @param verificationReason Reason for status change
     * @return Result indicating success or failure
     */
    @Transactional
    public Result<Boolean, String> updateKycStatus(Long userId, User.KycStatus newStatus,
                                                    Map<String, Object> kycDocuments, String verificationReason) {
        return findUser(userId)
            .flatMap(user -> validateKycTransition(user, newStatus))
            .flatMap(user -> validateApprovalDocuments(user, newStatus, kycDocuments))
            .flatMap(user -> persistKycUpdate(user.getId(), newStatus, kycDocuments))
            .flatMap(user -> auditKycChange(user, newStatus, verificationReason, kycDocuments))
            .map(user -> true);
    }

    /**
     * Legacy method for backward compatibility
     *
     * @param userId User identifier
     * @param status New KYC status
     */
    @Transactional
    public void updateKycStatus(Long userId, User.KycStatus status) {
        updateKycStatus(userId, status, null, "Legacy update");
    }

    // ========== Subscription Tier Management ==========

    /**
     * Update subscription tier with validation
     *
     * @param userId User identifier
     * @param newTier New subscription tier
     * @param changeReason Reason for tier change
     * @return Result indicating success or failure
     */
    @Transactional
    public Result<Boolean, String> updateSubscriptionTier(Long userId, User.SubscriptionTier newTier, String changeReason) {
        return findUser(userId)
            .flatMap(user -> validateSubscriptionTierChange(user, newTier))
            .flatMap(user -> validateKycForTier(user, newTier))
            .flatMap(user -> persistSubscriptionUpdate(user.getId(), newTier, changeReason))
            .map(user -> true);
    }

    /**
     * Legacy method for backward compatibility
     *
     * @param userId User identifier
     * @param tier New subscription tier
     */
    @Transactional
    public void updateSubscriptionTier(Long userId, User.SubscriptionTier tier) {
        updateSubscriptionTier(userId, tier, "Legacy update");
    }

    // ========== Private Helper Methods - KYC ==========

    /**
     * Find user by ID
     */
    private Result<User, String> findUser(Long userId) {
        return userRepository.findById(userId)
            .map(Result::<User, String>success)
            .orElse(Result.failure("User not found: " + userId));
    }

    /**
     * Validate KYC status transition
     */
    private Result<User, String> validateKycTransition(User user, User.KycStatus newStatus) {
        return Optional.of(user)
            .filter(u -> isValidKycStatusTransition(u.getKycStatus(), newStatus))
            .map(Result::<User, String>success)
            .orElse(Result.failure("Invalid KYC status transition from " + user.getKycStatus() + " to " + newStatus));
    }

    /**
     * Validate documents for KYC approval
     */
    private Result<User, String> validateApprovalDocuments(User user, User.KycStatus newStatus, Map<String, Object> kycDocuments) {
        return Optional.of(newStatus)
            .filter(status -> status == User.KycStatus.APPROVED)
            .map(status -> validateDocumentsForApproval(kycDocuments).map(valid -> user))
            .orElse(Result.success(user));
    }

    /**
     * Validate documents meet approval requirements
     */
    private Result<Boolean, String> validateDocumentsForApproval(Map<String, Object> kycDocuments) {
        return Optional.ofNullable(kycDocuments)
            .filter(this::hasRequiredKycDocuments)
            .filter(this::validateKycDocuments)
            .map(docs -> Result.<Boolean, String>success(true))
            .orElse(Result.failure("KYC document validation failed"));
    }

    /**
     * Persist KYC status update
     */
    private Result<User, String> persistKycUpdate(Long userId, User.KycStatus newStatus, Map<String, Object> kycDocuments) {
        return SafeOperations.safelyToResult(() -> {
            userRepository.updateKycStatus(userId, newStatus, LocalDateTime.now());
            Optional.ofNullable(kycDocuments)
                .filter(docs -> !docs.isEmpty())
                .ifPresent(docs -> updateUserProfileKycDocuments(userId, docs));
            return userRepository.findById(userId).orElseThrow();
        });
    }

    /**
     * Audit KYC status change
     */
    private Result<User, String> auditKycChange(User user, User.KycStatus newStatus,
                                                 String verificationReason, Map<String, Object> kycDocuments) {
        return SafeOperations.safelyToResult(() -> {
            User.KycStatus oldStatus = user.getKycStatus();

            Map<String, Object> auditDetails = Map.of(
                "old_status", oldStatus,
                "new_status", newStatus,
                "verification_reason", Optional.ofNullable(verificationReason).orElse("Manual update"),
                "documents_count", Optional.ofNullable(kycDocuments).map(Map::size).orElse(0)
            );

            auditService.logAuthenticationEvent(user.getId(), "KYC_STATUS_CHANGED", "SUCCESS",
                null, null, null, auditDetails, null);

            sendKycStatusNotification(user, oldStatus, newStatus);

            log.info("KYC status updated for user {}: {} -> {} (reason: {})",
                    user.getId(), oldStatus, newStatus, verificationReason);

            return user;
        });
    }

    /**
     * Validate KYC status transition rules using switch expression
     */
    private boolean isValidKycStatusTransition(User.KycStatus from, User.KycStatus to) {
        return Optional.of(from)
            .filter(f -> f == to)
            .map(f -> true)
            .orElseGet(() -> switch (from) {
                case PENDING -> Set.of(User.KycStatus.IN_PROGRESS, User.KycStatus.REJECTED).contains(to);
                case IN_PROGRESS -> Set.of(User.KycStatus.APPROVED, User.KycStatus.REJECTED, User.KycStatus.PENDING).contains(to);
                case APPROVED -> Set.of(User.KycStatus.IN_PROGRESS).contains(to);
                case REJECTED -> Set.of(User.KycStatus.PENDING, User.KycStatus.IN_PROGRESS).contains(to);
            });
    }

    /**
     * Check if required KYC documents are present
     */
    private boolean hasRequiredKycDocuments(Map<String, Object> documents) {
        Set<String> requiredDocs = Set.of(
            "identity_document",    // Government ID, Passport, Driver's License
            "address_proof",        // Utility bill, Bank statement
            "selfie_with_id"        // Selfie holding identity document
        );

        return Optional.ofNullable(documents)
            .filter(docs -> !docs.isEmpty())
            .map(docs -> requiredDocs.stream().allMatch(docs::containsKey))
            .orElse(false);
    }

    /**
     * Validate KYC documents integrity and format
     */
    private boolean validateKycDocuments(Map<String, Object> documents) {
        // In production, implement:
        // 1. Document format validation (PDF, JPG, PNG)
        // 2. File size limits
        // 3. OCR verification
        // 4. Face matching between selfie and ID
        // 5. Document authenticity checks

        return documents.entrySet().stream()
            .allMatch(entry -> isValidKycDocument(entry.getKey(), entry.getValue()));
    }

    /**
     * Validate individual KYC document
     */
    private boolean isValidKycDocument(String docType, Object docData) {
        // Validate document structure and content
        return Optional.ofNullable(docData)
            .filter(data -> data instanceof Map<?, ?>)
            .map(data -> (Map<?, ?>) data)
            .filter(docMap -> docMap.containsKey("filename") &&
                             docMap.containsKey("content_type") &&
                             docMap.containsKey("size"))
            .isPresent();
    }

    /**
     * Update user profile with KYC documents metadata
     */
    private void updateUserProfileKycDocuments(Long userId, Map<String, Object> kycDocuments) {
        // Update UserProfile entity with KYC document metadata
        // Note: Actual document storage should be handled by a separate document service
        log.info("Updating KYC documents for user profile: {}", userId);
    }

    /**
     * Send KYC status notification
     */
    private void sendKycStatusNotification(User user, User.KycStatus oldStatus, User.KycStatus newStatus) {
        // Send appropriate notification based on status change
        String notificationType = switch (newStatus) {
            case APPROVED -> "KYC_APPROVED";
            case REJECTED -> "KYC_REJECTED";
            case IN_PROGRESS -> "KYC_IN_PROGRESS";
            default -> "KYC_STATUS_UPDATED";
        };

        // In production, integrate with notification service
        log.info("KYC notification {} sent to user: {}", notificationType, user.getEmail());
    }

    // ========== Private Helper Methods - Subscription ==========

    /**
     * Validate subscription tier change
     */
    private Result<User, String> validateSubscriptionTierChange(User user, User.SubscriptionTier newTier) {
        return Optional.of(user)
            .filter(u -> isValidSubscriptionTierChange(u, u.getSubscriptionTier(), newTier))
            .map(Result::<User, String>success)
            .orElse(Result.failure("Invalid subscription tier change from " + user.getSubscriptionTier() + " to " + newTier));
    }

    /**
     * Validate KYC approval for premium tiers
     */
    private Result<User, String> validateKycForTier(User user, User.SubscriptionTier newTier) {
        return Optional.of(newTier)
            .filter(this::requiresKycForTier)
            .filter(tier -> user.getKycStatus() != User.KycStatus.APPROVED)
            .map(tier -> Result.<User, String>failure("KYC approval required for " + newTier + " subscription tier"))
            .orElse(Result.success(user));
    }

    /**
     * Persist subscription tier update
     */
    private Result<User, String> persistSubscriptionUpdate(Long userId, User.SubscriptionTier newTier, String changeReason) {
        return SafeOperations.safelyToResult(() -> {
            userRepository.updateSubscriptionTier(userId, newTier, LocalDateTime.now());

            User user = userRepository.findById(userId).orElseThrow();

            // Audit subscription tier change
            auditService.logAuthenticationEvent(userId, "SUBSCRIPTION_TIER_CHANGED", "SUCCESS",
                null, null, null,
                Map.of("new_tier", newTier, "change_reason", changeReason), null);

            sendSubscriptionTierNotification(user, user.getSubscriptionTier(), newTier);

            log.info("Subscription tier updated for user {}: {} (reason: {})", userId, newTier, changeReason);

            return user;
        });
    }

    /**
     * Validate subscription tier change rules
     */
    private boolean isValidSubscriptionTierChange(User user, User.SubscriptionTier from, User.SubscriptionTier to) {
        return Optional.of(from)
            .filter(f -> f == to)
            .map(f -> true)
            .orElseGet(() -> validateTierUpgradeEligibility(user, from, to));
    }

    /**
     * Validate tier upgrade eligibility
     */
    private boolean validateTierUpgradeEligibility(User user, User.SubscriptionTier from, User.SubscriptionTier to) {
        return Optional.of(to.ordinal() > from.ordinal())
            .filter(Boolean::booleanValue)
            .map(isUpgrade -> user.getAccountStatus() == User.AccountStatus.ACTIVE)
            .orElse(true); // Downgrades are always allowed
    }

    /**
     * Check if tier requires KYC approval
     */
    private boolean requiresKycForTier(User.SubscriptionTier tier) {
        // Premium tiers require KYC approval
        return Set.of(User.SubscriptionTier.PREMIUM, User.SubscriptionTier.ENTERPRISE).contains(tier);
    }

    /**
     * Send subscription tier notification
     */
    private void sendSubscriptionTierNotification(User user, User.SubscriptionTier oldTier, User.SubscriptionTier newTier) {
        String notificationType = newTier.ordinal() > oldTier.ordinal() ?
            "SUBSCRIPTION_UPGRADED" : "SUBSCRIPTION_DOWNGRADED";

        // In production, integrate with notification service
        log.info("Subscription notification {} sent to user: {}", notificationType, user.getEmail());
    }
}
