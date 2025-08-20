package com.trademaster.userprofile.dto;

import com.trademaster.userprofile.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class DocumentUploadRequest {
    
    @NotNull(message = "File is required")
    private MultipartFile file;
    
    @NotNull(message = "Document type is required")
    private DocumentType documentType;
    
    private String description;
}