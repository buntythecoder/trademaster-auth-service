package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Technical Analysis Service
 * 
 * Provides comprehensive technical indicator calculations including
 * trend indicators, momentum oscillators, volatility measures,
 * and volume indicators.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class TechnicalAnalysisService {
    
    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    
    /**
     * Calculate all available technical indicators
     */
    public Map<String, BigDecimal> calculateAllIndicators(List<MarketDataPoint> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }
        
        Map<String, BigDecimal> indicators = new HashMap<>();
        
        try {
            // Sort data by timestamp
            List<MarketDataPoint> sortedData = data.stream()
                .sorted(Comparator.comparing(MarketDataPoint::timestamp))
                .toList();
            
            // Momentum Indicators
            indicators.putAll(calculateMomentumIndicators(sortedData));
            
            // Trend Indicators
            indicators.putAll(calculateTrendIndicators(sortedData));
            
            // Volatility Indicators
            indicators.putAll(calculateVolatilityIndicators(sortedData));
            
            // Volume Indicators
            indicators.putAll(calculateVolumeIndicators(sortedData));
            
            log.debug("Calculated {} technical indicators for {} data points", 
                indicators.size(), data.size());
                
        } catch (Exception e) {
            log.error("Failed to calculate technical indicators: {}", e.getMessage(), e);
        }
        
        return indicators;
    }
    
    /**
     * Calculate momentum indicators (RSI, Stochastic, Williams %R, etc.)
     */
    public Map<String, BigDecimal> calculateMomentumIndicators(List<MarketDataPoint> data) {
        Map<String, BigDecimal> indicators = new HashMap<>();
        
        if (data.size() < 14) {
            return indicators; // Not enough data
        }
        
        // RSI (14-period)
        BigDecimal rsi = calculateRSI(data, 14);
        if (rsi != null) {
            indicators.put("RSI", rsi);
            indicators.put("RSI_14", rsi);
        }
        
        // Stochastic Oscillator
        var stochastic = calculateStochastic(data, 14, 3);
        if (stochastic.containsKey("%K")) {
            indicators.put("STOCH_K", stochastic.get("%K"));
        }
        if (stochastic.containsKey("%D")) {
            indicators.put("STOCH_D", stochastic.get("%D"));
        }
        
        // Williams %R
        BigDecimal williamsR = calculateWilliamsR(data, 14);
        if (williamsR != null) {
            indicators.put("WILLIAMS_R", williamsR);
        }
        
        // MACD
        var macd = calculateMACD(data, 12, 26, 9);
        indicators.putAll(macd);
        
        return indicators;
    }
    
    /**
     * Calculate trend indicators (SMA, EMA, MACD, etc.)
     */
    public Map<String, BigDecimal> calculateTrendIndicators(List<MarketDataPoint> data) {
        Map<String, BigDecimal> indicators = new HashMap<>();
        
        if (data.size() < 20) {
            return indicators;
        }
        
        // Simple Moving Averages
        indicators.put("SMA_10", calculateSMA(data, 10));
        indicators.put("SMA_20", calculateSMA(data, 20));
        indicators.put("SMA_50", calculateSMA(data, 50));
        
        if (data.size() >= 200) {
            indicators.put("SMA_200", calculateSMA(data, 200));
        }
        
        // Exponential Moving Averages
        indicators.put("EMA_12", calculateEMA(data, 12));
        indicators.put("EMA_26", calculateEMA(data, 26));
        
        // ADX (Average Directional Index)
        BigDecimal adx = calculateADX(data, 14);
        if (adx != null) {
            indicators.put("ADX", adx);
        }
        
        // Parabolic SAR
        BigDecimal psar = calculateParabolicSAR(data);
        if (psar != null) {
            indicators.put("PSAR", psar);
        }
        
        return indicators;
    }
    
    /**
     * Calculate volatility indicators (Bollinger Bands, ATR, etc.)
     */
    public Map<String, BigDecimal> calculateVolatilityIndicators(List<MarketDataPoint> data) {
        Map<String, BigDecimal> indicators = new HashMap<>();
        
        if (data.size() < 20) {
            return indicators;
        }
        
        // Bollinger Bands
        var bollingerBands = calculateBollingerBands(data, 20, new BigDecimal("2"));
        indicators.putAll(bollingerBands);
        
        // Average True Range
        BigDecimal atr = calculateATR(data, 14);
        if (atr != null) {
            indicators.put("ATR", atr);
        }
        
        // Standard Deviation
        BigDecimal stdDev = calculateStandardDeviation(data, 20);
        if (stdDev != null) {
            indicators.put("STDDEV", stdDev);
        }
        
        return indicators;
    }
    
    /**
     * Calculate volume indicators (OBV, VWAP, etc.)
     */
    public Map<String, BigDecimal> calculateVolumeIndicators(List<MarketDataPoint> data) {
        Map<String, BigDecimal> indicators = new HashMap<>();
        
        if (data.size() < 10) {
            return indicators;
        }
        
        // On Balance Volume
        BigDecimal obv = calculateOBV(data);
        if (obv != null) {
            indicators.put("OBV", obv);
        }
        
        // Volume Weighted Average Price
        BigDecimal vwap = calculateVWAP(data);
        if (vwap != null) {
            indicators.put("VWAP", vwap);
        }
        
        // Volume Rate of Change
        BigDecimal vroc = calculateVolumeROC(data, 10);
        if (vroc != null) {
            indicators.put("VROC", vroc);
        }
        
        return indicators;
    }
    
    /**
     * Calculate RSI (Relative Strength Index)
     */
    public BigDecimal calculateRSI(List<MarketDataPoint> data, int period) {
        if (data.size() <= period) {
            return null;
        }
        
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        // Calculate gains and losses
        for (int i = 1; i < data.size(); i++) {
            BigDecimal currentPrice = data.get(i).price();
            BigDecimal previousPrice = data.get(i - 1).price();
            
            if (currentPrice != null && previousPrice != null) {
                BigDecimal change = currentPrice.subtract(previousPrice);
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    gains.add(change);
                    losses.add(BigDecimal.ZERO);
                } else {
                    gains.add(BigDecimal.ZERO);
                    losses.add(change.abs());
                }
            }
        }
        
        if (gains.size() < period) {
            return null;
        }
        
        // Calculate average gain and loss for the first period
        BigDecimal avgGain = gains.subList(0, period).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(period), MC);
            
        BigDecimal avgLoss = losses.subList(0, period).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(period), MC);
        
        // Use smoothed averages for subsequent periods
        for (int i = period; i < gains.size(); i++) {
            avgGain = avgGain.multiply(new BigDecimal(period - 1))
                .add(gains.get(i))
                .divide(new BigDecimal(period), MC);
                
            avgLoss = avgLoss.multiply(new BigDecimal(period - 1))
                .add(losses.get(i))
                .divide(new BigDecimal(period), MC);
        }
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return HUNDRED; // Avoid division by zero
        }
        
        BigDecimal rs = avgGain.divide(avgLoss, MC);
        return HUNDRED.subtract(HUNDRED.divide(BigDecimal.ONE.add(rs), MC));
    }
    
    /**
     * Calculate Simple Moving Average
     */
    public BigDecimal calculateSMA(List<MarketDataPoint> data, int period) {
        if (data.size() < period) {
            return null;
        }
        
        BigDecimal sum = data.subList(data.size() - period, data.size()).stream()
            .map(MarketDataPoint::price)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return sum.divide(new BigDecimal(period), MC);
    }
    
    /**
     * Calculate Exponential Moving Average
     */
    public BigDecimal calculateEMA(List<MarketDataPoint> data, int period) {
        if (data.size() < period) {
            return null;
        }
        
        BigDecimal multiplier = TWO.divide(new BigDecimal(period + 1), MC);
        
        // Start with SMA for the first value
        BigDecimal ema = calculateSMA(data.subList(0, period), period);
        
        // Calculate EMA for remaining values
        for (int i = period; i < data.size(); i++) {
            BigDecimal price = data.get(i).price();
            if (price != null) {
                ema = price.subtract(ema).multiply(multiplier).add(ema);
            }
        }
        
        return ema;
    }
    
    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    public Map<String, BigDecimal> calculateMACD(List<MarketDataPoint> data, 
            int fastPeriod, int slowPeriod, int signalPeriod) {
        
        Map<String, BigDecimal> macd = new HashMap<>();
        
        if (data.size() < slowPeriod + signalPeriod) {
            return macd;
        }
        
        BigDecimal fastEMA = calculateEMA(data, fastPeriod);
        BigDecimal slowEMA = calculateEMA(data, slowPeriod);
        
        if (fastEMA != null && slowEMA != null) {
            BigDecimal macdLine = fastEMA.subtract(slowEMA);
            macd.put("MACD", macdLine);
            
            // Calculate signal line (EMA of MACD line)
            // This is simplified - in practice, you'd need historical MACD values
            BigDecimal signalLine = macdLine; // Placeholder
            macd.put("MACD_SIGNAL", signalLine);
            
            // Calculate histogram
            BigDecimal histogram = macdLine.subtract(signalLine);
            macd.put("MACD_HISTOGRAM", histogram);
        }
        
        return macd;
    }
    
    /**
     * Calculate Bollinger Bands
     */
    public Map<String, BigDecimal> calculateBollingerBands(List<MarketDataPoint> data, 
            int period, BigDecimal stdDevMultiplier) {
        
        Map<String, BigDecimal> bands = new HashMap<>();
        
        if (data.size() < period) {
            return bands;
        }
        
        BigDecimal sma = calculateSMA(data, period);
        BigDecimal stdDev = calculateStandardDeviation(data, period);
        
        if (sma != null && stdDev != null) {
            BigDecimal upperBand = sma.add(stdDev.multiply(stdDevMultiplier));
            BigDecimal lowerBand = sma.subtract(stdDev.multiply(stdDevMultiplier));
            
            bands.put("BB_UPPER", upperBand);
            bands.put("BB_MIDDLE", sma);
            bands.put("BB_LOWER", lowerBand);
            
            // Calculate %B (position within bands)
            BigDecimal currentPrice = data.get(data.size() - 1).price();
            if (currentPrice != null) {
                BigDecimal bandWidth = upperBand.subtract(lowerBand);
                if (bandWidth.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentB = currentPrice.subtract(lowerBand)
                        .divide(bandWidth, MC);
                    bands.put("BB_PERCENT_B", percentB);
                }
            }
        }
        
        return bands;
    }
    
    /**
     * Calculate Average True Range
     */
    public BigDecimal calculateATR(List<MarketDataPoint> data, int period) {
        if (data.size() < period + 1) {
            return null;
        }
        
        List<BigDecimal> trueRanges = new ArrayList<>();
        
        for (int i = 1; i < data.size(); i++) {
            MarketDataPoint current = data.get(i);
            MarketDataPoint previous = data.get(i - 1);
            
            if (current.high() != null && current.low() != null && 
                previous.price() != null) {
                
                BigDecimal tr1 = current.high().subtract(current.low());
                BigDecimal tr2 = current.high().subtract(previous.price()).abs();
                BigDecimal tr3 = current.low().subtract(previous.price()).abs();
                
                BigDecimal trueRange = tr1.max(tr2).max(tr3);
                trueRanges.add(trueRange);
            }
        }
        
        if (trueRanges.size() < period) {
            return null;
        }
        
        // Calculate average of the last 'period' true ranges
        return trueRanges.subList(trueRanges.size() - period, trueRanges.size())
            .stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(period), MC);
    }
    
    /**
     * Calculate Stochastic Oscillator
     */
    public Map<String, BigDecimal> calculateStochastic(List<MarketDataPoint> data, 
            int kPeriod, int dPeriod) {
        
        Map<String, BigDecimal> stochastic = new HashMap<>();
        
        if (data.size() < kPeriod) {
            return stochastic;
        }
        
        // Get the relevant data window
        List<MarketDataPoint> window = data.subList(data.size() - kPeriod, data.size());
        
        BigDecimal highestHigh = window.stream()
            .map(MarketDataPoint::high)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(null);
            
        BigDecimal lowestLow = window.stream()
            .map(MarketDataPoint::low)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);
            
        BigDecimal currentClose = data.get(data.size() - 1).price();
        
        if (highestHigh != null && lowestLow != null && currentClose != null) {
            BigDecimal range = highestHigh.subtract(lowestLow);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentK = currentClose.subtract(lowestLow)
                    .divide(range, MC)
                    .multiply(HUNDRED);
                    
                stochastic.put("%K", percentK);
                
                // %D is typically a 3-period SMA of %K
                // Simplified calculation using current %K
                stochastic.put("%D", percentK);
            }
        }
        
        return stochastic;
    }
    
    /**
     * Calculate Williams %R
     */
    public BigDecimal calculateWilliamsR(List<MarketDataPoint> data, int period) {
        if (data.size() < period) {
            return null;
        }
        
        List<MarketDataPoint> window = data.subList(data.size() - period, data.size());
        
        BigDecimal highestHigh = window.stream()
            .map(MarketDataPoint::high)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(null);
            
        BigDecimal lowestLow = window.stream()
            .map(MarketDataPoint::low)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);
            
        BigDecimal currentClose = data.get(data.size() - 1).price();
        
        if (highestHigh != null && lowestLow != null && currentClose != null) {
            BigDecimal range = highestHigh.subtract(lowestLow);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                return highestHigh.subtract(currentClose)
                    .divide(range, MC)
                    .multiply(new BigDecimal("-100"));
            }
        }
        
        return null;
    }
    
    /**
     * Calculate Standard Deviation
     */
    public BigDecimal calculateStandardDeviation(List<MarketDataPoint> data, int period) {
        if (data.size() < period) {
            return null;
        }
        
        List<BigDecimal> prices = data.subList(data.size() - period, data.size())
            .stream()
            .map(MarketDataPoint::price)
            .filter(Objects::nonNull)
            .toList();
            
        if (prices.size() < period) {
            return null;
        }
        
        BigDecimal mean = prices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(prices.size()), MC);
            
        BigDecimal variance = prices.stream()
            .map(price -> price.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(prices.size()), MC);
            
        // Calculate square root using Newton's method
        return sqrt(variance, MC);
    }
    
    /**
     * Calculate On Balance Volume
     */
    public BigDecimal calculateOBV(List<MarketDataPoint> data) {
        if (data.size() < 2) {
            return null;
        }
        
        BigDecimal obv = BigDecimal.ZERO;
        
        for (int i = 1; i < data.size(); i++) {
            MarketDataPoint current = data.get(i);
            MarketDataPoint previous = data.get(i - 1);
            
            if (current.price() != null && previous.price() != null && 
                current.volume() != null) {
                
                BigDecimal volume = new BigDecimal(current.volume());
                
                int priceComparison = current.price().compareTo(previous.price());
                if (priceComparison > 0) {
                    obv = obv.add(volume);
                } else if (priceComparison < 0) {
                    obv = obv.subtract(volume);
                }
                // If price is unchanged, volume doesn't affect OBV
            }
        }
        
        return obv;
    }
    
    /**
     * Calculate Volume Weighted Average Price
     */
    public BigDecimal calculateVWAP(List<MarketDataPoint> data) {
        if (data.isEmpty()) {
            return null;
        }
        
        BigDecimal totalVolumePrice = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        
        for (MarketDataPoint point : data) {
            if (point.price() != null && point.volume() != null) {
                BigDecimal volume = new BigDecimal(point.volume());
                BigDecimal typicalPrice = point.price(); // Simplified - could use (H+L+C)/3
                
                totalVolumePrice = totalVolumePrice.add(typicalPrice.multiply(volume));
                totalVolume = totalVolume.add(volume);
            }
        }
        
        if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
            return totalVolumePrice.divide(totalVolume, MC);
        }
        
        return null;
    }
    
    /**
     * Calculate Volume Rate of Change
     */
    public BigDecimal calculateVolumeROC(List<MarketDataPoint> data, int period) {
        if (data.size() < period + 1) {
            return null;
        }
        
        MarketDataPoint current = data.get(data.size() - 1);
        MarketDataPoint previous = data.get(data.size() - 1 - period);
        
        if (current.volume() != null && previous.volume() != null && 
            previous.volume() > 0) {
            
            BigDecimal currentVolume = new BigDecimal(current.volume());
            BigDecimal previousVolume = new BigDecimal(previous.volume());
            
            return currentVolume.subtract(previousVolume)
                .divide(previousVolume, MC)
                .multiply(HUNDRED);
        }
        
        return null;
    }
    
    // Additional helper methods
    
    private BigDecimal calculateADX(List<MarketDataPoint> data, int period) {
        // ADX calculation is complex - this is a placeholder
        return null;
    }
    
    private BigDecimal calculateParabolicSAR(List<MarketDataPoint> data) {
        // Parabolic SAR calculation - placeholder
        return null;
    }
    
    /**
     * Calculate square root using Newton's method
     */
    private BigDecimal sqrt(BigDecimal value, MathContext mc) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal x = value;
        BigDecimal prev;
        
        do {
            prev = x;
            x = x.add(value.divide(x, mc)).divide(TWO, mc);
        } while (x.subtract(prev).abs().compareTo(new BigDecimal("0.0001")) > 0);
        
        return x;
    }
    
    // AgentOS Integration Methods
    
    /**
     * Calculate technical indicators for multiple symbols (AgentOS compatibility)
     */
    public Object calculateIndicators(List<String> symbols, List<String> indicators) {
        log.info("Calculating indicators {} for symbols: {}", indicators, symbols);
        
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        for (String symbol : symbols) {
            Map<String, Object> symbolResults = new HashMap<>();
            
            for (String indicator : indicators) {
                switch (indicator.toUpperCase()) {
                    case "RSI":
                        symbolResults.put("RSI", 65.2); // Mock data
                        break;
                    case "MACD":
                        symbolResults.put("MACD", Map.of(
                            "macd", 0.15,
                            "signal", 0.12,
                            "histogram", 0.03
                        ));
                        break;
                    case "BOLLINGER_BANDS":
                        symbolResults.put("BOLLINGER_BANDS", Map.of(
                            "upper", 152.5,
                            "middle", 150.0,
                            "lower", 147.5
                        ));
                        break;
                    case "MOVING_AVERAGE":
                        symbolResults.put("MOVING_AVERAGE", Map.of(
                            "sma20", 149.8,
                            "sma50", 147.2,
                            "ema20", 150.1
                        ));
                        break;
                    default:
                        symbolResults.put(indicator, "Not implemented");
                }
            }
            
            results.put(symbol, symbolResults);
        }
        
        return results;
    }
}