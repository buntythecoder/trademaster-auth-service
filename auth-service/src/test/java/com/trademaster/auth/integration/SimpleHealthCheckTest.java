package com.trademaster.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple integration test focusing only on health check endpoint
 * to verify the basic Spring context loads correctly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Simple H2 configuration
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    
    // Disable problematic features
    "spring.flyway.enabled=false",
    "spring.session.store-type=none",
    "spring.cache.type=none",
    
    // JWT configuration
    "trademaster.jwt.secret=myTestSecretKeyThatIsAtLeast256BitsLongForHMACSHA256SecurityTestingPurpose",
    "trademaster.jwt.expiration=900000",
    "trademaster.jwt.refresh-expiration=86400000",
    "trademaster.jwt.issuer=test",
    
    // Test configuration
    "trademaster.aws.kms.key-id=alias/test-key",
    "trademaster.aws.region=us-east-1",
    "trademaster.audit.enabled=false",
    "trademaster.mfa.enabled=false"
})
@Transactional
class SimpleHealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockitoBean
    private KmsClient kmsClient;

    @Test
    void contextLoads() {
        // This test passes if the Spring context loads successfully
    }

    @Test
    void healthCheck_ShouldReturnServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("TradeMaster Auth Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
}