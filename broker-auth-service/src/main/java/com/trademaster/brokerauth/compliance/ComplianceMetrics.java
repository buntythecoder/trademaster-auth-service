package com.trademaster.brokerauth.compliance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compliance Metrics Service
 * 
 * Comprehensive compliance monitoring and metrics collection for:
 * - SOC2 Type II: Trust Service Criteria metrics and KPIs
 * - PCI-DSS: Cardholder data environment monitoring
 * - GDPR: Data protection and privacy rights metrics  
 * - CCPA: Consumer privacy compliance tracking
 * 
 * Features:
 * - Real-time compliance metrics collection
 * - Automated compliance scoring and reporting
 * - Threshold-based alerting for compliance violations
 * - Historical trend analysis and reporting
 * - Integration with monitoring systems (Prometheus, Grafana)
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComplianceMetrics {

    private final MeterRegistry meterRegistry;
    
    // Compliance Framework Counters
    private Counter soc2EventCounter;
    private Counter pciDssEventCounter;
    private Counter gdprEventCounter;
    private Counter ccpaEventCounter;
    
    // Security Event Counters
    private Counter authenticationFailureCounter;
    private Counter authenticationSuccessCounter;
    private Counter unauthorizedAccessCounter;
    private Counter privilegeEscalationCounter;
    private Counter dataBreachCounter;
    
    // Data Access and Privacy Counters
    private Counter dataAccessCounter;
    private Counter dataModificationCounter;
    private Counter personalDataAccessCounter;
    private Counter privacyRightsRequestCounter;
    private Counter consentWithdrawalCounter;
    private Counter dataExportRequestCounter;
    private Counter dataErasureRequestCounter;
    
    // System Availability and Performance
    private Counter systemFailureCounter;
    private Counter systemRecoveryCounter;
    private Timer responseTimeTimer;
    private Gauge systemUptimeGauge;
    private Gauge complianceScoreGauge;
    
    // Administrative Action Counters
    private Counter adminActionCounter;
    private Counter configurationChangeCounter;
    private Counter userPrivilegeChangeCounter;
    private Counter systemMaintenanceCounter;
    
    // Real-time Compliance Tracking
    private final Map<String, AtomicLong> complianceViolations = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> complianceEvents = new ConcurrentHashMap<>();
    private final AtomicReference<LocalDateTime> lastComplianceUpdate = new AtomicReference<>(LocalDateTime.now());
    
    // Compliance Thresholds and Targets
    private static final double SOC2_AVAILABILITY_TARGET = 99.9;
    private static final double PCI_DSS_SECURITY_SCORE_TARGET = 95.0;
    private static final double GDPR_PRIVACY_SCORE_TARGET = 90.0;
    private static final long MAX_PRIVACY_RESPONSE_TIME_DAYS = 30;
    private static final double CRITICAL_COMPLIANCE_THRESHOLD = 85.0;
    
    @PostConstruct
    public void initializeMetrics() {
        log.info("📊 Initializing Compliance Metrics...");
        
        // Initialize Compliance Framework Counters
        soc2EventCounter = Counter.builder("compliance.soc2.events")
            .description("SOC2 compliance events")
            .tag("framework", "soc2")
            .register(meterRegistry);
            
        pciDssEventCounter = Counter.builder("compliance.pci_dss.events")
            .description("PCI-DSS compliance events")
            .tag("framework", "pci_dss")
            .register(meterRegistry);
            
        gdprEventCounter = Counter.builder("compliance.gdpr.events")
            .description("GDPR compliance events")
            .tag("framework", "gdpr")
            .register(meterRegistry);
            
        ccpaEventCounter = Counter.builder("compliance.ccpa.events")
            .description("CCPA compliance events")
            .tag("framework", "ccpa")
            .register(meterRegistry);
        
        // Initialize Security Event Counters
        authenticationFailureCounter = Counter.builder("security.authentication.failures")
            .description("Authentication failure events")
            .register(meterRegistry);
            
        authenticationSuccessCounter = Counter.builder("security.authentication.successes")
            .description("Authentication success events")
            .register(meterRegistry);
            
        unauthorizedAccessCounter = Counter.builder("security.unauthorized.access")
            .description("Unauthorized access attempts")
            .register(meterRegistry);
            
        privilegeEscalationCounter = Counter.builder("security.privilege.escalation")
            .description("Privilege escalation attempts")
            .register(meterRegistry);
            
        dataBreachCounter = Counter.builder("security.data.breach")
            .description("Data breach incidents")
            .register(meterRegistry);
        
        // Initialize Data Access Counters
        dataAccessCounter = Counter.builder("data.access.events")
            .description("Data access events")
            .register(meterRegistry);
            
        dataModificationCounter = Counter.builder("data.modification.events")
            .description("Data modification events")
            .register(meterRegistry);
            
        personalDataAccessCounter = Counter.builder("privacy.personal_data.access")
            .description("Personal data access events")
            .register(meterRegistry);
        
        // Initialize Privacy Rights Counters
        privacyRightsRequestCounter = Counter.builder("privacy.rights.requests")
            .description("Privacy rights exercise requests")
            .register(meterRegistry);
            
        consentWithdrawalCounter = Counter.builder("privacy.consent.withdrawals")
            .description("Consent withdrawal requests")
            .register(meterRegistry);
            
        dataExportRequestCounter = Counter.builder("privacy.data_export.requests")
            .description("Data portability requests")
            .register(meterRegistry);
            
        dataErasureRequestCounter = Counter.builder("privacy.data_erasure.requests")
            .description("Right to erasure requests")
            .register(meterRegistry);
        
        // Initialize System Metrics
        systemFailureCounter = Counter.builder("system.failures")
            .description("System failure events")
            .register(meterRegistry);
            
        systemRecoveryCounter = Counter.builder("system.recoveries")
            .description("System recovery events")
            .register(meterRegistry);
            
        responseTimeTimer = Timer.builder("system.response_time")
            .description("System response time")
            .register(meterRegistry);
        
        // Initialize Administrative Counters
        adminActionCounter = Counter.builder("admin.actions")
            .description("Administrative actions")
            .register(meterRegistry);
            
        configurationChangeCounter = Counter.builder("admin.config.changes")
            .description("Configuration changes")
            .register(meterRegistry);
            
        userPrivilegeChangeCounter = Counter.builder("admin.user.privilege_changes")
            .description("User privilege changes")
            .register(meterRegistry);
            
        systemMaintenanceCounter = Counter.builder("admin.system.maintenance")
            .description("System maintenance events")
            .register(meterRegistry);
        
        // Initialize Dynamic Gauges
        systemUptimeGauge = Gauge.builder("system.uptime.percent", () -> calculateSystemUptime())
            .description("System uptime percentage")
            .register(meterRegistry);
            
        complianceScoreGauge = Gauge.builder("compliance.score.overall", () -> calculateOverallComplianceScore())
            .description("Overall compliance score")
            .register(meterRegistry);
        
        // Initialize compliance tracking maps
        initializeComplianceTracking();
        
        log.info("✅ Compliance Metrics initialized successfully");
    }
    
    /**
     * Record SOC2 compliance event
     */
    public void recordSoc2Event(String eventType, String category, boolean successful) {
        Counter.builder("compliance.soc2.events")
            .description("SOC2 compliance events")
            .tag("event_type", eventType)
            .tag("category", category)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        updateComplianceEventCount("SOC2", successful);
        
        if (!successful) {
            recordComplianceViolation("SOC2", eventType);
        }
        
        log.debug("SOC2 event recorded: type={}, category={}, successful={}", eventType, category, successful);
    }
    
    /**
     * Record PCI-DSS compliance event
     */
    public void recordPciDssEvent(String eventType, String dataType, boolean successful) {
        Counter.builder("compliance.pci_dss.events")
            .description("PCI-DSS compliance events")
            .tag("event_type", eventType)
            .tag("data_type", dataType)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        updateComplianceEventCount("PCI_DSS", successful);
        
        if (!successful) {
            recordComplianceViolation("PCI_DSS", eventType);
        }
        
        log.debug("PCI-DSS event recorded: type={}, dataType={}, successful={}", eventType, dataType, successful);
    }
    
    /**
     * Record GDPR compliance event
     */
    public void recordGdprEvent(String rightType, String processingBasis, boolean successful) {
        Counter.builder("compliance.gdpr.events")
            .description("GDPR compliance events")
            .tag("right_type", rightType)
            .tag("processing_basis", processingBasis)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        updateComplianceEventCount("GDPR", successful);
        
        if (!successful) {
            recordComplianceViolation("GDPR", rightType);
        }
        
        log.debug("GDPR event recorded: rightType={}, processingBasis={}, successful={}", rightType, processingBasis, successful);
    }
    
    /**
     * Record CCPA compliance event
     */
    public void recordCcpaEvent(String consumerRight, String requestType, boolean successful) {
        Counter.builder("compliance.ccpa.events")
            .description("CCPA compliance events")
            .tag("consumer_right", consumerRight)
            .tag("request_type", requestType)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        updateComplianceEventCount("CCPA", successful);
        
        if (!successful) {
            recordComplianceViolation("CCPA", consumerRight);
        }
        
        log.debug("CCPA event recorded: consumerRight={}, requestType={}, successful={}", consumerRight, requestType, successful);
    }
    
    /**
     * Record authentication event
     */
    public void recordAuthenticationEvent(String brokerType, String method, boolean successful) {
        if (successful) {
            Counter.builder("security.authentication.successes")
                .description("Authentication success events")
                .tag("broker", brokerType)
                .tag("method", method)
                .register(meterRegistry)
                .increment();
        } else {
            Counter.builder("security.authentication.failures")
                .description("Authentication failure events")
                .tag("broker", brokerType)
                .tag("method", method)
                .register(meterRegistry)
                .increment();
            recordSoc2Event("AUTHENTICATION_FAILURE", "SECURITY", false);
        }
    }
    
    /**
     * Record unauthorized access attempt
     */
    public void recordUnauthorizedAccess(String resource, String attemptType, String userId) {
        Counter.builder("security.unauthorized.access")
            .description("Unauthorized access attempts")
            .tag("resource", resource)
            .tag("attempt_type", attemptType)
            .register(meterRegistry)
            .increment();
        recordSoc2Event("UNAUTHORIZED_ACCESS", "SECURITY", false);
        
        log.warn("🚨 Unauthorized access attempt: user={}, resource={}, type={}", userId, resource, attemptType);
    }
    
    /**
     * Record data access event
     */
    public void recordDataAccess(String dataType, String accessType, String purpose, boolean successful) {
        Counter.builder("data.access.events")
            .description("Data access events")
            .tag("data_type", dataType)
            .tag("access_type", accessType)
            .tag("purpose", purpose)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        if (isPersonalData(dataType)) {
            Counter.builder("privacy.personal_data.access")
                .description("Personal data access events")
                .tag("data_type", dataType)
                .tag("purpose", purpose)
                .register(meterRegistry)
                .increment();
            recordGdprEvent("DATA_ACCESS", purpose, successful);
        }
        
        if (isPciData(dataType)) {
            recordPciDssEvent("DATA_ACCESS", dataType, successful);
        }
    }
    
    /**
     * Record privacy rights request
     */
    public void recordPrivacyRightsRequest(String rightType, String jurisdiction, String status) {
        Counter.builder("privacy.rights.requests")
            .description("Privacy rights exercise requests")
            .tag("right_type", rightType)
            .tag("jurisdiction", jurisdiction)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
        
        switch (rightType.toLowerCase()) {
            case "access":
                recordGdprEvent("RIGHT_OF_ACCESS", "LEGITIMATE_INTEREST", "COMPLETED".equals(status));
                break;
            case "portability":
                Counter.builder("privacy.data_export.requests")
                    .description("Data portability requests")
                    .tag("jurisdiction", jurisdiction)
                    .register(meterRegistry)
                    .increment();
                recordGdprEvent("DATA_PORTABILITY", "CONTRACT", "COMPLETED".equals(status));
                break;
            case "erasure":
                Counter.builder("privacy.data_erasure.requests")
                    .description("Right to erasure requests")
                    .tag("jurisdiction", jurisdiction)
                    .register(meterRegistry)
                    .increment();
                recordGdprEvent("RIGHT_TO_ERASURE", "CONSENT_WITHDRAWN", "COMPLETED".equals(status));
                break;
            case "opt_out":
                Counter.builder("privacy.consent.withdrawals")
                    .description("Consent withdrawal requests")
                    .tag("jurisdiction", jurisdiction)
                    .register(meterRegistry)
                    .increment();
                recordCcpaEvent("OPT_OUT", "SALE_SHARING", "COMPLETED".equals(status));
                break;
        }
    }
    
    /**
     * Record system availability event
     */
    public void recordSystemEvent(String componentId, String eventType, boolean successful, long responseTime) {
        if (successful) {
            Counter.builder("system.recoveries")
                .description("System recovery events")
                .tag("component", componentId)
                .tag("event", eventType)
                .register(meterRegistry)
                .increment();
        } else {
            Counter.builder("system.failures")
                .description("System failure events")
                .tag("component", componentId)
                .tag("event", eventType)
                .register(meterRegistry)
                .increment();
            recordSoc2Event("SYSTEM_FAILURE", "AVAILABILITY", false);
        }
        
        responseTimeTimer.record(responseTime, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record administrative action
     */
    public void recordAdminAction(String action, String targetResource, String adminUser, boolean successful) {
        Counter.builder("admin.actions")
            .description("Administrative actions")
            .tag("action", action)
            .tag("resource", targetResource)
            .tag("admin", adminUser)
            .tag("status", successful ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        switch (action.toLowerCase()) {
            case "config_change":
                Counter.builder("admin.config.changes")
                    .description("Configuration changes")
                    .tag("resource", targetResource)
                    .register(meterRegistry)
                    .increment();
                recordSoc2Event("CONFIGURATION_CHANGE", "GOVERNANCE", successful);
                break;
            case "privilege_change":
                Counter.builder("admin.user.privilege_changes")
                    .description("User privilege changes")
                    .tag("target_resource", targetResource)
                    .register(meterRegistry)
                    .increment();
                recordSoc2Event("PRIVILEGE_CHANGE", "GOVERNANCE", successful);
                break;
            case "maintenance":
                Counter.builder("admin.system.maintenance")
                    .description("System maintenance events")
                    .tag("component", targetResource)
                    .register(meterRegistry)
                    .increment();
                recordSoc2Event("SYSTEM_MAINTENANCE", "AVAILABILITY", successful);
                break;
        }
    }
    
    /**
     * Get compliance score for specific framework
     */
    public double getFrameworkComplianceScore(String framework) {
        long totalEvents = complianceEvents.getOrDefault(framework, new AtomicLong(0)).get();
        long violations = complianceViolations.getOrDefault(framework, new AtomicLong(0)).get();
        
        if (totalEvents == 0) return 100.0;
        
        double violationRate = (double) violations / totalEvents;
        return Math.max(0.0, 100.0 - (violationRate * 100.0));
    }
    
    /**
     * Get overall compliance score
     */
    public double getOverallComplianceScore() {
        return calculateOverallComplianceScore();
    }
    
    /**
     * Generate compliance report
     */
    public ComplianceReport generateComplianceReport() {
        return ComplianceReport.builder()
            .timestamp(ZonedDateTime.now())
            .overallScore(getOverallComplianceScore())
            .soc2Score(getFrameworkComplianceScore("SOC2"))
            .pciDssScore(getFrameworkComplianceScore("PCI_DSS"))
            .gdprScore(getFrameworkComplianceScore("GDPR"))
            .ccpaScore(getFrameworkComplianceScore("CCPA"))
            .systemUptime(calculateSystemUptime())
            .authenticationFailures(authenticationFailureCounter.count())
            .privacyRightsRequests(privacyRightsRequestCounter.count())
            .dataAccessEvents(dataAccessCounter.count())
            .securityIncidents(dataBreachCounter.count() + unauthorizedAccessCounter.count())
            .build();
    }
    
    // Scheduled compliance monitoring
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorComplianceThresholds() {
        double overallScore = getOverallComplianceScore();
        
        if (overallScore < CRITICAL_COMPLIANCE_THRESHOLD) {
            log.warn("🚨 CRITICAL: Overall compliance score below threshold: {}/100", overallScore);
            
            // Check individual framework scores
            checkFrameworkThresholds();
        }
        
        lastComplianceUpdate.set(LocalDateTime.now());
    }
    
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void generateDailyComplianceReport() {
        ComplianceReport report = generateComplianceReport();
        log.info("📊 Daily Compliance Report: Overall Score={}, SOC2={}, PCI-DSS={}, GDPR={}, CCPA={}", 
                report.getOverallScore(), report.getSoc2Score(), report.getPciDssScore(), 
                report.getGdprScore(), report.getCcpaScore());
    }
    
    // Private helper methods
    private void initializeComplianceTracking() {
        complianceEvents.put("SOC2", new AtomicLong(0));
        complianceEvents.put("PCI_DSS", new AtomicLong(0));
        complianceEvents.put("GDPR", new AtomicLong(0));
        complianceEvents.put("CCPA", new AtomicLong(0));
        
        complianceViolations.put("SOC2", new AtomicLong(0));
        complianceViolations.put("PCI_DSS", new AtomicLong(0));
        complianceViolations.put("GDPR", new AtomicLong(0));
        complianceViolations.put("CCPA", new AtomicLong(0));
    }
    
    private void updateComplianceEventCount(String framework, boolean successful) {
        complianceEvents.get(framework).incrementAndGet();
    }
    
    private void recordComplianceViolation(String framework, String violationType) {
        complianceViolations.get(framework).incrementAndGet();
        log.warn("⚠️ Compliance violation recorded: framework={}, type={}", framework, violationType);
    }
    
    private double calculateOverallComplianceScore() {
        double soc2 = getFrameworkComplianceScore("SOC2");
        double pciDss = getFrameworkComplianceScore("PCI_DSS");
        double gdpr = getFrameworkComplianceScore("GDPR");
        double ccpa = getFrameworkComplianceScore("CCPA");
        
        // Weighted average based on framework criticality
        return (soc2 * 0.3 + pciDss * 0.3 + gdpr * 0.25 + ccpa * 0.15);
    }
    
    private double calculateSystemUptime() {
        long failures = (long) systemFailureCounter.count();
        long recoveries = (long) systemRecoveryCounter.count();
        long totalEvents = failures + recoveries + 1000; // Add baseline events
        
        return ((double) (totalEvents - failures) / totalEvents) * 100.0;
    }
    
    private void checkFrameworkThresholds() {
        Map<String, Double> scores = Map.of(
            "SOC2", getFrameworkComplianceScore("SOC2"),
            "PCI_DSS", getFrameworkComplianceScore("PCI_DSS"),
            "GDPR", getFrameworkComplianceScore("GDPR"),
            "CCPA", getFrameworkComplianceScore("CCPA")
        );
        
        scores.forEach((framework, score) -> {
            if (score < CRITICAL_COMPLIANCE_THRESHOLD) {
                log.error("🚨 CRITICAL: {} compliance score below threshold: {}/100", framework, score);
            }
        });
    }
    
    private boolean isPersonalData(String dataType) {
        return dataType != null && (
            dataType.toUpperCase().contains("PERSONAL") ||
            dataType.toUpperCase().contains("PII") ||
            dataType.toUpperCase().contains("EMAIL") ||
            dataType.toUpperCase().contains("PHONE") ||
            dataType.toUpperCase().contains("ADDRESS")
        );
    }
    
    private boolean isPciData(String dataType) {
        return dataType != null && (
            dataType.toUpperCase().contains("CARD") ||
            dataType.toUpperCase().contains("PAYMENT") ||
            dataType.toUpperCase().contains("PAN") ||
            dataType.toUpperCase().contains("CVV")
        );
    }
    
    /**
     * Compliance Report Data Class
     */
    @lombok.Data
    @lombok.Builder
    public static class ComplianceReport {
        private ZonedDateTime timestamp;
        private double overallScore;
        private double soc2Score;
        private double pciDssScore;
        private double gdprScore;
        private double ccpaScore;
        private double systemUptime;
        private double authenticationFailures;
        private double privacyRightsRequests;
        private double dataAccessEvents;
        private double securityIncidents;
    }
}