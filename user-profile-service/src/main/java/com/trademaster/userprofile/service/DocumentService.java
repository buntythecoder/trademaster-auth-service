package com.trademaster.userprofile.service;

import com.trademaster.userprofile.dto.DocumentResponse;
import com.trademaster.userprofile.dto.DocumentUploadRequest;
import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.UserDocument;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.entity.VerificationStatus;
import com.trademaster.userprofile.event.ProfileEvent;
import com.trademaster.userprofile.exception.DocumentNotFoundException;
import com.trademaster.userprofile.exception.UnauthorizedAccessException;
import com.trademaster.userprofile.repository.UserDocumentRepository;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user documents and KYC verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocumentService {

    private final UserDocumentRepository documentRepository;
    private final UserProfileRepository profileRepository;
    private final FileStorageService fileStorageService;
    private final EventPublisher eventPublisher;

    /**
     * Upload a new document
     */
    @Transactional
    public DocumentResponse uploadDocument(UUID userId, DocumentUploadRequest request) {
        UserProfile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new DocumentNotFoundException("User profile not found: " + userId));

        // Check if user already has this document type (for mandatory docs)
        if (request.getDocumentType().isMandatory()) {
            documentRepository.findLatestByUserProfileAndType(userProfile.getId(), request.getDocumentType())
                    .ifPresent(existingDoc -> {
                        log.info("Replacing existing {} document for user {}", 
                                request.getDocumentType(), userId);
                        deleteDocument(userId, existingDoc.getId());
                    });
        }

        // Upload to MinIO and create document entity
        UserDocument document = fileStorageService.uploadDocument(
                request.getFile(), 
                request.getDocumentType(), 
                userProfile, 
                request.getDescription()
        );

        // Save document to database
        UserDocument savedDocument = documentRepository.save(document);
        
        // Publish document uploaded event
        ProfileEvent event = ProfileEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_UPLOADED")
                .userId(userId)
                .profileId(userProfile.getId())
                .timestamp(java.time.LocalDateTime.now())
                .source("user-profile-service")
                .data(savedDocument)
                .build();
        
        eventPublisher.publishDocumentUploaded(event);
        
        log.info("Document {} uploaded successfully for user {}", 
                savedDocument.getFileName(), userId);
        
        return convertToResponse(savedDocument);
    }

    /**
     * Get document by ID with authorization check
     */
    public DocumentResponse getDocument(UUID userId, UUID documentId) {
        UserDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        
        // Check if user owns this document
        if (!document.getUserProfile().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not have access to this document");
        }
        
        return convertToResponse(document);
    }

    /**
     * Get all documents for a user
     */
    public List<DocumentResponse> getUserDocuments(UUID userId) {
        UserProfile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new DocumentNotFoundException("User profile not found: " + userId));
        
        List<UserDocument> documents = documentRepository.findByUserProfileIdOrderByUploadedAtDesc(
                userProfile.getId());
        
        return documents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get documents by type
     */
    public List<DocumentResponse> getDocumentsByType(UUID userId, DocumentType documentType) {
        UserProfile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new DocumentNotFoundException("User profile not found: " + userId));
        
        List<UserDocument> documents = documentRepository.findByUserProfileIdAndDocumentType(
                userProfile.getId(), documentType);
        
        return documents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Download document file
     */
    public ResponseEntity<Resource> downloadDocument(UUID userId, UUID documentId) {
        UserDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        
        // Check authorization
        if (!document.getUserProfile().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not have access to this document");
        }
        
        try {
            InputStream fileStream = fileStorageService.downloadDocument(document);
            InputStreamResource resource = new InputStreamResource(fileStream);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Failed to download document {}: {}", documentId, e.getMessage(), e);
            throw new DocumentNotFoundException("Failed to download document: " + e.getMessage());
        }
    }

    /**
     * Delete a document
     */
    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        UserDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        
        // Check authorization
        if (!document.getUserProfile().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not have access to this document");
        }
        
        try {
            // Delete from MinIO
            fileStorageService.deleteDocument(document);
            
            // Delete from database
            documentRepository.delete(document);
            
            log.info("Document {} deleted successfully for user {}", 
                    document.getFileName(), userId);
                    
        } catch (Exception e) {
            log.error("Failed to delete document {}: {}", documentId, e.getMessage(), e);
            throw new DocumentNotFoundException("Failed to delete document: " + e.getMessage());
        }
    }

    /**
     * Verify a document (admin/compliance officer function)
     */
    @Transactional
    public DocumentResponse verifyDocument(UUID documentId, VerificationStatus status, String remarks) {
        UserDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        
        document.setVerificationStatus(status);
        document.setVerificationRemarks(remarks);
        document.setVerifiedAt(Instant.now());
        
        UserDocument savedDocument = documentRepository.save(document);
        
        // Publish document verified event
        ProfileEvent event = ProfileEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_VERIFIED")
                .userId(document.getUserProfile().getUserId())
                .profileId(document.getUserProfile().getId())
                .timestamp(java.time.LocalDateTime.now())
                .source("user-profile-service")
                .data(savedDocument)
                .build();
        
        eventPublisher.publishDocumentVerified(event);
        
        log.info("Document {} verified with status {} for user {}", 
                document.getFileName(), status, document.getUserProfile().getUserId());
        
        return convertToResponse(savedDocument);
    }

    /**
     * Get KYC completion status for user
     */
    public KYCStatus getKYCStatus(UUID userId) {
        UserProfile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new DocumentNotFoundException("User profile not found: " + userId));
        
        List<UserDocument> documents = documentRepository.findByUserProfileIdOrderByUploadedAtDesc(userProfile.getId());
        
        boolean hasPAN = documents.stream().anyMatch(doc -> 
            doc.getDocumentType() == DocumentType.PAN_CARD && doc.isVerified());
        boolean hasAadhaar = documents.stream().anyMatch(doc -> 
            doc.getDocumentType() == DocumentType.AADHAAR_CARD && doc.isVerified());
        boolean hasAddressProof = documents.stream().anyMatch(doc -> 
            doc.getDocumentType().isAddressProof() && doc.isVerified());
        boolean hasBankProof = documents.stream().anyMatch(doc -> 
            doc.getDocumentType().isFinancialDocument() && doc.isVerified());
        
        int completedMandatory = 0;
        int totalMandatory = 4; // PAN, Aadhaar, Address Proof, Bank Proof
        
        if (hasPAN) completedMandatory++;
        if (hasAadhaar) completedMandatory++;
        if (hasAddressProof) completedMandatory++;
        if (hasBankProof) completedMandatory++;
        
        boolean isComplete = completedMandatory == totalMandatory;
        double completionPercentage = (double) completedMandatory / totalMandatory * 100;
        
        return new KYCStatus(isComplete, completionPercentage, completedMandatory, totalMandatory,
                hasPAN, hasAadhaar, hasAddressProof, hasBankProof);
    }

    /**
     * Generate temporary download URL
     */
    public String generateDownloadUrl(UUID userId, UUID documentId, int expiryHours) {
        UserDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        
        // Check authorization
        if (!document.getUserProfile().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not have access to this document");
        }
        
        return fileStorageService.generatePresignedUrl(document, expiryHours);
    }

    private DocumentResponse convertToResponse(UserDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .userId(document.getUserProfile().getUserId())
                .documentType(document.getDocumentType())
                .fileName(document.getFileName())
                .originalFileName(document.getFileName())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .fileSizeFormatted(document.getFileSizeFormatted())
                .verificationStatus(document.getVerificationStatus())
                .verificationComments(document.getVerificationRemarks())
                .createdAt(document.getUploadedAt() != null ? 
                          document.getUploadedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null)
                .verifiedAt(document.getVerifiedAt() != null ? 
                           document.getVerifiedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null)
                .build();
    }

    // KYC Status DTO
    public record KYCStatus(
            boolean isComplete,
            double completionPercentage,
            int completedMandatoryDocs,
            int totalMandatoryDocs,
            boolean hasPAN,
            boolean hasAadhaar,
            boolean hasAddressProof,
            boolean hasBankProof
    ) {}
}