# TradeMaster Agent OS API Specification

## Overview

Complete REST API specification for the TradeMaster Agent Orchestration Service with integrated design patterns, security layers, and comprehensive endpoint documentation.

## Base Configuration

```yaml
openapi: 3.0.3
info:
  title: TradeMaster Agent Orchestration Service API
  description: Enterprise-grade Agent OS with advanced design patterns and zero-trust security
  version: 1.0.0
  contact:
    name: TradeMaster API Support
    email: api-support@trademaster.com
servers:
  - url: https://api.trademaster.com/agent-os/v1
    description: Production server
  - url: https://staging-api.trademaster.com/agent-os/v1
    description: Staging server
  - url: http://localhost:8080/agent-os/v1
    description: Development server
```

## Security Schemes

```yaml
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT token-based authentication via Security Facade
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
      description: API key for service-to-service communication
security:
  - BearerAuth: []
  - ApiKeyAuth: []
```

## Agent Management Endpoints

### GET /agents
**Chain of Responsibility Pattern Integration**

```yaml
get:
  summary: List all agents with filtering and pagination
  description: |
    Retrieves agents using Chain of Responsibility pattern for optimal selection.
    Supports filtering by type, status, capabilities, and load metrics.
  tags:
    - Agent Management
  parameters:
    - name: agentType
      in: query
      schema:
        $ref: '#/components/schemas/AgentType'
      description: Filter by agent type (triggers Type-based Handler)
    - name: status
      in: query
      schema:
        $ref: '#/components/schemas/AgentStatus'
      description: Filter by agent status
    - name: capabilities
      in: query
      schema:
        type: array
        items:
          $ref: '#/components/schemas/AgentCapability'
      description: Filter by required capabilities (triggers Capability-based Handler)
    - name: maxLoad
      in: query
      schema:
        type: integer
        minimum: 0
        maximum: 100
      description: Filter by maximum current load (triggers Load-based Handler)
    - name: page
      in: query
      schema:
        type: integer
        default: 0
        minimum: 0
    - name: size
      in: query
      schema:
        type: integer
        default: 20
        minimum: 1
        maximum: 100
  responses:
    '200':
      description: Agents retrieved successfully (may be cached via Proxy Pattern)
      headers:
        X-Cache-Status:
          description: Cache hit/miss status from Caching Proxy
          schema:
            type: string
            enum: [HIT, MISS, EXPIRED]
        X-Selection-Chain:
          description: Which handler in chain was used
          schema:
            type: string
            enum: [TYPE_BASED, CAPABILITY_BASED, LOAD_BASED, DEFAULT]
      content:
        application/json:
          schema:
            type: object
            properties:
              content:
                type: array
                items:
                  $ref: '#/components/schemas/AgentResponse'
              pageable:
                $ref: '#/components/schemas/PageInfo'
              totalElements:
                type: integer
              totalPages:
                type: integer
    '400':
      $ref: '#/components/responses/BadRequest'
    '401':
      $ref: '#/components/responses/Unauthorized'
    '403':
      $ref: '#/components/responses/Forbidden'
```

### POST /agents
**Factory Pattern Integration**

```yaml
post:
  summary: Create a new agent
  description: |
    Creates a new agent using Factory pattern for proper initialization.
    Includes automatic capability assignment and security validation.
  tags:
    - Agent Management
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CreateAgentRequest'
  responses:
    '201':
      description: Agent created successfully
      headers:
        X-Agent-Factory:
          description: Factory pattern used for creation
          schema:
            type: string
            enum: [STANDARD_FACTORY, ENHANCED_FACTORY]
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/AgentResponse'
    '400':
      $ref: '#/components/responses/BadRequest'
    '409':
      description: Agent name already exists
      content:
        application/json:
          schema:
            $ref: '#/components/responses/ConflictError'
```

### GET /agents/{agentId}/analytics
**Visitor Pattern Integration**

```yaml
get:
  summary: Get agent analytics using Visitor pattern
  description: |
    Retrieves comprehensive analytics for an agent using Visitor pattern.
    Different analytics types trigger different visitor implementations.
  tags:
    - Agent Analytics
  parameters:
    - name: agentId
      in: path
      required: true
      schema:
        type: integer
        format: int64
    - name: analyticsType
      in: query
      required: true
      schema:
        type: string
        enum: [PERFORMANCE, USAGE, ERROR_ANALYSIS, COMPREHENSIVE]
      description: Type of analytics to collect (determines visitor implementation)
    - name: timeRange
      in: query
      schema:
        type: string
        enum: [LAST_HOUR, LAST_DAY, LAST_WEEK, LAST_MONTH, CUSTOM]
        default: LAST_DAY
    - name: startTime
      in: query
      schema:
        type: string
        format: date-time
      description: Start time for CUSTOM time range
    - name: endTime
      in: query
      schema:
        type: string
        format: date-time
      description: End time for CUSTOM time range
  responses:
    '200':
      description: Analytics data collected successfully
      headers:
        X-Visitor-Type:
          description: Visitor pattern implementation used
          schema:
            type: string
            enum: [PERFORMANCE_VISITOR, USAGE_VISITOR, ERROR_VISITOR, COMPREHENSIVE_VISITOR]
        X-Analytics-Cache:
          description: Cache status for analytics data
          schema:
            type: string
            enum: [FRESH, CACHED, PARTIAL]
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/AgentAnalyticsResponse'
```

## Task Management Endpoints

### POST /tasks
**Template Method Pattern Integration**

```yaml
post:
  summary: Create and execute a task
  description: |
    Creates a task using Template Method pattern for structured execution.
    Workflow type determines specific template implementation.
  tags:
    - Task Management
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CreateTaskRequest'
  responses:
    '201':
      description: Task created and queued successfully
      headers:
        X-Workflow-Template:
          description: Template Method pattern used
          schema:
            type: string
            enum: [MARKET_ANALYSIS_WORKFLOW, RISK_ASSESSMENT_WORKFLOW, CUSTOM_WORKFLOW]
        X-Agent-Selection:
          description: Agent selection method used
          schema:
            type: string
            enum: [TYPE_BASED, CAPABILITY_BASED, LOAD_BASED, DEFAULT]
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/TaskResponse'
```

### POST /tasks/coordination
**Mediator Pattern Integration**

```yaml
post:
  summary: Coordinate multi-agent task execution
  description: |
    Coordinates complex multi-agent tasks using Mediator pattern.
    Supports various interaction patterns for agent collaboration.
  tags:
    - Task Coordination
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CoordinationRequest'
  responses:
    '200':
      description: Coordination completed successfully
      headers:
        X-Interaction-Type:
          description: Mediator interaction pattern used
          schema:
            type: string
            enum: [PEER_TO_PEER, BROADCAST, CHAIN_COLLABORATION, VOTING_CONSENSUS, HIERARCHICAL_COORDINATION]
        X-Coordination-Stats:
          description: Coordination performance statistics
          schema:
            type: string
            example: "agents:5,duration:150ms,success_rate:100%"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CoordinationResponse'
```

## Performance and Health Endpoints

### GET /health
**Proxy Pattern Integration**

```yaml
get:
  summary: System health check with caching
  description: |
    Comprehensive health check using Caching Proxy for performance.
    Includes pattern-specific health metrics and system status.
  tags:
    - Health & Monitoring
  responses:
    '200':
      description: System health status
      headers:
        X-Health-Cache:
          description: Health data cache status
          schema:
            type: string
            enum: [FRESH, CACHED, PARTIAL]
        X-Pattern-Health:
          description: Design pattern health status
          schema:
            type: string
            example: "chain:OK,visitor:OK,template:OK,proxy:OK,mediator:OK"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/HealthResponse'
```

### GET /metrics
**Visitor Pattern Analytics Integration**

```yaml
get:
  summary: System metrics with pattern-specific analytics
  description: |
    Comprehensive system metrics using Visitor pattern for detailed analytics.
    Includes performance data for all design patterns.
  tags:
    - Metrics & Analytics
  parameters:
    - name: pattern
      in: query
      schema:
        type: string
        enum: [CHAIN_OF_RESPONSIBILITY, VISITOR, TEMPLATE_METHOD, PROXY, MEDIATOR, ALL]
        default: ALL
      description: Specific pattern metrics to retrieve
    - name: format
      in: query
      schema:
        type: string
        enum: [JSON, PROMETHEUS, CSV]
        default: JSON
      description: Response format for metrics
  responses:
    '200':
      description: System metrics retrieved successfully
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/MetricsResponse'
        text/plain:
          description: Prometheus format metrics
          schema:
            type: string
```

## Data Models

### Agent Models

```yaml
components:
  schemas:
    AgentResponse:
      type: object
      required:
        - agentId
        - agentName
        - agentType
        - status
        - userId
      properties:
        agentId:
          type: integer
          format: int64
          example: 12345
        agentName:
          type: string
          maxLength: 100
          example: "market-analyzer-001"
        agentType:
          $ref: '#/components/schemas/AgentType'
        description:
          type: string
          maxLength: 500
          example: "Advanced market analysis agent with ML capabilities"
        status:
          $ref: '#/components/schemas/AgentStatus'
        userId:
          type: integer
          format: int64
          example: 67890
        capabilities:
          type: array
          items:
            $ref: '#/components/schemas/AgentCapability'
        maxConcurrentTasks:
          type: integer
          minimum: 1
          maximum: 100
          default: 5
        currentLoad:
          type: integer
          minimum: 0
          example: 3
        successRate:
          type: number
          format: double
          minimum: 0.0
          maximum: 1.0
          example: 0.95
        averageResponseTime:
          type: integer
          format: int64
          description: Average response time in milliseconds
          example: 1500
        totalTasksCompleted:
          type: integer
          format: int64
          example: 1542
        lastHeartbeat:
          type: string
          format: date-time
          example: "2024-01-15T10:30:00Z"
        lastError:
          type: string
          maxLength: 1000
          nullable: true
          example: "Connection timeout to external service"
        lastErrorTimestamp:
          type: string
          format: date-time
          nullable: true
          example: "2024-01-15T09:45:00Z"
        createdAt:
          type: string
          format: date-time
          example: "2024-01-01T00:00:00Z"
        updatedAt:
          type: string
          format: date-time
          example: "2024-01-15T10:30:00Z"

    AgentType:
      type: string
      enum:
        - MARKET_ANALYSIS
        - PORTFOLIO_MANAGEMENT
        - TRADING_EXECUTION
        - RISK_MANAGEMENT
        - NOTIFICATION
        - CUSTOM
      description: |
        Agent type determines capabilities and specialization:
        - MARKET_ANALYSIS: Technical/fundamental analysis, market screening
        - PORTFOLIO_MANAGEMENT: Portfolio optimization, asset allocation  
        - TRADING_EXECUTION: Order execution, smart routing
        - RISK_MANAGEMENT: Risk monitoring, compliance checking
        - NOTIFICATION: Alerts, reports, communications
        - CUSTOM: User-defined capabilities

    AgentStatus:
      type: string
      enum:
        - INITIALIZING
        - IDLE
        - ACTIVE
        - OVERLOADED
        - MAINTENANCE
        - FAILED
        - SHUTDOWN
        # Legacy states for backward compatibility
        - INACTIVE
        - STARTING  
        - BUSY
        - ERROR
        - STOPPING
        - UNRESPONSIVE
      description: |
        Agent operational status:
        - INITIALIZING: Agent starting up and performing initialization
        - IDLE: Ready and waiting for tasks  
        - ACTIVE: Actively processing tasks
        - OVERLOADED: At maximum capacity
        - MAINTENANCE: Planned maintenance mode
        - FAILED: Critical error requiring recovery
        - SHUTDOWN: Permanently shut down

    AgentCapability:
      type: string
      enum:
        # Market Analysis Capabilities
        - TECHNICAL_ANALYSIS
        - FUNDAMENTAL_ANALYSIS
        - SENTIMENT_ANALYSIS
        - MARKET_SCREENING
        - PRICE_PREDICTION
        - PATTERN_RECOGNITION
        # Portfolio Management Capabilities
        - PORTFOLIO_OPTIMIZATION
        - ASSET_ALLOCATION
        - RISK_ASSESSMENT
        - PERFORMANCE_ANALYSIS
        - REBALANCING
        - DIVERSIFICATION_ANALYSIS
        # Trading Execution Capabilities
        - ORDER_EXECUTION
        - BROKER_ROUTING
        - EXECUTION_OPTIMIZATION
        - SLIPPAGE_MONITORING
        - LIQUIDITY_ANALYSIS
        - ALGORITHMIC_TRADING
        # Risk Management Capabilities
        - RISK_MONITORING
        - VAR_CALCULATION
        - STRESS_TESTING
        - COMPLIANCE_CHECK
        - CORRELATION_ANALYSIS
        - DRAWDOWN_MONITORING
        # Communication & Notification Capabilities
        - EMAIL_ALERTS
        - SMS_ALERTS
        - PUSH_NOTIFICATIONS
        - REPORT_GENERATION
        - REAL_TIME_ALERTS
        - SCHEDULED_REPORTS
        # Data & Integration Capabilities
        - MARKET_DATA_INTEGRATION
        - BROKER_INTEGRATION
        - DATABASE_OPERATIONS
        - API_INTEGRATION
        - DATA_VALIDATION
        - REAL_TIME_STREAMING
        # Advanced AI Capabilities
        - MACHINE_LEARNING
        - NATURAL_LANGUAGE_PROCESSING
        - ANOMALY_DETECTION
        - PREDICTIVE_MODELING
        - REINFORCEMENT_LEARNING
        # Workflow & Orchestration Capabilities
        - WORKFLOW_EXECUTION
        - TASK_COORDINATION
        - EVENT_PROCESSING
        - DECISION_MAKING
        - MULTI_AGENT_COMMUNICATION
        # Custom & Extensible Capabilities
        - CUSTOM_LOGIC
        - PLUGIN_SUPPORT
        - SCRIPTING
```

### Task Models

```yaml
    TaskResponse:
      type: object
      required:
        - taskId
        - taskName
        - taskType
        - status
        - priority
        - userId
      properties:
        taskId:
          type: integer
          format: int64
          example: 98765
        taskName:
          type: string
          maxLength: 200
          example: "Market Analysis for AAPL"
        taskType:
          $ref: '#/components/schemas/TaskType'
        description:
          type: string
          maxLength: 1000
          example: "Comprehensive technical and fundamental analysis for Apple Inc."
        status:
          $ref: '#/components/schemas/TaskStatus'
        priority:
          $ref: '#/components/schemas/TaskPriority'
        userId:
          type: integer
          format: int64
          example: 67890
        agentId:
          type: integer
          format: int64
          nullable: true
          example: 12345
        workflowId:
          type: integer
          format: int64
          nullable: true
          example: 555
        parentTaskId:
          type: integer
          format: int64
          nullable: true
          example: 98764
        requiredCapabilities:
          type: array
          items:
            $ref: '#/components/schemas/AgentCapability'
        inputParameters:
          type: object
          description: JSON object containing task input parameters
          example:
            symbol: "AAPL"
            timeframe: "1D"
            indicators: ["RSI", "MACD", "EMA"]
        outputResult:
          type: object
          nullable: true
          description: JSON object containing task execution results
          example:
            analysis: "Bullish trend confirmed"
            confidence: 0.85
            signals: ["BUY"]
        errorMessage:
          type: string
          maxLength: 2000
          nullable: true
          example: "Market data temporarily unavailable"
        retryCount:
          type: integer
          minimum: 0
          default: 0
          example: 1
        maxRetries:
          type: integer
          minimum: 0
          default: 3
          example: 3
        timeoutSeconds:
          type: integer
          minimum: 1
          default: 300
          example: 600
        estimatedDurationSeconds:
          type: integer
          minimum: 1
          nullable: true
          example: 120
        actualDurationSeconds:
          type: integer
          minimum: 0
          nullable: true
          example: 95
        progressPercentage:
          type: integer
          minimum: 0
          maximum: 100
          default: 0
          example: 75
        startedAt:
          type: string
          format: date-time
          nullable: true
          example: "2024-01-15T10:15:00Z"
        completedAt:
          type: string
          format: date-time
          nullable: true
          example: "2024-01-15T10:16:35Z"
        deadline:
          type: string
          format: date-time
          nullable: true
          example: "2024-01-15T11:00:00Z"
        createdAt:
          type: string
          format: date-time
          example: "2024-01-15T10:10:00Z"
        updatedAt:
          type: string
          format: date-time
          example: "2024-01-15T10:16:35Z"

    TaskType:
      type: string
      enum:
        # Market Analysis Tasks
        - MARKET_ANALYSIS
        - TECHNICAL_ANALYSIS
        - FUNDAMENTAL_ANALYSIS
        - SENTIMENT_ANALYSIS
        - MARKET_SCREENING
        - PRICE_PREDICTION
        # Portfolio Management Tasks
        - PORTFOLIO_ANALYSIS
        - PORTFOLIO_OPTIMIZATION
        - ASSET_ALLOCATION
        - REBALANCING
        - PERFORMANCE_ANALYSIS
        - DIVERSIFICATION_ANALYSIS
        # Trading Execution Tasks
        - ORDER_EXECUTION
        - SMART_ROUTING
        - EXECUTION_MONITORING
        - SLIPPAGE_ANALYSIS
        - LIQUIDITY_ANALYSIS
        - ALGORITHMIC_EXECUTION
        # Risk Management Tasks
        - RISK_ASSESSMENT
        - RISK_MONITORING
        - VAR_CALCULATION
        - STRESS_TESTING
        - COMPLIANCE_CHECK
        - DRAWDOWN_MONITORING
        # Notification Tasks
        - ALERT_GENERATION
        - REPORT_GENERATION
        - EMAIL_NOTIFICATION
        - SMS_NOTIFICATION
        - PUSH_NOTIFICATION
        # Workflow and Orchestration Tasks
        - WORKFLOW_EXECUTION
        - TASK_COORDINATION
        - AGENT_COMMUNICATION
        - DECISION_MAKING
        # Custom Tasks
        - CUSTOM_TASK
        - SCRIPT_EXECUTION
        - PLUGIN_EXECUTION

    TaskStatus:
      type: string
      enum:
        - PENDING
        - QUEUED
        - IN_PROGRESS
        - PAUSED
        - COMPLETED
        - FAILED
        - ERROR
        - CANCELLED
        - TIMEOUT
        - WAITING

    TaskPriority:
      type: string
      enum:
        - CRITICAL
        - HIGH
        - NORMAL
        - LOW
        - DEFERRED
```

## Pattern-Specific Endpoints

### POST /coordination/agents
**Mediator Pattern Endpoint**

```yaml
post:
  summary: Coordinate multi-agent collaboration
  description: |
    Uses Mediator pattern to coordinate complex agent interactions.
    Supports various interaction patterns with comprehensive statistics.
  tags:
    - Agent Coordination
  requestBody:
    required: true
    content:
      application/json:
        schema:
          type: object
          required:
            - interactionType
            - participants
            - messageType
          properties:
            interactionType:
              type: string
              enum: [PEER_TO_PEER, BROADCAST, CHAIN_COLLABORATION, VOTING_CONSENSUS, HIERARCHICAL_COORDINATION]
            participants:
              type: array
              items:
                type: integer
                format: int64
              minItems: 2
              maxItems: 50
            messageType:
              type: string
              example: "MARKET_DATA_SYNC"
            requestId:
              type: string
              example: "coord-req-12345"
            initiatorId:
              type: string
              format: uuid
              example: "123e4567-e89b-12d3-a456-426614174000"
            timeout:
              type: integer
              default: 30000
              description: Coordination timeout in milliseconds
            metadata:
              type: object
              description: Additional coordination metadata
  responses:
    '200':
      description: Agent coordination completed
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CoordinationResponse'
```

### GET /analytics/comprehensive
**Visitor Pattern Analytics Endpoint**

```yaml
get:
  summary: Comprehensive system analytics using Visitor pattern
  description: |
    Collects comprehensive analytics across all agents using Visitor pattern.
    Provides detailed insights with performance metrics and trend analysis.
  tags:
    - System Analytics
  parameters:
    - name: scope
      in: query
      schema:
        type: string
        enum: [SYSTEM, AGENT_TYPE, INDIVIDUAL_AGENT]
        default: SYSTEM
    - name: agentType
      in: query
      schema:
        $ref: '#/components/schemas/AgentType'
      description: Required when scope=AGENT_TYPE
    - name: agentId
      in: query
      schema:
        type: integer
        format: int64
      description: Required when scope=INDIVIDUAL_AGENT
    - name: includePatterns
      in: query
      schema:
        type: boolean
        default: true
      description: Include design pattern performance metrics
  responses:
    '200':
      description: Comprehensive analytics data
      headers:
        X-Analytics-Patterns:
          description: Pattern analytics included
          schema:
            type: string
            example: "chain,visitor,template,proxy,mediator"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ComprehensiveAnalyticsResponse'
```

## Response Models

### Analytics Models

```yaml
    AgentAnalyticsResponse:
      type: object
      properties:
        agentId:
          type: integer
          format: int64
        analyticsType:
          type: string
          enum: [PERFORMANCE, USAGE, ERROR_ANALYSIS, COMPREHENSIVE]
        timeRange:
          $ref: '#/components/schemas/TimeRange'
        performanceMetrics:
          $ref: '#/components/schemas/PerformanceMetrics'
        usageMetrics:
          $ref: '#/components/schemas/UsageMetrics'
        errorAnalytics:
          $ref: '#/components/schemas/ErrorAnalytics'
        patternMetrics:
          $ref: '#/components/schemas/PatternMetrics'
        generatedAt:
          type: string
          format: date-time
        cacheStatus:
          type: string
          enum: [FRESH, CACHED, PARTIAL]

    PerformanceMetrics:
      type: object
      properties:
        averageResponseTime:
          type: integer
          format: int64
          description: Average response time in milliseconds
        successRate:
          type: number
          format: double
          minimum: 0.0
          maximum: 1.0
        throughput:
          type: number
          format: double
          description: Tasks per second
        resourceUtilization:
          type: number
          format: double
          minimum: 0.0
          maximum: 1.0
        patternEfficiency:
          type: object
          properties:
            chainOfResponsibility:
              type: number
              format: double
            visitorPattern:
              type: number
              format: double
            templateMethod:
              type: number
              format: double
            proxyPattern:
              type: number
              format: double
            mediatorPattern:
              type: number
              format: double

    CoordinationResponse:
      type: object
      properties:
        coordinationId:
          type: string
          format: uuid
        interactionType:
          type: string
          enum: [PEER_TO_PEER, BROADCAST, CHAIN_COLLABORATION, VOTING_CONSENSUS, HIERARCHICAL_COORDINATION]
        participants:
          type: array
          items:
            type: integer
            format: int64
        results:
          type: object
          description: Coordination results from all participants
        statistics:
          type: object
          properties:
            totalParticipants:
              type: integer
            successfulParticipants:
              type: integer
            averageResponseTime:
              type: integer
              format: int64
            coordinationDuration:
              type: integer
              format: int64
        success:
          type: boolean
        completedAt:
          type: string
          format: date-time
```

## Error Responses

```yaml
    responses:
      BadRequest:
        description: Invalid request parameters
        content:
          application/json:
            schema:
              type: object
              properties:
                error:
                  type: string
                  example: "VALIDATION_ERROR"
                message:
                  type: string
                  example: "Invalid agent type specified"
                details:
                  type: object
                  properties:
                    field:
                      type: string
                    rejectedValue:
                      type: string
                    allowedValues:
                      type: array
                      items:
                        type: string
                timestamp:
                  type: string
                  format: date-time
                requestId:
                  type: string
                  format: uuid

      Unauthorized:
        description: Authentication required or failed
        content:
          application/json:
            schema:
              type: object
              properties:
                error:
                  type: string
                  example: "AUTHENTICATION_FAILED"
                message:
                  type: string
                  example: "Valid JWT token required"
                securityLayer:
                  type: string
                  example: "SecurityFacade -> SecurityMediator"

      Forbidden:
        description: Insufficient permissions
        content:
          application/json:
            schema:
              type: object
              properties:
                error:
                  type: string
                  example: "AUTHORIZATION_FAILED"
                message:
                  type: string
                  example: "Insufficient permissions for requested operation"
                requiredPermissions:
                  type: array
                  items:
                    type: string
                userPermissions:
                  type: array
                  items:
                    type: string
```

## Pattern Performance Headers

All endpoints include pattern-specific performance headers:

```yaml
headers:
  X-Pattern-Performance:
    description: Performance metrics for design patterns used
    schema:
      type: string
      example: "chain:5ms,proxy:2ms,visitor:100ms,template:50ms,mediator:30ms"
  X-Cache-Performance:
    description: Cache performance metrics
    schema:
      type: string
      example: "hit_rate:85%,avg_time:2ms,entries:1024"
  X-Security-Layer:
    description: Security processing information
    schema:
      type: string
      example: "facade:2ms,mediator:5ms,total:7ms"
  X-Request-ID:
    description: Unique request identifier for tracing
    schema:
      type: string
      format: uuid
  X-Response-Time:
    description: Total response time in milliseconds
    schema:
      type: integer
      format: int64
```

## Rate Limiting and Quotas

```yaml
components:
  headers:
    X-RateLimit-Limit:
      description: Request limit per hour
      schema:
        type: integer
        example: 1000
    X-RateLimit-Remaining:
      description: Remaining requests in current window
      schema:
        type: integer
        example: 987
    X-RateLimit-Reset:
      description: Time when rate limit resets
      schema:
        type: string
        format: date-time
```

## Webhook Support

```yaml
paths:
  /webhooks/task-events:
    post:
      summary: Task lifecycle event webhooks
      description: |
        Webhook endpoint for real-time task status updates.
        Includes pattern execution metrics and performance data.
      tags:
        - Webhooks
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                eventType:
                  type: string
                  enum: [TASK_CREATED, TASK_STARTED, TASK_COMPLETED, TASK_FAILED, TASK_CANCELLED]
                taskId:
                  type: integer
                  format: int64
                agentId:
                  type: integer
                  format: int64
                patternMetrics:
                  type: object
                  properties:
                    patternUsed:
                      type: array
                      items:
                        type: string
                    executionTime:
                      type: integer
                      format: int64
                timestamp:
                  type: string
                  format: date-time
      responses:
        '200':
          description: Webhook received successfully
```

## API Integration Examples

### Creating a Market Analysis Task

```bash
curl -X POST "https://api.trademaster.com/agent-os/v1/tasks" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "AAPL Technical Analysis",
    "taskType": "TECHNICAL_ANALYSIS",
    "description": "Comprehensive technical analysis for Apple Inc.",
    "priority": "HIGH",
    "requiredCapabilities": ["TECHNICAL_ANALYSIS", "PATTERN_RECOGNITION"],
    "inputParameters": {
      "symbol": "AAPL",
      "timeframe": "1D",
      "indicators": ["RSI", "MACD", "Bollinger Bands"]
    },
    "timeoutSeconds": 300,
    "deadline": "2024-01-15T11:00:00Z"
  }'
```

### Agent Coordination Request

```bash
curl -X POST "https://api.trademaster.com/agent-os/v1/coordination/agents" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "interactionType": "VOTING_CONSENSUS",
    "participants": [12345, 12346, 12347, 12348, 12349],
    "messageType": "PORTFOLIO_REBALANCING_DECISION",
    "requestId": "coord-req-67890",
    "timeout": 60000,
    "metadata": {
      "portfolioId": "PORT-123",
      "rebalanceThreshold": 0.05
    }
  }'
```

## Performance SLAs

| Endpoint Category | Target Response Time | Throughput | Cache Hit Rate |
|------------------|---------------------|------------|----------------|
| Agent Queries | <100ms | 1000 req/s | 85% |
| Task Operations | <200ms | 500 req/s | 70% |
| Analytics | <500ms | 200 req/s | 90% |
| Coordination | <1000ms | 100 req/s | 60% |
| Health Checks | <50ms | 2000 req/s | 95% |

## Security Considerations

- **Zero Trust Architecture**: All endpoints protected by SecurityFacade + SecurityMediator
- **JWT Token Validation**: All requests require valid JWT tokens
- **Role-Based Access Control**: Fine-grained permissions per endpoint
- **Rate Limiting**: Protection against abuse and DoS attacks
- **Audit Logging**: Complete audit trail for all API operations
- **Input Validation**: Comprehensive validation with pattern-specific rules