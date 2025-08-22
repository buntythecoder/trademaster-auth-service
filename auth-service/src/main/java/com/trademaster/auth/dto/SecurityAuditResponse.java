package com.trademaster.auth.dto;

import com.trademaster.auth.entity.SecurityAuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class SecurityAuditResponse {
    private String id;
    private String eventType;
    private String description;
    private String riskLevel;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String location;
    private Map<String, Object> metadata;
    
    public static SecurityAuditResponse fromEntity(SecurityAuditLog log) {
        return SecurityAuditResponse.builder()
                .id(log.getId().toString())
                .eventType(log.getEventType())
                .description(log.getDescription())
                .riskLevel(log.getRiskLevel() != null ? log.getRiskLevel().toString() : null)
                .timestamp(log.getTimestamp())
                .ipAddress(log.getIpAddress() != null ? log.getIpAddress().getHostAddress() : null)
                .location(log.getLocation())
                .metadata(log.getMetadata())
                .build();
    }
}