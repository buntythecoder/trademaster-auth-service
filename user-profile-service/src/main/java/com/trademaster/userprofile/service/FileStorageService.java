package com.trademaster.userprofile.service;

import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.UserDocument;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.exception.FileStorageException;
import io.minio.*;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file storage operations with MinIO
 * Manages document uploads, downloads, and bucket operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.buckets.documents:trademaster-documents}")
    private String documentsBucket;

    @Value("${minio.buckets.kyc-docs:trademaster-kyc-docs}")
    private String kycDocsBucket;

    @Value("${minio.buckets.profile-pics:trademaster-profile-pics}")
    private String profilePicsBucket;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB global limit
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Upload a document file to MinIO storage
     */
    public UserDocument uploadDocument(MultipartFile file, DocumentType documentType, 
                                     UserProfile userProfile, String description) {
        
        validateFile(file, documentType);
        String bucketName = determineBucket(documentType);
        String objectKey = generateObjectKey(userProfile.getId(), documentType, file.getOriginalFilename());
        
        try {
            // Ensure bucket exists
            ensureBucketExists(bucketName);
            
            // Upload file to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .userMetadata(java.util.Map.of(
                        "user-id", userProfile.getId().toString(),
                        "document-type", documentType.name(),
                        "original-name", StringUtils.cleanPath(file.getOriginalFilename()),
                        "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build()
            );
            
            log.info("Successfully uploaded file {} for user {} to bucket {} with key {}", 
                    file.getOriginalFilename(), userProfile.getId(), bucketName, objectKey);
            
            // Create UserDocument entity
            return UserDocument.builder()
                    .userProfile(userProfile)
                    .documentType(documentType)
                    .fileName(generateFileName(userProfile.getId(), documentType, file.getOriginalFilename()))
                    .filePath(String.format("%s/%s", bucketName, objectKey))
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to upload file {} for user {}: {}", 
                     file.getOriginalFilename(), userProfile.getId(), e.getMessage(), e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from MinIO storage
     */
    public InputStream downloadDocument(UserDocument document) {
        try {
            String[] pathParts = document.getFilePath().split("/", 2);
            String bucketName = pathParts[0];
            String objectKey = pathParts[1];
            
            GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            
            log.info("Successfully downloaded document {} from bucket {}", 
                    document.getFileName(), bucketName);
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to download document {}: {}", document.getFileName(), e.getMessage(), e);
            throw new FileStorageException("Failed to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from MinIO storage
     */
    public void deleteDocument(UserDocument document) {
        try {
            String[] pathParts = document.getFilePath().split("/", 2);
            String bucketName = pathParts[0];
            String objectKey = pathParts[1];
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            
            log.info("Successfully deleted document {} from bucket {}", 
                    document.getFileName(), bucketName);
                    
        } catch (Exception e) {
            log.error("Failed to delete document {}: {}", document.getFileName(), e.getMessage(), e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a presigned URL for temporary file access
     */
    public String generatePresignedUrl(UserDocument document, int expiryHours) {
        try {
            String[] pathParts = document.getFilePath().split("/", 2);
            String bucketName = pathParts[0];
            String objectKey = pathParts[1];
            
            String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expiryHours * 60 * 60) // Convert hours to seconds
                    .build()
            );
            
            log.debug("Generated presigned URL for document {} valid for {} hours", 
                     document.getFileName(), expiryHours);
            
            return presignedUrl;
            
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for document {}: {}", 
                     document.getFileName(), e.getMessage(), e);
            throw new FileStorageException("Failed to generate download URL: " + e.getMessage(), e);
        }
    }

    /**
     * Check if file exists in storage
     */
    public boolean fileExists(UserDocument document) {
        try {
            String[] pathParts = document.getFilePath().split("/", 2);
            String bucketName = pathParts[0];
            String objectKey = pathParts[1];
            
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            
            return true;
            
        } catch (Exception e) {
            log.warn("File does not exist: {}", document.getFilePath());
            return false;
        }
    }

    private void validateFile(MultipartFile file, DocumentType documentType) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is required and cannot be empty");
        }
        
        // Check file size against document type limit
        if (file.getSize() > documentType.getMaxSizeBytes()) {
            throw new FileStorageException(String.format(
                "File size exceeds maximum allowed size of %s for %s", 
                documentType.getMaxSizeFormatted(), documentType.getDisplayName()
            ));
        }
        
        // Check global file size limit
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum allowed size of 50MB");
        }
        
        // Validate MIME type
        if (!documentType.isValidMimeType(file.getContentType())) {
            throw new FileStorageException(String.format(
                "Invalid file type. Allowed types for %s: %s", 
                documentType.getDisplayName(), 
                String.join(", ", documentType.getAllowedMimeTypes())
            ));
        }
        
        // Validate filename
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + filename);
        }
    }

    private String determineBucket(DocumentType documentType) {
        return switch (documentType) {
            case PROFILE_PHOTO, SIGNATURE -> profilePicsBucket;
            case PAN_CARD, AADHAAR_CARD, PASSPORT, DRIVING_LICENSE, VOTER_ID -> kycDocsBucket;
            default -> documentsBucket;
        };
    }

    private String generateObjectKey(UUID userId, DocumentType documentType, String originalFilename) {
        String datePrefix = LocalDateTime.now().format(DATE_FORMAT);
        String fileExtension = getFileExtension(originalFilename);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s/%s/%s/%s_%s%s", 
                datePrefix, userId, documentType.name().toLowerCase(), 
                documentType.name().toLowerCase(), uniqueId, fileExtension);
    }

    private String generateFileName(UUID userId, DocumentType documentType, String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        return String.format("%s_%s_%s%s", 
                documentType.name().toLowerCase(), userId.toString().substring(0, 8), 
                timestamp, fileExtension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );
        
        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            log.info("Created new bucket: {}", bucketName);
        }
    }

    /**
     * Health check for MinIO service
     */
    public boolean isMinioHealthy() {
        try {
            List<Bucket> buckets = minioClient.listBuckets();
            log.debug("MinIO health check successful. Found {} buckets", buckets.size());
            return true;
        } catch (Exception e) {
            log.error("MinIO health check failed: {}", e.getMessage());
            return false;
        }
    }
}