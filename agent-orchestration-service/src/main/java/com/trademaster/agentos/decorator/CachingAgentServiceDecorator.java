package com.trademaster.agentos.decorator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.service.IAgentService;
import com.trademaster.agentos.service.IAgentHealthService;
import com.trademaster.agentos.domain.entity.AgentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Caching Decorator for AgentService
 * 
 * Adds intelligent caching to frequently accessed agent data with TTL and cache invalidation.
 */
@Slf4j
public class CachingAgentServiceDecorator implements IAgentService {
    
    private final IAgentService decoratedService;
    private final Map<Long, CacheEntry<Agent>> agentCache = new ConcurrentHashMap<>();
    private final Map<AgentCapability, CacheEntry<List<Agent>>> capabilityCache = new ConcurrentHashMap<>();
    private final Duration cacheTtl;
    
    public CachingAgentServiceDecorator(IAgentService decoratedService, Duration cacheTtl) {
        this.decoratedService = decoratedService;
        this.cacheTtl = cacheTtl;
    }
    
    @Override
    public CompletableFuture<Agent> registerAgent(Agent agent) {
        CompletableFuture<Agent> result = decoratedService.registerAgent(agent);
        
        // Invalidate caches on successful registration
        result.thenAccept(registeredAgent -> invalidateCaches());
        
        return result;
    }
    
    @Override
    public void updateAgentStatus(Long agentId, AgentStatus status) {
        decoratedService.updateAgentStatus(agentId, status);
        
        // Invalidate specific agent cache
        agentCache.remove(agentId);
        capabilityCache.clear(); // Clear capability cache as availability changed
    }
    
    @Override
    public Result<String, AgentError> deregisterAgent(Long agentId) {
        Result<String, AgentError> result = decoratedService.deregisterAgent(agentId);
        
        if (result.isSuccess()) {
            invalidateCaches();
        }
        
        return result;
    }
    
    @Override
    public Optional<Agent> findById(Long agentId) {
        CacheEntry<Agent> cached = agentCache.get(agentId);
        
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for agent {}", agentId);
            return Optional.of(cached.value());
        }
        
        log.debug("Cache miss for agent {}, fetching from service", agentId);
        Optional<Agent> agent = decoratedService.findById(agentId);
        
        agent.ifPresent(a -> agentCache.put(agentId, new CacheEntry<>(a, Instant.now().plus(cacheTtl))));
        
        return agent;
    }
    
    @Override
    @Cacheable(value = "agent-capabilities", key = "#capability")
    public List<Agent> findAgentsWithCapability(AgentCapability capability) {
        CacheEntry<List<Agent>> cached = capabilityCache.get(capability);
        
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for capability {}", capability);
            return cached.value();
        }
        
        log.debug("Cache miss for capability {}, fetching from service", capability);
        List<Agent> agents = decoratedService.findAgentsWithCapability(capability);
        
        capabilityCache.put(capability, new CacheEntry<>(agents, Instant.now().plus(cacheTtl)));
        
        return agents;
    }
    
    // This method doesn't exist in IAgentService - removing
    
    @Override
    public void processHeartbeat(Long agentId) {
        decoratedService.processHeartbeat(agentId);
        // Invalidate agent cache as heartbeat may change status
        agentCache.remove(agentId);
    }
    
    // Additional required methods from IAgentService interface
    
    @Override
    public Optional<Agent> findByName(String agentName) {
        return decoratedService.findByName(agentName);
    }
    
    @Override
    public Result<Agent, AgentError> findOptimalAgentForTask(AgentType agentType, List<AgentCapability> requiredCapabilities) {
        return decoratedService.findOptimalAgentForTask(agentType, requiredCapabilities);
    }
    
    @Override
    public Page<Agent> findAllAgents(Pageable pageable) {
        return decoratedService.findAllAgents(pageable);
    }
    
    @Override
    public List<Agent> findByType(AgentType agentType) {
        return decoratedService.findByType(agentType);
    }
    
    @Override
    public List<Agent> findByStatus(AgentStatus status) {
        return decoratedService.findByStatus(status);
    }
    
    @Override
    public List<Agent> findByUserId(Long userId) {
        return decoratedService.findByUserId(userId);
    }
    
    @Override
    public List<Agent> findAvailableAgentsByType(AgentType agentType) {
        return decoratedService.findAvailableAgentsByType(agentType);
    }
    
    @Override
    public List<Agent> findTopPerformingAgents(Long minTasksCompleted) {
        return decoratedService.findTopPerformingAgents(minTasksCompleted);
    }
    
    @Override
    public IAgentHealthService.AgentHealthSummary getSystemHealthSummary() {
        return decoratedService.getSystemHealthSummary();
    }
    
    @Override
    public List<Object[]> getAgentStatisticsByType() {
        return decoratedService.getAgentStatisticsByType();
    }
    
    @Override
    public void updatePerformanceMetrics(Long agentId, boolean taskSuccess, long responseTimeMs) {
        decoratedService.updatePerformanceMetrics(agentId, taskSuccess, responseTimeMs);
        agentCache.remove(agentId); // Invalidate cache as metrics changed
    }
    
    @Override
    public Result<Agent, AgentError> incrementAgentLoad(Long agentId) {
        Result<Agent, AgentError> result = decoratedService.incrementAgentLoad(agentId);
        agentCache.remove(agentId); // Invalidate cache as load changed
        return result;
    }
    
    @Override
    public void decrementAgentLoad(Long agentId) {
        decoratedService.decrementAgentLoad(agentId);
        agentCache.remove(agentId); // Invalidate cache as load changed
    }
    
    @Override
    public void performHealthCheck() {
        decoratedService.performHealthCheck();
    }
    
    private void invalidateCaches() {
        agentCache.clear();
        capabilityCache.clear();
        log.debug("Cleared all agent service caches");
    }
    
    private record CacheEntry<T>(T value, Instant expiry) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}