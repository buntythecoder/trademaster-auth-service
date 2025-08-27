package com.trademaster.brokerauth.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration
 * 
 * Configures Kafka for audit event publishing.
 * Includes topic configuration and producer settings optimized for audit trails.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${audit.kafka.producer.retries:3}")
    private int retries;
    
    @Value("${audit.kafka.producer.batch-size:16384}")
    private int batchSize;
    
    @Value("${audit.kafka.producer.linger-ms:5}")
    private int lingerMs;
    
    @Value("${audit.kafka.producer.buffer-memory:33554432}")
    private long bufferMemory;
    
    @Value("${audit.kafka.producer.acks:all}")
    private String acks;
    
    @Value("${audit.kafka.producer.compression-type:gzip}")
    private String compressionType;
    
    @Value("${audit.kafka.topic.partitions:3}")
    private int defaultPartitions;
    
    @Value("${audit.kafka.topic.replication-factor:1}")
    private short replicationFactor;
    
    /**
     * Kafka Producer Factory
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Performance configuration
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        
        // Reliability configuration
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Timeout configuration
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);
        
        // Client identification
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "broker-auth-service-audit");
        
        log.info("Configured Kafka producer with bootstrap servers: {}", bootstrapServers);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Kafka Template
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        
        // Configure default topic
        template.setDefaultTopic("trademaster.audit.events");
        
        // Enable observation for metrics
        template.setObservationEnabled(true);
        
        return template;
    }
    
    /**
     * Audit Events Topic
     */
    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("trademaster.audit.events")
                .partitions(defaultPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "gzip")
                .config("cleanup.policy", "delete")
                .config("segment.ms", "86400000") // 1 day
                .build();
    }
    
    /**
     * Security Events Topic
     */
    @Bean
    public NewTopic securityEventsTopic() {
        return TopicBuilder.name("trademaster.security.events")
                .partitions(defaultPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "gzip")
                .config("cleanup.policy", "delete")
                .config("segment.ms", "86400000") // 1 day
                .build();
    }
    
    /**
     * Compliance Events Topic
     */
    @Bean
    public NewTopic complianceEventsTopic() {
        return TopicBuilder.name("trademaster.compliance.events")
                .partitions(defaultPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "31536000000") // 365 days (1 year)
                .config("compression.type", "gzip")
                .config("cleanup.policy", "delete")
                .config("segment.ms", "86400000") // 1 day
                .build();
    }
    
    /**
     * Dead Letter Topic for failed audit events
     */
    @Bean
    public NewTopic auditDeadLetterTopic() {
        return TopicBuilder.name("trademaster.audit.events.dlt")
                .partitions(1) // Single partition for DLT
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "gzip")
                .config("cleanup.policy", "delete")
                .build();
    }
}