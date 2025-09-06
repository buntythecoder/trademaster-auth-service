package com.trademaster.agentos.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * System Event
 * 
 * Represents system-level events in the Agent OS framework.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SystemEvent extends BaseEvent {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("severity")
    private String severity;
    
    /**
     * Create a system event
     */
    public static SystemEvent create(String eventType, String message) {
        return SystemEvent.builder()
            .eventId(generateEventId())
            .eventType(eventType)
            .message(message)
            .severity("INFO")
            .timestamp(Instant.now())
            .sourceService("agent-orchestration-service")
            .build();
    }
    
    /**
     * Create a system event with severity
     */
    public static SystemEvent create(String eventType, String message, String severity) {
        return SystemEvent.builder()
            .eventId(generateEventId())
            .eventType(eventType)
            .message(message)
            .severity(severity)
            .timestamp(Instant.now())
            .sourceService("agent-orchestration-service")
            .build();
    }
}