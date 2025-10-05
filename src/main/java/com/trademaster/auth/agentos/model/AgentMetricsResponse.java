package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Builder
@Data
public class AgentMetricsResponse {
    private String agentId;
    private Double healthScore;
    private Map<String, String> capabilityMetrics;
    private String errorMessage;
    private Long timestamp;
}