package com.trademaster.agentos.service;

import java.util.Map;

/**
 * ✅ INTERFACE SEGREGATION: Application Logging Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only application logging operations
 * - Interface Segregation: Separated from security/audit logging
 * - Dependency Inversion: Abstractions for logging implementations
 */
public interface IApplicationLogger {
    
    /**
     * ✅ SRP: Log info - single responsibility
     */
    void logInfo(String operation, Map<String, Object> context);
    
    /**
     * ✅ SRP: Log debug - single responsibility
     */
    void logDebug(String operation, Map<String, Object> context);
    
    /**
     * ✅ SRP: Log warning - single responsibility
     */
    void logWarning(String operation, Map<String, Object> context);
    
    /**
     * ✅ SRP: Log error - single responsibility
     */
    void logError(String operation, String errorMessage, Exception exception, Map<String, Object> context);
}