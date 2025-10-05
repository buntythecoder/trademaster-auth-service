package com.trademaster.auth.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simplified integration test that validates basic Spring Boot application startup
 * without complex database or Redis dependencies.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
    "trademaster.jwt.secret=myTestSecretKeyThatIsAtLeast256BitsLongForHMACSHA256SecurityTestingPurpose",
    "trademaster.aws.kms.key-id=alias/test-key"
})
class SimpleIntegrationTest {

    @Test
    void contextLoads() {
        // This test passes if the Spring application context loads successfully
        assertTrue(true, "Spring Boot application context loaded successfully");
    }
}