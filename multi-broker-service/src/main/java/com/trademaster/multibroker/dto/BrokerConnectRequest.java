package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;

/**
 * Broker Connect Request DTO
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 */
@Builder
public record BrokerConnectRequest(
    BrokerType brokerType,
    String redirectUri,
    String state,
    String scope,
    Boolean sandboxMode
) {
    
    public BrokerType getBrokerType() {
        return brokerType;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public String getState() {
        return state;
    }
    
    public String getScope() {
        return scope;
    }
    
    public Boolean getSandboxMode() {
        return sandboxMode;
    }
    
    public boolean isSandboxMode() {
        return Boolean.TRUE.equals(sandboxMode);
    }
}