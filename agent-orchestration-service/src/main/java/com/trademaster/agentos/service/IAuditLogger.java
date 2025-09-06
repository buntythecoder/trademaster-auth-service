package com.trademaster.agentos.service;

import java.util.Map;

/**
 * ✅ INTERFACE SEGREGATION: Audit Logging Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only audit logging operations
 * - Interface Segregation: Separated from application/security logging
 * - Dependency Inversion: Abstractions for audit logging
 */
public interface IAuditLogger {
    
    /**
     * ✅ SRP: Log business transaction - single responsibility
     */
    void logBusinessTransaction(String transactionType, String entityId, String action,
                               String performedBy, Map<String, Object> changes);
    
    /**
     * ✅ SRP: Log data access - single responsibility
     */
    void logDataAccess(String dataType, String entityId, String accessType, String userId);
    
    /**
     * ✅ SRP: Log configuration change - single responsibility
     */
    void logConfigurationChange(String configKey, String oldValue, String newValue, String changedBy);
}