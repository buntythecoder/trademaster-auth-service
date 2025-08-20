package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.dto.DocumentResponse;
import com.trademaster.userprofile.dto.DocumentUploadRequest;
import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.VerificationStatus;
import com.trademaster.userprofile.security.JwtUserDetails;
import com.trademaster.userprofile.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Management", description = "APIs for managing user documents, uploads, and verification")
@SecurityRequirement(name = "Bearer Authentication")
public class DocumentController {
    
    private final DocumentService documentService;
    
    // User document endpoints - /me/* paths for self-service
    
    @Operation(
        summary = "Get current user's documents",
        description = "Retrieve all documents uploaded by the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @GetMapping("/me")
    public ResponseEntity<List<DocumentResponse>> getCurrentUserDocuments(
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        
        UUID userId = userDetails.getUserId();
        log.info("Retrieving documents for user: {}", userId);
        
        List<DocumentResponse> documents = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(documents);
    }
    
    @Operation(
        summary = "Upload document",
        description = "Upload a document for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or request", content = @Content()),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @PostMapping(value = "/me/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        
        UUID userId = userDetails.getUserId();
        log.info("Uploading document for user: {}, type: {}", userId, documentType);
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
            .file(file)
            .documentType(documentType)
            .description(description)
            .build();
        
        DocumentResponse response = documentService.uploadDocument(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "Get document by ID",
        description = "Retrieve a specific document belonging to the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content()),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @GetMapping("/me/{documentId}")
    public ResponseEntity<DocumentResponse> getCurrentUserDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID documentId,
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        
        UUID userId = userDetails.getUserId();
        log.info("User {} retrieving document: {}", userId, documentId);
        
        DocumentResponse document = documentService.getDocument(userId, documentId);
        return ResponseEntity.ok(document);
    }
    
    @Operation(
        summary = "Delete document",
        description = "Delete a document belonging to the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content()),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @DeleteMapping("/me/{documentId}")
    public ResponseEntity<Void> deleteCurrentUserDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID documentId,
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        
        UUID userId = userDetails.getUserId();
        log.info("User {} deleting document: {}", userId, documentId);
        
        documentService.deleteDocument(userId, documentId);
        return ResponseEntity.noContent().build();
    }
    
    // Admin/Compliance endpoints
    
    @Operation(
        summary = "Verify document (Admin/Compliance)",
        description = "Verify a document's authenticity and update its verification status"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document verification updated"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges", content = @Content())
    })
    @PatchMapping("/{documentId}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<DocumentResponse> verifyDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID documentId,
            @RequestParam VerificationStatus status,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        
        UUID verifierId = userDetails.getUserId();
        log.info("User {} verifying document {} with status {}", verifierId, documentId, status);
        
        DocumentResponse response = documentService.verifyDocument(documentId, status, comments);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get pending verification documents (Admin/Compliance)",
        description = "Retrieve documents that are pending verification"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pending documents retrieved"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges", content = @Content())
    })
    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DocumentResponse>> getPendingVerificationDocuments() {
        
        log.info("Retrieving documents pending verification");
        
        // TODO: Implement pending verification documents query
        List<DocumentResponse> documents = List.of();
        return ResponseEntity.ok(documents);
    }
    
    @Operation(
        summary = "Get documents by user (Admin/Support)",
        description = "Retrieve all documents for a specific user - Admin/Support only"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User documents retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges", content = @Content())
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT_AGENT') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        
        log.info("Admin retrieving documents for user: {}", userId);
        
        List<DocumentResponse> documents = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(documents);
    }
    
    @Operation(
        summary = "Get document verification history (Admin/Compliance)",
        description = "Retrieve the verification history for a document"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification history retrieved"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges", content = @Content())
    })
    @GetMapping("/{documentId}/verification-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DocumentResponse.VerificationHistoryEntry>> getDocumentVerificationHistory(
            @Parameter(description = "Document UUID") @PathVariable UUID documentId) {
        
        log.info("Retrieving verification history for document: {}", documentId);
        
        // TODO: Implement verification history query
        List<DocumentResponse.VerificationHistoryEntry> history = List.of();
        return ResponseEntity.ok(history);
    }
}