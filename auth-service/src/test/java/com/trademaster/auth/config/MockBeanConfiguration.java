package com.trademaster.auth.config;

import com.trademaster.auth.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides mock beans for external dependencies
 * during test execution.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@TestConfiguration
@Profile("test")
public class MockBeanConfiguration {

    @MockBean
    private KmsClient kmsClient;

    @MockBean 
    private EmailService emailService;

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }
}