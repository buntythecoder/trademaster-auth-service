package com.trademaster.auth.config;

import com.trademaster.auth.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for integration tests
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @MockitoBean
    private EmailService emailService;

    @Bean
    @Primary
    public KmsClient kmsClient() throws Exception {
        KmsClient kmsClient = mock(KmsClient.class);

        // Mock KMS response with a real AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        byte[] encryptedKeyBytes = new byte[256];
        new SecureRandom().nextBytes(encryptedKeyBytes);

        GenerateDataKeyResponse mockResponse = GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(keyBytes))
            .ciphertextBlob(SdkBytes.fromByteArray(encryptedKeyBytes))
            .build();

        lenient().when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class)))
            .thenReturn(mockResponse);

        return kmsClient;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    @Bean("stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean
    @Primary
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabase(Database.H2);
        adapter.setGenerateDdl(true);
        adapter.setShowSql(false);
        return adapter;
    }

    @Bean
    @Primary
    public KongConfiguration.KongServiceConfig kongServiceConfig() {
        return new KongConfiguration.KongServiceConfig(
            "http://localhost:8001",  // adminUrl
            "http://localhost:8000",  // gatewayUrl
            "trademaster-auth-service-test",  // serviceName
            "http://localhost:8080",  // serviceUrl
            false,  // adminAuthEnabled
            ""  // adminAuthToken
        );
    }

    @Bean
    @Primary
    public ConsulConfig.ConsulServiceConfiguration consulServiceConfiguration() {
        return new ConsulConfig.ConsulServiceConfiguration(
            false,  // enabled - disabled for tests
            "localhost",  // host
            8500,  // port
            "trademaster-auth-service-test",  // serviceName
            java.util.List.of("test", "auth-service"),  // tags
            java.util.Map.of("environment", "test")  // metadata
        );
    }
}