package com.trademaster.userprofile.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event class for profile-related events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileEvent {
    
    private String eventId;
    private String eventType;
    private UUID userId;
    private UUID profileId;
    private LocalDateTime timestamp;
    private String source;
    private Object data;
    
    public static ProfileEvent profileCreated(UUID userId, UUID profileId, Object profileData) {
        return ProfileEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("PROFILE_CREATED")
            .userId(userId)
            .profileId(profileId)
            .timestamp(LocalDateTime.now())
            .source("user-profile-service")
            .data(profileData)
            .build();
    }
    
    public static ProfileEvent profileUpdated(UUID userId, UUID profileId, Object profileData) {
        return ProfileEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("PROFILE_UPDATED")
            .userId(userId)
            .profileId(profileId)
            .timestamp(LocalDateTime.now())
            .source("user-profile-service")
            .data(profileData)
            .build();
    }
    
    public static ProfileEvent profileDeleted(UUID userId, UUID profileId) {
        return ProfileEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("PROFILE_DELETED")
            .userId(userId)
            .profileId(profileId)
            .timestamp(LocalDateTime.now())
            .source("user-profile-service")
            .build();
    }
}