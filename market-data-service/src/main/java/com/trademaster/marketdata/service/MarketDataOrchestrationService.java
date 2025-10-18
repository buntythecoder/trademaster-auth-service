package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Orchestration Service (Facade Pattern)
 *
 * Single Responsibility: Coordinate specialized market data services
 * Following Rule #2 (SRP) and Rule #4 (Facade Design Pattern)
 *
 * This service provides a unified interface for controllers while delegating
 * to specialized services. Each service handles one concern:
 * - MarketDataQueryService: Read operations
 * - MarketDataWriteService: Write operations
 * - DataQualityService: Quality monitoring
 * - AgentOSMarketDataService: AgentOS integration
 *
 * Benefits:
 * - Single entry point for controllers (simplified dependencies)
 * - Clean separation of concerns (SRP compliance)
 * - Easy to test and maintain
 * - Future-proof for service composition and orchestration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataOrchestrationService {

    // Specialized services (Rule #2: Dependency Inversion)
    private final MarketDataQueryService queryService;
    private final MarketDataWriteService writeService;
    private final DataQualityService qualityService;
    private final AgentOSMarketDataService agentOSService;

    // Query Operations (delegate to MarketDataQueryService)

    /**
     * Get current price for a symbol
     */
    public CompletableFuture<Optional<MarketDataPoint>> getCurrentPrice(String symbol, String exchange) {
        return queryService.getCurrentPrice(symbol, exchange);
    }

    /**
     * Get historical OHLC data
     */
    public CompletableFuture<List<MarketDataPoint>> getHistoricalData(String symbol, String exchange,
            Instant from, Instant to, String interval) {
        return queryService.getHistoricalData(symbol, exchange, from, to, interval);
    }

    /**
     * Get bulk price data for multiple symbols
     */
    public CompletableFuture<Map<String, MarketDataPoint>> getBulkPriceData(List<String> symbols, String exchange) {
        return queryService.getBulkPriceData(symbols, exchange);
    }

    /**
     * Get active symbols for an exchange
     */
    public CompletableFuture<List<String>> getActiveSymbols(String exchange, int minutes) {
        return queryService.getActiveSymbols(exchange, minutes);
    }

    // Write Operations (delegate to MarketDataWriteService)

    /**
     * Write market data point
     */
    public CompletableFuture<Boolean> writeMarketData(MarketDataPoint dataPoint) {
        return writeService.writeMarketData(dataPoint);
    }

    /**
     * Batch write market data points
     */
    public CompletableFuture<MarketDataWriteService.BatchWriteResult> batchWriteMarketData(List<MarketDataPoint> dataPoints) {
        return writeService.batchWriteMarketData(dataPoints);
    }

    // Quality Monitoring Operations (delegate to DataQualityService)

    /**
     * Generate data quality report
     */
    public CompletableFuture<DataQualityService.DataQualityReport> generateQualityReport(
            String symbol, String exchange, int hours) {
        return qualityService.generateQualityReport(symbol, exchange, hours);
    }

    // AgentOS Integration Operations (delegate to AgentOSMarketDataService)

    /**
     * Get real-time data for AgentOS agents
     */
    public Object getRealTimeData(List<String> symbols) {
        return agentOSService.getRealTimeData(symbols);
    }

    /**
     * Get historical data for AgentOS agents (different signature from standard getHistoricalData)
     */
    public Object getHistoricalDataForAgentOS(List<String> symbols, String timeframe) {
        return agentOSService.getHistoricalDataForAgentOS(symbols, timeframe);
    }

    /**
     * Subscribe to real-time updates
     */
    public Object subscribeToRealTimeUpdates(List<String> symbols, Integer updateFrequencyMs,
            Map<String, Object> callbackConfig) {
        return agentOSService.subscribeToRealTimeUpdates(symbols, updateFrequencyMs, callbackConfig);
    }

    /**
     * Create price alert
     */
    public Object createPriceAlert(Map<String, Object> alertConfig) {
        return agentOSService.createPriceAlert(alertConfig);
    }

    /**
     * Update price alert
     */
    public Object updatePriceAlert(Map<String, Object> alertConfig) {
        return agentOSService.updatePriceAlert(alertConfig);
    }

    /**
     * Delete price alert
     */
    public Object deletePriceAlert(Map<String, Object> alertConfig) {
        return agentOSService.deletePriceAlert(alertConfig);
    }

    /**
     * List price alerts
     */
    public Object listPriceAlerts(Map<String, Object> criteria) {
        return agentOSService.listPriceAlerts(criteria);
    }
}
