package com.trademaster.behavioralai.security;

import com.trademaster.behavioralai.functional.BehavioralAIError;
import com.trademaster.behavioralai.functional.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Risk Assessment Service
 * 
 * Evaluates security risks for API requests based on various factors.
 */
@Service
@RequiredArgsConstructor

public final class RiskAssessmentService {
    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private static final double HIGH_RISK_THRESHOLD = 0.8;
    private static final double MEDIUM_RISK_THRESHOLD = 0.5;

    /**
     * Assess security risk for the request
     * 
     * @param context Security context
     * @return Result with context or risk-based error
     */
    public Result<SecurityContext, BehavioralAIError> assessRisk(SecurityContext context) {
        RiskScore riskScore = calculateRiskScore(context);
        
        if (riskScore.totalRisk() > HIGH_RISK_THRESHOLD) {
            return Result.failure(BehavioralAIError.ValidationError.businessRuleViolation(
                "high_security_risk", "Request blocked due to high security risk: " + riskScore.totalRisk()));
        }

        if (riskScore.totalRisk() > MEDIUM_RISK_THRESHOLD) {
            log.warn("Medium security risk detected for user {} on endpoint {}: {}", 
                context.userId(), context.endpoint(), riskScore.totalRisk());
        }

        return Result.success(context);
    }

    private RiskScore calculateRiskScore(SecurityContext context) {
        double locationRisk = assessLocationRisk(context);
        double timeRisk = assessTimeRisk(context);
        double behaviorRisk = assessBehaviorRisk(context);
        double deviceRisk = assessDeviceRisk(context);

        // Weighted risk calculation
        double totalRisk = (locationRisk * 0.3) + 
                          (timeRisk * 0.2) + 
                          (behaviorRisk * 0.3) + 
                          (deviceRisk * 0.2);

        return new RiskScore(
            totalRisk,
            locationRisk,
            timeRisk,
            behaviorRisk,
            deviceRisk,
            Instant.now()
        );
    }

    private double assessLocationRisk(SecurityContext context) {
        if (context.clientInfo() == null) {
            return 0.3; // Medium risk for missing location info
        }

        String location = context.clientInfo().location();
        if (location == null) {
            return 0.3;
        }

        // Mock location-based risk assessment
        return switch (location) {
            case "UNKNOWN" -> 0.6;
            case "VPN" -> 0.4;
            case "FOREIGN" -> 0.5;
            default -> 0.1; // Known good location
        };
    }

    private double assessTimeRisk(SecurityContext context) {
        Instant now = Instant.now();
        int hour = now.atZone(java.time.ZoneOffset.UTC).getHour();
        
        // Higher risk for requests outside business hours
        if (hour < 6 || hour > 22) {
            return 0.4;
        }
        
        return 0.1;
    }

    private double assessBehaviorRisk(SecurityContext context) {
        // Mock behavioral risk assessment based on request patterns
        String endpoint = context.endpoint();
        
        // Administrative endpoints carry higher risk
        if (endpoint.contains("/admin/")) {
            return 0.6;
        }
        
        // Data modification operations
        if ("POST".equals(context.httpMethod()) || "PUT".equals(context.httpMethod())) {
            return 0.3;
        }
        
        return 0.1;
    }

    private double assessDeviceRisk(SecurityContext context) {
        if (context.clientInfo() == null) {
            return 0.4;
        }

        String deviceFingerprint = context.clientInfo().deviceFingerprint();
        if (deviceFingerprint == null) {
            return 0.3;
        }

        // Mock device risk assessment
        if (deviceFingerprint.contains("suspicious")) {
            return 0.8;
        }

        if (context.clientInfo().firstSeen() != null && 
            context.clientInfo().firstSeen().isAfter(Instant.now().minusSeconds(3600))) {
            return 0.4; // New device within last hour
        }

        return 0.1;
    }

    public record RiskScore(
        double totalRisk,
        double locationRisk,
        double timeRisk,
        double behaviorRisk,
        double deviceRisk,
        Instant assessedAt
    ) {}
}