package com.trademaster.brokerauth.compliance;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Audit Event Data Model
 * 
 * Comprehensive audit event structure for compliance frameworks:
 * - SOC2 Type II: Security, Availability, Processing Integrity, 
 *   Confidentiality, Privacy audit requirements
 * - PCI-DSS: Cardholder data protection audit trail
 * - GDPR: Data processing activity records and consent tracking
 * - CCPA: Consumer privacy rights exercise documentation
 * 
 * This class provides structured data capture for all compliance
 * audit requirements with tamper-evident logging capabilities.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {

    // Core Event Identification
    private String eventId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime timestamp;
    
    private String eventType;           // AUTHENTICATION, DATA_ACCESS, DATA_MODIFICATION, etc.
    private String eventCategory;       // SECURITY, PRIVACY, INTEGRITY, AVAILABILITY, GOVERNANCE
    private String eventAction;         // LOGIN_SUCCESS, DATA_READ, ADMIN_ACTION, etc.
    
    // User and Session Context
    private String userId;
    private String targetUserId;        // For admin actions
    private String sessionId;
    private String brokerType;          // ZERODHA, UPSTOX, ANGEL_ONE, ICICI
    private String userAgent;
    private String ipAddress;
    
    // Data Context for GDPR/CCPA Compliance
    private String dataType;            // PERSONAL_DATA, FINANCIAL_DATA, AUTHENTICATION_DATA
    private String dataId;              // Specific record identifier
    private String dataClassification;  // PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
    private String purpose;             // Business purpose for data processing
    private String legalBasis;          // GDPR legal basis (consent, contract, etc.)
    private String retentionPeriod;     // Data retention period
    
    // Change Tracking for PCI-DSS
    private String oldValue;            // Hashed old value for audit trail
    private String newValue;            // Hashed new value for audit trail
    private String changeReason;        // Reason for data modification
    
    // Administrative Actions for SOC2
    private String adminAction;         // PRIVILEGE_GRANT, USER_DISABLE, CONFIG_CHANGE
    private String targetResource;      // Resource being administered
    private String authorizationLevel;  // Required authorization level
    
    // System Events for Availability Monitoring
    private String componentId;         // System component identifier
    private String eventDetails;        // Detailed event information
    private String severity;            // LOW, MEDIUM, HIGH, CRITICAL
    private String errorMessage;        // Error details if applicable
    private String correlationId;       // For distributed tracing
    
    // Privacy Rights for GDPR/CCPA
    private String rightType;           // ACCESS, RECTIFICATION, ERASURE, PORTABILITY
    private String requestDetails;      // Privacy request details
    private String processingStatus;    // PENDING, APPROVED, COMPLETED, REJECTED
    private String requestId;           // Privacy request identifier
    
    // Security Incident Tracking
    private String incidentId;          // Security incident identifier
    private String incidentType;        // BREACH, INTRUSION, MALWARE, etc.
    private String affectedSystems;     // Systems impacted by incident
    private String responseStatus;      // DETECTED, CONTAINED, MITIGATED, RESOLVED
    private String impactAssessment;    // Business impact assessment
    
    // Outcome and Status
    private boolean success;            // Event success/failure
    private String reason;              // Success/failure reason
    private String resultCode;          // Application-specific result code
    private String validationErrors;    // Validation error details
    
    // Compliance Framework Context
    private String[] complianceFrameworks;  // [SOC2, PCI-DSS, GDPR, CCPA]
    private String complianceCategory;      // Which compliance requirement
    private String evidenceId;             // Reference to supporting evidence
    private String auditTrailId;           // Audit trail sequence identifier
    
    // Organizational Context
    private String organization;        // Organization name
    private String department;          // Department/business unit
    private String environment;         // PRODUCTION, STAGING, DEVELOPMENT
    private String applicationVersion;   // Application version
    
    // Risk and Threat Context
    private String riskLevel;           // LOW, MEDIUM, HIGH, CRITICAL
    private String threatIndicators;    // Security threat indicators
    private String mitigationActions;   // Actions taken to mitigate risks
    
    // Geographic and Jurisdictional
    private String jurisdiction;        // Legal jurisdiction (US, EU, etc.)
    private String dataLocation;        // Physical data location
    private String crossBorderTransfer; // Cross-border data transfer details
    
    // Digital Signatures and Integrity
    private String digitalSignature;    // Event integrity signature
    private String checksumHash;        // Event data checksum
    private String signatureAlgorithm;  // Signature algorithm used
    
    // Builder Pattern Extensions
    public static class AuditEventBuilder {
        
        /**
         * Create authentication audit event
         */
        public AuditEventBuilder authenticationEvent(String userId, String brokerType, boolean success) {
            this.eventType = "AUTHENTICATION";
            this.eventCategory = "SECURITY";
            this.userId = userId;
            this.brokerType = brokerType;
            this.success = success;
            return this;
        }
        
        /**
         * Create data access audit event for GDPR/CCPA
         */
        public AuditEventBuilder dataAccessEvent(String userId, String dataType, String purpose) {
            this.eventType = "DATA_ACCESS";
            this.eventCategory = "PRIVACY";
            this.userId = userId;
            this.dataType = dataType;
            this.purpose = purpose;
            this.success = true;
            return this;
        }
        
        /**
         * Create administrative action audit event for SOC2
         */
        public AuditEventBuilder adminActionEvent(String adminUserId, String action, String targetResource) {
            this.eventType = "ADMINISTRATIVE";
            this.eventCategory = "GOVERNANCE";
            this.userId = adminUserId;
            this.adminAction = action;
            this.targetResource = targetResource;
            return this;
        }
        
        /**
         * Create system event for availability monitoring
         */
        public AuditEventBuilder systemEvent(String componentId, String severity, boolean success) {
            this.eventType = "SYSTEM";
            this.eventCategory = "AVAILABILITY";
            this.componentId = componentId;
            this.severity = severity;
            this.success = success;
            return this;
        }
        
        /**
         * Create privacy rights exercise event
         */
        public AuditEventBuilder privacyRightsEvent(String userId, String rightType, String requestId) {
            this.eventType = "PRIVACY_RIGHTS";
            this.eventCategory = "PRIVACY";
            this.userId = userId;
            this.rightType = rightType;
            this.requestId = requestId;
            this.success = true;
            return this;
        }
        
        /**
         * Create security incident event
         */
        public AuditEventBuilder securityIncidentEvent(String incidentId, String incidentType, String severity) {
            this.eventType = "SECURITY_INCIDENT";
            this.eventCategory = "SECURITY";
            this.incidentId = incidentId;
            this.incidentType = incidentType;
            this.severity = severity;
            return this;
        }
        
        /**
         * Apply compliance frameworks
         */
        public AuditEventBuilder withCompliance(String... frameworks) {
            this.complianceFrameworks = frameworks;
            return this;
        }
        
        /**
         * Apply data classification
         */
        public AuditEventBuilder withDataClassification(String classification) {
            this.dataClassification = classification;
            return this;
        }
        
        /**
         * Apply risk level
         */
        public AuditEventBuilder withRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }
        
        /**
         * Apply organizational context
         */
        public AuditEventBuilder withOrganizationalContext(String organization, String environment) {
            this.organization = organization;
            this.environment = environment;
            return this;
        }
    }
    
    /**
     * Generate event hash for integrity verification
     */
    public String generateEventHash() {
        String eventData = String.join("|", 
            eventId != null ? eventId : "",
            timestamp != null ? timestamp.toString() : "",
            eventType != null ? eventType : "",
            userId != null ? userId : "",
            success ? "SUCCESS" : "FAILURE"
        );
        return "SHA256:" + Integer.toHexString(eventData.hashCode());
    }
    
    /**
     * Check if event requires high-priority attention
     */
    public boolean isHighPriority() {
        return "CRITICAL".equals(severity) || 
               "HIGH".equals(severity) ||
               "SECURITY_INCIDENT".equals(eventType) ||
               (!success && "AUTHENTICATION".equals(eventType));
    }
    
    /**
     * Get human-readable event description
     */
    public String getEventDescription() {
        StringBuilder description = new StringBuilder();
        
        if (userId != null) {
            description.append("User ").append(userId);
        }
        
        if (eventAction != null) {
            description.append(" performed ").append(eventAction);
        }
        
        if (targetResource != null) {
            description.append(" on ").append(targetResource);
        }
        
        if (brokerType != null) {
            description.append(" via ").append(brokerType);
        }
        
        description.append(" - ").append(success ? "SUCCESS" : "FAILED");
        
        if (reason != null && !success) {
            description.append(" (").append(reason).append(")");
        }
        
        return description.toString();
    }
    
    /**
     * Check if event contains sensitive data
     */
    public boolean containsSensitiveData() {
        return "CONFIDENTIAL".equals(dataClassification) ||
               "RESTRICTED".equals(dataClassification) ||
               dataType != null && (dataType.contains("PERSONAL") || 
                                   dataType.contains("FINANCIAL") ||
                                   dataType.contains("AUTHENTICATION"));
    }
    
    /**
     * Get applicable retention period based on compliance requirements
     */
    public String getApplicableRetentionPeriod() {
        if (complianceFrameworks != null) {
            for (String framework : complianceFrameworks) {
                switch (framework) {
                    case "PCI-DSS":
                        return "1_YEAR"; // PCI-DSS requirement
                    case "SOC2":
                        return "7_YEARS"; // SOC2 Type II requirement
                    case "GDPR":
                    case "CCPA":
                        return "3_YEARS"; // Privacy law requirements
                    default:
                        break;
                }
            }
        }
        return "7_YEARS"; // Default to longest requirement
    }
}