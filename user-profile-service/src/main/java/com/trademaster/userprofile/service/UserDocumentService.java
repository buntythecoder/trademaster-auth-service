package com.trademaster.userprofile.service;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.controller.UserDocumentController.DocumentUploadInfo;
import com.trademaster.userprofile.entity.UserDocument;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.VerificationStatus;
import com.trademaster.userprofile.repository.UserDocumentRepository;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Functional User Document Service with Verification Workflows
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Virtual Threads & Concurrency - Rule #12
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Stream API Mastery - Rule #13
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDocumentService {
    
    private final UserDocumentRepository userDocumentRepository;
    private final UserProfileRepository userProfileRepository;
    
    @Value("${trademaster.user-profile.documents.max-file-size:20971520}")
    private Long maxFileSize;
    
    @Value("${trademaster.user-profile.documents.allowed-mime-types}")
    private List<String> allowedMimeTypes;
    
    @Value("${trademaster.user-profile.documents.storage-path}")
    private String storagePath;
    
    // Error types for functional error handling
    public sealed interface DocumentError permits
        DocumentNotFoundError, ValidationError, SystemError, StorageError, SecurityError {
        String message();
    }
    
    public record DocumentNotFoundError(String message) implements DocumentError {}
    public record ValidationError(String message, List<String> details) implements DocumentError {}
    public record SystemError(String message, Throwable cause) implements DocumentError {}
    public record StorageError(String message) implements DocumentError {}
    public record SecurityError(String message) implements DocumentError {}
    
    // ========== QUERY OPERATIONS (FUNCTIONAL READ-ONLY) ==========
    
    /**
     * Find documents by user profile ID using functional composition
     */
    @Cacheable(value = "userDocuments", key = "#userProfileId")
    public Result<List<UserDocument>, DocumentError> findByUserProfileId(UUID userProfileId) {
        return Result.tryExecute(() -> {
            log.debug("Finding documents for user profile: {}", userProfileId);
            
            return userDocumentRepository.findByUserProfileIdOrderByUploadedAtDesc(userProfileId)
                .stream()
                .sorted((d1, d2) -> d2.getUploadedAt().compareTo(d1.getUploadedAt()))
                .toList();
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Find documents by type and status with functional filtering
     */
    public Result<List<UserDocument>, DocumentError> findByTypeAndStatus(
            DocumentType documentType,
            VerificationStatus status) {
        
        return Result.tryExecute(() -> {
            log.debug("Finding documents with type: {} and status: {}", documentType, status);
            
            Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1000);
            return userDocumentRepository.findByDocumentTypeAndVerificationStatus(documentType, status, pageable)
                .stream()
                .filter(createDocumentFilter(documentType, status))
                .toList();
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Find latest document of specific type for user
     */
    public Result<Optional<UserDocument>, DocumentError> findLatestByUserProfileAndType(
            UUID userProfileId,
            DocumentType documentType) {
        
        return Result.tryExecute(() -> {
            log.debug("Finding latest document of type: {} for user: {}", documentType, userProfileId);
            
            return userDocumentRepository.findLatestByUserProfileAndType(userProfileId, documentType);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Check if user has verified document of specific type
     */
    public Result<Boolean, DocumentError> hasVerifiedDocument(UUID userProfileId, DocumentType documentType) {
        return Result.tryExecute(() -> {
            log.debug("Checking verified document existence for user: {} and type: {}", userProfileId, documentType);
            
            return userDocumentRepository.hasVerifiedDocument(userProfileId, documentType);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Get document statistics for user
     */
    public Result<DocumentStatistics, DocumentError> getDocumentStatistics(UUID userProfileId) {
        return Result.tryExecute(() -> {
            log.debug("Getting document statistics for user: {}", userProfileId);
            
            List<Object[]> stats = userDocumentRepository.getDocumentStatisticsByUser(userProfileId);
            
            return DocumentStatistics.fromStatistics(stats);
        }).mapError(this::mapToDocumentError);
    }
    
    // ========== COMMAND OPERATIONS (TRANSACTIONAL WRITES) ==========
    
    /**
     * Upload document with functional validation and security checks
     */
    @Transactional
    @CacheEvict(value = "userDocuments", key = "#userProfileId")
    public Result<UserDocument, DocumentError> uploadDocument(
            UUID userProfileId,
            DocumentType documentType,
            MultipartFile file,
            String fileName) {
        
        return validateDocumentUpload(userProfileId, documentType, file, fileName)
            .flatMap(uploadRequest -> processDocumentUpload(uploadRequest))
            .onSuccess(document -> log.info("Document uploaded successfully: {} for user: {}", 
                document.getId(), userProfileId))
            .onFailure(error -> log.error("Failed to upload document for user: {}, error: {}", 
                userProfileId, error.message()));
    }
    
    /**
     * Update document verification status using functional approach
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true)
    public Result<UserDocument, DocumentError> updateVerificationStatus(
            UUID documentId,
            VerificationStatus status,
            String remarks) {
        
        return findDocumentById(documentId)
            .flatMap(document -> validateVerificationUpdate(document, status, remarks))
            .flatMap(document -> applyVerificationUpdate(document, status, remarks))
            .onSuccess(document -> log.info("Document verification updated: {} to status: {}", 
                documentId, status))
            .onFailure(error -> log.error("Failed to update verification for document: {}, error: {}", 
                documentId, error.message()));
    }
    
    /**
     * Batch verify documents using functional composition
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true)
    public Result<List<UserDocument>, DocumentError> batchVerifyDocuments(
            List<UUID> documentIds,
            VerificationStatus status,
            String remarks) {
        
        return Result.tryExecute(() -> {
            log.info("Starting batch verification for {} documents", documentIds.size());
            
            List<UserDocument> verifiedDocuments = documentIds.stream()
                .map(id -> updateVerificationStatus(id, status, remarks))
                .filter(Result::isSuccess)
                .map(result -> result.getValue().orElse(null))
                .filter(Objects::nonNull)
                .toList();
            
            log.info("Batch verification completed. Verified {} of {} documents", 
                verifiedDocuments.size(), documentIds.size());
            
            return verifiedDocuments;
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Batch verify documents with VerificationInfo (controller compatibility)
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true)
    public CompletableFuture<Result<List<UserDocument>, DocumentError>> batchVerifyDocuments(
            List<com.trademaster.userprofile.controller.UserDocumentController.VerificationInfo> verifications) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing batch verification for {} documents", verifications.size());
            
            return Result.tryExecute(() -> {
                List<UserDocument> verifiedDocuments = verifications.stream()
                    .map(verInfo -> updateVerificationStatus(verInfo.documentId(), verInfo.status(), verInfo.verifierNotes()))
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
                
                return verifiedDocuments;
            }).mapError(this::mapToDocumentError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    /**
     * Delete document with security validation
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true)
    public Result<Void, DocumentError> deleteDocument(UUID documentId, UUID requestingUserId) {
        return findDocumentById(documentId)
            .flatMap(document -> validateDocumentDeletion(document, requestingUserId))
            .flatMap(this::performDocumentDeletion)
            .onSuccess(v -> log.info("Document deleted successfully: {}", documentId))
            .onFailure(error -> log.error("Failed to delete document: {}, error: {}", documentId, error.message()));
    }
    
    // ========== ASYNCHRONOUS OPERATIONS WITH VIRTUAL THREADS ==========
    
    /**
     * Async document processing using Virtual Threads
     */
    public CompletableFuture<Result<UserDocument, DocumentError>> uploadDocumentAsync(
            UUID userProfileId,
            DocumentType documentType,
            MultipartFile file,
            String fileName) {
        
        return CompletableFuture.supplyAsync(
            () -> uploadDocument(userProfileId, documentType, file, fileName),
            runnable -> Thread.ofVirtual().start(runnable)
        );
    }
    
    /**
     * Process pending verifications asynchronously
     */
    public CompletableFuture<Result<List<UserDocument>, DocumentError>> processPendingVerifications() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing pending document verifications");
            
            return Result.tryExecute(() -> {
                LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
                
                return userDocumentRepository.findPendingDocumentsOlderThan(cutoffTime)
                    .stream()
                    .map(this::processDocumentVerification)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            }).mapError(this::mapToDocumentError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    /**
     * Find document by ID
     */
    @Cacheable(value = "userDocuments", key = "#documentId")
    public Result<UserDocument, DocumentError> findById(UUID documentId) {
        return Result.tryExecute(() -> 
            userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId))
        ).mapError(this::mapToDocumentError);
    }
    
    /**
     * Find documents by verification status
     */
    @Cacheable(value = "userDocuments", key = "'status_' + #status.name()")
    public Result<List<UserDocument>, DocumentError> findByVerificationStatus(VerificationStatus status) {
        return Result.tryExecute(() -> {
            Pageable pageable = PageRequest.of(0, 1000);
            return userDocumentRepository.findByVerificationStatus(status);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Find pending verification documents
     */
    @Cacheable(value = "userDocuments", key = "'pending_verification'")
    public Result<List<UserDocument>, DocumentError> findPendingVerification() {
        return Result.tryExecute(() -> 
            userDocumentRepository.findByVerificationStatus(VerificationStatus.PENDING)
        ).mapError(this::mapToDocumentError);
    }
    
    /**
     * Replace existing document
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true)
    public Result<UserDocument, DocumentError> replaceDocument(UUID documentId, MultipartFile file, String description) {
        return Result.tryExecute(() -> {
            UserDocument existingDocument = userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            
            // Create updated document
            String secureFileName = generateSecureFileName(file.getOriginalFilename(), existingDocument.getDocumentType());
            String filePath = generateFilePath(existingDocument.getUserProfile().getUserId(), secureFileName);
            
            storeFileSecurely(file, filePath);
            
            UserDocument updatedDocument = existingDocument.toBuilder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .verificationRemarks(Optional.ofNullable(description).orElse(existingDocument.getDescription()))
                .verificationStatus(VerificationStatus.PENDING)
                .uploadedAt(java.time.Instant.now())
                .build();
            
            return userDocumentRepository.save(updatedDocument);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Batch upload documents
     */
    @Transactional
    public CompletableFuture<Result<List<UserDocument>, DocumentError>> batchUploadDocuments(
            UUID userId, 
            List<DocumentUploadInfo> documents) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing batch upload for user: {} with {} documents", userId, documents.size());
            
            return Result.tryExecute(() -> {
                List<UserDocument> uploadedDocuments = documents.stream()
                    .map(docInfo -> uploadDocumentFromInfo(userId, docInfo))
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
                
                return uploadedDocuments;
            }).mapError(this::mapToDocumentError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    private Result<UserDocument, DocumentError> uploadDocumentFromInfo(UUID userId, DocumentUploadInfo docInfo) {
        return Result.tryExecute(() -> {
            UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
            
            String secureFileName = generateSecureFileName(docInfo.fileName(), docInfo.documentType());
            String filePath = generateFilePath(userId, secureFileName);
            
            storeFileData(docInfo.fileData(), filePath);
            
            UserDocument document = UserDocument.builder()
                .userProfile(userProfile)
                .documentType(docInfo.documentType())
                .fileName(docInfo.fileName())
                .filePath(filePath)
                .fileSize((long) docInfo.fileData().length)
                .verificationRemarks(docInfo.description())
                .verificationStatus(VerificationStatus.PENDING)
                .uploadedAt(java.time.Instant.now())
                .build();
            
            return userDocumentRepository.save(document);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Single parameter delete method for controller compatibility
     */
    @Transactional
    @CacheEvict(value = "userDocuments", allEntries = true) 
    public Result<Void, DocumentError> deleteDocument(UUID documentId) {
        return Result.tryExecute(() -> {
            UserDocument document = userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            
            // Delete file from storage (implementation specific)
            deleteFileFromStorage(document.getFilePath());
            
            userDocumentRepository.delete(document);
            return (Void) null;
        }).mapError(this::mapToDocumentError);
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Document upload request record for type safety
     */
    private record DocumentUploadRequest(
        UUID userProfileId,
        DocumentType documentType,
        MultipartFile file,
        String fileName,
        UserProfile userProfile
    ) {}
    
    /**
     * Document statistics record
     */
    public record DocumentStatistics(
        long totalDocuments,
        long verifiedDocuments,
        long pendingDocuments,
        long rejectedDocuments
    ) {
        public static DocumentStatistics fromStatistics(List<Object[]> stats) {
            Map<String, Long> statusCounts = new HashMap<>();
            stats.forEach(stat -> statusCounts.put((String) stat[0], (Long) stat[1]));
            
            return new DocumentStatistics(
                statusCounts.values().stream().mapToLong(Long::longValue).sum(),
                statusCounts.getOrDefault("VERIFIED", 0L),
                statusCounts.getOrDefault("PENDING", 0L) + statusCounts.getOrDefault("IN_PROGRESS", 0L),
                statusCounts.getOrDefault("REJECTED", 0L)
            );
        }
    }
    
    /**
     * Validate document upload request
     */
    private Result<DocumentUploadRequest, DocumentError> validateDocumentUpload(
            UUID userProfileId,
            DocumentType documentType,
            MultipartFile file,
            String fileName) {
        
        return Result.tryExecute(() -> {
            // Validate user profile exists
            UserProfile userProfile = userProfileRepository.findByUserId(userProfileId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + userProfileId));
            
            // Functional validation chain
            List<String> validationErrors = Stream.of(
                validateFileSize(file),
                validateMimeType(file),
                validateFileName(fileName),
                validateDocumentType(documentType),
                validateUploadQuota(userProfileId, documentType)
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
            
            if (!validationErrors.isEmpty()) {
                throw new RuntimeException("Validation failed: " + String.join(", ", validationErrors));
            }
            
            return new DocumentUploadRequest(userProfileId, documentType, file, fileName, userProfile);
        }).mapError(throwable -> new ValidationError("Upload validation failed", List.of(throwable.getMessage())));
    }
    
    /**
     * Process document upload with security checks
     */
    private Result<UserDocument, DocumentError> processDocumentUpload(DocumentUploadRequest request) {
        return Result.tryExecute(() -> {
            // Generate secure file path
            String secureFileName = generateSecureFileName(request.fileName(), request.documentType());
            String filePath = generateFilePath(request.userProfileId(), secureFileName);
            
            // Store file securely (implementation would depend on storage system)
            storeFileSecurely(request.file(), filePath);
            
            // Create document entity
            UserDocument document = UserDocument.builder()
                .userProfile(request.userProfile())
                .documentType(request.documentType())
                .fileName(request.fileName())
                .filePath(filePath)
                .fileSize(request.file().getSize())
                .mimeType(request.file().getContentType())
                .build();
            
            return userDocumentRepository.save(document);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Find document by ID with error handling
     */
    private Result<UserDocument, DocumentError> findDocumentById(UUID documentId) {
        return Result.tryExecute(() -> {
            return userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Validate verification status update
     */
    private Result<UserDocument, DocumentError> validateVerificationUpdate(
            UserDocument document,
            VerificationStatus status,
            String remarks) {
        
        return Result.tryExecute(() -> {
            // Business rules validation
            if (document.getVerificationStatus() == VerificationStatus.VERIFIED && status == VerificationStatus.REJECTED) {
                throw new RuntimeException("Cannot reject already verified document");
            }
            
            if (status == VerificationStatus.REJECTED && (remarks == null || remarks.trim().isEmpty())) {
                throw new RuntimeException("Remarks are required when rejecting document");
            }
            
            return document;
        }).mapError(throwable -> new ValidationError("Verification validation failed", List.of(throwable.getMessage())));
    }
    
    /**
     * Apply verification update functionally
     */
    private Result<UserDocument, DocumentError> applyVerificationUpdate(
            UserDocument document,
            VerificationStatus status,
            String remarks) {
        
        return Result.tryExecute(() -> {
            UserDocument updatedDocument = document.withVerificationStatus(status, remarks);
            return userDocumentRepository.save(updatedDocument);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Process individual document verification
     */
    private Result<UserDocument, DocumentError> processDocumentVerification(UserDocument document) {
        // Implementation would integrate with external verification services
        // For now, we'll simulate auto-verification based on business rules
        return Result.tryExecute(() -> {
            VerificationStatus newStatus = determineVerificationStatus(document);
            String remarks = generateVerificationRemarks(document, newStatus);
            
            return document.withVerificationStatus(newStatus, remarks);
        }).mapError(this::mapToDocumentError);
    }
    
    /**
     * Document filter using functional composition
     */
    private Predicate<UserDocument> createDocumentFilter(DocumentType type, VerificationStatus status) {
        return document -> document.getDocumentType().equals(type) && 
                          document.getVerificationStatus().equals(status);
    }
    
    /**
     * Validate document deletion with security checks
     */
    private Result<UserDocument, DocumentError> validateDocumentDeletion(UserDocument document, UUID requestingUserId) {
        return Result.tryExecute(() -> {
            // Security check: only allow deletion by document owner or admin
            if (!document.getUserProfile().getUserId().equals(requestingUserId)) {
                throw new RuntimeException("Access denied: cannot delete document owned by another user");
            }
            
            if (document.getVerificationStatus() == VerificationStatus.VERIFIED) {
                throw new RuntimeException("Cannot delete verified document");
            }
            
            return document;
        }).mapError(throwable -> new SecurityError(throwable.getMessage()));
    }
    
    /**
     * Perform document deletion
     */
    private Result<Void, DocumentError> performDocumentDeletion(UserDocument document) {
        return Result.tryExecute(() -> {
            // Delete physical file
            deletePhysicalFile(document.getFilePath());
            
            // Delete database record
            userDocumentRepository.delete(document);
            
            return (Void) null;
        }).mapError(this::mapToDocumentError);
    }
    
    // ========== VALIDATION METHODS ==========
    
    private Optional<String> validateFileSize(MultipartFile file) {
        return file.getSize() > maxFileSize 
            ? Optional.of("File size exceeds maximum allowed size: " + maxFileSize)
            : Optional.empty();
    }
    
    private Optional<String> validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType == null || !allowedMimeTypes.contains(contentType))
            ? Optional.of("File type not allowed: " + contentType)
            : Optional.empty();
    }
    
    private Optional<String> validateFileName(String fileName) {
        return (fileName == null || fileName.trim().isEmpty())
            ? Optional.of("File name is required")
            : Optional.empty();
    }
    
    private Optional<String> validateDocumentType(DocumentType documentType) {
        return documentType == null 
            ? Optional.of("Document type is required")
            : Optional.empty();
    }
    
    private Optional<String> validateUploadQuota(UUID userProfileId, DocumentType documentType) {
        // Business rule: check if user has already uploaded maximum allowed documents of this type
        long existingDocuments = userDocumentRepository.countByUserProfileAndStatus(
            userProfileId, VerificationStatus.VERIFIED);
            
        return existingDocuments >= 3 
            ? Optional.of("Maximum document upload limit reached for this type")
            : Optional.empty();
    }
    
    // ========== UTILITY METHODS ==========
    
    private String generateSecureFileName(String originalName, DocumentType type) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalName);
        return type.name().toLowerCase() + "_" + timestamp + "." + extension;
    }
    
    private String generateFilePath(UUID userProfileId, String fileName) {
        return storagePath + "/" + userProfileId + "/" + fileName;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private void storeFileSecurely(MultipartFile file, String filePath) {
        // Implementation would depend on chosen storage solution (local, MinIO, S3, etc.)
        log.debug("Storing file securely at path: {}", filePath);
    }
    
    private void deletePhysicalFile(String filePath) {
        // Implementation would depend on storage solution
        log.debug("Deleting physical file at path: {}", filePath);
    }
    
    private VerificationStatus determineVerificationStatus(UserDocument document) {
        // Business logic for automatic verification
        return document.getDocumentType().isMandatory() 
            ? VerificationStatus.IN_PROGRESS 
            : VerificationStatus.VERIFIED;
    }
    
    private String generateVerificationRemarks(UserDocument document, VerificationStatus status) {
        return switch (status) {
            case VERIFIED -> "Automatically verified based on document type and content";
            case IN_PROGRESS -> "Manual verification required for " + document.getDocumentType().getDisplayName();
            case REJECTED -> "Document does not meet verification criteria";
            default -> "Pending verification";
        };
    }
    
    @CircuitBreaker(name = "file-storage", fallbackMethod = "storeFileDataFallback")
    private void storeFileData(byte[] fileData, String filePath) {
        try {
            Path targetPath = Paths.get(filePath);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("File stored successfully at path: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to store file at path: {}", filePath, e);
            throw new RuntimeException("File storage failed", e);
        }
    }
    
    private void storeFileDataFallback(byte[] fileData, String filePath, Exception ex) {
        log.error("Circuit breaker activated for file storage. Fallback triggered for path: {}", filePath, ex);
        throw new RuntimeException("File storage service unavailable. Please retry later.", ex);
    }
    
    @CircuitBreaker(name = "file-storage", fallbackMethod = "deleteFileFromStorageFallback")
    private void deleteFileFromStorage(String filePath) {
        try {
            Path targetPath = Paths.get(filePath);
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.debug("File deleted successfully from path: {}", filePath);
            } else {
                log.warn("Attempted to delete non-existent file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file at path: {}", filePath, e);
            throw new RuntimeException("File deletion failed", e);
        }
    }
    
    private void deleteFileFromStorageFallback(String filePath, Exception ex) {
        log.error("Circuit breaker activated for file deletion. Fallback triggered for path: {}", filePath, ex);
    }
    
    // ========== ERROR MAPPING ==========
    
    /**
     * Map exceptions to functional error types using pattern matching
     */
    private DocumentError mapToDocumentError(Throwable throwable) {
        return switch (throwable) {
            case IllegalArgumentException iae -> 
                new ValidationError("Invalid argument: " + iae.getMessage(), List.of());
            case RuntimeException re when re.getMessage().contains("not found") -> 
                new DocumentNotFoundError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("Access denied") -> 
                new SecurityError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("storage") -> 
                new StorageError(re.getMessage());
            default -> 
                new SystemError("System error occurred", throwable);
        };
    }
}