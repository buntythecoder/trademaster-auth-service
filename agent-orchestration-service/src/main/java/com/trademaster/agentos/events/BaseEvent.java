package com.trademaster.agentos.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base Event
 * 
 * Base class for all events in the Agent OS system.
 * Provides common event properties and utilities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("sourceService")
    private String sourceService;
    
    /**
     * Generate a unique event ID
     */
    protected static String generateEventId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Check if event is valid
     */
    public boolean isValid() {
        return eventId != null && 
               eventType != null && 
               timestamp != null && 
               sourceService != null;
    }
}