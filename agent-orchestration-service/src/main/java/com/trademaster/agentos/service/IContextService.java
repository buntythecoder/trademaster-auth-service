package com.trademaster.agentos.service;

import java.util.Map;

/**
 * ✅ INTERFACE SEGREGATION: Context Management Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only context management operations
 * - Interface Segregation: Separated from logging operations
 * - Dependency Inversion: Abstractions for context management
 */
public interface IContextService {
    
    /**
     * ✅ SRP: Initialize request context - single responsibility
     */
    void initializeRequestContext(String userId, String sessionId, String ipAddress, String userAgent);
    
    /**
     * ✅ SRP: Clear context - single responsibility
     */
    void clearContext();
    
    /**
     * ✅ SRP: Set operation context - single responsibility
     */
    void setOperationContext(String operation);
    
    /**
     * ✅ SRP: Get correlation ID - single responsibility
     */
    String getCorrelationId();
    
    /**
     * ✅ SRP: Get context copy - single responsibility
     */
    Map<String, String> getCopyOfContextMap();
    
    /**
     * ✅ SRP: Set context map - single responsibility
     */
    void setContextMap(Map<String, String> contextMap);
}