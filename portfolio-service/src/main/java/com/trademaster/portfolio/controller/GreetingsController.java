package com.trademaster.portfolio.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Greetings Controller for internal service-to-service health checks.
 *
 * Purpose: Provides a simple greeting endpoint for internal communication
 * and service discovery health verification following TradeMaster Golden Specification.
 *
 * Design Pattern: Simple REST controller with no business logic
 * Security: Protected by ServiceApiKeyFilter for internal paths
 *
 * @see com.trademaster.common.security.filter.AbstractServiceApiKeyFilter
 */
@Tag(name = "Service Discovery", description = "Internal greeting endpoint for service health verification and discovery")
@Slf4j
@RestController
@RequestMapping("/api/internal")
public class GreetingsController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${trademaster.common.service.version:1.0.0}")
    private String serviceVersion;

    /**
     * Internal greeting endpoint for service-to-service communication.
     *
     * This endpoint is used by other services to verify that the portfolio-service
     * is alive and responsive. It returns basic service metadata.
     *
     * Pattern: Simple information provider following functional programming principles
     * Security: Requires valid service API key (enforced by ServiceApiKeyFilter)
     * Performance: <10ms response time, no database calls
     *
     * @return ResponseEntity with service greeting and metadata
     */
    @GetMapping("/greetings")
    public ResponseEntity<Map<String, Object>> greetings() {
        log.debug("Internal greetings request received");

        return ResponseEntity.ok(
            Map.of(
                "service", serviceName,
                "version", serviceVersion,
                "message", "Portfolio Service is running and ready",
                "timestamp", Instant.now().toString(),
                "status", "OPERATIONAL",
                "capabilities", Map.of(
                    "portfolioTracking", "ACTIVE",
                    "pnlCalculation", "ACTIVE",
                    "riskAnalytics", "ACTIVE",
                    "performanceReporting", "ACTIVE"
                )
            )
        );
    }
}
