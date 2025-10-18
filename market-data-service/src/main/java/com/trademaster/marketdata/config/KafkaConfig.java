package com.trademaster.marketdata.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for Market Data Service
 * 
 * Configures:
 * - High-throughput producers for market data publishing
 * - Consumer groups for real-time data processing
 * - Kafka Streams for complex event processing
 * - Schema Registry integration for Avro serialization
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // Topic Names
    public static final String MARKET_DATA_TOPIC = "market-data-raw";
    public static final String TICK_DATA_TOPIC = "tick-data";
    public static final String OHLC_DATA_TOPIC = "ohlc-data";
    public static final String ORDER_BOOK_TOPIC = "order-book-updates";
    public static final String TRADE_EVENTS_TOPIC = "trade-events";
    public static final String MARKET_STATUS_TOPIC = "market-status-updates";
    public static final String ERROR_TOPIC = "market-data-errors";
    public static final String DLQ_TOPIC = "market-data-dlq";

    /**
     * Producer Factory for High-Throughput Market Data Publishing
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic Configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        
        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("auto.register.schemas", true);
        props.put("use.latest.version", true);
        
        // Performance Configuration
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Batching for High Throughput
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB batches
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Wait 5ms for batching
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        
        // Timeouts
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        log.info("Kafka Producer configured with high-throughput settings");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(MARKET_DATA_TOPIC);
        return template;
    }

    /**
     * Consumer Factory for Real-time Data Processing
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic Configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);
        
        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        
        // Performance Configuration
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024); // 1MB
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024); // 1MB
        
        // Session and Heartbeat
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        log.info("Kafka Consumer configured for real-time processing");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Concurrency for High Throughput
        factory.setConcurrency(4);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Error Handling
        factory.setCommonErrorHandler(marketDataErrorHandler());
        
        // Performance Monitoring
        factory.getContainerProperties().setMissingTopicsFatal(false);
        factory.getContainerProperties().setIdleEventInterval(60000L);
        
        log.info("Kafka Listener Container Factory configured with {} threads", 4);
        return factory;
    }

    /**
     * JSON Consumer Factory for Non-Avro Messages
     */
    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId + "-json");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.trademaster.marketdata.dto");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "jsonKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }

    /**
     * Kafka Streams Configuration for Complex Event Processing
     */
    @Bean(name = "streamsConfig")
    public StreamsConfig streamsConfig() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic Configuration
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "market-data-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde");
        
        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);
        
        // Performance Configuration
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE);
        
        // State Store Configuration
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-market-data");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // Cache and Buffer Configuration
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 10 * 1024 * 1024); // 10MB
        props.put(StreamsConfig.RECEIVE_BUFFER_CONFIG, 64 * 1024); // 64KB
        props.put(StreamsConfig.SEND_BUFFER_CONFIG, 128 * 1024); // 128KB
        
        log.info("Kafka Streams configured for complex event processing");
        return new StreamsConfig(props);
    }

    /**
     * Error Handler for Market Data Processing
     */
    @Bean
    public org.springframework.kafka.listener.CommonErrorHandler marketDataErrorHandler() {
        return new org.springframework.kafka.listener.DefaultErrorHandler((record, exception) -> {
            log.error("Error processing market data record: {}", record, exception);
            // Send to DLQ topic with proper type conversion
            kafkaTemplate().send(DLQ_TOPIC, 
                record.key() != null ? record.key().toString() : null, 
                record.value() != null ? record.value().toString() : null);
        });
    }

    /**
     * Admin Configuration for Topic Management
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Topic Creation Beans
     */
    @Bean
    public org.apache.kafka.clients.admin.NewTopic marketDataTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(MARKET_DATA_TOPIC, 12, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic tickDataTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(TICK_DATA_TOPIC, 12, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic ohlcDataTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(OHLC_DATA_TOPIC, 6, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic orderBookTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(ORDER_BOOK_TOPIC, 12, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic tradeEventsTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(TRADE_EVENTS_TOPIC, 6, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic marketStatusTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(MARKET_STATUS_TOPIC, 3, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic errorTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(ERROR_TOPIC, 3, (short) 1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic dlqTopic() {
        return new org.apache.kafka.clients.admin.NewTopic(DLQ_TOPIC, 3, (short) 1);
    }
}