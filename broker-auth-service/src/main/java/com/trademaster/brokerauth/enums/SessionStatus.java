package com.trademaster.brokerauth.enums;

/**
 * Broker Session Status
 * 
 * Represents the current status of a broker authentication session.
 * Used to track session lifecycle and determine appropriate actions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum SessionStatus {
    
    PENDING("Authentication initiated but not completed"),
    ACTIVE("Session is active and valid"),
    EXPIRED("Session has expired and needs renewal"),
    REVOKED("Session was manually revoked by user"),
    ERROR("Session is in error state"),
    RATE_LIMITED("Session is temporarily rate limited"),
    REFRESHING("Session token is being refreshed");
    
    private final String description;
    
    SessionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if session can be used for API calls
     */
    public boolean isUsable() {
        return this == ACTIVE;
    }
    
    /**
     * Check if session can be refreshed
     */
    public boolean canBeRefreshed() {
        return this == EXPIRED || this == ERROR;
    }
    
    /**
     * Check if session needs user intervention
     */
    public boolean needsUserIntervention() {
        return this == PENDING || this == REVOKED || 
               (this == ERROR && !canBeRefreshed());
    }
    
    /**
     * Check if session is in a final state (cannot transition)
     */
    public boolean isFinal() {
        return this == REVOKED;
    }
    
    /**
     * Get valid transitions from current status
     */
    public SessionStatus[] getValidTransitions() {
        return switch (this) {
            case PENDING -> new SessionStatus[]{ACTIVE, ERROR, REVOKED};
            case ACTIVE -> new SessionStatus[]{EXPIRED, REVOKED, ERROR, RATE_LIMITED, REFRESHING};
            case EXPIRED -> new SessionStatus[]{ACTIVE, ERROR, REVOKED, REFRESHING};
            case REVOKED -> new SessionStatus[]{}; // Final state
            case ERROR -> new SessionStatus[]{ACTIVE, REVOKED, REFRESHING};
            case RATE_LIMITED -> new SessionStatus[]{ACTIVE, EXPIRED, ERROR, REVOKED};
            case REFRESHING -> new SessionStatus[]{ACTIVE, ERROR, EXPIRED};
        };
    }
    
    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(SessionStatus targetStatus) {
        SessionStatus[] validTransitions = getValidTransitions();
        for (SessionStatus status : validTransitions) {
            if (status == targetStatus) {
                return true;
            }
        }
        return false;
    }
}