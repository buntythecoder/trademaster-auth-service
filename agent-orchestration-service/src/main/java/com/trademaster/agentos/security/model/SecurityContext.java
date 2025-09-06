package com.trademaster.agentos.security.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable security context for Zero Trust authentication and authorization.
 * Represents the complete security state for a request.
 */
public record SecurityContext(
    String correlationId,
    String userId,
    String sessionId,
    String token,
    Set<String> roles,
    Set<String> permissions,
    Map<String, Object> attributes,
    String ipAddress,
    String userAgent,
    Instant timestamp,
    SecurityLevel securityLevel,
    RiskScore riskScore
) {
    // Compact constructor with validation
    public SecurityContext {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (securityLevel == null) {
            securityLevel = SecurityLevel.STANDARD;
        }
        if (riskScore == null) {
            riskScore = new RiskScore(0.0, RiskLevel.LOW, Set.of());
        }
        
        // Defensive copies for collections
        roles = Set.copyOf(roles != null ? roles : Set.of());
        permissions = Set.copyOf(permissions != null ? permissions : Set.of());
        attributes = Map.copyOf(attributes != null ? attributes : Map.of());
    }
    
    /**
     * Security level enumeration for tiered access control
     */
    public enum SecurityLevel {
        PUBLIC(0),      // No authentication required
        STANDARD(1),    // Basic authentication
        ELEVATED(2),    // MFA required
        PRIVILEGED(3),  // Admin access
        CRITICAL(4);    // System-level access
        
        private final int level;
        
        SecurityLevel(int level) {
            this.level = level;
        }
        
        public boolean isHigherThan(SecurityLevel other) {
            return this.level > other.level;
        }
    }
    
    /**
     * Risk score for adaptive security
     */
    public record RiskScore(
        Double score,
        RiskLevel level,
        Set<String> riskFactors
    ) {
        public RiskScore {
            score = Math.max(0.0, Math.min(1.0, score != null ? score : 0.0));
            level = level != null ? level : RiskLevel.LOW;
            riskFactors = Set.copyOf(riskFactors != null ? riskFactors : Set.of());
        }
    }
    
    /**
     * Risk level enumeration
     */
    public enum RiskLevel {
        LOW(0.0, 0.3),
        MEDIUM(0.3, 0.6),
        HIGH(0.6, 0.8),
        CRITICAL(0.8, 1.0);
        
        private final double minScore;
        private final double maxScore;
        
        RiskLevel(double minScore, double maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        
        public static RiskLevel fromScore(double score) {
            if (score < 0.3) {
                return LOW;
            } else if (score < 0.6) {
                return MEDIUM;
            } else if (score < 0.8) {
                return HIGH;
            } else {
                return CRITICAL;
            }
        }
    }
    
    // Builder pattern for controlled object creation
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String correlationId;
        private String userId;
        private String sessionId;
        private String token;
        private Set<String> roles = Set.of();
        private Set<String> permissions = Set.of();
        private Map<String, Object> attributes = Map.of();
        private String ipAddress;
        private String userAgent;
        private Instant timestamp;
        private SecurityLevel securityLevel;
        private RiskScore riskScore;
        
        private Builder() {}
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }
        
        public Builder permissions(Set<String> permissions) {
            this.permissions = permissions;
            return this;
        }
        
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder securityLevel(SecurityLevel securityLevel) {
            this.securityLevel = securityLevel;
            return this;
        }
        
        public Builder riskScore(RiskScore riskScore) {
            this.riskScore = riskScore;
            return this;
        }
        
        public SecurityContext build() {
            return new SecurityContext(
                correlationId, userId, sessionId, token, roles, permissions,
                attributes, ipAddress, userAgent, timestamp, securityLevel, riskScore
            );
        }
    }
}