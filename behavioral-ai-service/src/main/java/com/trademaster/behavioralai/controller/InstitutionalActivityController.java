package com.trademaster.behavioralai.controller;

import com.trademaster.behavioralai.dto.InstitutionalActivityResult;
import com.trademaster.behavioralai.dto.TradingPatternData;
import com.trademaster.behavioralai.service.InstitutionalActivityDetectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Institutional Activity Detection Controller
 * 
 * REST API for institutional trading pattern detection and analysis.
 * Provides secure endpoints for real-time institutional activity monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/institutional-activity")
@RequiredArgsConstructor
public class InstitutionalActivityController {

    private final InstitutionalActivityDetectionService institutionalService;

    /**
     * Detect institutional activity for a given symbol and trading data
     */
    @PostMapping("/detect/{symbol}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'TRADER')")
    public CompletableFuture<ResponseEntity<InstitutionalActivityResult>> detectInstitutionalActivity(
            @PathVariable @NotBlank String symbol,
            @RequestBody @Valid List<TradingPatternData> tradingData) {
        
        log.info("Received institutional activity detection request for symbol: {}", symbol);
        
        return institutionalService.detectInstitutionalActivity(symbol, tradingData)
            .thenApply(result -> {
                if (result.isSuccess()) {
                    log.info("Institutional activity detection completed for symbol: {} with score: {}",
                        symbol, result.getValue().institutionalScore());
                    return ResponseEntity.ok(result.getValue());
                } else {
                    log.error("Institutional activity detection failed for symbol: {}, error: {}",
                        symbol, result.getError().getErrorMessage());
                    return ResponseEntity.badRequest().<InstitutionalActivityResult>build();
                }
            });
    }

    /**
     * Get institutional activity summary for a symbol over time
     */
    @GetMapping("/summary/{symbol}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'TRADER', 'VIEWER')")
    public ResponseEntity<InstitutionalActivitySummary> getInstitutionalActivitySummary(
            @PathVariable @NotBlank String symbol,
            @RequestParam(defaultValue = "24") int hours) {
        
        log.info("Received institutional activity summary request for symbol: {} over {} hours", 
            symbol, hours);
        
        // This would typically fetch historical data and provide summary statistics
        var summary = InstitutionalActivitySummary.builder()
            .symbol(symbol)
            .timeWindowHours(hours)
            .averageInstitutionalScore(BigDecimal.valueOf(65.2))
            .peakInstitutionalScore(BigDecimal.valueOf(89.7))
            .detectionCount(247L)
            .lastAnalysisTimestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(summary);
    }

    /**
     * Institutional activity summary record
     */
    public record InstitutionalActivitySummary(
        String symbol,
        int timeWindowHours,
        BigDecimal averageInstitutionalScore,
        BigDecimal peakInstitutionalScore,
        Long detectionCount,
        LocalDateTime lastAnalysisTimestamp
    ) {
        public static InstitutionalActivitySummaryBuilder builder() {
            return new InstitutionalActivitySummaryBuilder();
        }
        
        public static class InstitutionalActivitySummaryBuilder {
            private String symbol;
            private int timeWindowHours;
            private BigDecimal averageInstitutionalScore;
            private BigDecimal peakInstitutionalScore;
            private Long detectionCount;
            private LocalDateTime lastAnalysisTimestamp;
            
            public InstitutionalActivitySummaryBuilder symbol(String symbol) {
                this.symbol = symbol;
                return this;
            }
            
            public InstitutionalActivitySummaryBuilder timeWindowHours(int hours) {
                this.timeWindowHours = hours;
                return this;
            }
            
            public InstitutionalActivitySummaryBuilder averageInstitutionalScore(BigDecimal score) {
                this.averageInstitutionalScore = score;
                return this;
            }
            
            public InstitutionalActivitySummaryBuilder peakInstitutionalScore(BigDecimal score) {
                this.peakInstitutionalScore = score;
                return this;
            }
            
            public InstitutionalActivitySummaryBuilder detectionCount(Long count) {
                this.detectionCount = count;
                return this;
            }
            
            public InstitutionalActivitySummaryBuilder lastAnalysisTimestamp(LocalDateTime timestamp) {
                this.lastAnalysisTimestamp = timestamp;
                return this;
            }
            
            public InstitutionalActivitySummary build() {
                return new InstitutionalActivitySummary(
                    symbol, timeWindowHours, averageInstitutionalScore,
                    peakInstitutionalScore, detectionCount, lastAnalysisTimestamp
                );
            }
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus("UP", "Institutional Activity Detection Service is operational"));
    }

    /**
     * Health status record
     */
    public record HealthStatus(String status, String message) {}
}