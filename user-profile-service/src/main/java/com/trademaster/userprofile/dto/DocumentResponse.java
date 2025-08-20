package com.trademaster.userprofile.dto;

import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    
    private UUID id;
    private UUID userId;
    private DocumentType documentType;
    private String fileName;
    private String originalFileName;
    private String mimeType;
    private long fileSize;
    private String fileSizeFormatted;
    private String description;
    private VerificationStatus verificationStatus;
    private String verificationComments;
    private UUID verifiedBy;
    private String verifierName;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Nested class for verification history
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationHistoryEntry {
        private UUID verifierId;
        private String verifierName;
        private VerificationStatus status;
        private String comments;
        private LocalDateTime verifiedAt;
    }
}