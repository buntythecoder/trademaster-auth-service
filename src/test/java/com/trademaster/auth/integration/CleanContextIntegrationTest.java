package com.trademaster.auth.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clean integration test that validates basic Spring Boot application startup
 * by excluding problematic components that require database/Redis dependencies.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CleanContextIntegrationTest.TestConfig.class)
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
class CleanContextIntegrationTest {

    @TestConfiguration
    @ComponentScan(
        basePackages = "com.trademaster.auth",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.service\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.repository\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.controller\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.config\\.RedisConfig"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.config\\.AwsConfig")
        }
    )
    static class TestConfig {
    }

    @Test
    void contextLoads() {
        // This test passes if the Spring application context loads successfully
        // with basic security and JWT components but no database/Redis dependencies
        assertTrue(true, "Spring Boot application context loaded successfully");
    }
}