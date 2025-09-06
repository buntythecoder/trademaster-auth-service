package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.UserDocument;
import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.VerificationStatus;
import com.trademaster.userprofile.service.UserDocumentService;
import com.trademaster.userprofile.service.ProfileAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Functional User Document REST Controller
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Structured Logging & Monitoring - Rule #15
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "${trademaster.cors.allowed-origins:http://localhost:3000}")
public class UserDocumentController {
    
    private final UserDocumentService userDocumentService;
    private final ProfileAuditService profileAuditService;
    
    // ========== QUERY OPERATIONS ==========
    
    /**
     * Get documents for user with functional error handling
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> getUserDocuments(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting documents for user: {} [correlation: {}]", userId, correlationId);
        
        return userDocumentService.findByUserProfileId(userId)
            .map(documents -> documents.stream().map(this::mapToDocumentResponse).toList())
            .fold(
                documents -> {
                    logAuditEvent("DOCUMENT_VIEW", userId, authentication, request, correlationId);
                    return ResponseEntity.ok(Map.of(
                        "documents", documents,
                        "count", documents.size(),
                        "correlationId", correlationId
                    ));
                },
                error -> handleDocumentError(error)
            );
    }
    
    /**
     * Get specific document by ID
     */
    @GetMapping("/{documentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getDocument(
            @PathVariable UUID documentId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting document: {} [correlation: {}]", documentId, correlationId);
        
        return userDocumentService.findById(documentId)
            .map(this::mapToDocumentResponse)
            .fold(
                document -> {
                    logAuditEvent("DOCUMENT_VIEW", documentId, authentication, request, correlationId);
                    return ResponseEntity.ok(document);
                },
                error -> handleDocumentError(error)
            );
    }
    
    /**
     * Get documents by verification status (admin only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDocumentsByStatus(@PathVariable VerificationStatus status) {
        
        return userDocumentService.findByVerificationStatus(status)
            .map(documents -> documents.stream().map(this::mapToDocumentResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get documents pending verification (admin only)
     */
    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingDocuments() {
        
        return userDocumentService.findPendingVerification()
            .map(documents -> documents.stream().map(this::mapToDocumentResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    // ========== COMMAND OPERATIONS ==========
    
    /**
     * Upload document with functional validation
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Uploading document for user: {} type: {} [correlation: {}]", 
            userId, documentType, correlationId);
        
        return userDocumentService.uploadDocument(userId, documentType, file, description)
            .map(this::mapToDocumentResponse)
            .fold(
                document -> {
                    logAuditEvent("DOCUMENT_UPLOAD", userId, authentication, request, correlationId);
                    return ResponseEntity.status(201).body(Map.of(
                        "document", document,
                        "message", "Document uploaded successfully",
                        "correlationId", correlationId
                    ));
                },
                error -> handleDocumentError(error)
            );
    }
    
    /**
     * Update document verification status (admin only)
     */
    @PutMapping("/{documentId}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateVerificationStatus(
            @PathVariable UUID documentId,
            @Valid @RequestBody VerificationUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating verification for document: {} status: {} [correlation: {}]", 
            documentId, request.status(), correlationId);
        
        return userDocumentService.updateVerificationStatus(
                documentId, 
                request.status(), 
                request.verifierNotes()
            )
            .map(this::mapToDocumentResponse)
            .fold(
                document -> {
                    logAuditEvent("DOCUMENT_VERIFY", documentId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(Map.of(
                        "document", document,
                        "message", "Verification status updated",
                        "correlationId", correlationId
                    ));
                },
                error -> handleDocumentError(error)
            );
    }
    
    /**
     * Replace existing document
     */
    @PutMapping("/{documentId}/replace")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> replaceDocument(
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Replacing document: {} [correlation: {}]", documentId, correlationId);
        
        return userDocumentService.replaceDocument(documentId, file, description)
            .map(this::mapToDocumentResponse)
            .fold(
                document -> {
                    logAuditEvent("DOCUMENT_REPLACE", documentId, authentication, request, correlationId);
                    return ResponseEntity.ok(Map.of(
                        "document", document,
                        "message", "Document replaced successfully",
                        "correlationId", correlationId
                    ));
                },
                error -> handleDocumentError(error)
            );
    }
    
    /**
     * Delete document
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Deleting document: {} [correlation: {}]", documentId, correlationId);
        
        return userDocumentService.deleteDocument(documentId)
            .fold(
                success -> {
                    logAuditEvent("DOCUMENT_DELETE", documentId, authentication, request, correlationId);
                    return ResponseEntity.ok(Map.of(
                        "message", "Document deleted successfully",
                        "correlationId", correlationId
                    ));
                },
                error -> handleDocumentError(error)
            );
    }
    
    // ========== ASYNCHRONOUS OPERATIONS ==========
    
    /**
     * Async batch document upload
     */
    @PostMapping("/batch-upload")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<?>> batchUploadDocuments(
            @Valid @RequestBody BatchUploadRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Batch uploading {} documents [correlation: {}]", 
            request.documents().size(), correlationId);
        
        return userDocumentService.batchUploadDocuments(
                request.userId(),
                request.documents()
            )
            .thenApply(result -> result
                .map(documents -> Map.of(
                    "uploaded", documents.size(),
                    "requested", request.documents().size(),
                    "documents", documents.stream().map(this::mapToDocumentResponse).toList(),
                    "correlationId", correlationId
                ))
                .fold(
                    ResponseEntity::ok,
                    error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
                ));
    }
    
    /**
     * Async batch verification processing (admin only)
     */
    @PostMapping("/batch-verify")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> batchVerifyDocuments(
            @Valid @RequestBody BatchVerificationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Batch verifying {} documents [correlation: {}]", 
            request.verifications().size(), correlationId);
        
        return userDocumentService.batchVerifyDocuments(request.verifications())
            .thenApply(result -> result
                .map(documents -> Map.of(
                    "verified", documents.size(),
                    "requested", request.verifications().size(),
                    "correlationId", correlationId
                ))
                .fold(
                    ResponseEntity::ok,
                    error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
                ));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Map UserDocument to response DTO
     */
    private DocumentResponse mapToDocumentResponse(UserDocument document) {
        return new DocumentResponse(
            document.getId(),
            document.getUserId(),
            document.getDocumentType(),
            document.getFileName(),
            document.getFileSize(),
            document.getContentType(),
            maskSensitiveStoragePath(document.getStoragePath()),
            document.getDescription(),
            document.getVerificationStatus(),
            document.getVerifierNotes(),
            document.getUploadTimestamp(),
            document.getVerificationTimestamp(),
            document.getVersion(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
    
    /**
     * Mask sensitive storage path information
     */
    private String maskSensitiveStoragePath(String storagePath) {
        return Optional.ofNullable(storagePath)
            .map(path -> {
                String[] pathParts = path.split("/");
                return pathParts.length > 0 ? ".../" + pathParts[pathParts.length - 1] : path;
            })
            .orElse(null);
    }
    
    /**
     * Handle document errors with functional pattern matching
     */
    private ResponseEntity<?> handleDocumentError(UserDocumentService.DocumentError error) {
        return switch (error) {
            case UserDocumentService.DocumentNotFoundError notFound -> 
                ResponseEntity.notFound().build();
            case UserDocumentService.ValidationError validation -> 
                ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", validation.message(),
                    "details", validation.details()
                ));
            case UserDocumentService.StorageError storage -> 
                ResponseEntity.status(507).body(Map.of(
                    "error", "Storage error",
                    "message", "Unable to process document"
                ));
            case UserDocumentService.SecurityError security -> 
                ResponseEntity.status(403).body(Map.of(
                    "error", "Security violation",
                    "message", "Document access denied"
                ));
            case UserDocumentService.SystemError system -> 
                ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", "Please contact support"
                ));
        };
    }
    
    /**
     * Log audit event with security context
     */
    private void logAuditEvent(
            String eventType,
            Object entityId,
            Authentication authentication,
            HttpServletRequest request,
            String correlationId) {
        
        try {
            log.info("Audit event: {} for entity: {} by: {} [correlation: {}]", 
                eventType, entityId, authentication.getName(), correlationId);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }
    
    // ========== REQUEST/RESPONSE DTOs ==========
    
    /**
     * Verification update request record
     */
    public record VerificationUpdateRequest(
        @NotNull VerificationStatus status,
        String verifierNotes
    ) {}
    
    /**
     * Batch upload request record
     */
    public record BatchUploadRequest(
        @NotNull UUID userId,
        @NotNull List<DocumentUploadInfo> documents
    ) {}
    
    /**
     * Document upload info record
     */
    public record DocumentUploadInfo(
        @NotNull DocumentType documentType,
        @NotNull String fileName,
        @NotNull byte[] fileData,
        String description
    ) {}
    
    /**
     * Batch verification request record
     */
    public record BatchVerificationRequest(
        @NotNull List<VerificationInfo> verifications
    ) {}
    
    /**
     * Verification info record
     */
    public record VerificationInfo(
        @NotNull UUID documentId,
        @NotNull VerificationStatus status,
        String verifierNotes
    ) {}
    
    /**
     * Document response record
     */
    public record DocumentResponse(
        UUID id,
        UUID userId,
        DocumentType documentType,
        String fileName,
        Long fileSize,
        String contentType,
        String storagePath,
        String description,
        VerificationStatus verificationStatus,
        String verifierNotes,
        java.time.LocalDateTime uploadTimestamp,
        java.time.LocalDateTime verificationTimestamp,
        Integer version,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}
}