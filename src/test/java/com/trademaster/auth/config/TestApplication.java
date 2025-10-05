package com.trademaster.auth.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

/**
 * Test Application Configuration
 * 
 * Minimal Spring Boot application for testing without complex dependencies
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.trademaster.auth",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.config\\.AwsConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.auth\\.config\\.RedisConfig")
    }
)
@Profile("test")
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}