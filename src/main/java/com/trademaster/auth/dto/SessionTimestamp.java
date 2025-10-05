package com.trademaster.auth.dto;

import java.time.LocalDateTime;

/**
 * Session timestamp record
 */
public record SessionTimestamp(String sessionId, LocalDateTime timestamp) {
    
    public static SessionTimestamp of(String sessionId, LocalDateTime timestamp) {
        return new SessionTimestamp(sessionId, timestamp);
    }
}