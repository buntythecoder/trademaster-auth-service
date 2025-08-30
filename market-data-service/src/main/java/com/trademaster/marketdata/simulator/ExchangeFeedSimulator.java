package com.trademaster.marketdata.simulator;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Exchange Feed Simulator for NSE/BSE Development and Testing
 * 
 * Features:
 * - Realistic market data simulation with volatility
 * - Configurable symbol universe and trading patterns
 * - Market hours and session management
 * - Volume and price movement algorithms
 * - Order book depth simulation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trademaster.simulator.enabled", havingValue = "true", matchIfMissing = false)
public class ExchangeFeedSimulator {

    private final MarketDataService marketDataService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Simulation control
    private final AtomicBoolean simulationActive = new AtomicBoolean(false);
    private final AtomicLong tickCounter = new AtomicLong(0);
    private ScheduledExecutorService simulatorExecutor;
    
    // Market simulation data
    private final Map<String, SimulatedSymbol> nseSymbols = new ConcurrentHashMap<>();
    private final Map<String, SimulatedSymbol> bseSymbols = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    // Market session state
    private volatile boolean marketOpen = false;
    private volatile MarketSession currentSession = MarketSession.CLOSED;
    
    @PostConstruct
    public void initializeSimulator() {
        log.info("Initializing Exchange Feed Simulator");
        
        // Initialize NSE symbols
        initializeNSESymbols();
        
        // Initialize BSE symbols
        initializeBSESymbols();
        
        // Create scheduled virtual thread executor for high-frequency simulation
        simulatorExecutor = Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory());
        
        log.info("Exchange Feed Simulator initialized with {} NSE and {} BSE symbols",
            nseSymbols.size(), bseSymbols.size());
    }
    
    /**
     * Start market data simulation
     */
    @Scheduled(cron = "0 15 9 * * MON-FRI") // 9:15 AM on weekdays
    public void startMarketSession() {
        if (!simulationActive.get()) {
            log.info("Starting market session simulation");
            simulationActive.set(true);
            marketOpen = true;
            currentSession = MarketSession.REGULAR;
            
            startHighFrequencySimulation();
        }
    }
    
    /**
     * End market data simulation
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI") // 3:30 PM on weekdays
    public void endMarketSession() {
        if (simulationActive.get()) {
            log.info("Ending market session simulation");
            simulationActive.set(false);
            marketOpen = false;
            currentSession = MarketSession.CLOSED;
        }
    }
    
    /**
     * Pre-market session simulation
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 9:00 AM on weekdays
    public void startPreMarketSession() {
        log.info("Starting pre-market session simulation");
        currentSession = MarketSession.PRE_MARKET;
        simulatePreMarketActivity();
    }
    
    /**
     * High-frequency tick data simulation
     */
    private void startHighFrequencySimulation() {
        simulatorExecutor.submit(() -> {
            while (simulationActive.get() && marketOpen) {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Simulate NSE ticks
                    var nseTask = scope.fork(this::simulateNSETicks);
                    
                    // Simulate BSE ticks  
                    var bseTask = scope.fork(this::simulateBSETicks);
                    
                    scope.join();
                    scope.throwIfFailed();
                    
                    // Throttle to realistic tick frequency (100-1000 ticks/second)
                    try {
                        Thread.sleep(random.nextInt(10) + 1); // 1-10ms between ticks
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                } catch (Exception e) {
                    log.error("Error in high-frequency simulation: {}", e.getMessage());
                    try {
                        Thread.sleep(100); // Backoff on error
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Simulate NSE market data ticks
     */
    private Void simulateNSETicks() {
        try {
            List<String> activeSymbols = selectActiveSymbols(nseSymbols, 50); // Top 50 most active
            
            for (String symbol : activeSymbols) {
                if (!simulationActive.get()) break;
                
                SimulatedSymbol simSymbol = nseSymbols.get(symbol);
                MarketDataPoint tickData = generateTickData(simSymbol, "NSE");
                
                // Send to market data service
                marketDataService.writeMarketData(tickData);
                
                // Send to Kafka for real-time processing
                kafkaTemplate.send("market-data-raw", symbol, tickData);
                
                tickCounter.incrementAndGet();
            }
            
        } catch (Exception e) {
            log.error("Error simulating NSE ticks: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Simulate BSE market data ticks
     */
    private Void simulateBSETicks() {
        try {
            List<String> activeSymbols = selectActiveSymbols(bseSymbols, 30); // Top 30 most active
            
            for (String symbol : activeSymbols) {
                if (!simulationActive.get()) break;
                
                SimulatedSymbol simSymbol = bseSymbols.get(symbol);
                MarketDataPoint tickData = generateTickData(simSymbol, "BSE");
                
                // Send to market data service
                marketDataService.writeMarketData(tickData);
                
                // Send to Kafka for real-time processing
                kafkaTemplate.send("market-data-raw", symbol, tickData);
                
                tickCounter.incrementAndGet();
            }
            
        } catch (Exception e) {
            log.error("Error simulating BSE ticks: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Generate realistic tick data for a symbol
     */
    private MarketDataPoint generateTickData(SimulatedSymbol symbol, String exchange) {
        // Price movement simulation with volatility
        double priceChange = (random.nextGaussian() * symbol.volatility * symbol.currentPrice.doubleValue()) / 100.0;
        BigDecimal newPrice = symbol.currentPrice.add(BigDecimal.valueOf(priceChange))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Ensure price doesn't go negative or below circuit limits
        newPrice = newPrice.max(symbol.currentPrice.multiply(BigDecimal.valueOf(0.8))) // -20% circuit
                          .min(symbol.currentPrice.multiply(BigDecimal.valueOf(1.2))); // +20% circuit
        
        // Update symbol state
        symbol.currentPrice = newPrice;
        symbol.lastUpdate = Instant.now();
        
        // Generate volume based on symbol liquidity
        long volume = (long) (symbol.avgVolume * (0.5 + random.nextDouble())); // 50-150% of avg volume
        
        // Calculate bid/ask spread
        BigDecimal spread = newPrice.multiply(BigDecimal.valueOf(symbol.spread));
        BigDecimal bid = newPrice.subtract(spread.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        BigDecimal ask = newPrice.add(spread.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        
        return MarketDataPoint.createOrderBookData(
            symbol.symbol,
            exchange,
            bid,
            ask,
            volume / 2, // Bid size
            volume / 2, // Ask size  
            Instant.now()
        );
    }
    
    /**
     * Select most active symbols for simulation
     */
    private List<String> selectActiveSymbols(Map<String, SimulatedSymbol> symbolMap, int count) {
        return symbolMap.values().stream()
            .sorted((a, b) -> Long.compare(b.avgVolume, a.avgVolume))
            .limit(count)
            .map(s -> s.symbol)
            .toList();
    }
    
    /**
     * Simulate pre-market activity
     */
    private void simulatePreMarketActivity() {
        simulatorExecutor.submit(() -> {
            while (currentSession == MarketSession.PRE_MARKET) {
                try {
                    // Simulate lower frequency pre-market trades
                    String[] preMarketSymbols = {"RELIANCE", "TCS", "INFY", "HDFC", "ICICIBANK"};
                    
                    for (String symbol : preMarketSymbols) {
                        if (nseSymbols.containsKey(symbol)) {
                            SimulatedSymbol simSymbol = nseSymbols.get(symbol);
                            MarketDataPoint preMarketData = generatePreMarketData(simSymbol, "NSE");
                            
                            marketDataService.writeMarketData(preMarketData);
                            kafkaTemplate.send("market-data-raw", symbol, preMarketData);
                        }
                    }
                    
                    Thread.sleep(5000); // 5-second intervals in pre-market
                    
                } catch (Exception e) {
                    log.error("Error in pre-market simulation: {}", e.getMessage());
                }
            }
        });
    }
    
    /**
     * Generate pre-market data with reduced volatility
     */
    private MarketDataPoint generatePreMarketData(SimulatedSymbol symbol, String exchange) {
        // Reduced volatility for pre-market
        double priceChange = (random.nextGaussian() * symbol.volatility * 0.3 * symbol.currentPrice.doubleValue()) / 100.0;
        BigDecimal newPrice = symbol.currentPrice.add(BigDecimal.valueOf(priceChange))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Lower volume for pre-market
        long volume = (long) (symbol.avgVolume * 0.1 * (0.5 + random.nextDouble()));
        
        return MarketDataPoint.createTickData(
            symbol.symbol,
            exchange,
            newPrice,
            volume,
            Instant.now()
        );
    }
    
    /**
     * Initialize NSE symbols for simulation
     */
    private void initializeNSESymbols() {
        // Top NSE symbols with realistic parameters
        var nseData = Map.of(
            "RELIANCE", new SimulatedSymbol("RELIANCE", new BigDecimal("2450.50"), 1500000L, 2.1, 0.02),
            "TCS", new SimulatedSymbol("TCS", new BigDecimal("3850.75"), 800000L, 1.8, 0.015),
            "INFY", new SimulatedSymbol("INFY", new BigDecimal("1650.25"), 1200000L, 2.0, 0.018),
            "HDFC", new SimulatedSymbol("HDFC", new BigDecimal("2750.40"), 900000L, 2.3, 0.02),
            "ICICIBANK", new SimulatedSymbol("ICICIBANK", new BigDecimal("950.80"), 2000000L, 2.5, 0.025),
            "SBIN", new SimulatedSymbol("SBIN", new BigDecimal("720.60"), 1800000L, 2.8, 0.03),
            "BAJFINANCE", new SimulatedSymbol("BAJFINANCE", new BigDecimal("6850.90"), 400000L, 3.2, 0.02),
            "BHARTIARTL", new SimulatedSymbol("BHARTIARTL", new BigDecimal("850.45"), 1100000L, 2.4, 0.022),
            "ITC", new SimulatedSymbol("ITC", new BigDecimal("425.30"), 1600000L, 1.9, 0.02),
            "KOTAKBANK", new SimulatedSymbol("KOTAKBANK", new BigDecimal("1750.85"), 700000L, 2.2, 0.018)
        );
        
        nseSymbols.putAll(nseData);
    }
    
    /**
     * Initialize BSE symbols for simulation
     */
    private void initializeBSESymbols() {
        // Top BSE symbols (some overlap with NSE)
        var bseData = Map.of(
            "RELIANCE", new SimulatedSymbol("RELIANCE", new BigDecimal("2448.75"), 800000L, 2.0, 0.022),
            "TCS", new SimulatedSymbol("TCS", new BigDecimal("3845.20"), 500000L, 1.7, 0.016),
            "HDFC", new SimulatedSymbol("HDFC", new BigDecimal("2745.60"), 600000L, 2.2, 0.021),
            "WIPRO", new SimulatedSymbol("WIPRO", new BigDecimal("565.40"), 900000L, 2.6, 0.024),
            "SUNPHARMA", new SimulatedSymbol("SUNPHARMA", new BigDecimal("1150.25"), 700000L, 2.3, 0.019)
        );
        
        bseSymbols.putAll(bseData);
    }
    
    /**
     * Get simulation statistics
     */
    public SimulationStats getSimulationStats() {
        return new SimulationStats(
            tickCounter.get(),
            simulationActive.get(),
            currentSession,
            nseSymbols.size(),
            bseSymbols.size(),
            Instant.now()
        );
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down Exchange Feed Simulator");
        simulationActive.set(false);
        if (simulatorExecutor != null && !simulatorExecutor.isShutdown()) {
            simulatorExecutor.shutdown();
            try {
                if (!simulatorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    simulatorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                simulatorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Simulated symbol data structure
     */
    private static class SimulatedSymbol {
        String symbol;
        BigDecimal currentPrice;
        long avgVolume;
        double volatility; // Percentage volatility
        double spread; // Bid-ask spread percentage
        Instant lastUpdate;
        
        SimulatedSymbol(String symbol, BigDecimal currentPrice, long avgVolume, 
                       double volatility, double spread) {
            this.symbol = symbol;
            this.currentPrice = currentPrice;
            this.avgVolume = avgVolume;
            this.volatility = volatility;
            this.spread = spread;
            this.lastUpdate = Instant.now();
        }
    }
    
    /**
     * Market session enumeration
     */
    public enum MarketSession {
        CLOSED("Market Closed"),
        PRE_MARKET("Pre-Market Session"),
        REGULAR("Regular Trading Session"),
        POST_MARKET("Post-Market Session");
        
        private final String description;
        
        MarketSession(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Simulation statistics
     */
    public record SimulationStats(
        long totalTicks,
        boolean isActive,
        MarketSession currentSession,
        int nseSymbolCount,
        int bseSymbolCount,
        Instant timestamp
    ) {}
}