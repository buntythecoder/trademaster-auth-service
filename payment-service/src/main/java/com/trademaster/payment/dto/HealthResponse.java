package com.trademaster.payment.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Health Response DTO
 * 
 * Response object for service health checks.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    
    private String status;
    private String service;
    private Instant timestamp;
    private String version;
    private Map<String, Object> details;
}