# TradeMaster MCP Protocol Specification

## Overview
Model Context Protocol (MCP) implementation for TradeMaster Agent OS, providing standardized communication between AI agents and TradeMaster services. This specification extends the base MCP protocol with trading-specific capabilities and integrations.

## MCP Protocol Foundation

### Base MCP Operations
The TradeMaster implementation supports all standard MCP operations while adding trading-specific extensions:

```typescript
interface MCPProtocol {
  // Standard MCP Core
  initialize(params: InitializeParams): Promise<InitializeResult>;
  listResources(): Promise<ListResourcesResult>;
  readResource(params: ReadResourceParams): Promise<ReadResourceResult>;
  listTools(): Promise<ListToolsResult>;
  callTool(params: CallToolParams): Promise<CallToolResult>;
  
  // TradeMaster Extensions
  listBrokers(): Promise<ListBrokersResult>;
  getMarketData(params: MarketDataParams): Promise<MarketDataResult>;
  executeTradeOrder(params: TradeOrderParams): Promise<TradeOrderResult>;
  getPortfolioData(params: PortfolioParams): Promise<PortfolioResult>;
}
```

## TradeMaster MCP Extensions

### 1. Trading Resources

#### Broker Resources
```typescript
interface BrokerResource {
  uri: `broker://${string}`;
  name: string;
  description: string;
  mimeType: 'application/json';
  metadata: {
    brokerId: string;
    brokerName: string;
    status: 'connected' | 'disconnected' | 'error';
    capabilities: BrokerCapability[];
    supportedAssets: AssetType[];
  };
}
```

#### Market Data Resources
```typescript
interface MarketDataResource {
  uri: `market://${string}/${string}`; // market://symbol/exchange
  name: string;
  description: string;
  mimeType: 'application/json';
  metadata: {
    symbol: string;
    exchange: string;
    assetType: AssetType;
    lastUpdate: string;
    dataProvider: string;
  };
}
```

#### Portfolio Resources
```typescript
interface PortfolioResource {
  uri: `portfolio://${string}`;
  name: string;
  description: string;
  mimeType: 'application/json';
  metadata: {
    userId: string;
    portfolioId: string;
    totalValue: number;
    lastUpdate: string;
    currency: string;
  };
}
```

### 2. Trading Tools

#### Market Analysis Tool
```typescript
interface MarketAnalysisTool {
  name: 'analyze_market';
  description: 'Analyze market data and generate trading insights';
  inputSchema: {
    type: 'object';
    properties: {
      symbols: { type: 'array'; items: { type: 'string' } };
      timeframe: { type: 'string'; enum: ['1m', '5m', '15m', '1h', '1d'] };
      indicators: { type: 'array'; items: { type: 'string' } };
      analysis_type: { type: 'string'; enum: ['technical', 'fundamental', 'sentiment'] };
    };
    required: ['symbols', 'timeframe'];
  };
}
```

#### Order Execution Tool
```typescript
interface OrderExecutionTool {
  name: 'execute_order';
  description: 'Execute trading orders across connected brokers';
  inputSchema: {
    type: 'object';
    properties: {
      symbol: { type: 'string' };
      side: { type: 'string'; enum: ['buy', 'sell'] };
      quantity: { type: 'number'; minimum: 0 };
      orderType: { type: 'string'; enum: ['market', 'limit', 'stop'] };
      price?: { type: 'number'; minimum: 0 };
      brokerId?: { type: 'string' };
      timeInForce: { type: 'string'; enum: ['GTC', 'IOC', 'FOK', 'DAY'] };
    };
    required: ['symbol', 'side', 'quantity', 'orderType'];
  };
}
```

#### Portfolio Optimization Tool
```typescript
interface PortfolioOptimizationTool {
  name: 'optimize_portfolio';
  description: 'Optimize portfolio allocation based on specified criteria';
  inputSchema: {
    type: 'object';
    properties: {
      portfolioId: { type: 'string' };
      objective: { type: 'string'; enum: ['maximize_return', 'minimize_risk', 'maximize_sharpe'] };
      constraints: {
        type: 'object';
        properties: {
          maxPositionSize: { type: 'number'; minimum: 0; maximum: 1 };
          minCashRatio: { type: 'number'; minimum: 0; maximum: 1 };
          excludedAssets: { type: 'array'; items: { type: 'string' } };
        };
      };
      riskTolerance: { type: 'string'; enum: ['conservative', 'moderate', 'aggressive'] };
    };
    required: ['portfolioId', 'objective'];
  };
}
```

#### Risk Assessment Tool
```typescript
interface RiskAssessmentTool {
  name: 'assess_risk';
  description: 'Perform comprehensive risk analysis on portfolio or positions';
  inputSchema: {
    type: 'object';
    properties: {
      portfolioId: { type: 'string' };
      riskMetrics: { 
        type: 'array'; 
        items: { type: 'string'; enum: ['var', 'cvar', 'sharpe', 'sortino', 'max_drawdown'] }
      };
      confidenceLevel: { type: 'number'; minimum: 0.9; maximum: 0.99 };
      timeHorizon: { type: 'number'; minimum: 1; maximum: 252 };
    };
    required: ['portfolioId', 'riskMetrics'];
  };
}
```

## MCP Server Implementation

### Core MCP Server
```java
@Component
public class TradeMasterMCPServer implements MCPServer {
    
    private final BrokerService brokerService;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final TradingService tradingService;
    
    @Override
    public InitializeResult initialize(InitializeParams params) {
        return InitializeResult.builder()
            .protocolVersion("1.0.0")
            .serverInfo(ServerInfo.builder()
                .name("TradeMaster MCP Server")
                .version("1.0.0")
                .build())
            .capabilities(ServerCapabilities.builder()
                .resources(true)
                .tools(true)
                .prompts(false)
                .logging(true)
                .build())
            .build();
    }
    
    @Override
    public ListResourcesResult listResources() {
        List<Resource> resources = new ArrayList<>();
        
        // Add broker resources
        brokerService.getAllBrokers().forEach(broker -> 
            resources.add(createBrokerResource(broker))
        );
        
        // Add market data resources
        marketDataService.getAvailableSymbols().forEach(symbol ->
            resources.add(createMarketDataResource(symbol))
        );
        
        // Add portfolio resources  
        portfolioService.getAllPortfolios().forEach(portfolio ->
            resources.add(createPortfolioResource(portfolio))
        );
        
        return ListResourcesResult.builder()
            .resources(resources)
            .build();
    }
    
    @Override
    public ListToolsResult listTools() {
        return ListToolsResult.builder()
            .tools(Arrays.asList(
                createMarketAnalysisTool(),
                createOrderExecutionTool(),
                createPortfolioOptimizationTool(),
                createRiskAssessmentTool()
            ))
            .build();
    }
    
    @Override
    public CallToolResult callTool(CallToolParams params) {
        switch (params.getName()) {
            case "analyze_market":
                return handleMarketAnalysis(params);
            case "execute_order":
                return handleOrderExecution(params);
            case "optimize_portfolio":
                return handlePortfolioOptimization(params);
            case "assess_risk":
                return handleRiskAssessment(params);
            default:
                throw new UnsupportedOperationException("Tool not supported: " + params.getName());
        }
    }
}
```

### Tool Implementation

#### Market Analysis Tool Handler
```java
private CallToolResult handleMarketAnalysis(CallToolParams params) {
    MarketAnalysisRequest request = objectMapper.convertValue(
        params.getArguments(), 
        MarketAnalysisRequest.class
    );
    
    List<MarketAnalysis> analyses = request.getSymbols().parallelStream()
        .map(symbol -> marketDataService.analyzeSymbol(
            symbol, 
            request.getTimeframe(), 
            request.getIndicators(),
            request.getAnalysisType()
        ))
        .collect(Collectors.toList());
    
    return CallToolResult.builder()
        .content(Arrays.asList(
            TextContent.builder()
                .type("text")
                .text(formatMarketAnalysis(analyses))
                .build()
        ))
        .isError(false)
        .build();
}
```

#### Order Execution Tool Handler
```java
private CallToolResult handleOrderExecution(CallToolParams params) {
    OrderRequest orderRequest = objectMapper.convertValue(
        params.getArguments(),
        OrderRequest.class
    );
    
    try {
        // Validate order parameters
        orderValidationService.validate(orderRequest);
        
        // Route to optimal broker
        String brokerId = orderRequest.getBrokerId() != null 
            ? orderRequest.getBrokerId()
            : brokerRoutingService.selectOptimalBroker(orderRequest);
        
        // Execute order
        OrderExecutionResult result = tradingService.executeOrder(orderRequest, brokerId);
        
        return CallToolResult.builder()
            .content(Arrays.asList(
                TextContent.builder()
                    .type("text")
                    .text(formatOrderResult(result))
                    .build()
            ))
            .isError(false)
            .build();
            
    } catch (OrderValidationException | OrderExecutionException e) {
        return CallToolResult.builder()
            .content(Arrays.asList(
                TextContent.builder()
                    .type("text")
                    .text("Order execution failed: " + e.getMessage())
                    .build()
            ))
            .isError(true)
            .build();
    }
}
```

## Agent MCP Client Implementation

### MCP Client Interface
```typescript
export class TradeMasterMCPClient {
  private transport: Transport;
  private client: Client;
  
  constructor(serverUrl: string) {
    this.transport = new WebSocketTransport(serverUrl);
    this.client = new Client({ name: "TradeMaster Agent", version: "1.0.0" }, {
      capabilities: {
        resources: { subscribe: true },
        tools: { listChanged: true }
      }
    });
  }
  
  async initialize(): Promise<void> {
    await this.client.connect(this.transport);
  }
  
  // Resource operations
  async getBrokerData(brokerId: string): Promise<BrokerData> {
    const result = await this.client.readResource({
      uri: `broker://${brokerId}`
    });
    return JSON.parse(result.contents[0].text);
  }
  
  async getMarketData(symbol: string, exchange: string): Promise<MarketData> {
    const result = await this.client.readResource({
      uri: `market://${symbol}/${exchange}`
    });
    return JSON.parse(result.contents[0].text);
  }
  
  async getPortfolioData(portfolioId: string): Promise<PortfolioData> {
    const result = await this.client.readResource({
      uri: `portfolio://${portfolioId}`
    });
    return JSON.parse(result.contents[0].text);
  }
  
  // Tool operations
  async analyzeMarket(params: MarketAnalysisParams): Promise<MarketAnalysis[]> {
    const result = await this.client.callTool({
      name: "analyze_market",
      arguments: params
    });
    return this.parseToolResult<MarketAnalysis[]>(result);
  }
  
  async executeOrder(params: OrderParams): Promise<OrderResult> {
    const result = await this.client.callTool({
      name: "execute_order",
      arguments: params
    });
    return this.parseToolResult<OrderResult>(result);
  }
  
  async optimizePortfolio(params: OptimizationParams): Promise<OptimizationResult> {
    const result = await this.client.callTool({
      name: "optimize_portfolio", 
      arguments: params
    });
    return this.parseToolResult<OptimizationResult>(result);
  }
  
  async assessRisk(params: RiskAssessmentParams): Promise<RiskAssessment> {
    const result = await this.client.callTool({
      name: "assess_risk",
      arguments: params
    });
    return this.parseToolResult<RiskAssessment>(result);
  }
  
  private parseToolResult<T>(result: CallToolResult): T {
    if (result.isError) {
      throw new Error(`Tool execution failed: ${result.content[0].text}`);
    }
    return JSON.parse(result.content[0].text);
  }
}
```

### Agent Integration Example
```typescript
export class MarketAnalysisAgent extends BaseAgent {
  private mcpClient: TradeMasterMCPClient;
  
  constructor(config: AgentConfig) {
    super(config);
    this.mcpClient = new TradeMasterMCPClient(config.mcpServerUrl);
  }
  
  async analyzeMarket(symbols: string[]): Promise<MarketAnalysis[]> {
    try {
      const analyses = await this.mcpClient.analyzeMarket({
        symbols,
        timeframe: '1h',
        indicators: ['RSI', 'MACD', 'SMA'],
        analysis_type: 'technical'
      });
      
      return analyses;
    } catch (error) {
      this.logger.error('Market analysis failed', error);
      throw error;
    }
  }
  
  async generateTradingSignals(symbols: string[]): Promise<TradingSignal[]> {
    const analyses = await this.analyzeMarket(symbols);
    
    return analyses.map(analysis => ({
      symbol: analysis.symbol,
      signal: this.interpretAnalysis(analysis),
      confidence: this.calculateConfidence(analysis),
      timestamp: new Date()
    }));
  }
}
```

## Error Handling & Resilience

### Error Types
```typescript
enum MCPErrorType {
  INVALID_REQUEST = -32600,
  METHOD_NOT_FOUND = -32601,
  INVALID_PARAMS = -32602,
  INTERNAL_ERROR = -32603,
  BROKER_UNAVAILABLE = -40001,
  MARKET_DATA_ERROR = -40002,
  ORDER_REJECTED = -40003,
  INSUFFICIENT_FUNDS = -40004
}
```

### Retry Logic
```java
@Retryable(value = {MCPException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public CallToolResult callToolWithRetry(CallToolParams params) {
    return mcpServer.callTool(params);
}

@Recover
public CallToolResult recover(MCPException e, CallToolParams params) {
    logger.error("Failed to execute MCP tool after retries: {}", params.getName(), e);
    return CallToolResult.builder()
        .isError(true)
        .content(Arrays.asList(TextContent.builder()
            .type("text")
            .text("Tool execution failed after retries: " + e.getMessage())
            .build()))
        .build();
}
```

## Performance Optimization

### Connection Pooling
```java
@Configuration
public class MCPConnectionConfig {
    
    @Bean
    public MCPConnectionPool mcpConnectionPool() {
        return MCPConnectionPool.builder()
            .maxConnections(100)
            .minIdleConnections(10)
            .maxIdleTime(Duration.ofMinutes(30))
            .connectionTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

### Caching Strategy
```java
@Service
public class MCPCacheService {
    
    @Cacheable(value = "market-data", key = "#symbol + '_' + #exchange")
    public MarketData getCachedMarketData(String symbol, String exchange) {
        return mcpClient.getMarketData(symbol, exchange);
    }
    
    @CacheEvict(value = "market-data", key = "#symbol + '_' + #exchange")
    public void evictMarketDataCache(String symbol, String exchange) {
        // Cache eviction logic
    }
}
```

## Security Considerations

### Authentication
```java
@Component
public class MCPAuthenticationProvider {
    
    public boolean authenticate(MCPRequest request) {
        String token = request.getHeaders().get("Authorization");
        return jwtTokenValidator.validate(token);
    }
    
    public boolean authorize(MCPRequest request, String operation) {
        User user = getCurrentUser(request);
        return permissionService.hasPermission(user, operation);
    }
}
```

### Data Encryption
```java
@Service
public class MCPDataEncryption {
    
    public String encryptSensitiveData(Object data) {
        return aesEncryption.encrypt(objectMapper.writeValueAsString(data));
    }
    
    public <T> T decryptSensitiveData(String encryptedData, Class<T> type) {
        String decrypted = aesEncryption.decrypt(encryptedData);
        return objectMapper.readValue(decrypted, type);
    }
}
```

## Monitoring & Observability

### Metrics Collection
```java
@Component
public class MCPMetrics {
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onToolCall(MCPToolCallEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("mcp.tool.call.duration")
            .tag("tool", event.getToolName())
            .tag("agent", event.getAgentId())
            .register(meterRegistry));
    }
}
```

## Testing Strategy

### Integration Tests
```java
@SpringBootTest
@Testcontainers
class MCPIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("trademaster_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void testMarketAnalysisTool() {
        // Test market analysis tool functionality
        CallToolParams params = CallToolParams.builder()
            .name("analyze_market")
            .arguments(Map.of(
                "symbols", Arrays.asList("AAPL", "GOOGL"),
                "timeframe", "1h",
                "analysis_type", "technical"
            ))
            .build();
            
        CallToolResult result = mcpServer.callTool(params);
        
        assertFalse(result.isError());
        assertNotNull(result.getContent());
    }
}
```

This specification provides comprehensive documentation for implementing MCP protocol support in TradeMaster Agent OS, enabling standardized communication between AI agents and trading services.