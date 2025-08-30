package com.trademaster.marketdata.dto;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder pattern for creating consistent API responses
 * 
 * This pattern eliminates repetitive Map.ofEntries() code and provides
 * a fluent interface for building complex API responses with proper
 * type safety and consistency.
 * 
 * @param <T> The type of data in paginated responses
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class ResponseBuilder<T> {
    
    private final Map<String, Object> data = new LinkedHashMap<>();
    
    private ResponseBuilder() {}
    
    /**
     * Create a new ResponseBuilder instance
     */
    public static <T> ResponseBuilder<T> create() {
        return new ResponseBuilder<>();
    }
    
    /**
     * Add a key-value pair to the response
     */
    public ResponseBuilder<T> add(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    /**
     * Add symbol information to the response
     */
    public ResponseBuilder<T> addSymbol(String symbol) {
        return add("symbol", symbol);
    }
    
    /**
     * Add timeframe information to the response
     */
    public ResponseBuilder<T> addTimeframe(Object timeframe) {
        return add("timeframe", timeframe);
    }
    
    /**
     * Add time range to the response
     */
    public ResponseBuilder<T> addTimeRange(Object startTime, Object endTime) {
        return add("startTime", startTime)
               .add("endTime", endTime);
    }
    
    /**
     * Add pagination information from a Spring Data Page
     */
    public ResponseBuilder<T> addPageInfo(Page<T> page) {
        return add("data", page.getContent())
               .add("page", page.getNumber())
               .add("size", page.getSize())
               .add("totalElements", page.getTotalElements())
               .add("totalPages", page.getTotalPages())
               .add("hasNext", page.hasNext())
               .add("hasPrevious", page.hasPrevious());
    }
    
    /**
     * Add success status
     */
    public ResponseBuilder<T> success() {
        return add("success", true);
    }
    
    /**
     * Add error information
     */
    public ResponseBuilder<T> error(String errorMessage) {
        return add("success", false)
               .add("errorMessage", errorMessage);
    }
    
    /**
     * Add processing metadata
     */
    public ResponseBuilder<T> addMetadata(long processingTimeMs, String requestId) {
        return add("processingTimeMs", processingTimeMs)
               .add("requestId", requestId)
               .add("timestamp", java.time.Instant.now());
    }
    
    /**
     * Build the final ResponseEntity with OK status
     */
    public ResponseEntity<Map<String, Object>> ok() {
        return ResponseEntity.ok(Collections.unmodifiableMap(data));
    }
    
    /**
     * Build the final ResponseEntity with specified status
     */
    public ResponseEntity<Map<String, Object>> build(org.springframework.http.HttpStatus status) {
        return ResponseEntity.status(status).body(Collections.unmodifiableMap(data));
    }
    
    /**
     * Get the raw data map (for testing purposes)
     */
    Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }
}