package com.trademaster.behavioralai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Behavioral AI Service Application
 * 
 * Provides intelligent behavioral analysis and pattern recognition for trading activities.
 * Uses advanced ML algorithms to detect emotions, behavioral patterns, and provide
 * coaching interventions for improved trading performance.
 * 
 * Key Features:
 * - Real-time emotion detection from trading behavior
 * - Pattern recognition for risk profiling
 * - Automated coaching interventions
 * - Trading psychology analytics
 * - Continuous learning from user behavior
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class BehavioralAIServiceApplication {

    public static void main(String[] args) {
        // Enable virtual threads and preview features
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(BehavioralAIServiceApplication.class, args);
    }
}