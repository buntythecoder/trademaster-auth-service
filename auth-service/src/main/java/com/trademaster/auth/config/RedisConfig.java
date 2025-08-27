package com.trademaster.auth.config;

import com.trademaster.auth.constants.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import java.time.Duration;

/**
 * Redis Configuration for session management and caching
 * 
 * Features:
 * - Redis connection configuration
 * - Session management with 24-hour TTL
 * - Custom serialization for security
 * - Connection pooling and timeout settings
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = AuthConstants.SESSION_MAX_INACTIVE_SECONDS) // 24 hours
@RequiredArgsConstructor
@Profile("!test") // Exclude this configuration in test profile
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:2000ms}")
    private Duration redisTimeout;

    /**
     * Redis connection factory configuration
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = 
            new RedisStandaloneConfiguration(redisHost, redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisStandaloneConfiguration.setPassword(redisPassword);
        }
        
        redisStandaloneConfiguration.setDatabase(redisDatabase);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Template method to configure Redis template with common settings
     * Follows Template Method pattern for consistent configuration
     */
    private <T> RedisTemplate<String, T> createConfiguredRedisTemplate(RedisConnectionFactory connectionFactory,
                                                                       Class<T> valueType) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Common key serialization configuration
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Configure value serializers based on type
        configureValueSerializers(template, valueType);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Strategy method to configure serializers based on value type
     */
    private <T> void configureValueSerializers(RedisTemplate<String, T> template, Class<T> valueType) {
        if (String.class.equals(valueType)) {
            // String-based serialization for sessions
            template.setValueSerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            template.setDefaultSerializer(new StringRedisSerializer());
        } else {
            // JSON-based serialization for general objects
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        }
    }

    /**
     * Redis template for general operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        return createConfiguredRedisTemplate(connectionFactory, Object.class);
    }

    /**
     * Redis template specifically for session data
     */
    @Bean
    public RedisTemplate<String, String> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        return createConfiguredRedisTemplate(connectionFactory, String.class);
    }
}