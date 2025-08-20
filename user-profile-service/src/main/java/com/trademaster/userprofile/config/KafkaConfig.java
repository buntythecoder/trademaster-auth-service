package com.trademaster.userprofile.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for TradeMaster User Profile Service
 * Configures topics, producers, consumers, and admin client
 */
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Topic names
    public static final String PROFILE_CREATED_TOPIC = "profile-created";
    public static final String PROFILE_UPDATED_TOPIC = "profile-updated";
    public static final String PROFILE_DELETED_TOPIC = "profile-deleted";
    public static final String KYC_STATUS_CHANGED_TOPIC = "kyc-status-changed";
    public static final String DOCUMENT_UPLOADED_TOPIC = "document-uploaded";
    public static final String DOCUMENT_VERIFIED_TOPIC = "document-verified";
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    // Profile Events Topics
    @Bean
    public NewTopic profileCreatedTopic() {
        return TopicBuilder.name(PROFILE_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic profileUpdatedTopic() {
        return TopicBuilder.name(PROFILE_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic profileDeletedTopic() {
        return TopicBuilder.name(PROFILE_DELETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // KYC Events Topics
    @Bean
    public NewTopic kycStatusChangedTopic() {
        return TopicBuilder.name(KYC_STATUS_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // Document Events Topics
    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder.name(DOCUMENT_UPLOADED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentVerifiedTopic() {
        return TopicBuilder.name(DOCUMENT_VERIFIED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}