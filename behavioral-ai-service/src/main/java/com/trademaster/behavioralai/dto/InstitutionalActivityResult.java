package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Institutional Activity Detection Result
 * 
 * Comprehensive analysis result containing institutional trading detection scores
 * and confidence metrics. Uses Records for immutability and functional programming.
 */
public record InstitutionalActivityResult(
    @NotBlank(message = "Symbol cannot be blank")
    String symbol,
    
    @NotNull(message = "Institutional score cannot be null")
    BigDecimal institutionalScore,
    
    @NotNull(message = "Volume score cannot be null")
    BigDecimal volumeScore,
    
    @NotNull(message = "Price impact score cannot be null")
    BigDecimal priceImpactScore,
    
    @NotNull(message = "Time series score cannot be null")
    BigDecimal timeSeriesScore,
    
    @NotNull(message = "Block trade score cannot be null")
    BigDecimal blockTradeScore,
    
    @NotNull(message = "Iceberg score cannot be null")
    BigDecimal icebergScore,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime detectionTimestamp,
    
    @NotNull(message = "Confidence level cannot be null")
    BigDecimal confidenceLevel,
    
    @NotBlank(message = "Algorithm version cannot be blank")
    String algorithmVersion
) {
    
    /**
     * Builder pattern for complex construction
     */
    public static InstitutionalActivityResultBuilder builder() {
        return new InstitutionalActivityResultBuilder();
    }
    
    /**
     * Determine if institutional activity is likely present
     */
    public boolean isInstitutionalActivityDetected() {
        return institutionalScore.compareTo(BigDecimal.valueOf(70)) >= 0;
    }
    
    /**
     * Get risk level based on institutional score
     */
    public RiskLevel getRiskLevel() {
        if (institutionalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return RiskLevel.VERY_HIGH;
        } else if (institutionalScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return RiskLevel.HIGH;
        } else if (institutionalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return RiskLevel.MODERATE;
        } else if (institutionalScore.compareTo(BigDecimal.valueOf(30)) >= 0) {
            return RiskLevel.LOW;
        } else {
            return RiskLevel.MINIMAL;
        }
    }
    
    /**
     * Get the dominant detection pattern
     */
    public DetectionPattern getDominantPattern() {
        var maxScore = BigDecimal.ZERO;
        var dominantPattern = DetectionPattern.VOLUME_ANOMALY;
        
        if (volumeScore.compareTo(maxScore) > 0) {
            maxScore = volumeScore;
            dominantPattern = DetectionPattern.VOLUME_ANOMALY;
        }
        
        if (priceImpactScore.compareTo(maxScore) > 0) {
            maxScore = priceImpactScore;
            dominantPattern = DetectionPattern.PRICE_IMPACT;
        }
        
        if (timeSeriesScore.compareTo(maxScore) > 0) {
            maxScore = timeSeriesScore;
            dominantPattern = DetectionPattern.TIME_SERIES_PATTERN;
        }
        
        if (blockTradeScore.compareTo(maxScore) > 0) {
            maxScore = blockTradeScore;
            dominantPattern = DetectionPattern.BLOCK_TRADE;
        }
        
        if (icebergScore.compareTo(maxScore) > 0) {
            dominantPattern = DetectionPattern.ICEBERG_ORDER;
        }
        
        return dominantPattern;
    }
    
    /**
     * Risk level enumeration
     */
    public enum RiskLevel {
        MINIMAL, LOW, MODERATE, HIGH, VERY_HIGH
    }
    
    /**
     * Detection pattern enumeration
     */
    public enum DetectionPattern {
        VOLUME_ANOMALY, PRICE_IMPACT, TIME_SERIES_PATTERN, BLOCK_TRADE, ICEBERG_ORDER
    }
    
    /**
     * Builder implementation
     */
    public static class InstitutionalActivityResultBuilder {
        private String symbol;
        private BigDecimal institutionalScore;
        private BigDecimal volumeScore;
        private BigDecimal priceImpactScore;
        private BigDecimal timeSeriesScore;
        private BigDecimal blockTradeScore;
        private BigDecimal icebergScore;
        private LocalDateTime detectionTimestamp;
        private BigDecimal confidenceLevel;
        private String algorithmVersion;
        
        public InstitutionalActivityResultBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public InstitutionalActivityResultBuilder institutionalScore(BigDecimal score) {
            this.institutionalScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder volumeScore(BigDecimal score) {
            this.volumeScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder priceImpactScore(BigDecimal score) {
            this.priceImpactScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder timeSeriesScore(BigDecimal score) {
            this.timeSeriesScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder blockTradeScore(BigDecimal score) {
            this.blockTradeScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder icebergScore(BigDecimal score) {
            this.icebergScore = score;
            return this;
        }
        
        public InstitutionalActivityResultBuilder detectionTimestamp(LocalDateTime timestamp) {
            this.detectionTimestamp = timestamp;
            return this;
        }
        
        public InstitutionalActivityResultBuilder confidenceLevel(BigDecimal confidence) {
            this.confidenceLevel = confidence;
            return this;
        }
        
        public InstitutionalActivityResultBuilder algorithmVersion(String version) {
            this.algorithmVersion = version;
            return this;
        }
        
        public InstitutionalActivityResult build() {
            return new InstitutionalActivityResult(
                symbol,
                institutionalScore,
                volumeScore,
                priceImpactScore,
                timeSeriesScore,
                blockTradeScore,
                icebergScore,
                detectionTimestamp,
                confidenceLevel,
                algorithmVersion
            );
        }
    }
}