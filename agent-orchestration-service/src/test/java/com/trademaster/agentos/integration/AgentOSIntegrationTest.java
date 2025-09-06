package com.trademaster.agentos.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.AgentOSApplication;
import com.trademaster.agentos.domain.entity.*;
import com.trademaster.agentos.domain.immutable.ImmutableTaskQueue;
import com.trademaster.agentos.events.BaseEvent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.mediator.AgentInteractionMediator;
import com.trademaster.agentos.repository.AgentRepository;
import com.trademaster.agentos.repository.TaskRepository;
import com.trademaster.agentos.service.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive Integration Tests for Agent Orchestration Service (Agent OS)
 * 
 * MANDATORY: TestContainers - Rule #20
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: AgentOS Integration - Financial Domain Rules
 */
@SpringBootTest(
    classes = AgentOSApplication.class, 
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "spring.threads.virtual.enabled=true",
        "spring.test.context.cache.maxSize=3"
    }
)
@Testcontainers
@DisplayName("Agent OS Comprehensive Integration Tests")
class AgentOSIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("agentos_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass testpass")
            .withReuse(false);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withEmbeddedZookeeper()
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withReuse(false);

    @Container
    static GenericContainer<?> prometheus = new GenericContainer<>(DockerImageName.parse("prom/prometheus:v2.48.0"))
            .withExposedPorts(9090)
            .withCommand(
                "--config.file=/etc/prometheus/prometheus.yml",
                "--storage.tsdb.path=/prometheus",
                "--web.console.libraries=/etc/prometheus/console_libraries",
                "--web.console.templates=/etc/prometheus/consoles",
                "--web.enable-lifecycle"
            )
            .waitingFor(Wait.forHttp("/-/ready").forPort(9090))
            .withReuse(false);

    @Container
    static GenericContainer<?> mcp = new GenericContainer<>(DockerImageName.parse("alpine:3.19"))
            .withExposedPorts(8080)
            .withCommand("sh", "-c", 
                "apk add --no-cache python3 py3-pip && " +
                "echo 'from http.server import HTTPServer, BaseHTTPRequestHandler; " +
                "import json; " +
                "class MCPHandler(BaseHTTPRequestHandler): " +
                "    def do_POST(self): " +
                "        self.send_response(200); " +
                "        self.send_header(\"Content-type\", \"application/json\"); " +
                "        self.end_headers(); " +
                "        self.wfile.write(json.dumps({\"result\": \"success\", \"capabilities\": [\"ANALYSIS\", \"EXECUTION\"]}).encode()); " +
                "HTTPServer((\"\", 8080), MCPHandler).serve_forever()' > /tmp/mcp_server.py && " +
                "python3 /tmp/mcp_server.py"
            )
            .waitingFor(Wait.forListeningPort())
            .withReuse(false);

    @Autowired
    private AgentOrchestrationService orchestrationService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AgentRegistryService agentRegistryService;

    @Autowired
    private EventPublishingService eventPublishingService;

    @Autowired
    private MCPProtocolService mcpProtocolService;

    @Autowired
    private AgentInteractionMediator interactionMediator;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        // Redis configuration for Agent Registry
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "testpass");
        registry.add("spring.cache.redis.time-to-live", () -> "600000");

        // Kafka configuration for Event Publishing
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.consumer.group-id", () -> "agentos-test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        // Prometheus metrics configuration
        registry.add("management.endpoints.web.exposure.include", () -> "health,metrics,prometheus");
        registry.add("management.metrics.export.prometheus.enabled", () -> "true");
        registry.add("management.endpoint.prometheus.enabled", () -> "true");

        // MCP Protocol configuration
        registry.add("agentos.mcp.server.host", mcp::getHost);
        registry.add("agentos.mcp.server.port", mcp::getFirstMappedPort);
        registry.add("agentos.mcp.server.protocol", () -> "http");
        registry.add("agentos.mcp.enabled", () -> "true");

        // Agent OS specific configuration
        registry.add("agentos.registry.heartbeat-interval", () -> "5s");
        registry.add("agentos.registry.cleanup-interval", () -> "30s");
        registry.add("agentos.task-queue.default-timeout", () -> "300s");
        registry.add("agentos.task-queue.max-concurrent-tasks", () -> "100");
        registry.add("agentos.orchestration.load-balancing-strategy", () -> "ROUND_ROBIN");
        registry.add("agentos.orchestration.health-check-interval", () -> "10s");

        // Circuit breaker configuration for Agent OS
        registry.add("resilience4j.circuitbreaker.instances.agent-communication.failure-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.agent-communication.sliding-window-size", () -> "10");
        registry.add("resilience4j.retry.instances.agent-communication.max-attempts", () -> "3");
    }

    @BeforeAll
    static void beforeAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = serverPort;
        RestAssured.basePath = "/api/v1/agentos";

        // Setup test data
        setupTestAgents();
        setupTestCapabilities();
    }

    /**
     * Test complete agent registration and discovery lifecycle
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: AgentOS Integration - Financial Domain Rules
     */
    @Test
    @DisplayName("Agent Registration & Discovery - Complete Lifecycle")
    void agentRegistrationDiscovery_CompleteLifecycle_ShouldRegisterAndDiscoverAgents() throws Exception {
        // Given: New agent registration request
        Agent marketAnalysisAgent = Agent.builder()
                .agentName("MarketAnalysisAgent-Integration")
                .agentType(AgentType.MARKET_ANALYSIS)
                .status(AgentStatus.STARTING)
                .maxConcurrentTasks(5)
                .currentLoad(0)
                .userId(1L)
                .capabilities(Set.of(AgentCapability.ANALYSIS, AgentCapability.PREDICTION))
                .lastHeartbeat(Instant.now())
                .build();

        // When: Register agent via API
        Agent registeredAgent = given()
                .contentType(ContentType.JSON)
                .body(marketAnalysisAgent)
                .auth().basic("admin", "admin123")
                .when()
                .post("/agents/register")
                .then()
                .statusCode(201)
                .extract()
                .as(Agent.class);

        // Then: Verify agent was registered
        assertThat(registeredAgent).isNotNull();
        assertThat(registeredAgent.getAgentId()).isNotNull();
        assertThat(registeredAgent.getAgentName()).isEqualTo("MarketAnalysisAgent-Integration");
        assertThat(registeredAgent.getStatus()).isEqualTo(AgentStatus.STARTING);

        // And: Verify agent persisted in database
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<Agent> persistedAgent = agentRepository.findById(registeredAgent.getAgentId());
                    assertThat(persistedAgent).isPresent();
                    assertThat(persistedAgent.get().getAgentType()).isEqualTo(AgentType.MARKET_ANALYSIS);
                });

        // And: Verify agent discoverable via registry
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<Agent> availableAgents = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/agents/available/{agentType}", AgentType.MARKET_ANALYSIS)
                            .then()
                            .statusCode(200)
                            .extract()
                            .jsonPath()
                            .getList(".", Agent.class);

                    assertThat(availableAgents).isNotEmpty();
                    assertThat(availableAgents).anyMatch(a -> 
                        a.getAgentName().equals("MarketAnalysisAgent-Integration"));
                });

        // When: Send heartbeat to activate agent
        given()
                .auth().basic("admin", "admin123")
                .when()
                .post("/agents/{agentId}/heartbeat", registeredAgent.getAgentId())
                .then()
                .statusCode(204);

        // Then: Agent should become active
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    Agent activeAgent = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/agents/{agentId}", registeredAgent.getAgentId())
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(Agent.class);

                    assertThat(activeAgent.getStatus()).isEqualTo(AgentStatus.ACTIVE);
                });
    }

    /**
     * Test complete task orchestration with agent assignment
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Functional Programming - Rule #3
     */
    @Test
    @DisplayName("Task Orchestration - Assignment and Execution Flow")
    void taskOrchestration_AssignmentAndExecution_ShouldOrchestrateTasks() {
        // Given: Active agents available
        Agent portfolioAgent = createAndRegisterAgent("PortfolioAgent", AgentType.PORTFOLIO_MANAGEMENT, 
                Set.of(AgentCapability.PORTFOLIO_ANALYSIS, AgentCapability.OPTIMIZATION));
        
        Agent riskAgent = createAndRegisterAgent("RiskAgent", AgentType.RISK_MANAGEMENT,
                Set.of(AgentCapability.RISK_ANALYSIS, AgentCapability.MONITORING));

        // When: Submit high priority task
        Task portfolioAnalysisTask = Task.builder()
                .taskName("Portfolio Risk Analysis")
                .taskType(TaskType.PORTFOLIO_ANALYSIS)
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.PENDING)
                .requiredCapabilities(Set.of(AgentCapability.PORTFOLIO_ANALYSIS))
                .taskData(Map.of(
                    "userId", 123L,
                    "portfolioId", "PORT_001",
                    "analysisType", "RISK_ASSESSMENT"
                ))
                .createdAt(Instant.now())
                .build();

        Task submittedTask = given()
                .contentType(ContentType.JSON)
                .body(portfolioAnalysisTask)
                .auth().basic("admin", "admin123")
                .when()
                .post("/tasks/submit")
                .then()
                .statusCode(201)
                .extract()
                .as(Task.class);

        // Then: Task should be created and potentially assigned immediately
        assertThat(submittedTask).isNotNull();
        assertThat(submittedTask.getTaskId()).isNotNull();
        assertThat(submittedTask.getPriority()).isEqualTo(TaskPriority.HIGH);

        // And: Verify task is eventually assigned to appropriate agent
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    Task assignedTask = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/tasks/{taskId}", submittedTask.getTaskId())
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(Task.class);

                    assertThat(assignedTask.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
                    assertThat(assignedTask.getAgentId()).isNotNull();
                });

        // When: Simulate task completion
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "success", true,
                    "result", "Portfolio analysis completed successfully",
                    "responseTimeMs", 2500L
                ))
                .auth().basic("admin", "admin123")
                .when()
                .post("/tasks/{taskId}/complete", submittedTask.getTaskId())
                .then()
                .statusCode(204);

        // Then: Task should be completed
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Task completedTask = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/tasks/{taskId}", submittedTask.getTaskId())
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(Task.class);

                    assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
                    assertThat(completedTask.getResult()).contains("completed successfully");
                });
    }

    /**
     * Test concurrent agent orchestration with Virtual Threads
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Concurrent Processing - Rule #12
     */
    @Test
    @DisplayName("Concurrent Orchestration - Virtual Threads Performance")
    void concurrentOrchestration_VirtualThreadsPerformance_ShouldHandleConcurrentLoad() {
        // Given: Multiple agents of different types
        List<Agent> agents = List.of(
                createAndRegisterAgent("MarketAgent1", AgentType.MARKET_ANALYSIS, 
                        Set.of(AgentCapability.ANALYSIS)),
                createAndRegisterAgent("MarketAgent2", AgentType.MARKET_ANALYSIS, 
                        Set.of(AgentCapability.ANALYSIS)),
                createAndRegisterAgent("TradingAgent1", AgentType.TRADING_EXECUTION, 
                        Set.of(AgentCapability.EXECUTION)),
                createAndRegisterAgent("NotificationAgent1", AgentType.NOTIFICATION, 
                        Set.of(AgentCapability.COMMUNICATION))
        );

        // When: Submit multiple concurrent tasks
        List<TaskType> taskTypes = List.of(
                TaskType.MARKET_ANALYSIS, TaskType.TECHNICAL_ANALYSIS, 
                TaskType.ORDER_EXECUTION, TaskType.ALERT_GENERATION
        );

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Task>> taskFutures = IntStream.range(0, 20)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    TaskType taskType = taskTypes.get(i % taskTypes.size());
                    Task task = Task.builder()
                            .taskName("ConcurrentTask-" + i)
                            .taskType(taskType)
                            .priority(i % 2 == 0 ? TaskPriority.HIGH : TaskPriority.MEDIUM)
                            .status(TaskStatus.PENDING)
                            .requiredCapabilities(determineRequiredCapabilities(taskType))
                            .taskData(Map.of("taskIndex", i, "timestamp", System.currentTimeMillis()))
                            .createdAt(Instant.now())
                            .build();

                    return given()
                            .contentType(ContentType.JSON)
                            .body(task)
                            .auth().basic("admin", "admin123")
                            .when()
                            .post("/tasks/submit")
                            .then()
                            .statusCode(201)
                            .extract()
                            .as(Task.class);
                }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()))
                .toList();

        List<Task> submittedTasks = taskFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Then: All tasks should be submitted successfully
        assertThat(submittedTasks).hasSize(20);
        assertThat(submittedTasks).allMatch(task -> task.getTaskId() != null);

        // And: Processing should be fast due to Virtual Threads
        assertThat(processingTime).isLessThan(10000); // Should complete in under 10 seconds

        // And: Verify tasks get assigned over time
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    List<Task> allTasks = taskRepository.findAll();
                    long assignedCount = allTasks.stream()
                            .filter(t -> t.getAgentId() != null)
                            .count();
                    assertThat(assignedCount).isGreaterThan(10); // At least half should be assigned
                });
    }

    /**
     * Test MCP Protocol integration with external Agent OS components
     * MANDATORY: MCP Protocol - AgentOS Integration
     * MANDATORY: External Integration - Rule #24
     */
    @Test
    @DisplayName("MCP Protocol Integration - External Agent Communication")
    void mcpProtocolIntegration_ExternalAgentCommunication_ShouldCommunicateViaMCP() throws Exception {
        // Given: Agent with MCP capabilities
        Agent mcpAgent = createAndRegisterAgent("MCPAgent", AgentType.CUSTOM,
                Set.of(AgentCapability.MCP_COMMUNICATION, AgentCapability.ANALYSIS));

        // When: Test MCP protocol communication
        Map<String, Object> mcpRequest = Map.of(
                "method", "agent.capabilities",
                "params", Map.of("agentId", mcpAgent.getAgentId()),
                "id", UUID.randomUUID().toString()
        );

        Map<String, Object> mcpResponse = given()
                .contentType(ContentType.JSON)
                .body(mcpRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/mcp/request")
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeReference<Map<String, Object>>() {});

        // Then: Verify MCP response
        assertThat(mcpResponse).isNotNull();
        assertThat(mcpResponse.get("result")).isEqualTo("success");

        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) mcpResponse.get("capabilities");
        assertThat(capabilities).contains("ANALYSIS");
    }

    /**
     * Test event publishing and inter-agent communication
     * MANDATORY: Event-Driven Architecture - AgentOS Integration
     * MANDATORY: Kafka Integration - Rule #20
     */
    @Test
    @DisplayName("Event Publishing - Inter-Agent Communication")
    void eventPublishing_InterAgentCommunication_ShouldPublishAndConsumeEvents() {
        // Given: Multiple agents subscribed to events
        Agent sourceAgent = createAndRegisterAgent("SourceAgent", AgentType.MARKET_ANALYSIS,
                Set.of(AgentCapability.ANALYSIS));
        
        Agent targetAgent = createAndRegisterAgent("TargetAgent", AgentType.TRADING_EXECUTION,
                Set.of(AgentCapability.EXECUTION));

        // When: Publish agent collaboration event
        BaseEvent collaborationEvent = BaseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("AGENT_COLLABORATION")
                .sourceAgentId(sourceAgent.getAgentId())
                .targetAgentId(targetAgent.getAgentId())
                .eventData(Map.of(
                    "action", "MARKET_SIGNAL",
                    "signal", "BUY",
                    "symbol", "AAPL",
                    "confidence", 0.85
                ))
                .timestamp(Instant.now())
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(collaborationEvent)
                .auth().basic("admin", "admin123")
                .when()
                .post("/events/publish")
                .then()
                .statusCode(202); // Accepted for async processing

        // Then: Verify event was published to Kafka
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    // Verify through metrics or event store that event was processed
                    Map<String, Object> eventStats = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/events/stats")
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeReference<Map<String, Object>>() {});

                    assertThat(eventStats.get("totalEvents")).isNotNull();
                    assertThat(((Number) eventStats.get("totalEvents")).intValue()).isGreaterThan(0);
                });
    }

    /**
     * Test system metrics and health monitoring
     * MANDATORY: Monitoring - AgentOS Integration
     * MANDATORY: Prometheus Metrics - Rule #15
     */
    @Test
    @DisplayName("System Monitoring - Metrics and Health")
    void systemMonitoring_MetricsAndHealth_ShouldProvideSystemInsights() {
        // Given: Active system with agents and tasks
        setupActiveSystem();

        // When: Get orchestration metrics
        AgentOrchestrationService.OrchestrationMetrics metrics = given()
                .auth().basic("admin", "admin123")
                .when()
                .get("/orchestration/metrics")
                .then()
                .statusCode(200)
                .extract()
                .as(AgentOrchestrationService.OrchestrationMetrics.class);

        // Then: Verify comprehensive metrics
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalAgents()).isGreaterThan(0);
        assertThat(metrics.getActiveAgents()).isGreaterThan(0);
        assertThat(metrics.getTotalTasks()).isGreaterThan(0);
        assertThat(metrics.getSystemUtilization()).isBetween(0.0, 100.0);

        // And: Verify Prometheus metrics endpoint
        String prometheusMetrics = given()
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(prometheusMetrics).contains("agentos_agents_total");
        assertThat(prometheusMetrics).contains("agentos_tasks_total");
        assertThat(prometheusMetrics).contains("agentos_system_utilization");
    }

    /**
     * Test agent collaboration and mediator patterns
     * MANDATORY: Mediator Pattern - Rule #4
     * MANDATORY: Agent Collaboration - AgentOS Integration
     */
    @Test
    @DisplayName("Agent Collaboration - Mediator Pattern Implementation")
    void agentCollaboration_MediatorPattern_ShouldCoordinateComplexInteractions() throws Exception {
        // Given: Multiple agents for collaboration
        List<Agent> collaboratingAgents = List.of(
                createAndRegisterAgent("MarketAnalyst", AgentType.MARKET_ANALYSIS,
                        Set.of(AgentCapability.ANALYSIS, AgentCapability.PREDICTION)),
                createAndRegisterAgent("RiskManager", AgentType.RISK_MANAGEMENT,
                        Set.of(AgentCapability.RISK_ANALYSIS, AgentCapability.MONITORING)),
                createAndRegisterAgent("PortfolioManager", AgentType.PORTFOLIO_MANAGEMENT,
                        Set.of(AgentCapability.PORTFOLIO_ANALYSIS, AgentCapability.OPTIMIZATION))
        );

        // When: Initiate voting consensus for investment decision
        Map<String, Object> consensusRequest = Map.of(
                "decisionTopic", "INVESTMENT_OPPORTUNITY_AAPL",
                "participantIds", collaboratingAgents.stream()
                        .map(Agent::getAgentId).toList(),
                "requestId", UUID.randomUUID().toString(),
                "timeoutMinutes", 5
        );

        Map<String, Object> consensusResult = given()
                .contentType(ContentType.JSON)
                .body(consensusRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/orchestration/collaboration/consensus")
                .then()
                .statusCode(202) // Accepted for async processing
                .extract()
                .as(new TypeReference<Map<String, Object>>() {});

        // Then: Verify collaboration was initiated
        assertThat(consensusResult).isNotNull();
        assertThat(consensusResult.get("status")).isEqualTo("INITIATED");

        // And: Verify collaboration statistics
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    Map<String, Object> collaborationStats = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/orchestration/collaboration/stats")
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeReference<Map<String, Object>>() {});

                    assertThat(collaborationStats.get("totalCollaborations")).isNotNull();
                    assertThat(((Number) collaborationStats.get("totalCollaborations")).intValue()).isGreaterThan(0);
                });
    }

    /**
     * Test task queue immutability and functional processing
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Immutable Data Structures - Rule #9
     */
    @Test
    @DisplayName("Task Queue Processing - Immutable Data Structures")
    void taskQueueProcessing_ImmutableDataStructures_ShouldProcessFunctionally() {
        // Given: Multiple tasks for queue processing
        List<Task> taskBatch = IntStream.range(0, 10)
                .mapToObj(i -> Task.builder()
                        .taskName("BatchTask-" + i)
                        .taskType(TaskType.MARKET_ANALYSIS)
                        .priority(i % 2 == 0 ? TaskPriority.HIGH : TaskPriority.LOW)
                        .status(TaskStatus.PENDING)
                        .requiredCapabilities(Set.of(AgentCapability.ANALYSIS))
                        .taskData(Map.of("batchIndex", i))
                        .createdAt(Instant.now())
                        .build())
                .toList();

        // When: Submit batch via immutable task queue
        List<Task> submittedTasks = taskBatch.stream()
                .map(task -> given()
                        .contentType(ContentType.JSON)
                        .body(task)
                        .auth().basic("admin", "admin123")
                        .when()
                        .post("/tasks/submit")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(Task.class))
                .toList();

        // Then: All tasks should be processed through immutable queue
        assertThat(submittedTasks).hasSize(10);
        
        // And: Verify queue statistics through immutable data structures
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    Map<String, Object> queueStats = given()
                            .auth().basic("admin", "admin123")
                            .when()
                            .get("/tasks/queue/stats")
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeReference<Map<String, Object>>() {});

                    assertThat(queueStats.get("totalEnqueued")).isNotNull();
                    assertThat(((Number) queueStats.get("totalEnqueued")).intValue()).isGreaterThanOrEqualTo(10);
                });
    }

    // Helper Methods

    private void setupTestAgents() {
        // Create test agents for various scenarios
        List<Agent> testAgents = List.of(
                Agent.builder()
                        .agentName("TestMarketAgent")
                        .agentType(AgentType.MARKET_ANALYSIS)
                        .status(AgentStatus.ACTIVE)
                        .maxConcurrentTasks(5)
                        .currentLoad(0)
                        .userId(1L)
                        .capabilities(Set.of(AgentCapability.ANALYSIS, AgentCapability.PREDICTION))
                        .lastHeartbeat(Instant.now())
                        .build(),
                
                Agent.builder()
                        .agentName("TestPortfolioAgent")
                        .agentType(AgentType.PORTFOLIO_MANAGEMENT)
                        .status(AgentStatus.ACTIVE)
                        .maxConcurrentTasks(3)
                        .currentLoad(0)
                        .userId(1L)
                        .capabilities(Set.of(AgentCapability.PORTFOLIO_ANALYSIS, AgentCapability.OPTIMIZATION))
                        .lastHeartbeat(Instant.now())
                        .build()
        );
        
        agentRepository.saveAll(testAgents);
    }

    private void setupTestCapabilities() {
        // Capabilities are enum-based, no setup needed
    }

    private void setupActiveSystem() {
        // Create active agents and tasks for monitoring tests
        createAndRegisterAgent("MonitoringAgent1", AgentType.MARKET_ANALYSIS,
                Set.of(AgentCapability.ANALYSIS));
        createAndRegisterAgent("MonitoringAgent2", AgentType.TRADING_EXECUTION,
                Set.of(AgentCapability.EXECUTION));
        
        // Submit some tasks for metrics
        Task monitoringTask = Task.builder()
                .taskName("MonitoringTestTask")
                .taskType(TaskType.MARKET_ANALYSIS)
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING)
                .requiredCapabilities(Set.of(AgentCapability.ANALYSIS))
                .taskData(Map.of("purpose", "monitoring"))
                .createdAt(Instant.now())
                .build();
        
        taskRepository.save(monitoringTask);
    }

    private Agent createAndRegisterAgent(String name, AgentType type, Set<AgentCapability> capabilities) {
        Agent agent = Agent.builder()
                .agentName(name)
                .agentType(type)
                .status(AgentStatus.ACTIVE)
                .maxConcurrentTasks(5)
                .currentLoad(0)
                .userId(1L)
                .capabilities(capabilities)
                .lastHeartbeat(Instant.now())
                .build();

        return agentRepository.save(agent);
    }

    private Set<AgentCapability> determineRequiredCapabilities(TaskType taskType) {
        return switch (taskType) {
            case MARKET_ANALYSIS, TECHNICAL_ANALYSIS -> Set.of(AgentCapability.ANALYSIS);
            case ORDER_EXECUTION -> Set.of(AgentCapability.EXECUTION);
            case ALERT_GENERATION -> Set.of(AgentCapability.COMMUNICATION);
            case PORTFOLIO_ANALYSIS -> Set.of(AgentCapability.PORTFOLIO_ANALYSIS);
            case RISK_ASSESSMENT -> Set.of(AgentCapability.RISK_ANALYSIS);
            default -> Set.of(AgentCapability.ANALYSIS);
        };
    }
}