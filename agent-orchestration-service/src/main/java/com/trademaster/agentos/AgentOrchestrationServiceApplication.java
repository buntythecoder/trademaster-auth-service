package com.trademaster.agentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * TradeMaster Agent OS - Orchestration Service
 * 
 * Main application class for the Agent Orchestration Service.
 * This service coordinates AI agents, manages workflows, and handles
 * task distribution across the TradeMaster trading platform.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class AgentOrchestrationServiceApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads for maximum scalability
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(AgentOrchestrationServiceApplication.class, args);
    }
}