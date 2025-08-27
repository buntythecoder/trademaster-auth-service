package com.trademaster.agentos.controller;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.service.AgentService;
import com.trademaster.agentos.service.AgentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Agent Controller
 * 
 * REST API endpoints for agent management including registration,
 * status updates, heartbeat processing, and agent discovery.
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AgentController {

    private final AgentService agentService;
    private final AgentOrchestrationService orchestrationService;

    // Agent Registration & Management

    /**
     * Register a new agent
     */
    @PostMapping
    public ResponseEntity<Agent> registerAgent(@Valid @RequestBody Agent agent) {
        log.info("REST: Registering new agent: {}", agent.getAgentName());
        
        try {
            Agent registeredAgent = orchestrationService.registerAgent(agent);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredAgent);
        } catch (IllegalArgumentException e) {
            log.warn("Agent registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error registering agent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get agent by ID
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<Agent> getAgent(@PathVariable Long agentId) {
        log.debug("REST: Getting agent: {}", agentId);
        
        Optional<Agent> agent = agentService.findById(agentId);
        return agent.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all agents with pagination
     */
    @GetMapping
    public ResponseEntity<Page<Agent>> getAllAgents(Pageable pageable) {
        log.debug("REST: Getting all agents (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Agent> agents = agentService.findAllAgents(pageable);
        return ResponseEntity.ok(agents);
    }

    /**
     * Get agents by type
     */
    @GetMapping("/type/{agentType}")
    public ResponseEntity<List<Agent>> getAgentsByType(@PathVariable AgentType agentType) {
        log.debug("REST: Getting agents by type: {}", agentType);
        
        List<Agent> agents = agentService.findByType(agentType);
        return ResponseEntity.ok(agents);
    }

    /**
     * Get agents by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Agent>> getAgentsByStatus(@PathVariable AgentStatus status) {
        log.debug("REST: Getting agents by status: {}", status);
        
        List<Agent> agents = agentService.findByStatus(status);
        return ResponseEntity.ok(agents);
    }

    /**
     * Get agents by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Agent>> getAgentsByUser(@PathVariable Long userId) {
        log.debug("REST: Getting agents for user: {}", userId);
        
        List<Agent> agents = agentService.findByUserId(userId);
        return ResponseEntity.ok(agents);
    }

    /**
     * Update agent status
     */
    @PutMapping("/{agentId}/status")
    public ResponseEntity<Void> updateAgentStatus(
            @PathVariable Long agentId,
            @RequestParam AgentStatus status) {
        log.info("REST: Updating agent {} status to {}", agentId, status);
        
        try {
            agentService.updateAgentStatus(agentId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating agent status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Process agent heartbeat
     */
    @PostMapping("/{agentId}/heartbeat")
    public ResponseEntity<Void> processHeartbeat(@PathVariable Long agentId) {
        log.debug("REST: Processing heartbeat for agent: {}", agentId);
        
        try {
            orchestrationService.processAgentHeartbeat(agentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing agent heartbeat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deregister an agent
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> deregisterAgent(@PathVariable Long agentId) {
        log.info("REST: Deregistering agent: {}", agentId);
        
        try {
            orchestrationService.deregisterAgent(agentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deregistering agent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Agent Discovery & Monitoring

    /**
     * Get available agents by type
     */
    @GetMapping("/available/type/{agentType}")
    public ResponseEntity<List<Agent>> getAvailableAgentsByType(@PathVariable AgentType agentType) {
        log.debug("REST: Getting available agents by type: {}", agentType);
        
        List<Agent> availableAgents = agentService.findAvailableAgentsByType(agentType);
        return ResponseEntity.ok(availableAgents);
    }

    /**
     * Get top performing agents
     */
    @GetMapping("/top-performing")
    public ResponseEntity<List<Agent>> getTopPerformingAgents(
            @RequestParam(defaultValue = "10") Long minTasksCompleted) {
        log.debug("REST: Getting top performing agents (min tasks: {})", minTasksCompleted);
        
        List<Agent> topPerformers = agentService.findTopPerformingAgents(minTasksCompleted);
        return ResponseEntity.ok(topPerformers);
    }

    /**
     * Get system health summary
     */
    @GetMapping("/health/summary")
    public ResponseEntity<AgentService.AgentHealthSummary> getSystemHealthSummary() {
        log.debug("REST: Getting system health summary");
        
        AgentService.AgentHealthSummary healthSummary = agentService.getSystemHealthSummary();
        return ResponseEntity.ok(healthSummary);
    }

    /**
     * Get agent statistics by type
     */
    @GetMapping("/statistics/by-type")
    public ResponseEntity<List<Object[]>> getAgentStatisticsByType() {
        log.debug("REST: Getting agent statistics by type");
        
        List<Object[]> statistics = agentService.getAgentStatisticsByType();
        return ResponseEntity.ok(statistics);
    }

    // Agent Performance Management

    /**
     * Update agent performance metrics
     */
    @PostMapping("/{agentId}/performance")
    public ResponseEntity<Void> updatePerformanceMetrics(
            @PathVariable Long agentId,
            @RequestParam boolean taskSuccess,
            @RequestParam long responseTimeMs) {
        log.debug("REST: Updating performance metrics for agent: {} (success: {}, responseTime: {}ms)", 
                 agentId, taskSuccess, responseTimeMs);
        
        try {
            agentService.updatePerformanceMetrics(agentId, taskSuccess, responseTimeMs);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating agent performance metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Increment agent load
     */
    @PostMapping("/{agentId}/load/increment")
    public ResponseEntity<Void> incrementAgentLoad(@PathVariable Long agentId) {
        log.debug("REST: Incrementing load for agent: {}", agentId);
        
        try {
            agentService.incrementAgentLoad(agentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error incrementing agent load", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Decrement agent load
     */
    @PostMapping("/{agentId}/load/decrement")
    public ResponseEntity<Void> decrementAgentLoad(@PathVariable Long agentId) {
        log.debug("REST: Decrementing load for agent: {}", agentId);
        
        try {
            agentService.decrementAgentLoad(agentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error decrementing agent load", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}