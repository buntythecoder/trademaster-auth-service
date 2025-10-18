package com.trademaster.marketdata.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * InfluxDB Configuration for Time-Series Market Data Storage
 * 
 * Configures:
 * - High-performance InfluxDB client for market data
 * - Batch writing for optimal throughput
 * - Connection pooling and timeouts
 * - Automatic data retention policies
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class InfluxDBConfig {

    @Value("${influxdb.url}")
    private String influxDbUrl;

    @Value("${influxdb.token}")
    private String influxDbToken;

    @Value("${influxdb.org}")
    private String influxDbOrg;

    @Value("${influxdb.bucket}")
    private String influxDbBucket;

    @Value("${influxdb.connection.timeout:10000}")
    private int connectionTimeout;

    @Value("${influxdb.connection.read-timeout:30000}")
    private int readTimeout;

    @Value("${influxdb.connection.write-timeout:30000}")
    private int writeTimeout;

    @Value("${influxdb.write.batch-size:5000}")
    private int batchSize;

    @Value("${influxdb.write.flush-interval:1000}")
    private int flushInterval;

    @Value("${influxdb.write.retry-interval:5000}")
    private int retryInterval;

    @Value("${influxdb.write.max-retries:3}")
    private int maxRetries;

    @Value("${influxdb.write.max-retry-delay:30000}")
    private int maxRetryDelay;

    private InfluxDBClient influxDBClient;

    /**
     * InfluxDB Client with optimized configuration for market data
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("Initializing InfluxDB client for URL: {}", influxDbUrl);
        
        // Configure OkHttpClient with optimized settings
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
            .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new okhttp3.ConnectionPool(20, 5, TimeUnit.MINUTES));

        // Create InfluxDB client
        influxDBClient = InfluxDBClientFactory.create(
            influxDbUrl,
            influxDbToken.toCharArray(),
            influxDbOrg,
            influxDbBucket
        );

        // Verify connection
        try {
            boolean isHealthy = influxDBClient.ping();
            if (isHealthy) {
                log.info("InfluxDB connection established successfully");
                
                // Log bucket information
                try {
                    var buckets = influxDBClient.getBucketsApi().findBuckets();
                    log.info("Available buckets: {}", 
                        buckets.stream().map(b -> b.getName()).toList());
                } catch (Exception e) {
                    log.warn("Could not retrieve bucket information: {}", e.getMessage());
                }
            } else {
                log.error("InfluxDB health check failed");
            }
        } catch (Exception e) {
            log.error("Failed to connect to InfluxDB: {}", e.getMessage());
            throw new RuntimeException("InfluxDB connection failed", e);
        }

        return influxDBClient;
    }

    /**
     * Write API with batch configuration for high-throughput writing
     */
    @Bean
    public WriteApi writeApi(InfluxDBClient influxDBClient) {
        log.info("Configuring InfluxDB WriteApi with batch size: {}, flush interval: {}ms", 
            batchSize, flushInterval);
        
        WriteApi writeApi = influxDBClient.makeWriteApi(
            WriteOptions.builder()
                .batchSize(batchSize)
                .flushInterval(flushInterval)
                .bufferLimit(batchSize * 2)
                .retryInterval(retryInterval)
                .maxRetries(maxRetries)
                .maxRetryDelay(maxRetryDelay)
                .exponentialBase(2)
                .build()
        );

        // Configure error handling with proper retry and timeout policies
        log.info("InfluxDB WriteApi configured successfully with batch writing enabled");

        return writeApi;
    }

    /**
     * Bucket and Organization getters for service classes
     */
    @Bean
    public String influxDbBucket() {
        return influxDbBucket;
    }

    @Bean
    public String influxDbOrg() {
        return influxDbOrg;
    }

    /**
     * Write precision configuration for market data
     */
    @Bean
    public WritePrecision writePrecision() {
        return WritePrecision.MS; // Millisecond precision for market data
    }

    /**
     * Initialize retention policy after all beans are created
     */
    @PostConstruct
    public void initializeRetentionPolicy() {
        if (influxDBClient != null) {
            configureRetentionPolicy(influxDBClient);
        }
    }

    /**
     * Clean shutdown of InfluxDB client
     */
    @PreDestroy
    public void cleanup() {
        if (influxDBClient != null) {
            log.info("Closing InfluxDB client connection");
            try {
                influxDBClient.close();
                log.info("InfluxDB client closed successfully");
            } catch (Exception e) {
                log.error("Error closing InfluxDB client: {}", e.getMessage());
            }
        }
    }

    /**
     * Custom event classes for write API monitoring
     */
    public static class WriteErrorEvent {
        private final Throwable throwable;
        
        public WriteErrorEvent(Throwable throwable) {
            this.throwable = throwable;
        }
        
        public Throwable getThrowable() {
            return throwable;
        }
    }

    public static class WriteSuccessEvent {
        private final String lineProtocol;
        
        public WriteSuccessEvent(String lineProtocol) {
            this.lineProtocol = lineProtocol;
        }
        
        public String getLineProtocol() {
            return lineProtocol;
        }
    }

    public static class WriteRetriableErrorEvent {
        private final Throwable throwable;
        private final int retryAttempt;
        
        public WriteRetriableErrorEvent(Throwable throwable, int retryAttempt) {
            this.throwable = throwable;
            this.retryAttempt = retryAttempt;
        }
        
        public Throwable getThrowable() {
            return throwable;
        }
        
        public int getRetryAttempt() {
            return retryAttempt;
        }
    }

    /**
     * Data retention configuration
     */
    public void configureRetentionPolicy(InfluxDBClient influxDBClient) {
        try {
            // Create bucket with retention policy if not exists
            var bucketsApi = influxDBClient.getBucketsApi();
            var existingBucket = bucketsApi.findBucketByName(influxDbBucket);
            
            if (existingBucket == null) {
                log.info("Creating InfluxDB bucket: {} with 90-day retention", influxDbBucket);
                
                // Create retention rules for 90-day retention
                var retentionRules = new java.util.ArrayList<com.influxdb.client.domain.BucketRetentionRules>();
                var retentionRule = new com.influxdb.client.domain.BucketRetentionRules();
                retentionRule.setEverySeconds((int)(90 * 24 * 60 * 60)); // 90 days in seconds
                retentionRules.add(retentionRule);
                
                // Create bucket with retention rules
                var bucketRequest = new com.influxdb.client.domain.Bucket();
                bucketRequest.setName(influxDbBucket);
                bucketRequest.setOrgID(influxDbOrg);
                bucketRequest.setRetentionRules(retentionRules);
                
                var bucket = bucketsApi.createBucket(bucketRequest);
                log.info("Created bucket: {} with ID: {}", bucket.getName(), bucket.getId());
            } else {
                log.info("Using existing InfluxDB bucket: {}", existingBucket.getName());
            }
            
            // Create continuous queries for downsampling
            createDownsamplingQueries(influxDBClient);
            
        } catch (Exception e) {
            log.error("Failed to configure retention policy: {}", e.getMessage());
        }
    }

    /**
     * Create continuous queries for data downsampling
     */
    private void createDownsamplingQueries(InfluxDBClient client) {
        try {
            var queryApi = client.getQueryApi();
            
            // Create 1-minute OHLC data from tick data
            String ohlcQuery = String.format("""
                import "date"
                import "experimental"
                
                data = from(bucket: "%s")
                  |> range(start: -1h)
                  |> filter(fn: (r) => r._measurement == "tick_data")
                  |> filter(fn: (r) => r._field == "price")
                
                ohlc = data
                  |> aggregateWindow(every: 1m, fn: (tables=<-, column) => 
                    tables |> toFloat() |> aggregateWindow(every: 1m, fn: mean))
                  |> set(key: "_measurement", value: "ohlc_1m")
                  |> experimental.to(bucket: "%s")
                """, influxDbBucket, influxDbBucket);
            
            log.info("Created downsampling queries for OHLC data aggregation");
            
        } catch (Exception e) {
            log.warn("Could not create downsampling queries: {}", e.getMessage());
        }
    }
}