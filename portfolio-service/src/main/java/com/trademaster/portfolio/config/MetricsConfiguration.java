package com.trademaster.portfolio.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prometheus Metrics Configuration for Portfolio Service
 * 
 * Comprehensive monitoring setup for Grafana dashboards without performance impact.
 * Uses Virtual Thread-optimized metrics collection with structured logging.
 * 
 * Key Metrics Categories:
 * - Business Metrics: Portfolio values, P&L, positions
 * - Performance Metrics: Response times, throughput, errors
 * - System Metrics: Virtual Thread usage, memory, database
 * - Risk Metrics: Concentration, volatility, exposure
 * 
 * Performance Targets:
 * - Metrics collection: <1ms overhead
 * - Zero blocking operations in Virtual Threads
 * - Minimal memory footprint
 * - Real-time dashboard updates
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@Slf4j
public class MetricsConfiguration {
    
    /**
     * Portfolio Business Metrics
     */
    @Bean
    public PortfolioMetrics portfolioMetrics(MeterRegistry meterRegistry) {
        return new PortfolioMetrics(meterRegistry);
    }
    
    /**
     * Position Tracking Metrics
     */
    @Bean
    public PositionMetrics positionMetrics(MeterRegistry meterRegistry) {
        return new PositionMetrics(meterRegistry);
    }
    
    /**
     * Performance and System Metrics
     */
    @Bean
    public PerformanceMetrics performanceMetrics(MeterRegistry meterRegistry) {
        return new PerformanceMetrics(meterRegistry);
    }
    
    /**
     * Risk Management Metrics
     */
    @Bean
    public RiskMetrics riskMetrics(MeterRegistry meterRegistry) {
        return new RiskMetrics(meterRegistry);
    }
}

/**
 * Portfolio Business Metrics Collector
 *
 * NOTE: PortfolioMetrics has been extracted to its own file for better maintainability.
 * See: com.trademaster.portfolio.config.PortfolioMetrics
 */


/**
 * Position Tracking Metrics Collector
 */
@Component
@RequiredArgsConstructor
@Slf4j
class PositionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Position Counters
    private final Counter positionsOpened;
    private final Counter positionsClosed;
    private final Counter positionUpdates;
    private final Counter priceUpdates;
    
    // Position Gauges
    private final AtomicLong totalPositionsCount = new AtomicLong(0);
    private final AtomicLong profitablePositionsCount = new AtomicLong(0);
    private final AtomicLong losingPositionsCount = new AtomicLong(0);
    
    // Position Timers
    private final Timer positionUpdateTimer;
    private final Timer priceUpdateTimer;
    
    public PositionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.positionsOpened = Counter.builder("position.opened.total")
            .description("Total number of positions opened")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.positionsClosed = Counter.builder("position.closed.total")
            .description("Total number of positions closed")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.positionUpdates = Counter.builder("position.updates.total")
            .description("Total number of position updates")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.priceUpdates = Counter.builder("position.price.updates.total")
            .description("Total number of position price updates")
            .tag("service", "portfolio")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("position.total.count", this, PositionMetrics::getTotalPositionsCount)
            .description("Total number of open positions")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        Gauge.builder("position.profitable.count", this, PositionMetrics::getProfitablePositionsCount)
            .description("Number of profitable positions")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        Gauge.builder("position.losing.count", this, PositionMetrics::getLosingPositionsCount)
            .description("Number of losing positions")
            .tag("service", "portfolio")
            .register(meterRegistry);
        
        // Initialize timers
        this.positionUpdateTimer = Timer.builder("position.update.duration")
            .description("Time taken to update position")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.priceUpdateTimer = Timer.builder("position.price.update.duration")
            .description("Time taken to update position price")
            .tag("service", "portfolio")
            .register(meterRegistry);
    }
    
    // Increment methods
    public void incrementPositionsOpened(String symbol, String exchange) {
        positionsOpened.increment();
        log.debug("Position opened for symbol: {} on exchange: {}", symbol, exchange);
    }
    
    public void incrementPositionsClosed(String symbol, String exchange, String reason) {
        positionsClosed.increment();
        log.info("Position closed for symbol: {} on exchange: {} - reason: {}", symbol, exchange, reason);
    }
    
    public void incrementPositionUpdates() {
        positionUpdates.increment();
    }
    
    public void incrementPriceUpdates(String symbol) {
        priceUpdates.increment();
        log.debug("Price update for symbol: {}", symbol);
    }
    
    // Update methods for gauges
    public void updatePositionCounts(long total, long profitable, long losing) {
        totalPositionsCount.set(total);
        profitablePositionsCount.set(profitable);
        losingPositionsCount.set(losing);
    }
    
    // Timer methods
    public Timer.Sample startPositionUpdateTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordPositionUpdateTime(Timer.Sample sample) {
        sample.stop(positionUpdateTimer);
    }
    
    public Timer.Sample startPriceUpdateTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordPriceUpdateTime(Timer.Sample sample) {
        sample.stop(priceUpdateTimer);
    }
    
    // Getter methods for gauges
    private double getTotalPositionsCount() {
        return totalPositionsCount.get();
    }
    
    private double getProfitablePositionsCount() {
        return profitablePositionsCount.get();
    }
    
    private double getLosingPositionsCount() {
        return losingPositionsCount.get();
    }
}

/**
 * Performance and System Metrics Collector
 */
@Component
@RequiredArgsConstructor
@Slf4j
class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Performance Counters
    private final Counter apiRequests;
    private final Counter apiErrors;
    private final Counter databaseQueries;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    
    // System Gauges
    private final AtomicLong virtualThreadsActive = new AtomicLong(0);
    private final AtomicLong databaseConnectionsActive = new AtomicLong(0);
    
    // Performance Timers
    private final Timer apiResponseTimer;
    private final Timer databaseQueryTimer;
    private final Timer cacheAccessTimer;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.apiRequests = Counter.builder("api.requests.total")
            .description("Total number of API requests")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.apiErrors = Counter.builder("api.errors.total")
            .description("Total number of API errors")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.databaseQueries = Counter.builder("database.queries.total")
            .description("Total number of database queries")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.cacheHits = Counter.builder("cache.hits.total")
            .description("Total number of cache hits")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.cacheMisses = Counter.builder("cache.misses.total")
            .description("Total number of cache misses")
            .tag("service", "portfolio")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("virtual.threads.active", this, PerformanceMetrics::getVirtualThreadsActive)
            .description("Number of active Virtual Threads")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        Gauge.builder("database.connections.active", this, PerformanceMetrics::getDatabaseConnectionsActive)
            .description("Number of active database connections")
            .tag("service", "portfolio")
            .register(meterRegistry);
        
        // Initialize timers
        this.apiResponseTimer = Timer.builder("api.response.duration")
            .description("API response time")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.databaseQueryTimer = Timer.builder("database.query.duration")
            .description("Database query execution time")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.cacheAccessTimer = Timer.builder("cache.access.duration")
            .description("Cache access time")
            .tag("service", "portfolio")
            .register(meterRegistry);
    }
    
    // Counter increment methods
    public void incrementApiRequests(String endpoint, String method) {
        apiRequests.increment();
        log.debug("API request - endpoint: {}, method: {}", endpoint, method);
    }
    
    public void incrementApiErrors(String endpoint, String errorType) {
        apiErrors.increment();
        log.warn("API error on endpoint: {} - error type: {}", endpoint, errorType);
    }
    
    public void incrementDatabaseQueries(String queryType) {
        databaseQueries.increment();
        log.debug("Database query executed - type: {}", queryType);
    }
    
    public void incrementCacheHits(String cacheType) {
        cacheHits.increment();
        log.debug("Cache hit - type: {}", cacheType);
    }
    
    public void incrementCacheMisses(String cacheType) {
        cacheMisses.increment();
        log.debug("Cache miss - type: {}", cacheType);
    }
    
    // Update methods for gauges
    public void updateVirtualThreadsActive(long count) {
        virtualThreadsActive.set(count);
    }
    
    public void updateDatabaseConnectionsActive(long count) {
        databaseConnectionsActive.set(count);
    }
    
    // Timer methods
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordApiTime(Timer.Sample sample, String endpoint) {
        sample.stop(Timer.builder("api.response.duration")
            .tag("endpoint", endpoint)
            .register(meterRegistry));
    }
    
    public Timer.Sample startDatabaseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDatabaseTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("database.query.duration")
            .tag("operation", operation)
            .register(meterRegistry));
    }
    
    // Getter methods for gauges
    private double getVirtualThreadsActive() {
        return virtualThreadsActive.get();
    }
    
    private double getDatabaseConnectionsActive() {
        return databaseConnectionsActive.get();
    }
}

/**
 * Risk Management Metrics Collector
 */
@Component
@RequiredArgsConstructor
@Slf4j
class RiskMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Risk Counters
    private final Counter riskViolations;
    private final Counter marginCalls;
    private final Counter concentrationAlerts;
    
    // Risk Gauges
    private final AtomicReference<BigDecimal> portfolioVaR = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> maxConcentration = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalExposure = new AtomicReference<>(BigDecimal.ZERO);
    
    public RiskMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.riskViolations = Counter.builder("risk.violations.total")
            .description("Total number of risk violations")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.marginCalls = Counter.builder("margin.calls.total")
            .description("Total number of margin calls")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        this.concentrationAlerts = Counter.builder("concentration.alerts.total")
            .description("Total number of concentration alerts")
            .tag("service", "portfolio")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("portfolio.var", this, RiskMetrics::getPortfolioVaR)
            .description("Portfolio Value at Risk")
            .tag("service", "portfolio")
            .tag("confidence", "95")
            .register(meterRegistry);
            
        Gauge.builder("portfolio.concentration.max", this, RiskMetrics::getMaxConcentration)
            .description("Maximum position concentration")
            .tag("service", "portfolio")
            .register(meterRegistry);
            
        Gauge.builder("portfolio.exposure.total", this, RiskMetrics::getTotalExposure)
            .description("Total portfolio exposure")
            .tag("service", "portfolio")
            .register(meterRegistry);
    }
    
    // Increment methods
    public void incrementRiskViolations(String violationType, String severity) {
        riskViolations.increment();
        log.warn("Risk violation detected - type: {}, severity: {}", violationType, severity);
    }
    
    public void incrementMarginCalls(String portfolioId) {
        marginCalls.increment();
        log.error("Margin call issued for portfolio: {}", portfolioId);
    }
    
    public void incrementConcentrationAlerts(String symbol, String portfolioId) {
        concentrationAlerts.increment();
        log.warn("Concentration alert for symbol: {} in portfolio: {}", symbol, portfolioId);
    }
    
    // Update methods for gauges
    public void updatePortfolioVaR(BigDecimal var) {
        portfolioVaR.set(var);
    }
    
    public void updateMaxConcentration(BigDecimal concentration) {
        maxConcentration.set(concentration);
    }
    
    public void updateTotalExposure(BigDecimal exposure) {
        totalExposure.set(exposure);
    }
    
    // Getter methods for gauges
    private double getPortfolioVaR() {
        return portfolioVaR.get().doubleValue();
    }
    
    private double getMaxConcentration() {
        return maxConcentration.get().doubleValue();
    }
    
    private double getTotalExposure() {
        return totalExposure.get().doubleValue();
    }
}