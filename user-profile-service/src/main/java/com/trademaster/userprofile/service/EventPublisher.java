package com.trademaster.userprofile.service;

import com.trademaster.userprofile.config.KafkaConfig;
import com.trademaster.userprofile.event.ProfileEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events to Kafka topics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final KafkaTemplate<String, ProfileEvent> kafkaTemplate;
    
    public void publishProfileCreated(ProfileEvent event) {
        publishEvent(KafkaConfig.PROFILE_CREATED_TOPIC, event);
    }
    
    public void publishProfileUpdated(ProfileEvent event) {
        publishEvent(KafkaConfig.PROFILE_UPDATED_TOPIC, event);
    }
    
    public void publishProfileDeleted(ProfileEvent event) {
        publishEvent(KafkaConfig.PROFILE_DELETED_TOPIC, event);
    }
    
    public void publishKycStatusChanged(ProfileEvent event) {
        publishEvent(KafkaConfig.KYC_STATUS_CHANGED_TOPIC, event);
    }
    
    public void publishDocumentUploaded(ProfileEvent event) {
        publishEvent(KafkaConfig.DOCUMENT_UPLOADED_TOPIC, event);
    }
    
    public void publishDocumentVerified(ProfileEvent event) {
        publishEvent(KafkaConfig.DOCUMENT_VERIFIED_TOPIC, event);
    }
    
    private void publishEvent(String topic, ProfileEvent event) {
        try {
            String key = event.getUserId().toString();
            
            CompletableFuture<SendResult<String, ProfileEvent>> future = 
                kafkaTemplate.send(topic, key, event);
                
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("Published event {} to topic {} with offset: {}", 
                            event.getEventId(), topic, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event {} to topic {}: {}", 
                            event.getEventId(), topic, throwable.getMessage(), throwable);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing event {} to topic {}: {}", 
                     event.getEventId(), topic, e.getMessage(), e);
        }
    }
}