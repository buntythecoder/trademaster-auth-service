package com.trademaster.marketdata.monitoring;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.kafka.MarketDataQualityValidator;
import com.trademaster.marketdata.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Quality Monitoring System
 * 
 * Features:
 * - Continuous data quality assessment
 * - Real-time anomaly detection
 * - Feed interruption monitoring
 * - Quality metrics collection and alerting
 * - SLA monitoring and reporting
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityMonitor {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataQualityValidator qualityValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Quality metrics tracking
    private final AtomicLong totalDataPoints = new AtomicLong(0);
    private final AtomicLong qualityViolations = new AtomicLong(0);
    private final AtomicLong feedInterruptions = new AtomicLong(0);
    private final AtomicLong dataGaps = new AtomicLong(0);
    
    // Feed monitoring
    private final Map<String, FeedHealth> feedHealthMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastDataReceived = new ConcurrentHashMap<>();
    
    // Quality thresholds
    private static final double MIN_QUALITY_SCORE = 0.85; // 85% minimum quality
    private static final long MAX_DATA_GAP_MINUTES = 5; // 5 minutes max gap
    private static final double MIN_UPTIME_PERCENT = 99.5; // 99.5% uptime SLA

    /**
     * Monitor data quality every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorDataQuality() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            log.debug("Starting data quality monitoring cycle");
            
            var qualityTask = scope.fork(this::assessDataQuality);
            var feedTask = scope.fork(this::monitorFeedHealth);
            var gapTask = scope.fork(this::detectDataGaps);
            
            scope.join();
            scope.throwIfFailed();
            
            var qualityResults = qualityTask.get();
            var feedResults = feedTask.get();
            var gapResults = gapTask.get();
            
            // Generate alerts if needed
            evaluateAlerts(qualityResults, feedResults, gapResults);
            
            log.debug("Data quality monitoring cycle completed");
            
        } catch (Exception e) {
            log.error("Error during data quality monitoring: {}", e.getMessage(), e);
        }
    }

    /**
     * Assess overall data quality
     */
    private QualityAssessment assessDataQuality() {
        try {
            var validationStats = qualityValidator.getValidationStats();
            
            // Calculate quality metrics
            double validationRate = validationStats.validationRate();
            long anomalies = validationStats.anomaliesDetected();
            
            // Assess quality level
            QualityLevel level;
            if (validationRate >= 98.0 && anomalies < 10) {
                level = QualityLevel.EXCELLENT;
            } else if (validationRate >= 95.0 && anomalies < 50) {
                level = QualityLevel.GOOD;
            } else if (validationRate >= MIN_QUALITY_SCORE * 100 && anomalies < 100) {
                level = QualityLevel.ACCEPTABLE;
            } else {
                level = QualityLevel.POOR;
            }
            
            var assessment = new QualityAssessment(
                validationRate,
                anomalies,
                level,
                validationStats.validRecords(),
                validationStats.invalidRecords(),
                Instant.now()
            );
            
            // Send metrics to monitoring system
            publishQualityMetrics(assessment);
            
            return assessment;
            
        } catch (Exception e) {
            log.error("Error assessing data quality: {}", e.getMessage());
            return new QualityAssessment(0.0, 0L, QualityLevel.POOR, 0L, 0L, Instant.now());
        }
    }

    /**
     * Monitor feed health and uptime
     */
    private FeedHealthResults monitorFeedHealth() {
        try {
            List<String> exchanges = List.of("NSE", "BSE", "MCX");
            Map<String, FeedHealthStatus> exchangeHealth = new ConcurrentHashMap<>();
            
            for (String exchange : exchanges) {
                FeedHealthStatus status = assessExchangeFeedHealth(exchange);
                exchangeHealth.put(exchange, status);
                feedHealthMap.put(exchange, 
                    new FeedHealth(exchange, status, Instant.now()));
            }
            
            return new FeedHealthResults(exchangeHealth, Instant.now());
            
        } catch (Exception e) {
            log.error("Error monitoring feed health: {}", e.getMessage());
            return new FeedHealthResults(Map.of(), Instant.now());
        }
    }

    /**
     * Assess individual exchange feed health
     */
    private FeedHealthStatus assessExchangeFeedHealth(String exchange) {
        try {
            // Check recent data activity
            var activeSymbols = marketDataRepository.getActiveSymbols(exchange, 5);
            Instant lastData = lastDataReceived.get(exchange);
            
            long minutesSinceLastData = lastData != null ? 
                ChronoUnit.MINUTES.between(lastData, Instant.now()) : Long.MAX_VALUE;
            
            // Determine health status
            if (minutesSinceLastData <= 1 && activeSymbols.size() > 10) {
                return FeedHealthStatus.HEALTHY;
            } else if (minutesSinceLastData <= 5 && activeSymbols.size() > 5) {
                return FeedHealthStatus.DEGRADED;
            } else if (minutesSinceLastData <= 15) {
                return FeedHealthStatus.UNSTABLE;
            } else {
                feedInterruptions.incrementAndGet();
                return FeedHealthStatus.DOWN;
            }
            
        } catch (Exception e) {
            log.error("Error assessing feed health for {}: {}", exchange, e.getMessage());
            return FeedHealthStatus.DOWN;
        }
    }

    /**
     * Detect data gaps across symbols
     */
    private DataGapResults detectDataGaps() {
        try {
            List<String> criticalSymbols = List.of("RELIANCE", "TCS", "INFY", "HDFC", "ICICIBANK");
            Map<String, Long> symbolGaps = new ConcurrentHashMap<>();
            
            for (String symbol : criticalSymbols) {
                var qualityReport = marketDataRepository.generateQualityReport(symbol, "NSE", 1);
                long gaps = qualityReport.dataGaps();
                
                if (gaps > 0) {
                    symbolGaps.put(symbol, gaps);
                    dataGaps.addAndGet(gaps);
                }
            }
            
            return new DataGapResults(symbolGaps, dataGaps.get(), Instant.now());
            
        } catch (Exception e) {
            log.error("Error detecting data gaps: {}", e.getMessage());
            return new DataGapResults(Map.of(), 0L, Instant.now());
        }
    }

    /**
     * Evaluate and generate alerts based on monitoring results
     */
    private void evaluateAlerts(QualityAssessment quality, FeedHealthResults feedHealth, 
                               DataGapResults gapResults) {
        
        // Quality alerts
        if (quality.qualityScore() < MIN_QUALITY_SCORE * 100) {
            generateAlert(AlertType.QUALITY_DEGRADATION, 
                String.format("Data quality below threshold: %.2f%%", quality.qualityScore()),
                AlertSeverity.HIGH);
        }
        
        // Feed health alerts
        feedHealth.exchangeHealth().forEach((exchange, status) -> {
            if (status == FeedHealthStatus.DOWN) {
                generateAlert(AlertType.FEED_DOWN,
                    String.format("Exchange feed down: %s", exchange),
                    AlertSeverity.CRITICAL);
            } else if (status == FeedHealthStatus.UNSTABLE) {
                generateAlert(AlertType.FEED_UNSTABLE,
                    String.format("Exchange feed unstable: %s", exchange),
                    AlertSeverity.MEDIUM);
            }
        });
        
        // Data gap alerts
        if (gapResults.totalGaps() > 10) {
            generateAlert(AlertType.DATA_GAPS,
                String.format("Excessive data gaps detected: %d", gapResults.totalGaps()),
                AlertSeverity.HIGH);
        }
        
        // Anomaly alerts
        if (quality.anomalies() > 100) {
            generateAlert(AlertType.HIGH_ANOMALIES,
                String.format("High anomaly count: %d", quality.anomalies()),
                AlertSeverity.MEDIUM);
        }
    }

    /**
     * Generate monitoring alert
     */
    private void generateAlert(AlertType type, String message, AlertSeverity severity) {
        try {
            var alert = new MonitoringAlert(
                type,
                message,
                severity,
                Instant.now(),
                Map.of(
                    "source", "DataQualityMonitor",
                    "service", "market-data-service"
                )
            );
            
            // Send to alerting system via Kafka
            kafkaTemplate.send("monitoring-alerts", type.name(), alert);
            
            log.warn("Generated {} alert: {}", severity, message);
            
        } catch (Exception e) {
            log.error("Failed to generate alert: {}", e.getMessage());
        }
    }

    /**
     * Publish quality metrics to monitoring system
     */
    private void publishQualityMetrics(QualityAssessment assessment) {
        try {
            var metrics = Map.of(
                "data_quality_score", assessment.qualityScore(),
                "validation_rate", assessment.qualityScore(),
                "anomaly_count", assessment.anomalies(),
                "valid_records", assessment.validRecords(),
                "invalid_records", assessment.invalidRecords(),
                "total_data_points", totalDataPoints.get(),
                "quality_violations", qualityViolations.get(),
                "feed_interruptions", feedInterruptions.get(),
                "data_gaps", dataGaps.get()
            );
            
            kafkaTemplate.send("quality-metrics", "market-data", metrics);
            
        } catch (Exception e) {
            log.error("Failed to publish quality metrics: {}", e.getMessage());
        }
    }

    /**
     * Record data point reception
     */
    public void recordDataReception(MarketDataPoint data) {
        totalDataPoints.incrementAndGet();
        lastDataReceived.put(data.exchange(), data.timestamp());
        
        // Update feed health
        FeedHealth health = feedHealthMap.get(data.exchange());
        if (health != null && health.status() != FeedHealthStatus.HEALTHY) {
            feedHealthMap.put(data.exchange(), 
                new FeedHealth(data.exchange(), FeedHealthStatus.HEALTHY, Instant.now()));
        }
    }

    /**
     * Generate comprehensive quality report
     */
    public CompletableFuture<DataQualityReport> generateQualityReport(int hours) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                var qualityTask = scope.fork(() -> assessDataQuality());
                var feedTask = scope.fork(() -> monitorFeedHealth());
                var gapTask = scope.fork(() -> detectDataGaps());
                var slaTask = scope.fork(() -> calculateSLAMetrics(hours));
                
                scope.join();
                scope.throwIfFailed();
                
                return new DataQualityReport(
                    qualityTask.get(),
                    feedTask.get(),
                    gapTask.get(),
                    slaTask.get(),
                    Instant.now()
                );
                
            } catch (Exception e) {
                log.error("Error generating quality report: {}", e.getMessage());
                return new DataQualityReport(
                    new QualityAssessment(0.0, 0L, QualityLevel.POOR, 0L, 0L, Instant.now()),
                    new FeedHealthResults(Map.of(), Instant.now()),
                    new DataGapResults(Map.of(), 0L, Instant.now()),
                    new SLAMetrics(0.0, 0.0, 0L, Instant.now()),
                    Instant.now()
                );
            }
        });
    }

    /**
     * Calculate SLA metrics
     */
    private SLAMetrics calculateSLAMetrics(int hours) {
        try {
            // Calculate uptime percentage
            long totalMinutes = hours * 60L;
            long downtime = feedInterruptions.get() * 5; // Assume 5 minutes per interruption
            double uptimePercent = ((double) (totalMinutes - downtime) / totalMinutes) * 100;
            
            // Calculate data availability
            double dataAvailability = totalDataPoints.get() > 0 ? 
                ((double) (totalDataPoints.get() - qualityViolations.get()) / totalDataPoints.get()) * 100 : 0.0;
            
            return new SLAMetrics(
                uptimePercent,
                dataAvailability,
                feedInterruptions.get(),
                Instant.now()
            );
            
        } catch (Exception e) {
            log.error("Error calculating SLA metrics: {}", e.getMessage());
            return new SLAMetrics(0.0, 0.0, 0L, Instant.now());
        }
    }

    // Data classes
    public record QualityAssessment(
        double qualityScore,
        long anomalies,
        QualityLevel level,
        long validRecords,
        long invalidRecords,
        Instant timestamp
    ) {}

    public record FeedHealthResults(
        Map<String, FeedHealthStatus> exchangeHealth,
        Instant timestamp
    ) {}

    public record DataGapResults(
        Map<String, Long> symbolGaps,
        long totalGaps,
        Instant timestamp
    ) {}

    public record FeedHealth(
        String exchange,
        FeedHealthStatus status,
        Instant timestamp
    ) {}

    public record SLAMetrics(
        double uptimePercent,
        double dataAvailability,
        long feedInterruptions,
        Instant timestamp
    ) {
        public boolean meetsSLA() {
            return uptimePercent >= MIN_UPTIME_PERCENT && dataAvailability >= MIN_QUALITY_SCORE * 100;
        }
    }

    public record DataQualityReport(
        QualityAssessment quality,
        FeedHealthResults feedHealth,
        DataGapResults dataGaps,
        SLAMetrics slaMetrics,
        Instant generatedAt
    ) {}

    public record MonitoringAlert(
        AlertType type,
        String message,
        AlertSeverity severity,
        Instant timestamp,
        Map<String, String> metadata
    ) {}

    // Enumerations
    public enum QualityLevel {
        EXCELLENT("Excellent data quality"),
        GOOD("Good data quality"),
        ACCEPTABLE("Acceptable data quality"),
        POOR("Poor data quality requiring attention");

        private final String description;

        QualityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum FeedHealthStatus {
        HEALTHY("Feed operating normally"),
        DEGRADED("Feed experiencing minor issues"),
        UNSTABLE("Feed experiencing significant issues"),
        DOWN("Feed is not operational");

        private final String description;

        FeedHealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AlertType {
        QUALITY_DEGRADATION,
        FEED_DOWN,
        FEED_UNSTABLE,
        DATA_GAPS,
        HIGH_ANOMALIES,
        SLA_VIOLATION
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}