package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Builder
@Data
public class AgentHealthResponse {
    private String agentId;
    private Double healthScore;
    private String status;
    private Map<String, String> capabilityHealth;
    private String errorMessage;
    private Long lastUpdated;
}