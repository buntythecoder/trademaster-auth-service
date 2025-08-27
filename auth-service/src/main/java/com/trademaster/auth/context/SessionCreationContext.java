package com.trademaster.auth.context;

import com.trademaster.auth.entity.SessionSettings;
import com.trademaster.auth.entity.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context for session creation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreationContext {
    private String userId;
    private String deviceFingerprint;
    private HttpServletRequest request;
    private SessionSettings settings;
    private String sessionId;
    private UserSession userSession;
    
    public SessionCreationContext(String userId, String deviceFingerprint, HttpServletRequest request, SessionSettings settings) {
        this.userId = userId;
        this.deviceFingerprint = deviceFingerprint;
        this.request = request;
        this.settings = settings;
    }
}