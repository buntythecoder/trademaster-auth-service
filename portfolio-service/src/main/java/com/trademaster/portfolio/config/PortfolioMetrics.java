package com.trademaster.portfolio.config;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Portfolio Business Metrics Collector
 *
 * Tracks core portfolio metrics for business intelligence and monitoring.
 *
 * Rule #15: Structured Logging & Monitoring
 * Rule #22: Performance Standards (<50ms operations)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioMetrics {

    private final MeterRegistry meterRegistry;

    // Portfolio Counters
    private final Counter portfoliosCreated;
    private final Counter portfolioValuationUpdates;
    private final Counter portfolioStatusChanges;
    private final Counter bulkValuationErrors;
    private final Counter dayTradesResetErrors;
    private final Counter aumCalculationErrors;
    private final Counter portfolioCreationErrors;

    // Portfolio Gauges
    private final AtomicLong activePortfoliosCount = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalAUM = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalRealizedPnL = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalUnrealizedPnL = new AtomicReference<>(BigDecimal.ZERO);

    // Portfolio Timers
    private final Timer portfolioCreationTimer;
    private final Timer portfolioValuationTimer;
    private final Timer portfolioUpdateTimer;
    private final Timer portfolioLookupTimer;
    private final Timer positionRetrievalTimer;
    private final Timer bulkValuationTimer;
    private final Timer cashUpdateTimer;
    private final Timer pnlUpdateTimer;
    private final Timer summaryGenerationTimer;
    private final Timer valuationLookupTimer;
    private final Timer dayTradesResetTimer;
    private final Timer aumCalculationTimer;
    private final Timer portfolioDeletionTimer;

    public PortfolioMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.portfoliosCreated = Counter.builder("portfolio.created.total")
            .description("Total number of portfolios created")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioValuationUpdates = Counter.builder("portfolio.valuation.updates.total")
            .description("Total number of portfolio valuation updates")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioStatusChanges = Counter.builder("portfolio.status.changes.total")
            .description("Total number of portfolio status changes")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.bulkValuationErrors = Counter.builder("portfolio.bulk.valuation.errors.total")
            .description("Total number of bulk valuation errors")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.dayTradesResetErrors = Counter.builder("portfolio.day.trades.reset.errors.total")
            .description("Total number of day trades reset errors")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.aumCalculationErrors = Counter.builder("portfolio.aum.calculation.errors.total")
            .description("Total number of AUM calculation errors")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioCreationErrors = Counter.builder("portfolio.creation.errors.total")
            .description("Total number of portfolio creation errors")
            .tag("service", "portfolio")
            .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("portfolio.active.count", this, PortfolioMetrics::getActivePortfoliosCount)
            .description("Number of active portfolios")
            .tag("service", "portfolio")
            .register(meterRegistry);

        Gauge.builder("portfolio.aum.total", this, PortfolioMetrics::getTotalAUM)
            .description("Total Assets Under Management")
            .tag("service", "portfolio")
            .tag("currency", "INR")
            .register(meterRegistry);

        Gauge.builder("portfolio.pnl.realized.total", this, PortfolioMetrics::getTotalRealizedPnL)
            .description("Total Realized P&L across all portfolios")
            .tag("service", "portfolio")
            .tag("currency", "INR")
            .register(meterRegistry);

        Gauge.builder("portfolio.pnl.unrealized.total", this, PortfolioMetrics::getTotalUnrealizedPnL)
            .description("Total Unrealized P&L across all portfolios")
            .tag("service", "portfolio")
            .tag("currency", "INR")
            .register(meterRegistry);

        // Initialize timers
        this.portfolioCreationTimer = Timer.builder("portfolio.creation.duration")
            .description("Time taken to create a new portfolio")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioValuationTimer = Timer.builder("portfolio.valuation.duration")
            .description("Time taken to update portfolio valuation")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioUpdateTimer = Timer.builder("portfolio.update.duration")
            .description("Time taken to update portfolio data")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioLookupTimer = Timer.builder("portfolio.lookup.duration")
            .description("Time taken to lookup portfolio by ID or user ID")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.positionRetrievalTimer = Timer.builder("portfolio.position.retrieval.duration")
            .description("Time taken to retrieve positions for a portfolio")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.bulkValuationTimer = Timer.builder("portfolio.bulk.valuation.duration")
            .description("Time taken to complete bulk portfolio valuations")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.cashUpdateTimer = Timer.builder("portfolio.cash.update.duration")
            .description("Time taken to update cash balance")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.pnlUpdateTimer = Timer.builder("portfolio.pnl.update.duration")
            .description("Time taken to update P&L")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.summaryGenerationTimer = Timer.builder("portfolio.summary.generation.duration")
            .description("Time taken to generate portfolio summary")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.valuationLookupTimer = Timer.builder("portfolio.valuation.lookup.duration")
            .description("Time taken to lookup portfolios requiring valuation")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.dayTradesResetTimer = Timer.builder("portfolio.day.trades.reset.duration")
            .description("Time taken to reset day trades count")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.aumCalculationTimer = Timer.builder("portfolio.aum.calculation.duration")
            .description("Time taken to calculate total AUM")
            .tag("service", "portfolio")
            .register(meterRegistry);

        this.portfolioDeletionTimer = Timer.builder("portfolio.deletion.duration")
            .description("Time taken to delete a portfolio")
            .tag("service", "portfolio")
            .register(meterRegistry);
    }

    // Increment methods
    public void incrementPortfoliosCreated() {
        portfoliosCreated.increment();
        log.debug("Portfolio created - total count incremented");
    }

    public void incrementValuationUpdates() {
        portfolioValuationUpdates.increment();
    }

    public void incrementStatusChanges(String fromStatus, String toStatus) {
        portfolioStatusChanges.increment();
        log.info("Portfolio status changed from {} to {}", fromStatus, toStatus);
    }

    // Update methods for gauges
    public void updateActivePortfoliosCount(long count) {
        activePortfoliosCount.set(count);
    }

    public void updateTotalAUM(BigDecimal aum) {
        totalAUM.set(aum);
        log.debug("Total AUM updated to: {}", aum);
    }

    public void updateTotalRealizedPnL(BigDecimal pnl) {
        totalRealizedPnL.set(pnl);
    }

    public void updateTotalUnrealizedPnL(BigDecimal pnl) {
        totalUnrealizedPnL.set(pnl);
    }

    // Timer methods
    public Timer.Sample startCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCreationTime(Timer.Sample sample) {
        sample.stop(portfolioCreationTimer);
    }

    public Timer.Sample startValuationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordValuationTime(Timer.Sample sample) {
        sample.stop(portfolioValuationTimer);
    }

    public Timer.Sample startUpdateTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordUpdateTime(Timer.Sample sample) {
        sample.stop(portfolioUpdateTimer);
    }

    public Timer.Sample startLookupTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLookupTime(Timer.Sample sample) {
        sample.stop(portfolioLookupTimer);
    }

    public Timer.Sample startPositionRetrievalTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPositionRetrievalTime(Timer.Sample sample) {
        sample.stop(positionRetrievalTimer);
    }

    public Timer.Sample startBulkValuationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordBulkValuationTime(Timer.Sample sample) {
        sample.stop(bulkValuationTimer);
    }

    public void incrementBulkValuationErrors() {
        bulkValuationErrors.increment();
        log.error("Bulk valuation error occurred");
    }

    public Timer.Sample startCashUpdateTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCashUpdateTime(Timer.Sample sample) {
        sample.stop(cashUpdateTimer);
    }

    public Timer.Sample startPnlUpdateTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPnlUpdateTime(Timer.Sample sample) {
        sample.stop(pnlUpdateTimer);
    }

    public Timer.Sample startSummaryGenerationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSummaryGenerationTime(Timer.Sample sample) {
        sample.stop(summaryGenerationTimer);
    }

    public Timer.Sample startValuationLookupTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordValuationLookupTime(Timer.Sample sample) {
        sample.stop(valuationLookupTimer);
    }

    public Timer.Sample startDayTradesResetTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDayTradesResetTime(Timer.Sample sample) {
        sample.stop(dayTradesResetTimer);
    }

    public void incrementDayTradesResetErrors() {
        dayTradesResetErrors.increment();
        log.error("Day trades reset error occurred");
    }

    public Timer.Sample startAumCalculationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordAumCalculationTime(Timer.Sample sample) {
        sample.stop(aumCalculationTimer);
    }

    public void incrementAumCalculationErrors() {
        aumCalculationErrors.increment();
        log.error("AUM calculation error occurred");
    }

    public Timer.Sample startDeletionTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDeletionTime(Timer.Sample sample) {
        sample.stop(portfolioDeletionTimer);
    }

    public void incrementPortfolioCreationErrors() {
        portfolioCreationErrors.increment();
        log.error("Portfolio creation error occurred");
    }

    // Getter methods for gauges
    private double getActivePortfoliosCount() {
        return activePortfoliosCount.get();
    }

    private double getTotalAUM() {
        return totalAUM.get().doubleValue();
    }

    private double getTotalRealizedPnL() {
        return totalRealizedPnL.get().doubleValue();
    }

    private double getTotalUnrealizedPnL() {
        return totalUnrealizedPnL.get().doubleValue();
    }
}
