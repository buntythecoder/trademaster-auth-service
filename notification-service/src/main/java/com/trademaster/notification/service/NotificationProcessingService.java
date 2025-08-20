package com.trademaster.notification.service;

import com.trademaster.notification.model.NotificationRequest;
import com.trademaster.notification.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {
    
    private final NotificationRequestRepository notificationRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final RateLimitService rateLimitService;
    
    @KafkaListener(topics = "notification-requests")
    @Async
    public void processNotificationFromKafka(NotificationRequest request) {
        log.info("Received notification request from Kafka: {}", request.getId());
        processNotification(request);
    }
    
    @Transactional
    public void processNotification(NotificationRequest request) {
        try {
            // Save to database first
            if (request.getId() == null) {
                request = notificationRepository.save(request);
            }
            
            // Check if notification is scheduled for later
            if (request.getScheduledAt().isAfter(LocalDateTime.now())) {
                log.info("Notification {} scheduled for later: {}", 
                        request.getId(), request.getScheduledAt());
                return;
            }
            
            // Check rate limiting
            if (!rateLimitService.isAllowed(request)) {
                log.warn("Rate limit exceeded for notification type: {}", request.getType());
                request.setStatus(NotificationRequest.NotificationStatus.FAILED);
                request.setErrorMessage("Rate limit exceeded");
                notificationRepository.save(request);
                return;
            }
            
            // Update status to processing
            request.setStatus(NotificationRequest.NotificationStatus.PROCESSING);
            notificationRepository.save(request);
            
            // Process based on notification type
            switch (request.getType()) {
                case EMAIL -> processEmailNotification(request);
                case SMS -> processSmsNotification(request);
                case PUSH -> log.warn("Push notifications not implemented yet");
                case IN_APP -> log.warn("In-app notifications not implemented yet");
                default -> throw new IllegalArgumentException("Unknown notification type: " + request.getType());
            }
            
        } catch (Exception e) {
            handleNotificationError(request, e);
        }
    }
    
    private void processEmailNotification(NotificationRequest request) {
        try {
            emailService.sendEmail(request);
            markNotificationAsSent(request);
        } catch (Exception e) {
            handleNotificationError(request, e);
        }
    }
    
    private void processSmsNotification(NotificationRequest request) {
        try {
            smsService.sendSms(request);
            markNotificationAsSent(request);
        } catch (Exception e) {
            handleNotificationError(request, e);
        }
    }
    
    private void markNotificationAsSent(NotificationRequest request) {
        request.setStatus(NotificationRequest.NotificationStatus.SENT);
        request.setSentAt(LocalDateTime.now());
        notificationRepository.save(request);
        log.info("Notification {} sent successfully", request.getId());
    }
    
    private void handleNotificationError(NotificationRequest request, Exception e) {
        log.error("Failed to process notification {}: {}", request.getId(), e.getMessage(), e);
        
        request.incrementRetryCount();
        request.setErrorMessage(e.getMessage());
        
        if (request.canRetry()) {
            request.setStatus(NotificationRequest.NotificationStatus.FAILED);
            log.info("Notification {} will be retried. Attempt: {}/{}", 
                    request.getId(), request.getRetryCount(), request.getMaxRetryAttempts());
        } else {
            request.setStatus(NotificationRequest.NotificationStatus.FAILED);
            log.error("Notification {} failed permanently after {} attempts", 
                     request.getId(), request.getRetryCount());
        }
        
        notificationRepository.save(request);
    }
    
    // Scheduled task to process scheduled notifications
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationRequest> scheduledNotifications = 
            notificationRepository.findByStatusAndScheduledAtLessThanEqual(
                NotificationRequest.NotificationStatus.PENDING, now);
        
        log.info("Found {} scheduled notifications to process", scheduledNotifications.size());
        
        for (NotificationRequest notification : scheduledNotifications) {
            try {
                processNotification(notification);
            } catch (Exception e) {
                log.error("Failed to process scheduled notification {}: {}", 
                         notification.getId(), e.getMessage(), e);
            }
        }
    }
    
    // Scheduled task to retry failed notifications
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        List<NotificationRequest> failedNotifications = 
            notificationRepository.findRetryableNotifications();
        
        log.info("Found {} failed notifications to retry", failedNotifications.size());
        
        for (NotificationRequest notification : failedNotifications) {
            if (notification.canRetry()) {
                log.info("Retrying notification {}, attempt: {}", 
                        notification.getId(), notification.getRetryCount() + 1);
                processNotification(notification);
            }
        }
    }
    
    // Cleanup old notifications
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up {} old notification records", deleted);
    }
}