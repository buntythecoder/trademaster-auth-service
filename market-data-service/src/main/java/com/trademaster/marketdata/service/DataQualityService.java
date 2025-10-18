package com.trademaster.marketdata.service;

import com.trademaster.marketdata.repository.MarketDataRepository;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Data Quality Service (Quality Monitoring and Reporting)
 *
 * Single Responsibility: Data quality assessment and reporting
 * Following Rule #2 (SRP) and Rule #25 (Circuit Breaker Protection)
 *
 * Features:
 * - Data quality report generation with metrics
 * - Quality score calculation and classification
 * - Data gap detection and analysis
 * - Circuit breaker protection for database operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    private final MarketDataRepository marketDataRepository;
    private final CircuitBreakerService circuitBreakerService;

    /**
     * Generate data quality report with circuit breaker protection
     * Follows Rule #25 (Circuit Breaker) and Rule #11 (Functional Error Handling)
     */
    public CompletableFuture<DataQualityReport> generateQualityReport(String symbol, String exchange, int hours) {
        return circuitBreakerService.executeDatabaseOperationWithFallback(
            () -> marketDataRepository.generateQualityReport(symbol, exchange, hours),
            () -> new MarketDataRepository.DataQualityReport(
                symbol, exchange, 0L, 0L, 0.0, Instant.now()
            )
        ).thenApply(report -> {
            log.debug("Generated quality report for {}:{} - Score: {}",
                symbol, exchange, report.qualityScore());
            return convertToServiceQualityReport(report);
        }).exceptionally(ex -> {
            log.error("Failed to generate quality report for {}:{}: {}",
                symbol, exchange, ex.getMessage());
            return new DataQualityReport(symbol, exchange, 0L, 0L, 0.0,
                QualityLevel.LOW, Instant.now());
        });
    }

    // Helper methods (Rule #5: Max 15 lines per method)

    /**
     * Convert repository quality report to service quality report
     * Using pattern matching (Rule #3, #14)
     */
    private DataQualityReport convertToServiceQualityReport(MarketDataRepository.DataQualityReport repoReport) {
        QualityLevel level = switch (repoReport.getQualityLevel()) {
            case HIGH -> QualityLevel.HIGH;
            case MEDIUM -> QualityLevel.MEDIUM;
            case LOW -> QualityLevel.LOW;
        };

        return new DataQualityReport(
            repoReport.symbol(),
            repoReport.exchange(),
            repoReport.totalRecords(),
            repoReport.dataGaps(),
            repoReport.qualityScore(),
            level,
            repoReport.generatedAt()
        );
    }

    // Data classes (Rule #9: Records for immutability)

    public record DataQualityReport(
        String symbol,
        String exchange,
        long totalRecords,
        long dataGaps,
        double qualityScore,
        QualityLevel qualityLevel,
        Instant generatedAt
    ) {}

    public enum QualityLevel {
        HIGH("Excellent data quality"),
        MEDIUM("Good data quality with minor issues"),
        LOW("Poor data quality requiring attention");

        private final String description;

        QualityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
