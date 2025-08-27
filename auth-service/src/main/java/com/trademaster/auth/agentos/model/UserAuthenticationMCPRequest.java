package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserAuthenticationMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String username;
    private String password;
    private String deviceId;
    private Long timestamp;
}
