package com.trademaster.agentos.service;

import com.trademaster.agentos.service.MCPProtocolService.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * ✅ INTERFACE SEGREGATION: MCP Protocol Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only MCP protocol operations
 * - Interface Segregation: Focused contract for MCP operations
 * - Dependency Inversion: Abstractions for MCP implementations
 */
public interface IMCPProtocolService {
    
    /**
     * ✅ SRP: List resources - single responsibility
     */
    CompletableFuture<List<MCPResource>> listResources();
    
    /**
     * ✅ SRP: Read resource - single responsibility
     */
    CompletableFuture<MCPResourceContent> readResource(String uri);
    
    /**
     * ✅ SRP: Call tool - single responsibility
     */
    CompletableFuture<MCPToolResult> callTool(String toolName, Object parameters);
    
    /**
     * ✅ SRP: Register resource - single responsibility
     */
    void registerResource(String uri, String name, String description, 
                         Supplier<MCPResourceContent> contentProvider);
    
    /**
     * ✅ SRP: Register tool - single responsibility
     */
    void registerTool(String name, String description, MCPToolExecutor executor);
}