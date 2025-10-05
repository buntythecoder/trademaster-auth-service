package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Builder
@Data
public class AgentRegistrationResponse {
    private String agentId;
    private String agentType;
    private List<String> capabilities;
    private Double healthScore;
    private String status;
    private String errorMessage;
    private Long registrationTime;
}