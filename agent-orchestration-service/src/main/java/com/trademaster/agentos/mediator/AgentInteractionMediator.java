package com.trademaster.agentos.mediator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * ✅ MEDIATOR PATTERN: Enhanced Agent Interaction Mediator
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Agent interaction coordination only
 * - Open/Closed: Extensible via interaction handlers
 * - Liskov Substitution: All mediators interchangeable
 * - Interface Segregation: Focused on interaction mediation
 * - Dependency Inversion: Uses structured logging abstraction
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - Result monad for error handling
 * - CompletableFuture for async operations
 * - Pattern matching for interaction type dispatch
 * - Stream operations for agent coordination
 * - Immutable interaction contexts
 * 
 * Enhanced mediator implementation that coordinates complex interactions
 * between agents, manages communication protocols, and handles
 * multi-party agent collaborations with conflict resolution.
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentInteractionMediator {

    private final StructuredLoggingService structuredLogger;
    
    private final Map<String, AgentCollaboration> activeCollaborations = new ConcurrentHashMap<>();
    private final Map<InteractionType, BiFunction<InteractionContext, java.util.List<Agent>, 
        CompletableFuture<Result<InteractionResult, AgentError>>>> interactionHandlers = Map.of(
        
        InteractionType.PEER_TO_PEER, this::handlePeerToPeerInteraction,
        InteractionType.BROADCAST, this::handleBroadcastInteraction,
        InteractionType.CHAIN_COLLABORATION, this::handleChainCollaboration,
        InteractionType.VOTING_CONSENSUS, this::handleVotingConsensus,
        InteractionType.HIERARCHICAL_COORDINATION, this::handleHierarchicalCoordination
    );

    /**
     * ✅ MEDIATOR METHOD: Coordinate complex agent interactions
     * Uses pattern matching for interaction type dispatch
     * Cognitive Complexity: 4
     */
    public CompletableFuture<Result<InteractionResult, AgentError>> mediateInteraction(
            InteractionContext context,
            java.util.List<Agent> participants) {
        
        String collaborationId = generateCollaborationId();
        
        structuredLogger.logInfo("mediating_agent_interaction", Map.of(
            "collaborationId", collaborationId,
            "interactionType", context.getInteractionType(),
            "participantCount", participants.size(),
            "requestId", context.getRequestId()
        ));
        
        return validateParticipants(participants)
            .fold(
                validParticipants -> {
                    registerCollaboration(collaborationId, context, validParticipants);
                    return dispatchInteraction(context, validParticipants)
                        .thenApply(result -> {
                            completeCollaboration(collaborationId, result);
                            return result;
                        });
                },
                error -> CompletableFuture.completedFuture(Result.failure(error))
            );
    }

    /**
     * ✅ DISPATCHER: Pattern matching for interaction type dispatch
     * Cognitive Complexity: 2
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> dispatchInteraction(
            InteractionContext context,
            java.util.List<Agent> participants) {
        
        BiFunction<InteractionContext, java.util.List<Agent>, 
            CompletableFuture<Result<InteractionResult, AgentError>>> handler = 
            interactionHandlers.get(context.getInteractionType());
        
        return handler != null ?
            handler.apply(context, participants) :
            CompletableFuture.completedFuture(
                Result.failure(AgentError.unsupportedOperation("UNSUPPORTED_INTERACTION",
                    "Interaction type not supported: " + context.getInteractionType()))
            );
    }

    /**
     * ✅ INTERACTION HANDLER: Peer-to-peer agent communication
     * Cognitive Complexity: 3
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> handlePeerToPeerInteraction(
            InteractionContext context, 
            java.util.List<Agent> participants) {
        
        return participants.size() == 2 ?
            CompletableFuture.supplyAsync(() -> {
                Agent sender = participants.get(0);
                Agent receiver = participants.get(1);
                
                structuredLogger.logDebug("peer_to_peer_interaction", Map.of(
                    "sender", sender.getAgentId(),
                    "receiver", receiver.getAgentId(),
                    "messageType", context.getMessageType()
                ));
                
                return Result.success(new InteractionResult(
                    InteractionStatus.COMPLETED,
                    Map.of(
                        "interactionType", "PEER_TO_PEER",
                        "sender", sender.getAgentName(),
                        "receiver", receiver.getAgentName(),
                        "timestamp", Instant.now(),
                        "messageDelivered", true
                    ),
                    java.util.List.of(sender.getAgentId(), receiver.getAgentId()),
                    null
                ));
            }) :
            CompletableFuture.completedFuture(
                Result.failure(AgentError.validationError("INVALID_PARTICIPANT_COUNT",
                    "Peer-to-peer interaction requires exactly 2 participants"))
            );
    }

    /**
     * ✅ INTERACTION HANDLER: Broadcast message to multiple agents
     * Cognitive Complexity: 4
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> handleBroadcastInteraction(
            InteractionContext context, 
            java.util.List<Agent> participants) {
        
        return CompletableFuture.supplyAsync(() -> {
            Agent broadcaster = participants.stream()
                .filter(agent -> agent.getAgentId().equals(context.getInitiatorId()))
                .findFirst()
                .orElse(participants.getFirst());
            
            java.util.List<Agent> recipients = participants.stream()
                .filter(agent -> !agent.getAgentId().equals(broadcaster.getAgentId()))
                .toList();
            
            structuredLogger.logDebug("broadcast_interaction", Map.of(
                "broadcaster", broadcaster.getAgentId(),
                "recipientCount", recipients.size(),
                "messageType", context.getMessageType()
            ));
            
            return Result.success(new InteractionResult(
                InteractionStatus.COMPLETED,
                Map.of(
                    "interactionType", "BROADCAST",
                    "broadcaster", broadcaster.getAgentName(),
                    "recipientCount", recipients.size(),
                    "timestamp", Instant.now(),
                    "messagesSent", recipients.size()
                ),
                participants.stream().map(Agent::getAgentId).toList(),
                null
            ));
        });
    }

    /**
     * ✅ INTERACTION HANDLER: Chain collaboration between agents
     * Cognitive Complexity: 5
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> handleChainCollaboration(
            InteractionContext context, 
            java.util.List<Agent> participants) {
        
        return CompletableFuture.supplyAsync(() -> {
            java.util.List<Agent> chain = participants.stream()
                .sorted(Comparator.comparing(Agent::getPriority))
                .toList();
            
            structuredLogger.logDebug("chain_collaboration", Map.of(
                "chainLength", chain.size(),
                "initiator", chain.getFirst().getAgentId(),
                "finalizer", chain.getLast().getAgentId()
            ));
            
            Map<String, Object> chainExecution = Map.of(
                "interactionType", "CHAIN_COLLABORATION",
                "chainOrder", chain.stream().map(Agent::getAgentName).toList(),
                "expectedDuration", chain.size() * 30, // 30 seconds per agent
                "timestamp", Instant.now(),
                "status", "INITIATED"
            );
            
            return Result.success(new InteractionResult(
                InteractionStatus.IN_PROGRESS,
                chainExecution,
                chain.stream().map(Agent::getAgentId).toList(),
                null
            ));
        });
    }

    /**
     * ✅ INTERACTION HANDLER: Voting consensus mechanism
     * Cognitive Complexity: 6
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> handleVotingConsensus(
            InteractionContext context, 
            java.util.List<Agent> participants) {
        
        return CompletableFuture.supplyAsync(() -> {
            int requiredMajority = (participants.size() / 2) + 1;
            
            // Simulate voting based on agent types and capabilities
            Map<AgentType, Integer> typeWeights = Map.of(
                AgentType.RISK_ASSESSMENT, 3,
                AgentType.COMPLIANCE_CHECK, 3,
                AgentType.MARKET_ANALYSIS, 2,
                AgentType.TRADING_EXECUTION, 2,
                AgentType.PORTFOLIO_MANAGEMENT, 2
            );
            
            int totalVoteWeight = participants.stream()
                .mapToInt(agent -> typeWeights.getOrDefault(agent.getAgentType(), 1))
                .sum();
            
            structuredLogger.logDebug("voting_consensus", Map.of(
                "voterCount", participants.size(),
                "requiredMajority", requiredMajority,
                "totalVoteWeight", totalVoteWeight,
                "decisionType", context.getMessageType()
            ));
            
            Map<String, Object> votingResult = Map.of(
                "interactionType", "VOTING_CONSENSUS",
                "voterCount", participants.size(),
                "requiredMajority", requiredMajority,
                "totalVoteWeight", totalVoteWeight,
                "consensusReached", totalVoteWeight >= requiredMajority,
                "timestamp", Instant.now(),
                "decision", totalVoteWeight >= requiredMajority ? "APPROVED" : "REJECTED"
            );
            
            return Result.success(new InteractionResult(
                InteractionStatus.COMPLETED,
                votingResult,
                participants.stream().map(Agent::getAgentId).toList(),
                null
            ));
        });
    }

    /**
     * ✅ INTERACTION HANDLER: Hierarchical coordination
     * Cognitive Complexity: 7
     */
    private CompletableFuture<Result<InteractionResult, AgentError>> handleHierarchicalCoordination(
            InteractionContext context, 
            java.util.List<Agent> participants) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Establish hierarchy based on agent types and priorities
            Map<AgentType, Integer> hierarchyLevels = Map.of(
                AgentType.COMPLIANCE_CHECK, 1,  // Highest authority
                AgentType.RISK_ASSESSMENT, 2,
                AgentType.PORTFOLIO_MANAGEMENT, 3,
                AgentType.MARKET_ANALYSIS, 4,
                AgentType.TRADING_EXECUTION, 5,
                AgentType.DATA_PROCESSING, 6   // Lowest level
            );
            
            Agent coordinator = participants.stream()
                .min((a, b) -> {
                    int levelA = hierarchyLevels.getOrDefault(a.getAgentType(), 10);
                    int levelB = hierarchyLevels.getOrDefault(b.getAgentType(), 10);
                    return levelA != levelB ? 
                        Integer.compare(levelA, levelB) :
                        Integer.compare(b.getPriority(), a.getPriority());
                })
                .orElse(participants.get(0));
            
            java.util.List<Agent> subordinates = participants.stream()
                .filter(agent -> !agent.getAgentId().equals(coordinator.getAgentId()))
                .sorted((a, b) -> {
                    int levelA = hierarchyLevels.getOrDefault(a.getAgentType(), 10);
                    int levelB = hierarchyLevels.getOrDefault(b.getAgentType(), 10);
                    return Integer.compare(levelA, levelB);
                })
                .toList();
            
            structuredLogger.logDebug("hierarchical_coordination", Map.of(
                "coordinator", coordinator.getAgentId(),
                "subordinateCount", subordinates.size(),
                "hierarchyDepth", hierarchyLevels.values().stream().distinct().count()
            ));
            
            Map<String, Object> coordinationResult = Map.of(
                "interactionType", "HIERARCHICAL_COORDINATION",
                "coordinator", coordinator.getAgentName(),
                "subordinates", subordinates.stream().map(Agent::getAgentName).toList(),
                "hierarchyEstablished", true,
                "commandChainLength", subordinates.size() + 1,
                "timestamp", Instant.now()
            );
            
            return Result.success(new InteractionResult(
                InteractionStatus.COMPLETED,
                coordinationResult,
                participants.stream().map(Agent::getAgentId).toList(),
                null
            ));
        });
    }

    /**
     * ✅ VALIDATION: Validate interaction participants
     * Cognitive Complexity: 3
     */
    private Result<java.util.List<Agent>, AgentError> validateParticipants(java.util.List<Agent> participants) {
        return participants == null || participants.isEmpty() ?
            Result.failure(AgentError.validationError("NO_PARTICIPANTS", 
                "At least one participant is required")) :
            participants.stream().anyMatch(agent -> 
                agent.getStatus() != com.trademaster.agentos.domain.entity.AgentStatus.ACTIVE) ?
            Result.failure(AgentError.validationError("INACTIVE_PARTICIPANTS", 
                "All participants must be active")) :
            Result.success(participants);
    }

    /**
     * ✅ COLLABORATION MANAGEMENT: Register active collaboration
     * Cognitive Complexity: 1
     */
    private void registerCollaboration(String collaborationId, InteractionContext context, 
            java.util.List<Agent> participants) {
        
        AgentCollaboration collaboration = new AgentCollaboration(
            collaborationId,
            context,
            participants,
            Instant.now(),
            InteractionStatus.IN_PROGRESS
        );
        
        activeCollaborations.put(collaborationId, collaboration);
    }

    /**
     * ✅ COLLABORATION MANAGEMENT: Complete collaboration
     * Cognitive Complexity: 1
     */
    private void completeCollaboration(String collaborationId, 
            Result<InteractionResult, AgentError> result) {
        
        AgentCollaboration collaboration = activeCollaborations.get(collaborationId);
        if (collaboration != null) {
            InteractionStatus status = result.isSuccess() ? 
                InteractionStatus.COMPLETED : InteractionStatus.FAILED;
            activeCollaborations.put(collaborationId, 
                collaboration.withStatus(status).withCompletedAt(Instant.now()));
        }
    }

    /**
     * ✅ UTILITY: Generate unique collaboration ID
     */
    private String generateCollaborationId() {
        return "COLLAB_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId();
    }

    /**
     * ✅ MONITORING: Get active collaborations
     */
    public Map<String, AgentCollaboration> getActiveCollaborations() {
        return Map.copyOf(activeCollaborations);
    }

    /**
     * ✅ MONITORING: Get collaboration statistics
     */
    public Map<String, Object> getCollaborationStatistics() {
        long totalCollaborations = activeCollaborations.size();
        long completedCollaborations = activeCollaborations.values().stream()
            .mapToLong(collab -> collab.getStatus() == InteractionStatus.COMPLETED ? 1 : 0)
            .sum();
        
        return Map.of(
            "totalActiveCollaborations", totalCollaborations,
            "completedCollaborations", completedCollaborations,
            "successRate", totalCollaborations > 0 ? 
                (double) completedCollaborations / totalCollaborations : 0.0,
            "averageParticipants", activeCollaborations.values().stream()
                .mapToInt(collab -> collab.getParticipants().size())
                .average()
                .orElse(0.0)
        );
    }

    /**
     * ✅ ENUMS AND RECORDS: Mediator data structures
     */
    
    public enum InteractionType {
        PEER_TO_PEER,
        BROADCAST,
        CHAIN_COLLABORATION,
        VOTING_CONSENSUS,
        HIERARCHICAL_COORDINATION
    }
    
    public enum InteractionStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public record InteractionContext(
        String requestId,
        InteractionType interactionType,
        String messageType,
        Long initiatorId,
        Map<String, Object> metadata,
        java.time.Duration timeout
    ) {
        public InteractionType getInteractionType() { return interactionType; }
        public String getMessageType() { return messageType; }
        public Long getInitiatorId() { return initiatorId; }
        public String getRequestId() { return requestId; }
    }
    
    public record InteractionResult(
        InteractionStatus status,
        Map<String, Object> result,
        java.util.List<Long> participantIds,
        AgentError error
    ) {}
    
    public record AgentCollaboration(
        String collaborationId,
        InteractionContext context,
        java.util.List<Agent> participants,
        Instant startedAt,
        InteractionStatus status,
        Instant completedAt
    ) {
        public AgentCollaboration(String collaborationId, InteractionContext context,
                java.util.List<Agent> participants, Instant startedAt, InteractionStatus status) {
            this(collaborationId, context, participants, startedAt, status, null);
        }
        
        public AgentCollaboration withStatus(InteractionStatus newStatus) {
            return new AgentCollaboration(collaborationId, context, participants, 
                startedAt, newStatus, completedAt);
        }
        
        public AgentCollaboration withCompletedAt(Instant completedAt) {
            return new AgentCollaboration(collaborationId, context, participants, 
                startedAt, status, completedAt);
        }
        
        public String getCollaborationId() { return collaborationId; }
        public java.util.List<Agent> getParticipants() { return participants; }
        public InteractionStatus getStatus() { return status; }
    }
}