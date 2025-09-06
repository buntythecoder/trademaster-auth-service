package com.trademaster.agentos.controller;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.security.facade.SecurityFacade;
import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import com.trademaster.agentos.service.AgentService;
import com.trademaster.agentos.service.AgentOrchestrationService;
import com.trademaster.agentos.service.IAgentHealthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Secure Agent Controller - Demonstrates Zero Trust Architecture.
 * 
 * This controller shows the MANDATORY tiered security approach:
 * - External Access: Uses SecurityFacade + SecurityMediator for all REST endpoints
 * - Internal Access: Services use simple constructor injection (no facade)
 * 
 * CRITICAL: This is the pattern ALL external-facing controllers must follow.
 */
@RestController
@RequestMapping("/api/v1/secure/agents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${security.cors.allowed-origins:http://localhost:3000}", maxAge = 3600)
public class SecureAgentController {

    // MANDATORY: SecurityFacade for ALL external access
    private final SecurityFacade securityFacade;
    
    // Internal services - simple constructor injection (no facade needed)
    private final AgentService agentService;
    private final AgentOrchestrationService orchestrationService;

    /**
     * Register a new agent with Zero Trust security.
     * Demonstrates full security stack for external access.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRADER')")
    public ResponseEntity<?> registerAgent(
            @Valid @RequestBody Agent agent,
            HttpServletRequest request) {
        
        // Extract security context from request (set by JWT filter)
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        log.info("Secure agent registration: correlationId={}, userId={}", 
            context.correlationId(), context.userId());
        
        // MANDATORY: Use SecurityFacade for external access with input
        Result<Agent, SecurityError> result = securityFacade.secureAccessWithInput(
            context,
            agent,
            validatedAgent -> orchestrationService.registerAgent(validatedAgent)
        );
        
        // Handle security result
        return switch (result) {
            case Result.Success(Agent registeredAgent) -> {
                log.info("Agent registered successfully: agentId={}, correlationId={}", 
                    registeredAgent.getAgentId(), context.correlationId());
                yield ResponseEntity.status(HttpStatus.CREATED).body(registeredAgent);
            }
            case Result.Failure(SecurityError error) -> {
                log.warn("Agent registration denied: error={}, correlationId={}", 
                    error.type(), context.correlationId());
                yield ResponseEntity.status(mapErrorToStatus(error))
                    .body(mapErrorToResponse(error));
            }
        };
    }

    /**
     * Get agent by ID with security validation.
     */
    @GetMapping("/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRADER', 'ANALYST')")
    public ResponseEntity<?> getAgent(
            @PathVariable Long agentId,
            HttpServletRequest request) {
        
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        log.debug("Secure get agent: agentId={}, correlationId={}", 
            agentId, context.correlationId());
        
        // Use SecurityFacade for secure access
        Result<Optional<Agent>, SecurityError> result = securityFacade.secureAccess(
            context,
            () -> agentService.findById(agentId)
        );
        
        return switch (result) {
            case Result.Success(Optional<Agent> agentOpt) -> 
                agentOpt.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            case Result.Failure(SecurityError error) -> 
                ResponseEntity.status(mapErrorToStatus(error))
                    .body(mapErrorToResponse(error));
        };
    }

    /**
     * Get all agents with pagination and security.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ANALYST')")
    public ResponseEntity<?> getAllAgents(
            Pageable pageable,
            HttpServletRequest request) {
        
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        log.debug("Secure get all agents: page={}, correlationId={}", 
            pageable.getPageNumber(), context.correlationId());
        
        Result<Page<Agent>, SecurityError> result = securityFacade.secureAccess(
            context,
            () -> agentService.findAllAgents(pageable)
        );
        
        return switch (result) {
            case Result.Success(Page<Agent> agents) -> ResponseEntity.ok(agents);
            case Result.Failure(SecurityError error) -> 
                ResponseEntity.status(mapErrorToStatus(error))
                    .body(mapErrorToResponse(error));
        };
    }

    /**
     * Update agent status with elevated security.
     * Requires higher privileges and risk assessment.
     */
    @PutMapping("/{agentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateAgentStatus(
            @PathVariable Long agentId,
            @RequestParam AgentStatus status,
            HttpServletRequest request) {
        
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        log.info("Secure update agent status: agentId={}, status={}, correlationId={}", 
            agentId, status, context.correlationId());
        
        // Check specific permission for status updates
        Result<Boolean, SecurityError> permissionCheck = 
            securityFacade.checkPermission(context, "agent:update");
        
        if (permissionCheck.isFailure()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Insufficient permissions for agent status update");
        }
        
        // Execute with security
        Result<Void, SecurityError> result = securityFacade.secureAccess(
            context,
            () -> {
                agentService.updateAgentStatus(agentId, status);
                return null;
            }
        );
        
        return switch (result) {
            case Result.Success(Void v) -> {
                log.info("Agent status updated: agentId={}, correlationId={}", 
                    agentId, context.correlationId());
                yield ResponseEntity.ok().build();
            }
            case Result.Failure(SecurityError error) -> 
                ResponseEntity.status(mapErrorToStatus(error))
                    .body(mapErrorToResponse(error));
        };
    }

    /**
     * Process agent heartbeat - uses async security processing.
     */
    @PostMapping("/{agentId}/heartbeat")
    @PreAuthorize("hasRole('AGENT') or hasRole('SERVICE')")
    public ResponseEntity<?> processHeartbeat(
            @PathVariable Long agentId,
            HttpServletRequest request) {
        
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        log.debug("Secure heartbeat: agentId={}, correlationId={}", 
            agentId, context.correlationId());
        
        // Use async security for non-blocking heartbeat processing
        var futureResult = securityFacade.secureAsyncAccess(
            context,
            () -> orchestrationService.processAgentHeartbeatAsync(agentId)
        );
        
        // For heartbeat, we return immediately
        return ResponseEntity.accepted().build();
    }

    /**
     * Deregister an agent - requires admin privileges.
     */
    @DeleteMapping("/{agentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deregisterAgent(
            @PathVariable Long agentId,
            HttpServletRequest request) {
        
        SecurityContext context = (SecurityContext) request.getAttribute("securityContext");
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Security context not found");
        }
        
        // Enhance context for critical operation
        SecurityContext elevatedContext = SecurityContext.builder()
            .correlationId(context.correlationId())
            .userId(context.userId())
            .sessionId(context.sessionId())
            .token(context.token())
            .roles(context.roles())
            .permissions(context.permissions())
            .attributes(context.attributes())
            .ipAddress(context.ipAddress())
            .userAgent(context.userAgent())
            .securityLevel(SecurityContext.SecurityLevel.ELEVATED)
            .build();
        
        log.warn("Secure agent deregistration: agentId={}, userId={}, correlationId={}", 
            agentId, context.userId(), context.correlationId());
        
        Result<Void, SecurityError> result = securityFacade.secureAccess(
            elevatedContext,
            () -> {
                orchestrationService.deregisterAgent(agentId);
                return null;
            }
        );
        
        return switch (result) {
            case Result.Success(Void v) -> {
                log.info("Agent deregistered: agentId={}, correlationId={}", 
                    agentId, context.correlationId());
                yield ResponseEntity.ok().build();
            }
            case Result.Failure(SecurityError error) -> 
                ResponseEntity.status(mapErrorToStatus(error))
                    .body(mapErrorToResponse(error));
        };
    }

    /**
     * Get system health summary - public endpoint with rate limiting.
     */
    @GetMapping("/health/summary")
    public ResponseEntity<?> getSystemHealthSummary(HttpServletRequest request) {
        // This is a public endpoint but still goes through rate limiting
        SecurityContext publicContext = SecurityContext.builder()
            .correlationId(java.util.UUID.randomUUID().toString())
            .userId("anonymous")
            .ipAddress(request.getRemoteAddr())
            .userAgent(request.getHeader("User-Agent"))
            .securityLevel(SecurityContext.SecurityLevel.PUBLIC)
            .build();
        
        Result<IAgentHealthService.AgentHealthSummary, SecurityError> result = 
            securityFacade.secureAccess(
                publicContext,
                () -> agentService.getSystemHealthSummary()
            );
        
        return switch (result) {
            case Result.Success(IAgentHealthService.AgentHealthSummary summary) -> 
                ResponseEntity.ok(summary);
            case Result.Failure(SecurityError error) -> 
                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded");
        };
    }

    // Helper methods for error mapping
    
    private HttpStatus mapErrorToStatus(SecurityError error) {
        return switch (error.type()) {
            case AUTHENTICATION_FAILED, TOKEN_EXPIRED, TOKEN_INVALID -> 
                HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION_DENIED, INSUFFICIENT_PRIVILEGES, ACCESS_DENIED -> 
                HttpStatus.FORBIDDEN;
            case RATE_LIMIT_EXCEEDED -> 
                HttpStatus.TOO_MANY_REQUESTS;
            case RISK_THRESHOLD_EXCEEDED, SUSPICIOUS_ACTIVITY, SECURITY_VIOLATION -> 
                HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    private ErrorResponse mapErrorToResponse(SecurityError error) {
        return new ErrorResponse(
            error.type().name(),
            error.message(),
            error.correlationId(),
            error.timestamp().toString()
        );
    }
    
    /**
     * Standard error response.
     */
    private record ErrorResponse(
        String error,
        String message,
        String correlationId,
        String timestamp
    ) {}
}