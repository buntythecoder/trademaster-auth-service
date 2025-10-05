package com.trademaster.auth.agentos.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MFASetupMCPResponse {
    private String requestId;
    private String status;
    private String result;
    private String errorMessage;
    private Long processingTimeMs;
}