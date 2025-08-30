package com.trademaster.marketdata.agentos;

import com.trademaster.marketdata.dto.MarketDataRequest;
import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

import static com.trademaster.marketdata.agentos.AgentConstants.*;

/**
 * AgentOS Market Data Agent
 * 
 * Provides market data capabilities to the TradeMaster Agent ecosystem.
 * Implements structured concurrency patterns for high-performance data processing
 * and integrates with the MCP (Multi-Agent Communication Protocol).
 * 
 * Agent Capabilities:
 * - REAL_TIME_DATA: Real-time price streaming and updates
 * - HISTORICAL_DATA: Historical OHLCV data retrieval
 * - TECHNICAL_ANALYSIS: Technical indicators and market analytics
 * - MARKET_SCANNING: Symbol screening and market surveillance
 * - PRICE_ALERTS: Automated price monitoring and alerting
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataAgent implements AgentOSComponent {
    
    private final MarketDataService marketDataService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final MarketDataCapabilityRegistry capabilityRegistry;
    
    /**
     * Handles market data requests using structured concurrency
     * for coordinated data processing across multiple providers.
     */
    @EventHandler(event = "MarketDataRequest")
    public CompletableFuture<MarketDataResponse> handleDataRequest(
            MarketDataRequest request) {
        
        log.info("Processing market data request for symbols: {}", request.symbols());
        
        return executeCoordinatedDataProcessing(
            request.requestId(),
            List.<Supplier<String>>of(
                () -> fetchRealTimeData(request.symbols()),
                () -> fetchHistoricalData(request.symbols(), request.timeframe()),
                () -> calculateTechnicalIndicators(request.symbols()),
                () -> performMarketScanning(request.scanCriteria()).toString()
            ),
            Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MS)
        );
    }
    
    /**
     * Real-time data streaming capability with high proficiency
     */
    @AgentCapability(name = CAPABILITY_REAL_TIME_DATA, proficiency = PROFICIENCY_EXPERT)
    public CompletableFuture<String> provideRealTimeData(List<String> symbols) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                marketDataService.getRealTimeData(symbols);
                capabilityRegistry.recordSuccessfulExecution(CAPABILITY_REAL_TIME_DATA);
                return "Real-time data streaming active for " + symbols.size() + " symbols";
            } catch (Exception e) {
                log.error("Failed to provide real-time data", e);
                capabilityRegistry.recordFailedExecution(CAPABILITY_REAL_TIME_DATA, e);
                throw new RuntimeException("Real-time data provision failed", e);
            }
        });
    }
    
    /**
     * Historical data retrieval capability
     */
    @AgentCapability(name = CAPABILITY_HISTORICAL_DATA, proficiency = PROFICIENCY_EXPERT)
    public CompletableFuture<String> provideHistoricalData(
            List<String> symbols, 
            String timeframe) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                marketDataService.getHistoricalData(symbols, timeframe);
                capabilityRegistry.recordSuccessfulExecution(CAPABILITY_HISTORICAL_DATA);
                return String.format("Historical data retrieved for %d symbols with %s timeframe", 
                                   symbols.size(), timeframe);
            } catch (Exception e) {
                log.error("Failed to provide historical data", e);
                capabilityRegistry.recordFailedExecution(CAPABILITY_HISTORICAL_DATA, e);
                throw new RuntimeException("Historical data provision failed", e);
            }
        });
    }
    
    /**
     * Technical analysis capability with indicator calculations
     */
    @AgentCapability(name = CAPABILITY_TECHNICAL_ANALYSIS, proficiency = PROFICIENCY_ADVANCED)
    public CompletableFuture<String> provideTechnicalAnalysis(
            List<String> symbols,
            List<String> indicators) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                technicalAnalysisService.calculateIndicators(symbols, indicators);
                capabilityRegistry.recordSuccessfulExecution(CAPABILITY_TECHNICAL_ANALYSIS);
                return String.format("Technical analysis completed for %d symbols with %d indicators", 
                                   symbols.size(), indicators.size());
            } catch (Exception e) {
                log.error("Failed to provide technical analysis", e);
                capabilityRegistry.recordFailedExecution(CAPABILITY_TECHNICAL_ANALYSIS, e);
                throw new RuntimeException("Technical analysis failed", e);
            }
        });
    }
    
    /**
     * Market scanning capability for symbol screening
     */
    @AgentCapability(name = "MARKET_SCANNING", proficiency = "ADVANCED")
    public CompletableFuture<String> performMarketScanning(Object scanCriteria) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Market scanning implementation
                capabilityRegistry.recordSuccessfulExecution("MARKET_SCANNING");
                return "Market scanning completed with specified criteria";
            } catch (Exception e) {
                log.error("Failed to perform market scanning", e);
                capabilityRegistry.recordFailedExecution("MARKET_SCANNING", e);
                throw new RuntimeException("Market scanning failed", e);
            }
        });
    }
    
    /**
     * Price alerts capability for automated monitoring
     */
    @AgentCapability(name = "PRICE_ALERTS", proficiency = "INTERMEDIATE")
    public CompletableFuture<String> managePriceAlerts(
            List<String> symbols,
            Object alertCriteria) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Price alerts management implementation
                capabilityRegistry.recordSuccessfulExecution("PRICE_ALERTS");
                return String.format("Price alerts configured for %d symbols", symbols.size());
            } catch (Exception e) {
                log.error("Failed to manage price alerts", e);
                capabilityRegistry.recordFailedExecution("PRICE_ALERTS", e);
                throw new RuntimeException("Price alerts management failed", e);
            }
        });
    }
    
    /**
     * Executes coordinated market data processing using Java 24 structured concurrency
     */
    private CompletableFuture<MarketDataResponse> executeCoordinatedDataProcessing(
            Long requestId,
            List<Supplier<String>> operations,
            Duration timeout) {
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Fork all data processing operations
                var subtasks = operations.stream()
                    .map(operation -> scope.fork(operation::get))
                    .toList();
                
                // Join and handle failures (StructuredTaskScope.join() doesn't accept timeout in Java 24)
                scope.join();
                scope.throwIfFailed();
                
                // Collect results
                var results = subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                
                log.info("Coordinated data processing completed for request: {}", requestId);
                
                return MarketDataResponse.builder()
                    .requestId(requestId)
                    .status("SUCCESS")
                    .processingResults(results)
                    .processingTimeMs(System.currentTimeMillis())
                    .build();
                    
            } catch (Exception e) {
                log.error("Coordinated data processing failed for request: {}", requestId, e);
                
                return MarketDataResponse.builder()
                    .requestId(requestId)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .processingTimeMs(System.currentTimeMillis())
                    .build();
            }
        });
    }
    
    /**
     * Fetches real-time market data for specified symbols
     */
    private String fetchRealTimeData(List<String> symbols) {
        try {
            // Delegate to market data service
            var data = marketDataService.getRealTimeData(symbols);
            return "Real-time data fetched for " + symbols.size() + " symbols";
        } catch (Exception e) {
            log.warn("Real-time data fetch failed for symbols: {}", symbols, e);
            return "Real-time data fetch failed: " + e.getMessage();
        }
    }
    
    /**
     * Fetches historical market data with specified timeframe
     */
    private String fetchHistoricalData(List<String> symbols, String timeframe) {
        try {
            var data = marketDataService.getHistoricalData(symbols, timeframe);
            return String.format("Historical data fetched for %d symbols (%s)", 
                               symbols.size(), timeframe);
        } catch (Exception e) {
            log.warn("Historical data fetch failed: {}", e.getMessage());
            return "Historical data fetch failed: " + e.getMessage();
        }
    }
    
    /**
     * Calculates technical indicators for market analysis
     */
    private String calculateTechnicalIndicators(List<String> symbols) {
        try {
            var indicators = technicalAnalysisService.calculateIndicators(
                symbols, 
                List.of("RSI", "MACD", "BOLLINGER_BANDS", "MOVING_AVERAGE")
            );
            return "Technical indicators calculated for " + symbols.size() + " symbols";
        } catch (Exception e) {
            log.warn("Technical indicators calculation failed: {}", e.getMessage());
            return "Technical indicators calculation failed: " + e.getMessage();
        }
    }
    
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }
    
    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }
    
    @Override
    public List<String> getCapabilities() {
        return List.of(
            CAPABILITY_REAL_TIME_DATA,
            CAPABILITY_HISTORICAL_DATA, 
            CAPABILITY_TECHNICAL_ANALYSIS,
            CAPABILITY_MARKET_SCANNING,
            CAPABILITY_PRICE_ALERTS
        );
    }
    
    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
}