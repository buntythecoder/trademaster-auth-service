# Agent Communication Patterns Specification

## Overview
Comprehensive specification for inter-agent communication patterns in TradeMaster Agent OS. This document defines the protocols, message formats, routing strategies, and coordination mechanisms that enable seamless collaboration between AI agents.

## Communication Architecture

### Core Communication Models

#### 1. Direct Agent-to-Agent Communication
```typescript
interface DirectCommunication {
  sendMessage(to: string, message: AgentMessage): Promise<MessageResult>;
  receiveMessage(from: string, message: AgentMessage): Promise<void>;
  establishChannel(agentId: string): Promise<CommunicationChannel>;
  closeChannel(channelId: string): Promise<void>;
}
```

#### 2. Publish-Subscribe Messaging
```typescript
interface PubSubCommunication {
  subscribe(topic: string, handler: MessageHandler): Promise<Subscription>;
  unsubscribe(subscriptionId: string): Promise<void>;
  publish(topic: string, message: AgentMessage): Promise<void>;
  createTopic(topic: string, config: TopicConfig): Promise<void>;
}
```

#### 3. Request-Response Pattern
```typescript
interface RequestResponseCommunication {
  sendRequest(to: string, request: AgentRequest): Promise<AgentResponse>;
  handleRequest(from: string, request: AgentRequest): Promise<AgentResponse>;
  setRequestHandler(handler: RequestHandler): void;
}
```

#### 4. Event-Driven Communication
```typescript
interface EventDrivenCommunication {
  emitEvent(event: AgentEvent): Promise<void>;
  addEventListener(eventType: string, handler: EventHandler): Promise<void>;
  removeEventListener(eventType: string, handler: EventHandler): Promise<void>;
}
```

## Message Types and Formats

### Base Message Structure
```typescript
interface AgentMessage {
  id: string;
  timestamp: Date;
  from: AgentIdentifier;
  to: AgentIdentifier | AgentIdentifier[];
  type: MessageType;
  priority: MessagePriority;
  metadata: MessageMetadata;
  payload: MessagePayload;
  correlationId?: string;
  replyTo?: string;
  ttl?: number;
}

interface AgentIdentifier {
  agentId: string;
  agentType: AgentType;
  instanceId?: string;
}

enum MessageType {
  TASK_REQUEST = 'task_request',
  TASK_RESPONSE = 'task_response',
  TASK_UPDATE = 'task_update',
  STATUS_UPDATE = 'status_update',
  RESOURCE_REQUEST = 'resource_request',
  RESOURCE_RESPONSE = 'resource_response',
  COORDINATION = 'coordination',
  NOTIFICATION = 'notification',
  HEARTBEAT = 'heartbeat',
  ERROR = 'error'
}

enum MessagePriority {
  CRITICAL = 0,
  HIGH = 1,
  NORMAL = 2,
  LOW = 3
}
```

### Specific Message Types

#### Task Request Message
```typescript
interface TaskRequestMessage extends AgentMessage {
  type: MessageType.TASK_REQUEST;
  payload: {
    taskId: string;
    taskType: TaskType;
    description: string;
    parameters: Record<string, any>;
    constraints: TaskConstraint[];
    deadline?: Date;
    requiredCapabilities: AgentCapability[];
    dependencies?: string[];
  };
}
```

#### Task Response Message
```typescript
interface TaskResponseMessage extends AgentMessage {
  type: MessageType.TASK_RESPONSE;
  payload: {
    taskId: string;
    status: TaskStatus;
    result?: TaskResult;
    error?: ErrorDetails;
    estimatedCompletion?: Date;
    progress?: number;
  };
}
```

#### Status Update Message
```typescript
interface StatusUpdateMessage extends AgentMessage {
  type: MessageType.STATUS_UPDATE;
  payload: {
    agentStatus: AgentStatus;
    currentTasks: string[];
    availableCapabilities: AgentCapability[];
    resourceUsage: ResourceUsage;
    performanceMetrics: PerformanceMetrics;
  };
}
```

#### Coordination Message
```typescript
interface CoordinationMessage extends AgentMessage {
  type: MessageType.COORDINATION;
  payload: {
    coordinationType: CoordinationType;
    participants: AgentIdentifier[];
    coordinationData: Record<string, any>;
    proposedAction?: ProposedAction;
  };
}

enum CoordinationType {
  CONSENSUS_REQUEST = 'consensus_request',
  LEADER_ELECTION = 'leader_election',
  RESOURCE_ALLOCATION = 'resource_allocation',
  WORKFLOW_COORDINATION = 'workflow_coordination',
  CONFLICT_RESOLUTION = 'conflict_resolution'
}
```

## Communication Patterns

### 1. Request-Response Pattern
Used for direct agent interactions where a response is expected.

```typescript
class RequestResponsePattern {
  async executePattern(
    requesterAgent: Agent,
    responderAgent: Agent,
    request: AgentRequest
  ): Promise<AgentResponse> {
    
    const message: AgentMessage = {
      id: generateMessageId(),
      timestamp: new Date(),
      from: { agentId: requesterAgent.id, agentType: requesterAgent.type },
      to: { agentId: responderAgent.id, agentType: responderAgent.type },
      type: MessageType.TASK_REQUEST,
      priority: MessagePriority.NORMAL,
      payload: request,
      correlationId: generateCorrelationId()
    };
    
    // Send request and wait for response
    return await this.communicationManager.sendAndWait(message, request.timeout);
  }
}
```

#### Use Cases:
- Market data requests
- Portfolio analysis queries
- Order execution commands
- Risk assessment requests

### 2. Publish-Subscribe Pattern
Enables one-to-many communication for event broadcasting.

```typescript
class PubSubPattern {
  async subscribeToMarketUpdates(agent: Agent): Promise<void> {
    await this.messageHub.subscribe('market.updates', (message: AgentMessage) => {
      if (message.payload.symbols.some(symbol => 
          agent.interestedSymbols.includes(symbol))) {
        agent.handleMarketUpdate(message.payload);
      }
    });
  }
  
  async publishTradeExecution(trade: TradeExecution): Promise<void> {
    const message: AgentMessage = {
      id: generateMessageId(),
      timestamp: new Date(),
      from: { agentId: 'trading-engine', agentType: AgentType.TRADING_EXECUTION },
      to: [], // Broadcast to all subscribers
      type: MessageType.NOTIFICATION,
      priority: MessagePriority.HIGH,
      payload: { tradeExecution: trade }
    };
    
    await this.messageHub.publish('trade.executed', message);
  }
}
```

#### Use Cases:
- Market data broadcasting
- Trade execution notifications
- System alerts and notifications
- Performance metrics updates

### 3. Chain of Responsibility Pattern
Routes messages through a series of agents until one handles it.

```typescript
class ChainOfResponsibilityPattern {
  private handlerChain: AgentHandler[] = [];
  
  addHandler(handler: AgentHandler): void {
    this.handlerChain.push(handler);
  }
  
  async processMessage(message: AgentMessage): Promise<MessageResult> {
    for (const handler of this.handlerChain) {
      if (await handler.canHandle(message)) {
        return await handler.handle(message);
      }
    }
    
    throw new Error(`No handler found for message type: ${message.type}`);
  }
}

interface AgentHandler {
  canHandle(message: AgentMessage): Promise<boolean>;
  handle(message: AgentMessage): Promise<MessageResult>;
}
```

#### Use Cases:
- Error escalation
- Task routing based on capabilities
- Fallback mechanisms
- Authorization chains

### 4. Orchestrated Workflow Pattern
Coordinates complex multi-agent workflows.

```typescript
class WorkflowOrchestrationPattern {
  async executeWorkflow(workflow: AgentWorkflow): Promise<WorkflowResult> {
    const context = new WorkflowContext();
    
    for (const step of workflow.steps) {
      try {
        const result = await this.executeStep(step, context);
        context.addResult(step.id, result);
        
        if (step.condition && !this.evaluateCondition(step.condition, context)) {
          break;
        }
      } catch (error) {
        if (step.onError === 'fail') {
          throw error;
        } else if (step.onError === 'retry') {
          // Implement retry logic
        } else if (step.onError === 'skip') {
          continue;
        }
      }
    }
    
    return context.getResult();
  }
  
  private async executeStep(step: WorkflowStep, context: WorkflowContext): Promise<any> {
    const targetAgent = await this.agentRegistry.findAgent(step.agentCriteria);
    
    const message: AgentMessage = {
      id: generateMessageId(),
      timestamp: new Date(),
      from: { agentId: 'workflow-orchestrator', agentType: AgentType.ORCHESTRATOR },
      to: { agentId: targetAgent.id, agentType: targetAgent.type },
      type: MessageType.TASK_REQUEST,
      priority: step.priority || MessagePriority.NORMAL,
      payload: {
        taskId: step.id,
        taskType: step.taskType,
        parameters: this.resolveParameters(step.parameters, context)
      }
    };
    
    return await this.communicationManager.sendAndWait(message);
  }
}
```

#### Use Cases:
- Complex trading strategies
- Portfolio rebalancing workflows
- Risk assessment procedures
- Compliance validation processes

### 5. Consensus Pattern
Enables agents to reach agreement on decisions.

```typescript
class ConsensusPattern {
  async reachConsensus(
    participants: Agent[],
    proposal: ConsensusProposal,
    threshold: number = 0.67
  ): Promise<ConsensusResult> {
    
    const votes: Vote[] = [];
    
    // Send proposal to all participants
    for (const agent of participants) {
      const message: CoordinationMessage = {
        id: generateMessageId(),
        timestamp: new Date(),
        from: { agentId: 'consensus-coordinator', agentType: AgentType.COORDINATOR },
        to: { agentId: agent.id, agentType: agent.type },
        type: MessageType.COORDINATION,
        priority: MessagePriority.HIGH,
        payload: {
          coordinationType: CoordinationType.CONSENSUS_REQUEST,
          participants: participants.map(p => ({ agentId: p.id, agentType: p.type })),
          coordinationData: { proposal }
        }
      };
      
      try {
        const response = await this.communicationManager.sendAndWait(message, 30000);
        votes.push({
          agentId: agent.id,
          vote: response.payload.vote,
          weight: agent.consensusWeight || 1
        });
      } catch (error) {
        // Handle non-responsive agents
        votes.push({
          agentId: agent.id,
          vote: 'abstain',
          weight: agent.consensusWeight || 1
        });
      }
    }
    
    return this.calculateConsensus(votes, threshold);
  }
}
```

#### Use Cases:
- Multi-agent trading decisions
- Risk threshold adjustments
- Portfolio allocation agreements
- Market sentiment consensus

## Message Routing and Delivery

### Routing Strategies

#### 1. Direct Routing
```typescript
class DirectRouter implements MessageRouter {
  async route(message: AgentMessage): Promise<RoutingResult> {
    if (Array.isArray(message.to)) {
      // Multicast to multiple agents
      const results = await Promise.allSettled(
        message.to.map(recipient => this.deliverMessage(message, recipient))
      );
      return { success: results.every(r => r.status === 'fulfilled'), results };
    } else {
      // Unicast to single agent
      const result = await this.deliverMessage(message, message.to);
      return { success: true, results: [result] };
    }
  }
}
```

#### 2. Capability-Based Routing
```typescript
class CapabilityRouter implements MessageRouter {
  async route(message: AgentMessage): Promise<RoutingResult> {
    const requiredCapabilities = message.payload.requiredCapabilities || [];
    
    if (requiredCapabilities.length === 0) {
      return this.directRouter.route(message);
    }
    
    const capableAgents = await this.agentRegistry.findAgentsByCapabilities(requiredCapabilities);
    
    if (capableAgents.length === 0) {
      throw new Error(`No agents found with required capabilities: ${requiredCapabilities.join(', ')}`);
    }
    
    // Select best agent based on load, performance, etc.
    const selectedAgent = this.selectOptimalAgent(capableAgents, message);
    
    message.to = { agentId: selectedAgent.id, agentType: selectedAgent.type };
    return this.directRouter.route(message);
  }
}
```

#### 3. Load-Balanced Routing
```typescript
class LoadBalancedRouter implements MessageRouter {
  async route(message: AgentMessage): Promise<RoutingResult> {
    const availableAgents = await this.agentRegistry.findAgentsByType(message.to.agentType);
    
    // Filter by availability and capacity
    const eligibleAgents = availableAgents.filter(agent => 
      agent.status === AgentStatus.ACTIVE && 
      agent.currentLoad < agent.maxCapacity
    );
    
    if (eligibleAgents.length === 0) {
      throw new Error('No available agents for routing');
    }
    
    // Select agent with lowest load
    const selectedAgent = eligibleAgents.reduce((prev, current) => 
      prev.currentLoad < current.currentLoad ? prev : current
    );
    
    message.to = { agentId: selectedAgent.id, agentType: selectedAgent.type };
    return this.directRouter.route(message);
  }
}
```

### Message Delivery Guarantees

#### 1. At-Least-Once Delivery
```typescript
class AtLeastOnceDelivery {
  private readonly maxRetries = 3;
  private readonly retryDelay = 1000;
  
  async deliver(message: AgentMessage, recipient: AgentIdentifier): Promise<DeliveryResult> {
    let attempt = 0;
    let lastError: Error | null = null;
    
    while (attempt < this.maxRetries) {
      try {
        const result = await this.attemptDelivery(message, recipient);
        
        if (result.delivered) {
          return result;
        }
        
        lastError = new Error(`Delivery failed: ${result.error}`);
      } catch (error) {
        lastError = error as Error;
      }
      
      attempt++;
      if (attempt < this.maxRetries) {
        await this.delay(this.retryDelay * Math.pow(2, attempt - 1));
      }
    }
    
    throw new Error(`Failed to deliver message after ${this.maxRetries} attempts: ${lastError?.message}`);
  }
}
```

#### 2. Exactly-Once Delivery
```typescript
class ExactlyOnceDelivery {
  private deliveredMessages = new Set<string>();
  
  async deliver(message: AgentMessage, recipient: AgentIdentifier): Promise<DeliveryResult> {
    const messageKey = `${message.id}-${recipient.agentId}`;
    
    if (this.deliveredMessages.has(messageKey)) {
      return { delivered: true, duplicate: true };
    }
    
    const result = await this.attemptDelivery(message, recipient);
    
    if (result.delivered) {
      this.deliveredMessages.add(messageKey);
      // Clean up old entries periodically
      this.scheduleCleanup(messageKey, message.ttl || 3600000);
    }
    
    return result;
  }
}
```

## Error Handling and Resilience

### Error Types
```typescript
enum CommunicationError {
  AGENT_UNREACHABLE = 'AGENT_UNREACHABLE',
  MESSAGE_TOO_LARGE = 'MESSAGE_TOO_LARGE',
  INVALID_FORMAT = 'INVALID_FORMAT',
  UNAUTHORIZED = 'UNAUTHORIZED',
  TIMEOUT = 'TIMEOUT',
  QUEUE_FULL = 'QUEUE_FULL',
  SERIALIZATION_ERROR = 'SERIALIZATION_ERROR'
}
```

### Circuit Breaker Pattern
```typescript
class CommunicationCircuitBreaker {
  private failures = 0;
  private lastFailureTime = 0;
  private state = CircuitState.CLOSED;
  
  async execute<T>(operation: () => Promise<T>): Promise<T> {
    if (this.state === CircuitState.OPEN) {
      if (Date.now() - this.lastFailureTime > this.resetTimeout) {
        this.state = CircuitState.HALF_OPEN;
      } else {
        throw new Error('Circuit breaker is OPEN');
      }
    }
    
    try {
      const result = await operation();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }
  
  private onSuccess(): void {
    this.failures = 0;
    this.state = CircuitState.CLOSED;
  }
  
  private onFailure(): void {
    this.failures++;
    this.lastFailureTime = Date.now();
    
    if (this.failures >= this.failureThreshold) {
      this.state = CircuitState.OPEN;
    }
  }
}
```

### Dead Letter Queue
```typescript
class DeadLetterQueue {
  private deadLetters: DeadLetter[] = [];
  
  async addDeadLetter(message: AgentMessage, error: Error): Promise<void> {
    const deadLetter: DeadLetter = {
      originalMessage: message,
      error: error.message,
      timestamp: new Date(),
      retryCount: 0
    };
    
    this.deadLetters.push(deadLetter);
    
    // Notify monitoring system
    await this.notificationService.sendAlert({
      type: 'DEAD_LETTER',
      message: `Message ${message.id} moved to dead letter queue`,
      severity: 'WARNING'
    });
  }
  
  async processDeadLetters(): Promise<void> {
    for (const deadLetter of this.deadLetters) {
      if (deadLetter.retryCount < this.maxRetries) {
        try {
          await this.retryMessage(deadLetter);
          this.removeDeadLetter(deadLetter);
        } catch (error) {
          deadLetter.retryCount++;
          deadLetter.lastRetry = new Date();
        }
      }
    }
  }
}
```

## Performance Optimization

### Message Batching
```typescript
class MessageBatcher {
  private batches = new Map<string, AgentMessage[]>();
  private batchTimers = new Map<string, NodeJS.Timeout>();
  
  addToBatch(recipient: string, message: AgentMessage): void {
    if (!this.batches.has(recipient)) {
      this.batches.set(recipient, []);
    }
    
    this.batches.get(recipient)!.push(message);
    
    // Set batch flush timer
    if (!this.batchTimers.has(recipient)) {
      const timer = setTimeout(() => {
        this.flushBatch(recipient);
      }, this.batchTimeout);
      
      this.batchTimers.set(recipient, timer);
    }
    
    // Flush if batch is full
    if (this.batches.get(recipient)!.length >= this.batchSize) {
      this.flushBatch(recipient);
    }
  }
  
  private async flushBatch(recipient: string): Promise<void> {
    const batch = this.batches.get(recipient);
    if (!batch || batch.length === 0) return;
    
    // Clear batch and timer
    this.batches.delete(recipient);
    const timer = this.batchTimers.get(recipient);
    if (timer) {
      clearTimeout(timer);
      this.batchTimers.delete(recipient);
    }
    
    // Send batch
    await this.sendBatchedMessages(recipient, batch);
  }
}
```

### Connection Pooling
```typescript
class ConnectionPool {
  private connections = new Map<string, Connection[]>();
  private maxConnectionsPerAgent = 5;
  
  async getConnection(agentId: string): Promise<Connection> {
    const connections = this.connections.get(agentId) || [];
    
    // Find available connection
    const availableConnection = connections.find(conn => !conn.inUse);
    if (availableConnection) {
      availableConnection.inUse = true;
      return availableConnection;
    }
    
    // Create new connection if under limit
    if (connections.length < this.maxConnectionsPerAgent) {
      const newConnection = await this.createConnection(agentId);
      connections.push(newConnection);
      this.connections.set(agentId, connections);
      return newConnection;
    }
    
    // Wait for available connection
    return this.waitForAvailableConnection(agentId);
  }
  
  releaseConnection(agentId: string, connection: Connection): void {
    connection.inUse = false;
    connection.lastUsed = new Date();
  }
}
```

## Monitoring and Observability

### Communication Metrics
```typescript
class CommunicationMetrics {
  private messageCounter = new Counter('agent_messages_total');
  private messageLatency = new Histogram('agent_message_latency_seconds');
  private activeConnections = new Gauge('agent_connections_active');
  
  recordMessageSent(message: AgentMessage): void {
    this.messageCounter.inc({
      from: message.from.agentType,
      to: Array.isArray(message.to) ? 'multicast' : message.to.agentType,
      type: message.type,
      priority: message.priority
    });
  }
  
  recordMessageLatency(message: AgentMessage, latency: number): void {
    this.messageLatency.observe({
      from: message.from.agentType,
      to: Array.isArray(message.to) ? 'multicast' : message.to.agentType,
      type: message.type
    }, latency);
  }
  
  updateActiveConnections(count: number): void {
    this.activeConnections.set(count);
  }
}
```

### Message Tracing
```typescript
class MessageTracer {
  traceMessage(message: AgentMessage): string {
    const traceId = generateTraceId();
    
    const span = tracer.startSpan(`agent.message.${message.type}`, {
      tags: {
        'agent.from': message.from.agentId,
        'agent.to': Array.isArray(message.to) ? 
          message.to.map(t => t.agentId).join(',') : 
          message.to.agentId,
        'message.type': message.type,
        'message.priority': message.priority,
        'message.size': JSON.stringify(message).length
      }
    });
    
    message.metadata = {
      ...message.metadata,
      traceId,
      spanId: span.context().spanId
    };
    
    return traceId;
  }
}
```

This specification provides comprehensive guidelines for implementing robust, scalable, and efficient communication patterns in the TradeMaster Agent OS system.