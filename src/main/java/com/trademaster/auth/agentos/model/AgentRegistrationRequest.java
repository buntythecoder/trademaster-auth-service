package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Builder
@Data
public class AgentRegistrationRequest {
    private String orchestratorId;
    private String requestId;
    private Long timestamp;
    private Map<String, Object> orchestratorConfig;
}
