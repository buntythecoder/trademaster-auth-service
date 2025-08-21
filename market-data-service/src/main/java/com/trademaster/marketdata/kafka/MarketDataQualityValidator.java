package com.trademaster.marketdata.kafka;

import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Market Data Quality Validator
 * 
 * Features:
 * - Real-time data quality validation
 * - Price movement anomaly detection
 * - Volume validation and circuit breaker checks
 * - Timestamp validation for data freshness
 * - Statistical outlier detection
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class MarketDataQualityValidator {

    // Price history for validation
    private final ConcurrentHashMap<String, PriceHistory> priceHistory = new ConcurrentHashMap<>();
    
    // Quality metrics
    private final AtomicLong validRecords = new AtomicLong(0);
    private final AtomicLong invalidRecords = new AtomicLong(0);
    private final AtomicLong anomaliesDetected = new AtomicLong(0);

    /**
     * Validate market data point
     */
    public ValidationResult validate(MarketDataPoint data) {
        try {
            // Basic field validation
            var basicValidation = validateBasicFields(data);
            if (!basicValidation.isValid()) {
                invalidRecords.incrementAndGet();
                return basicValidation;
            }
            
            // Price validation
            var priceValidation = validatePrice(data);
            if (!priceValidation.isValid()) {
                invalidRecords.incrementAndGet();
                return priceValidation;
            }
            
            // Volume validation
            var volumeValidation = validateVolume(data);
            if (!volumeValidation.isValid()) {
                invalidRecords.incrementAndGet();
                return volumeValidation;
            }
            
            // Timestamp validation
            var timestampValidation = validateTimestamp(data);
            if (!timestampValidation.isValid()) {
                invalidRecords.incrementAndGet();
                return timestampValidation;
            }
            
            // Anomaly detection
            var anomalyValidation = detectAnomalies(data);
            if (!anomalyValidation.isValid()) {
                anomaliesDetected.incrementAndGet();
                log.warn("Anomaly detected for {}:{}: {}", 
                    data.symbol(), data.exchange(), anomalyValidation.errorMessage());
                // Don't reject anomalies, just log them
            }
            
            // Update price history for future validations
            updatePriceHistory(data);
            
            validRecords.incrementAndGet();
            return ValidationResult.valid();
            
        } catch (Exception e) {
            log.error("Error during validation for {}:{}: {}", 
                data.symbol(), data.exchange(), e.getMessage());
            invalidRecords.incrementAndGet();
            return ValidationResult.invalid("Validation error: " + e.getMessage());
        }
    }

    /**
     * Validate basic required fields
     */
    private ValidationResult validateBasicFields(MarketDataPoint data) {
        if (data.symbol() == null || data.symbol().trim().isEmpty()) {
            return ValidationResult.invalid("Symbol is required");
        }
        
        if (data.exchange() == null || data.exchange().trim().isEmpty()) {
            return ValidationResult.invalid("Exchange is required");
        }
        
        if (data.timestamp() == null) {
            return ValidationResult.invalid("Timestamp is required");
        }
        
        if (data.price() == null || data.price().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Valid price is required");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate price data
     */
    private ValidationResult validatePrice(MarketDataPoint data) {
        BigDecimal price = data.price();
        
        // Price must be positive
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Price must be positive");
        }
        
        // Price scale validation (max 4 decimal places)
        if (price.scale() > 4) {
            return ValidationResult.invalid("Price precision exceeds maximum (4 decimal places)");
        }
        
        // Reasonable price range (0.01 to 100,000)
        if (price.compareTo(BigDecimal.valueOf(0.01)) < 0 || 
            price.compareTo(BigDecimal.valueOf(100000)) > 0) {
            return ValidationResult.invalid("Price outside reasonable range (0.01 - 100,000)");
        }
        
        // Order book validation
        if (data.hasOrderBookData()) {
            if (data.bid() == null || data.ask() == null) {
                return ValidationResult.invalid("Incomplete order book data");
            }
            
            if (data.bid().compareTo(data.ask()) >= 0) {
                return ValidationResult.invalid("Invalid order book: bid >= ask");
            }
            
            // Spread validation (max 10% spread)
            BigDecimal spread = data.ask().subtract(data.bid());
            BigDecimal spreadPercent = spread.divide(data.bid(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            if (spreadPercent.compareTo(BigDecimal.valueOf(10)) > 0) {
                return ValidationResult.invalid("Excessive spread: " + spreadPercent + "%");
            }
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate volume data
     */
    private ValidationResult validateVolume(MarketDataPoint data) {
        if (data.volume() != null) {
            if (data.volume() < 0) {
                return ValidationResult.invalid("Volume cannot be negative");
            }
            
            // Reasonable volume range (0 to 100M)
            if (data.volume() > 100_000_000L) {
                return ValidationResult.invalid("Volume exceeds reasonable limit (100M)");
            }
        }
        
        // Order book size validation
        if (data.bidSize() != null && data.bidSize() < 0) {
            return ValidationResult.invalid("Bid size cannot be negative");
        }
        
        if (data.askSize() != null && data.askSize() < 0) {
            return ValidationResult.invalid("Ask size cannot be negative");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate timestamp freshness
     */
    private ValidationResult validateTimestamp(MarketDataPoint data) {
        Instant now = Instant.now();
        Instant dataTime = data.timestamp();
        
        // Data cannot be from the future (with 1-minute tolerance for clock skew)
        if (dataTime.isAfter(now.plus(1, ChronoUnit.MINUTES))) {
            return ValidationResult.invalid("Data timestamp is in the future");
        }
        
        // Data cannot be too old (reject data older than 1 hour for tick data)
        if ("TICK".equals(data.dataType()) && 
            dataTime.isBefore(now.minus(1, ChronoUnit.HOURS))) {
            return ValidationResult.invalid("Tick data is too old (> 1 hour)");
        }
        
        // Historical data can be older, but validate reasonable limits
        if (dataTime.isBefore(now.minus(10, ChronoUnit.DAYS)) && 
            !"OHLC".equals(data.dataType())) {
            return ValidationResult.invalid("Data is unreasonably old (> 10 days)");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Detect anomalies in market data
     */
    private ValidationResult detectAnomalies(MarketDataPoint data) {
        String key = data.symbol() + ":" + data.exchange();
        PriceHistory history = priceHistory.get(key);
        
        if (history == null) {
            // First data point, no anomaly detection possible
            return ValidationResult.valid();
        }
        
        // Price movement validation
        if (history.lastPrice != null) {
            BigDecimal priceChange = data.price().subtract(history.lastPrice);
            BigDecimal changePercent = priceChange.divide(history.lastPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            // Circuit breaker check (20% price movement)
            if (changePercent.abs().compareTo(BigDecimal.valueOf(20)) > 0) {
                return ValidationResult.invalid("Price movement exceeds circuit breaker limit: " + 
                    changePercent + "%");
            }
            
            // Suspicious price movement (> 10% in single tick)
            if (changePercent.abs().compareTo(BigDecimal.valueOf(10)) > 0) {
                log.warn("Suspicious price movement for {}:{}: {}% change", 
                    data.symbol(), data.exchange(), changePercent);
                // Don't reject, just flag as anomaly
            }
        }
        
        // Volume spike detection
        if (data.volume() != null && history.avgVolume > 0) {
            double volumeRatio = (double) data.volume() / history.avgVolume;
            if (volumeRatio > 50.0) { // 50x average volume
                log.warn("Volume spike detected for {}:{}: {}x average volume", 
                    data.symbol(), data.exchange(), volumeRatio);
            }
        }
        
        // Time gap detection
        if (history.lastTimestamp != null) {
            long timeGapSeconds = ChronoUnit.SECONDS.between(history.lastTimestamp, data.timestamp());
            if (timeGapSeconds > 3600) { // > 1 hour gap
                log.warn("Large time gap detected for {}:{}: {} seconds", 
                    data.symbol(), data.exchange(), timeGapSeconds);
            }
        }
        
        return ValidationResult.valid();
    }

    /**
     * Update price history for symbol
     */
    private void updatePriceHistory(MarketDataPoint data) {
        String key = data.symbol() + ":" + data.exchange();
        
        priceHistory.compute(key, (k, existing) -> {
            if (existing == null) {
                existing = new PriceHistory();
            }
            
            existing.lastPrice = data.price();
            existing.lastTimestamp = data.timestamp();
            
            if (data.volume() != null) {
                // Update rolling average volume (simple exponential smoothing)
                if (existing.avgVolume == 0) {
                    existing.avgVolume = data.volume();
                } else {
                    existing.avgVolume = (long) (0.95 * existing.avgVolume + 0.05 * data.volume());
                }
            }
            
            existing.updateCount++;
            return existing;
        });
    }

    /**
     * Get validation statistics
     */
    public ValidationStats getValidationStats() {
        long totalRecords = validRecords.get() + invalidRecords.get();
        double validationRate = totalRecords > 0 ? 
            (double) validRecords.get() / totalRecords * 100 : 0.0;
        
        return new ValidationStats(
            validRecords.get(),
            invalidRecords.get(),
            anomaliesDetected.get(),
            validationRate,
            Instant.now()
        );
    }

    /**
     * Clear price history (for testing or memory management)
     */
    public void clearPriceHistory() {
        priceHistory.clear();
        log.info("Price history cleared");
    }

    /**
     * Price history tracking
     */
    private static class PriceHistory {
        BigDecimal lastPrice;
        Instant lastTimestamp;
        long avgVolume;
        int updateCount;
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String errorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validation statistics
     */
    public record ValidationStats(
        long validRecords,
        long invalidRecords,
        long anomaliesDetected,
        double validationRate,
        Instant timestamp
    ) {
        public boolean isHealthy() {
            return validationRate > 95.0; // > 95% validation rate considered healthy
        }
    }
}