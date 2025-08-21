package com.trademaster.marketdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

/**
 * Redis Configuration for Market Data Caching
 * 
 * Features:
 * - Sub-5ms response time optimization
 * - Connection pooling with virtual threads
 * - JSON serialization for complex objects
 * - TTL-based cache strategies
 * - Pipeline operations for batch updates
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:2000}")
    private int timeoutMs;

    @Value("${spring.redis.lettuce.pool.max-active:20}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Value("${spring.redis.lettuce.pool.max-wait:2000}")
    private int maxWaitMs;

    /**
     * Redis connection factory optimized for performance
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);
        
        // Redis standalone configuration
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(database);
        
        // Client resources for connection optimization
        ClientResources clientResources = DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build();
        
        // Connection pooling configuration
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(getPoolConfig())
            .clientResources(clientResources)
            .commandTimeout(Duration.ofMillis(timeoutMs))
            .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(true);
        
        log.info("Redis connection factory configured with pool size: max={}, idle={}", 
            maxActive, maxIdle);
        
        return factory;
    }
    
    /**
     * Connection pool configuration for high performance
     */
    private org.apache.commons.pool2.impl.GenericObjectPoolConfig<org.apache.commons.pool2.PooledObject> getPoolConfig() {
        var poolConfig = new org.apache.commons.pool2.impl.GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMs));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        poolConfig.setMinEvictableIdleDuration(Duration.ofSeconds(300));
        return poolConfig;
    }

    /**
     * Primary Redis template with JSON serialization
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Create optimized ObjectMapper for serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        
        // Use String serializer for keys (faster lookup)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        log.info("Redis template configured with JSON serialization");
        return template;
    }

    /**
     * String-only Redis template for simple caching
     */
    @Bean("stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache configuration for different data types
     */
    @Bean
    public MarketDataCacheConfig cacheConfig() {
        return MarketDataCacheConfig.builder()
            // Real-time data (very short TTL)
            .tickDataTtl(Duration.ofSeconds(5))
            .priceDataTtl(Duration.ofSeconds(10))
            .orderBookTtl(Duration.ofSeconds(3))
            
            // Aggregated data (longer TTL)
            .ohlcDataTtl(Duration.ofMinutes(1))
            .dailyStatsTtl(Duration.ofHours(1))
            .symbolListTtl(Duration.ofHours(6))
            
            // User session data
            .userSessionTtl(Duration.ofHours(24))
            .subscriptionDataTtl(Duration.ofMinutes(30))
            
            // System data
            .exchangeStatusTtl(Duration.ofMinutes(5))
            .marketHoursTtl(Duration.ofHours(12))
            .build();
    }

    /**
     * Redis key patterns for different data types
     */
    @Bean
    public RedisKeyPatterns keyPatterns() {
        return new RedisKeyPatterns();
    }

    /**
     * Health check for Redis connection using virtual threads
     */
    public sealed interface RedisHealthResult permits HealthyRedis, UnhealthyRedis {}
    
    record HealthyRedis(String info, long latencyMs) implements RedisHealthResult {}
    record UnhealthyRedis(String error, Exception cause) implements RedisHealthResult {}

    public RedisHealthResult checkRedisHealth() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            var healthTask = scope.fork(() -> {
                long startTime = System.currentTimeMillis();
                
                RedisTemplate<String, Object> template = redisTemplate(redisConnectionFactory());
                String testKey = "health:check:" + System.currentTimeMillis();
                
                // Test write and read
                template.opsForValue().set(testKey, "test", Duration.ofSeconds(1));
                String result = (String) template.opsForValue().get(testKey);
                template.delete(testKey);
                
                long latency = System.currentTimeMillis() - startTime;
                
                return "test".equals(result) 
                    ? new HealthyRedis("Connection healthy", latency)
                    : new UnhealthyRedis("Test data mismatch", null);
            });

            scope.join();
            scope.throwIfFailed();
            
            return healthTask.get();
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return new UnhealthyRedis("Health check exception", e);
        }
    }

    /**
     * Cache configuration record
     */
    public record MarketDataCacheConfig(
        Duration tickDataTtl,
        Duration priceDataTtl,
        Duration orderBookTtl,
        Duration ohlcDataTtl,
        Duration dailyStatsTtl,
        Duration symbolListTtl,
        Duration userSessionTtl,
        Duration subscriptionDataTtl,
        Duration exchangeStatusTtl,
        Duration marketHoursTtl
    ) {
        public static MarketDataCacheConfigBuilder builder() {
            return new MarketDataCacheConfigBuilder();
        }
    }

    public static class MarketDataCacheConfigBuilder {
        private Duration tickDataTtl;
        private Duration priceDataTtl;
        private Duration orderBookTtl;
        private Duration ohlcDataTtl;
        private Duration dailyStatsTtl;
        private Duration symbolListTtl;
        private Duration userSessionTtl;
        private Duration subscriptionDataTtl;
        private Duration exchangeStatusTtl;
        private Duration marketHoursTtl;

        public MarketDataCacheConfigBuilder tickDataTtl(Duration tickDataTtl) {
            this.tickDataTtl = tickDataTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder priceDataTtl(Duration priceDataTtl) {
            this.priceDataTtl = priceDataTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder orderBookTtl(Duration orderBookTtl) {
            this.orderBookTtl = orderBookTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder ohlcDataTtl(Duration ohlcDataTtl) {
            this.ohlcDataTtl = ohlcDataTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder dailyStatsTtl(Duration dailyStatsTtl) {
            this.dailyStatsTtl = dailyStatsTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder symbolListTtl(Duration symbolListTtl) {
            this.symbolListTtl = symbolListTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder userSessionTtl(Duration userSessionTtl) {
            this.userSessionTtl = userSessionTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder subscriptionDataTtl(Duration subscriptionDataTtl) {
            this.subscriptionDataTtl = subscriptionDataTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder exchangeStatusTtl(Duration exchangeStatusTtl) {
            this.exchangeStatusTtl = exchangeStatusTtl;
            return this;
        }

        public MarketDataCacheConfigBuilder marketHoursTtl(Duration marketHoursTtl) {
            this.marketHoursTtl = marketHoursTtl;
            return this;
        }

        public MarketDataCacheConfig build() {
            return new MarketDataCacheConfig(
                tickDataTtl, priceDataTtl, orderBookTtl, ohlcDataTtl,
                dailyStatsTtl, symbolListTtl, userSessionTtl, subscriptionDataTtl,
                exchangeStatusTtl, marketHoursTtl
            );
        }
    }

    /**
     * Redis key patterns for consistent naming
     */
    public static class RedisKeyPatterns {
        public static final String PRICE_PREFIX = "price:";
        public static final String TICK_PREFIX = "tick:";
        public static final String OHLC_PREFIX = "ohlc:";
        public static final String ORDER_BOOK_PREFIX = "orderbook:";
        public static final String SYMBOL_LIST_PREFIX = "symbols:";
        public static final String USER_SESSION_PREFIX = "session:";
        public static final String SUBSCRIPTION_PREFIX = "subscription:";
        public static final String EXCHANGE_STATUS_PREFIX = "exchange:";
        public static final String MARKET_HOURS_PREFIX = "hours:";

        public String priceKey(String symbol, String exchange) {
            return PRICE_PREFIX + exchange + ":" + symbol;
        }

        public String tickKey(String symbol, String exchange) {
            return TICK_PREFIX + exchange + ":" + symbol;
        }

        public String ohlcKey(String symbol, String exchange, String interval) {
            return OHLC_PREFIX + exchange + ":" + symbol + ":" + interval;
        }

        public String orderBookKey(String symbol, String exchange) {
            return ORDER_BOOK_PREFIX + exchange + ":" + symbol;
        }

        public String symbolListKey(String exchange) {
            return SYMBOL_LIST_PREFIX + exchange;
        }

        public String userSessionKey(String userId) {
            return USER_SESSION_PREFIX + userId;
        }

        public String subscriptionKey(String sessionId) {
            return SUBSCRIPTION_PREFIX + sessionId;
        }

        public String exchangeStatusKey(String exchange) {
            return EXCHANGE_STATUS_PREFIX + exchange;
        }

        public String marketHoursKey(String exchange) {
            return MARKET_HOURS_PREFIX + exchange;
        }
    }
}