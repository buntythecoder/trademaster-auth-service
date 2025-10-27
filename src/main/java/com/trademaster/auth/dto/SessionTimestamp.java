package com.trademaster.auth.dto;

import java.time.LocalDateTime;

/**
 * Session timestamp record with builder support
 */
public record SessionTimestamp(String sessionId, LocalDateTime timestamp) {

    public static SessionTimestamp of(String sessionId, LocalDateTime timestamp) {
        return new SessionTimestamp(sessionId, timestamp);
    }

    /**
     * Create builder instance
     */
    public static SessionTimestampBuilder builder() {
        return new SessionTimestampBuilder();
    }

    /**
     * Builder for SessionTimestamp
     * Provides fluent API for constructing session timestamps
     */
    public static class SessionTimestampBuilder {
        private String sessionId;
        private LocalDateTime timestamp;

        public SessionTimestampBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public SessionTimestampBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SessionTimestamp build() {
            return new SessionTimestamp(sessionId, timestamp);
        }
    }
}