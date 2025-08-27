package com.trademaster.auth.context;

import com.trademaster.auth.entity.AuthAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Risk assessment context for audit processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskContext {
    private Long userId;
    private String eventType;
    private AuthAuditLog.EventStatus eventStatus;
    private String ipAddress;
    private String deviceFingerprint;
    private Map<String, Object> details;
    private int baseRiskScore;
}