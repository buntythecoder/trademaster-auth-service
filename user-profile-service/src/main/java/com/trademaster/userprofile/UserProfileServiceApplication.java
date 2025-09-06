package com.trademaster.userprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * TradeMaster User Profile Service Application
 * 
 * MANDATORY: Java 24 + Virtual Threads Architecture - Rule #1
 * MANDATORY: Spring Boot 3.5.3 with Virtual Threads enabled
 * MANDATORY: AgentOS framework integration with structured concurrency
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class UserProfileServiceApplication {

    public static void main(String[] args) {
        // MANDATORY: Enable Virtual Threads per TradeMaster Rule #1
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(UserProfileServiceApplication.class, args);
    }
}