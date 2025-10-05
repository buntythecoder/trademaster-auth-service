package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MFASetupMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String userId;
    private String mfaType;
    private Long timestamp;
}