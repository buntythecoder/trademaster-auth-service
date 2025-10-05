package com.trademaster.auth.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Context for legacy session creation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacySessionContext {
    private Long userId;
    private String deviceFingerprint;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> sessionData;
    private String sessionId;
}