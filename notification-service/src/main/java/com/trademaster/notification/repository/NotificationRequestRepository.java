package com.trademaster.notification.repository;

import com.trademaster.notification.model.NotificationRequest;
import com.trademaster.notification.model.NotificationStatus;
import com.trademaster.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for NotificationRequest entities
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {

    /**
     * Find notifications by status
     */
    List<NotificationRequest> findByStatus(NotificationStatus status);

    /**
     * Find notifications by user ID
     */
    List<NotificationRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find notifications by type and status
     */
    List<NotificationRequest> findByTypeAndStatus(NotificationType type, NotificationStatus status);

    /**
     * Find failed notifications ready for retry
     */
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetryAttempts AND n.scheduledAt <= :now")
    List<NotificationRequest> findFailedNotificationsForRetry(@Param("now") Instant now);

    /**
     * Find notifications scheduled for delivery
     */
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = 'PENDING' AND n.scheduledAt <= :now")
    List<NotificationRequest> findPendingNotificationsForDelivery(@Param("now") Instant now);

    /**
     * Count notifications by status for a user
     */
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
    
    /**
     * Find notifications by status and scheduled date
     */
    List<NotificationRequest> findByStatusAndScheduledAtLessThanEqual(NotificationStatus status, LocalDateTime scheduledAt);
    
    /**
     * Find retryable notifications
     */
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetryAttempts")
    List<NotificationRequest> findRetryableNotifications();
    
    /**
     * Delete old notifications
     */
    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}