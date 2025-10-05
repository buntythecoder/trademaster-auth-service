package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Builder
@Data
public class AgentCapabilitiesResponse {
    private String agentId;
    private List<String> capabilities;
    private Map<String, Map<String, Object>> capabilityDetails;
    private Integer totalCapabilities;
    private String errorMessage;
}