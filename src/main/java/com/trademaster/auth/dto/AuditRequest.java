package com.trademaster.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Audit request context for functional audit processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequest {
    private Long userId;
    private String eventType;
    private String eventStatus;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private Map<String, Object> details;
    private String sessionId;
}