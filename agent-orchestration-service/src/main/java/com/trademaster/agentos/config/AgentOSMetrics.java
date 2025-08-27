package com.trademaster.agentos.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MANDATORY: Agent OS Metrics Collection
 * 
 * Comprehensive metrics for Agent Orchestration Service monitoring
 */
@Component
public class AgentOSMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Business Metrics - MANDATORY
    private final Counter agentsCreated;
    private final Counter agentsDestroyed;
    private final Counter tasksExecuted;
    private final Counter tasksCompleted;
    private final Counter tasksFailed;
    private final Counter workflowsCompleted;
    private final Timer taskExecutionTime;
    private final Timer workflowExecutionTime;
    
    // Performance Metrics - MANDATORY
    private final Timer apiResponseTime;
    private final Counter apiRequests;
    private final Timer databaseQueryTime;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final AtomicLong virtualThreadsActive;
    private final AtomicLong virtualThreadsCreated;
    
    // System Health Metrics - MANDATORY
    private final Counter errorsByEndpoint;
    private final Counter circuitBreakerOpenCount;
    private final AtomicLong databaseConnectionsActive;
    private final AtomicLong redisConnectionsActive;
    
    // Security Metrics - MANDATORY
    private final Counter authenticationAttempts;
    private final Counter authenticationSuccesses;
    private final Counter authenticationFailures;
    private final Counter securityIncidents;
    private final Counter rateLimitViolations;
    private final Timer authenticationTime;
    
    // Agent-specific metrics
    private final ConcurrentHashMap<String, AtomicLong> agentsByType;
    private final ConcurrentHashMap<String, AtomicLong> tasksByPriority;
    
    public AgentOSMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.agentsByType = new ConcurrentHashMap<>();
        this.tasksByPriority = new ConcurrentHashMap<>();
        this.virtualThreadsActive = new AtomicLong(0);
        this.virtualThreadsCreated = new AtomicLong(0);
        this.databaseConnectionsActive = new AtomicLong(0);
        this.redisConnectionsActive = new AtomicLong(0);
        
        // Initialize Business Metrics
        this.agentsCreated = Counter.builder("agentos.agents.created")
            .description("Total number of agents created")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.agentsDestroyed = Counter.builder("agentos.agents.destroyed")
            .description("Total number of agents destroyed")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.tasksExecuted = Counter.builder("agentos.tasks.executed")
            .description("Total number of tasks executed")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.tasksCompleted = Counter.builder("agentos.tasks.completed")
            .description("Total number of tasks completed successfully")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.tasksFailed = Counter.builder("agentos.tasks.failed")
            .description("Total number of tasks that failed")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.workflowsCompleted = Counter.builder("agentos.workflows.completed")
            .description("Total number of workflows completed")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.taskExecutionTime = Timer.builder("agentos.tasks.execution.time")
            .description("Task execution time")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.workflowExecutionTime = Timer.builder("agentos.workflows.execution.time")
            .description("Workflow execution time")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
        
        // Initialize Performance Metrics
        this.apiResponseTime = Timer.builder("agentos.api.response.time")
            .description("API endpoint response time")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.apiRequests = Counter.builder("agentos.api.requests")
            .description("Total API requests")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.databaseQueryTime = Timer.builder("agentos.database.query.time")
            .description("Database query execution time")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.cacheHits = Counter.builder("agentos.cache.hits")
            .description("Cache hits")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.cacheMisses = Counter.builder("agentos.cache.misses")
            .description("Cache misses")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
        
        // Initialize System Health Metrics
        this.errorsByEndpoint = Counter.builder("agentos.errors.by_endpoint")
            .description("Errors by API endpoint")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.circuitBreakerOpenCount = Counter.builder("agentos.circuit_breaker.open")
            .description("Circuit breaker open events")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
        
        // Initialize Security Metrics
        this.authenticationAttempts = Counter.builder("agentos.auth.attempts")
            .description("Authentication attempts")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.authenticationSuccesses = Counter.builder("agentos.auth.successes")
            .description("Successful authentications")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.authenticationFailures = Counter.builder("agentos.auth.failures")
            .description("Failed authentications")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.securityIncidents = Counter.builder("agentos.security.incidents")
            .description("Security incidents detected")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.rateLimitViolations = Counter.builder("agentos.security.rate_limit_violations")
            .description("Rate limit violations")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        this.authenticationTime = Timer.builder("agentos.auth.time")
            .description("Authentication processing time")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
        
        // Initialize Virtual Thread Gauges - MANDATORY for Virtual Thread monitoring
        // Fixed Gauge.builder() API usage
        Gauge.builder("agentos.virtual_threads.active", virtualThreadsActive, AtomicLong::get)
            .description("Currently active virtual threads")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        Gauge.builder("agentos.virtual_threads.created", virtualThreadsCreated, AtomicLong::get)
            .description("Total virtual threads created")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
        
        // Initialize Connection Pool Gauges
        Gauge.builder("agentos.database.connections.active", databaseConnectionsActive, AtomicLong::get)
            .description("Active database connections")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
            
        Gauge.builder("agentos.redis.connections.active", redisConnectionsActive, AtomicLong::get)
            .description("Active Redis connections")
            .tag("service", "agent-orchestration")
            .register(meterRegistry);
    }
    
    // Business Metrics Methods
    public void recordAgentCreated(String agentType) {
        agentsCreated.increment();
        agentsByType.computeIfAbsent(agentType, k -> {
            AtomicLong counter = new AtomicLong(0);
            Gauge.builder("agentos.agents.by_type", counter, AtomicLong::doubleValue)
                .tag("type", k)
                .register(meterRegistry);
            return counter;
        }).incrementAndGet();
    }
    
    public void recordAgentDestroyed(String agentType) {
        agentsDestroyed.increment();
        agentsByType.computeIfAbsent(agentType, k -> {
            AtomicLong counter = new AtomicLong(0);
            Gauge.builder("agentos.agents.by_type", counter, AtomicLong::doubleValue)
                .tag("type", k)
                .register(meterRegistry);
            return counter;
        }).decrementAndGet();
    }
    
    public void recordTaskExecuted(String taskType, String priority) {
        tasksExecuted.increment();
        tasksByPriority.computeIfAbsent(priority, k -> {
            AtomicLong counter = new AtomicLong(0);
            Gauge.builder("agentos.tasks.by_priority", counter, AtomicLong::doubleValue)
                .tag("priority", k)
                .register(meterRegistry);
            return counter;
        }).incrementAndGet();
    }
    
    public Timer.Sample startTaskTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTaskCompleted(Timer.Sample sample, String taskType, String status) {
        sample.stop(taskExecutionTime);
        if ("success".equals(status)) {
            tasksCompleted.increment();
        } else {
            tasksFailed.increment();
        }
    }
    
    // Performance Metrics Methods
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordApiRequest(Timer.Sample sample, String endpoint, String method, int statusCode) {
        sample.stop(apiResponseTime);
        apiRequests.increment();
    }
    
    public void recordCacheHit(String cacheType) {
        cacheHits.increment();
    }
    
    public void recordCacheMiss(String cacheType) {
        cacheMisses.increment();
    }
    
    // Security Metrics Methods
    public void recordAuthenticationAttempt(String method, String result, long durationMs) {
        authenticationAttempts.increment();
        authenticationTime.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        if ("success".equals(result)) {
            authenticationSuccesses.increment();
        } else {
            authenticationFailures.increment();
        }
    }
    
    public void recordSecurityIncident(String incidentType, String severity) {
        securityIncidents.increment();
    }
    
    public void recordRateLimitViolation(String endpoint, String clientIp) {
        rateLimitViolations.increment();
    }
    
    // System Health Methods
    public void recordError(String endpoint, String errorType) {
        errorsByEndpoint.increment();
    }
    
    public void recordCircuitBreakerOpen(String serviceName) {
        circuitBreakerOpenCount.increment();
    }
    
    // Virtual Thread Methods - MANDATORY
    public void incrementVirtualThreadsActive() {
        virtualThreadsActive.incrementAndGet();
    }
    
    public void decrementVirtualThreadsActive() {
        virtualThreadsActive.decrementAndGet();
    }
    
    public void recordVirtualThreadCreated() {
        virtualThreadsCreated.incrementAndGet();
    }
    
    // Connection Pool Methods
    public void updateDatabaseConnections(long active) {
        databaseConnectionsActive.set(active);
    }
    
    public void updateRedisConnections(long active) {
        redisConnectionsActive.set(active);
    }
    
    // Getter methods for accessing metrics components
    public Timer getApiResponseTime() {
        return apiResponseTime;
    }
    
    public Timer getTaskExecutionTime() {
        return taskExecutionTime;
    }
    
    public Timer getDatabaseQueryTime() {
        return databaseQueryTime;
    }
    
    public Counter getApiRequests() {
        return apiRequests;
    }
    
    public Timer getAuthenticationTime() {
        return authenticationTime;
    }
}