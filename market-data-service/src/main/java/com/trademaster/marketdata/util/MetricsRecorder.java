package com.trademaster.marketdata.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Fluent metrics recording utility using Builder pattern
 * 
 * This class eliminates repetitive MeterRegistry.counter().increment() 
 * and MeterRegistry.timer().record() patterns by providing a clean,
 * type-safe fluent interface for metrics recording.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MetricsRecorder {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Start building a counter metric
     */
    public CounterBuilder counter(String name) {
        return new CounterBuilder(meterRegistry, name);
    }
    
    /**
     * Start building a timer metric
     */
    public TimerBuilder timer(String name) {
        return new TimerBuilder(meterRegistry, name);
    }
    
    /**
     * Start building a gauge metric
     */
    public GaugeBuilder gauge(String name) {
        return new GaugeBuilder(meterRegistry, name);
    }
    
    /**
     * Builder for counter metrics
     */
    public static class CounterBuilder {
        private final MeterRegistry meterRegistry;
        private final String name;
        private Tags tags = Tags.empty();
        
        private CounterBuilder(MeterRegistry meterRegistry, String name) {
            this.meterRegistry = meterRegistry;
            this.name = name;
        }
        
        /**
         * Add a tag to the counter
         */
        public CounterBuilder tag(String key, String value) {
            this.tags = tags.and(key, value);
            return this;
        }
        
        /**
         * Add multiple tags to the counter
         */
        public CounterBuilder tags(String... keyValues) {
            this.tags = tags.and(keyValues);
            return this;
        }
        
        /**
         * Add symbol tag
         */
        public CounterBuilder symbol(String symbol) {
            return tag("symbol", symbol);
        }
        
        /**
         * Add exchange tag
         */
        public CounterBuilder exchange(String exchange) {
            return tag("exchange", exchange);
        }
        
        /**
         * Add operation tag
         */
        public CounterBuilder operation(String operation) {
            return tag("operation", operation);
        }
        
        /**
         * Add status tag
         */
        public CounterBuilder status(String status) {
            return tag("status", status);
        }
        
        /**
         * Add error type tag
         */
        public CounterBuilder errorType(String errorType) {
            return tag("error_type", errorType);
        }
        
        /**
         * Add provider tag
         */
        public CounterBuilder provider(String provider) {
            return tag("provider", provider);
        }
        
        /**
         * Increment the counter by 1
         */
        public void increment() {
            meterRegistry.counter(name, tags).increment();
        }
        
        /**
         * Increment the counter by specified amount
         */
        public void increment(double amount) {
            meterRegistry.counter(name, tags).increment(amount);
        }
    }
    
    /**
     * Builder for timer metrics
     */
    public static class TimerBuilder {
        private final MeterRegistry meterRegistry;
        private final String name;
        private Tags tags = Tags.empty();
        
        private TimerBuilder(MeterRegistry meterRegistry, String name) {
            this.meterRegistry = meterRegistry;
            this.name = name;
        }
        
        /**
         * Add a tag to the timer
         */
        public TimerBuilder tag(String key, String value) {
            this.tags = tags.and(key, value);
            return this;
        }
        
        /**
         * Add multiple tags to the timer
         */
        public TimerBuilder tags(String... keyValues) {
            this.tags = tags.and(keyValues);
            return this;
        }
        
        /**
         * Add operation tag
         */
        public TimerBuilder operation(String operation) {
            return tag("operation", operation);
        }
        
        /**
         * Add success tag
         */
        public TimerBuilder success(boolean success) {
            return tag("success", String.valueOf(success));
        }
        
        /**
         * Add endpoint tag
         */
        public TimerBuilder endpoint(String endpoint) {
            return tag("endpoint", endpoint);
        }
        
        /**
         * Add method tag
         */
        public TimerBuilder method(String method) {
            return tag("method", method);
        }
        
        /**
         * Record time in milliseconds
         */
        public void record(long durationMs) {
            meterRegistry.timer(name, tags).record(durationMs, TimeUnit.MILLISECONDS);
        }
        
        /**
         * Record time with custom TimeUnit
         */
        public void record(long duration, TimeUnit timeUnit) {
            meterRegistry.timer(name, tags).record(duration, timeUnit);
        }
    }
    
    /**
     * Builder for gauge metrics
     */
    public static class GaugeBuilder {
        private final MeterRegistry meterRegistry;
        private final String name;
        private Tags tags = Tags.empty();
        
        private GaugeBuilder(MeterRegistry meterRegistry, String name) {
            this.meterRegistry = meterRegistry;
            this.name = name;
        }
        
        /**
         * Add a tag to the gauge
         */
        public GaugeBuilder tag(String key, String value) {
            this.tags = tags.and(key, value);
            return this;
        }
        
        /**
         * Register a gauge with a supplier
         */
        public <T> void register(T obj, java.util.function.ToDoubleFunction<T> valueFunction) {
            io.micrometer.core.instrument.Gauge.builder(name, obj, valueFunction)
                .tags(tags)
                .register(meterRegistry);
        }
    }
}