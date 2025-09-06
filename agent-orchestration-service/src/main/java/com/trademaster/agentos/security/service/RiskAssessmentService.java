package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Risk Assessment Service - Real-time threat analysis and risk scoring.
 * Implements adaptive security based on behavioral patterns and threat indicators.
 */
@Slf4j
@Service
public class RiskAssessmentService {
    
    // Risk factors and their weights
    private static final double LOCATION_RISK_WEIGHT = 0.15;
    private static final double BEHAVIOR_RISK_WEIGHT = 0.25;
    private static final double TIME_RISK_WEIGHT = 0.10;
    private static final double VELOCITY_RISK_WEIGHT = 0.20;
    private static final double AUTHENTICATION_RISK_WEIGHT = 0.15;
    private static final double PRIVILEGE_RISK_WEIGHT = 0.15;
    
    private final Map<String, UserBehaviorProfile> behaviorProfiles = new ConcurrentHashMap<>();
    private final Map<String, List<SecurityEvent>> userEvents = new ConcurrentHashMap<>();
    private final Set<String> suspiciousIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> trustedIPs = ConcurrentHashMap.newKeySet();
    
    private final double riskThreshold;
    private final boolean adaptiveMode;
    
    public RiskAssessmentService(
            @Value("${security.risk.threshold:0.7}") double riskThreshold,
            @Value("${security.risk.adaptive:true}") boolean adaptiveMode) {
        
        this.riskThreshold = riskThreshold;
        this.adaptiveMode = adaptiveMode;
        initializeThreatIntelligence();
    }
    
    /**
     * Assess risk for security context.
     */
    public Result<SecurityContext, SecurityError> assessRisk(SecurityContext context) {
        log.debug("Assessing risk: userId={}, correlationId={}", 
            context.userId(), context.correlationId());
        
        try {
            // Calculate risk components
            double locationRisk = assessLocationRisk(context);
            double behaviorRisk = assessBehaviorRisk(context);
            double timeRisk = assessTimeRisk(context);
            double velocityRisk = assessVelocityRisk(context);
            double authenticationRisk = assessAuthenticationRisk(context);
            double privilegeRisk = assessPrivilegeRisk(context);
            
            // Calculate weighted risk score
            double totalRisk = (locationRisk * LOCATION_RISK_WEIGHT) +
                              (behaviorRisk * BEHAVIOR_RISK_WEIGHT) +
                              (timeRisk * TIME_RISK_WEIGHT) +
                              (velocityRisk * VELOCITY_RISK_WEIGHT) +
                              (authenticationRisk * AUTHENTICATION_RISK_WEIGHT) +
                              (privilegeRisk * PRIVILEGE_RISK_WEIGHT);
            
            // Collect risk factors
            Set<String> riskFactors = collectRiskFactors(
                locationRisk, behaviorRisk, timeRisk, 
                velocityRisk, authenticationRisk, privilegeRisk);
            
            // Update behavior profile
            updateBehaviorProfile(context, totalRisk);
            
            // Record security event
            recordSecurityEvent(context, totalRisk);
            
            // Determine risk level
            SecurityContext.RiskLevel riskLevel = SecurityContext.RiskLevel.fromScore(totalRisk);
            
            log.info("Risk assessment complete: userId={}, score={}, level={}", 
                context.userId(), String.format("%.2f", totalRisk), riskLevel);
            
            // Create updated context with risk score
            SecurityContext riskContext = SecurityContext.builder()
                .correlationId(context.correlationId())
                .userId(context.userId())
                .sessionId(context.sessionId())
                .token(context.token())
                .roles(context.roles())
                .permissions(context.permissions())
                .attributes(context.attributes())
                .ipAddress(context.ipAddress())
                .userAgent(context.userAgent())
                .timestamp(context.timestamp())
                .securityLevel(context.securityLevel())
                .riskScore(new SecurityContext.RiskScore(totalRisk, riskLevel, riskFactors))
                .build();
            
            // Check if risk exceeds threshold
            if (totalRisk > riskThreshold) {
                log.warn("Risk threshold exceeded: userId={}, score={}", 
                    context.userId(), totalRisk);
                
                if (adaptiveMode) {
                    // Add to suspicious IPs if threshold exceeded multiple times
                    addSuspiciousIP(context.ipAddress());
                }
                
                return Result.failure(SecurityError.riskThresholdExceeded(
                    totalRisk, context.correlationId()));
            }
            
            return Result.success(riskContext);
            
        } catch (Exception e) {
            log.error("Risk assessment failed: correlationId={}", 
                context.correlationId(), e);
            return Result.failure(SecurityError.authorizationDenied(
                "Risk assessment failed", context.correlationId()));
        }
    }
    
    /**
     * Get user behavior profile.
     */
    public Optional<UserBehaviorProfile> getUserProfile(String userId) {
        return Optional.ofNullable(behaviorProfiles.get(userId));
    }
    
    /**
     * Check if IP is suspicious.
     */
    public boolean isSuspiciousIP(String ipAddress) {
        return suspiciousIPs.contains(ipAddress);
    }
    
    /**
     * Add trusted IP address.
     */
    public void addTrustedIP(String ipAddress) {
        trustedIPs.add(ipAddress);
        suspiciousIPs.remove(ipAddress);
    }
    
    // Risk assessment methods
    
    private double assessLocationRisk(SecurityContext context) {
        String ip = context.ipAddress();
        
        if (ip == null || ip.isBlank()) {
            return 0.5; // Unknown location
        }
        
        // Check trusted IPs
        if (trustedIPs.contains(ip)) {
            return 0.0;
        }
        
        // Check suspicious IPs
        if (suspiciousIPs.contains(ip)) {
            return 1.0;
        }
        
        // Check if internal IP
        if (isInternalIP(ip)) {
            return 0.1;
        }
        
        // Check user's typical locations
        UserBehaviorProfile profile = behaviorProfiles.get(context.userId());
        if (profile != null && !profile.knownIPs.contains(ip)) {
            return 0.7; // New location
        }
        
        return 0.3; // Default moderate risk
    }
    
    private double assessBehaviorRisk(SecurityContext context) {
        UserBehaviorProfile profile = behaviorProfiles.get(context.userId());
        
        if (profile == null) {
            // New user, moderate risk
            return 0.5;
        }
        
        // Check for anomalous behavior
        double anomalyScore = 0.0;
        
        // Check access pattern
        if (isAnomalousAccessPattern(context, profile)) {
            anomalyScore += 0.3;
        }
        
        // Check privilege escalation
        if (isPrivilegeEscalation(context, profile)) {
            anomalyScore += 0.4;
        }
        
        // Check failed attempts
        if (profile.failedAttempts.get() > 3) {
            anomalyScore += 0.3;
        }
        
        return Math.min(1.0, anomalyScore);
    }
    
    private double assessTimeRisk(SecurityContext context) {
        int hour = java.time.LocalTime.now().getHour();
        
        // Higher risk during non-business hours
        if (hour < 6 || hour > 22) {
            return 0.7;
        }
        
        // Check user's typical access times
        UserBehaviorProfile profile = behaviorProfiles.get(context.userId());
        if (profile != null && !profile.typicalHours.contains(hour)) {
            return 0.5;
        }
        
        return 0.1;
    }
    
    private double assessVelocityRisk(SecurityContext context) {
        List<SecurityEvent> events = userEvents.getOrDefault(context.userId(), List.of());
        
        if (events.size() < 2) {
            return 0.0;
        }
        
        // Check for impossible travel (accessing from different locations too quickly)
        SecurityEvent lastEvent = events.get(events.size() - 1);
        Duration timeDiff = Duration.between(lastEvent.timestamp, Instant.now());
        
        if (!lastEvent.ipAddress.equals(context.ipAddress()) && 
            timeDiff.toMinutes() < 5) {
            // Different IP within 5 minutes - high risk
            return 0.9;
        }
        
        // Check for rapid requests
        long recentRequests = events.stream()
            .filter(e -> Duration.between(e.timestamp, Instant.now()).toMinutes() < 1)
            .count();
        
        if (recentRequests > 10) {
            return 0.8; // Too many requests
        }
        
        return 0.0;
    }
    
    private double assessAuthenticationRisk(SecurityContext context) {
        // Check security level
        return switch (context.securityLevel()) {
            case PUBLIC -> 0.8;
            case STANDARD -> 0.3;
            case ELEVATED -> 0.1;
            case PRIVILEGED -> 0.05;
            case CRITICAL -> 0.0;
        };
    }
    
    private double assessPrivilegeRisk(SecurityContext context) {
        // Higher risk for higher privileges
        if (context.roles().contains("ADMIN")) {
            return 0.7;
        }
        if (context.roles().contains("MANAGER")) {
            return 0.5;
        }
        if (context.roles().contains("SERVICE")) {
            return 0.6;
        }
        return 0.2;
    }
    
    // Helper methods
    
    private Set<String> collectRiskFactors(double... risks) {
        Set<String> factors = new HashSet<>();
        
        if (risks[0] > 0.5) factors.add("LOCATION_RISK");
        if (risks[1] > 0.5) factors.add("BEHAVIOR_ANOMALY");
        if (risks[2] > 0.5) factors.add("UNUSUAL_TIME");
        if (risks[3] > 0.5) factors.add("VELOCITY_RISK");
        if (risks[4] > 0.5) factors.add("WEAK_AUTHENTICATION");
        if (risks[5] > 0.5) factors.add("HIGH_PRIVILEGE");
        
        return factors;
    }
    
    private void updateBehaviorProfile(SecurityContext context, double riskScore) {
        behaviorProfiles.compute(context.userId(), (userId, profile) -> {
            if (profile == null) {
                profile = new UserBehaviorProfile(userId);
            }
            
            profile.knownIPs.add(context.ipAddress());
            profile.typicalHours.add(java.time.LocalTime.now().getHour());
            profile.lastAccess = Instant.now();
            profile.averageRiskScore = (profile.averageRiskScore + riskScore) / 2;
            
            if (riskScore < 0.3) {
                profile.failedAttempts.set(0); // Reset on successful low-risk access
            }
            
            return profile;
        });
    }
    
    private void recordSecurityEvent(SecurityContext context, double riskScore) {
        SecurityEvent event = new SecurityEvent(
            context.userId(),
            context.ipAddress(),
            context.userAgent(),
            Instant.now(),
            riskScore
        );
        
        userEvents.compute(context.userId(), (userId, events) -> {
            if (events == null) {
                events = new ArrayList<>();
            }
            events.add(event);
            
            // Keep only last 100 events
            if (events.size() > 100) {
                events = new ArrayList<>(events.subList(events.size() - 100, events.size()));
            }
            
            return events;
        });
    }
    
    private boolean isInternalIP(String ip) {
        return ip.startsWith("10.") || 
               ip.startsWith("192.168.") || 
               ip.startsWith("172.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("::1");
    }
    
    private boolean isAnomalousAccessPattern(SecurityContext context, UserBehaviorProfile profile) {
        // Check if access pattern deviates from normal
        return profile.averageRiskScore < 0.3 && 
               !profile.knownIPs.contains(context.ipAddress());
    }
    
    private boolean isPrivilegeEscalation(SecurityContext context, UserBehaviorProfile profile) {
        // Check if user suddenly has more privileges
        return context.roles().contains("ADMIN") && 
               profile.typicalRoles.stream().noneMatch(r -> r.equals("ADMIN"));
    }
    
    private void addSuspiciousIP(String ipAddress) {
        if (ipAddress != null && !isInternalIP(ipAddress)) {
            suspiciousIPs.add(ipAddress);
        }
    }
    
    private void initializeThreatIntelligence() {
        // Initialize with known suspicious IPs (would be loaded from threat intelligence feed)
        suspiciousIPs.addAll(Set.of(
            // Example suspicious IPs
        ));
        
        // Initialize trusted IPs
        trustedIPs.addAll(Set.of(
            "127.0.0.1",
            "::1"
        ));
    }
    
    // Inner classes
    
    private static class UserBehaviorProfile {
        final String userId;
        final Set<String> knownIPs = ConcurrentHashMap.newKeySet();
        final Set<Integer> typicalHours = ConcurrentHashMap.newKeySet();
        final Set<String> typicalRoles = ConcurrentHashMap.newKeySet();
        final AtomicInteger failedAttempts = new AtomicInteger(0);
        volatile Instant lastAccess = Instant.now();
        volatile double averageRiskScore = 0.5;
        
        UserBehaviorProfile(String userId) {
            this.userId = userId;
        }
    }
    
    private record SecurityEvent(
        String userId,
        String ipAddress,
        String userAgent,
        Instant timestamp,
        double riskScore
    ) {}
}