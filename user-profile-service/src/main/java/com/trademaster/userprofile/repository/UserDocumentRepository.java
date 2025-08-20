package com.trademaster.userprofile.repository;

import com.trademaster.userprofile.entity.DocumentType;
import com.trademaster.userprofile.entity.UserDocument;
import com.trademaster.userprofile.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, UUID> {
    
    /**
     * Find all documents for a user profile
     */
    List<UserDocument> findByUserProfileIdOrderByUploadedAtDesc(UUID userProfileId);
    
    /**
     * Find documents by user profile and document type
     */
    List<UserDocument> findByUserProfileIdAndDocumentType(UUID userProfileId, DocumentType documentType);
    
    /**
     * Find documents by verification status
     */
    List<UserDocument> findByUserProfileIdAndVerificationStatus(UUID userProfileId, VerificationStatus status);
    
    /**
     * Find documents by document type and verification status
     */
    Page<UserDocument> findByDocumentTypeAndVerificationStatus(
        DocumentType documentType, 
        VerificationStatus status, 
        Pageable pageable
    );
    
    /**
     * Find the latest document of a specific type for a user
     */
    @Query("SELECT ud FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId AND ud.documentType = :documentType " +
           "ORDER BY ud.uploadedAt DESC LIMIT 1")
    Optional<UserDocument> findLatestByUserProfileAndType(
        @Param("userProfileId") UUID userProfileId, 
        @Param("documentType") DocumentType documentType
    );
    
    /**
     * Check if user has verified document of specific type
     */
    @Query("SELECT COUNT(ud) > 0 FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId AND " +
           "ud.documentType = :documentType AND " +
           "ud.verificationStatus = 'VERIFIED'")
    boolean hasVerifiedDocument(
        @Param("userProfileId") UUID userProfileId, 
        @Param("documentType") DocumentType documentType
    );
    
    /**
     * Count documents by status for a user
     */
    @Query("SELECT COUNT(ud) FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId AND ud.verificationStatus = :status")
    long countByUserProfileAndStatus(
        @Param("userProfileId") UUID userProfileId, 
        @Param("status") VerificationStatus status
    );
    
    /**
     * Find documents uploaded within a date range
     */
    List<UserDocument> findByUserProfileIdAndUploadedAtBetween(
        UUID userProfileId, 
        Instant startDate, 
        Instant endDate
    );
    
    /**
     * Find documents pending verification for more than specified days
     */
    @Query("SELECT ud FROM UserDocument ud WHERE " +
           "ud.verificationStatus = 'PENDING' AND " +
           "ud.uploadedAt < :cutoffDate")
    List<UserDocument> findPendingDocumentsOlderThan(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Get document verification statistics for a user
     */
    @Query("SELECT ud.verificationStatus, COUNT(ud) FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId GROUP BY ud.verificationStatus")
    List<Object[]> getDocumentStatisticsByUser(@Param("userProfileId") UUID userProfileId);
    
    /**
     * Find all documents that need verification
     */
    @Query("SELECT ud FROM UserDocument ud WHERE " +
           "ud.verificationStatus IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY ud.uploadedAt ASC")
    Page<UserDocument> findDocumentsNeedingVerification(Pageable pageable);
    
    /**
     * Update verification status and remarks
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserDocument ud SET " +
           "ud.verificationStatus = :status, " +
           "ud.verificationRemarks = :remarks, " +
           "ud.verifiedAt = :verifiedAt " +
           "WHERE ud.id = :documentId")
    int updateVerificationStatus(
        @Param("documentId") UUID documentId,
        @Param("status") VerificationStatus status,
        @Param("remarks") String remarks,
        @Param("verifiedAt") Instant verifiedAt
    );
    
    /**
     * Delete documents older than specified date (for cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserDocument ud WHERE " +
           "ud.verificationStatus = 'REJECTED' AND " +
           "ud.uploadedAt < :cutoffDate")
    int deleteRejectedDocumentsOlderThan(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find documents by file path (for file cleanup)
     */
    Optional<UserDocument> findByFilePath(String filePath);
    
    /**
     * Count total documents by type
     */
    @Query("SELECT ud.documentType, COUNT(ud) FROM UserDocument ud " +
           "GROUP BY ud.documentType ORDER BY COUNT(ud) DESC")
    List<Object[]> countDocumentsByType();
    
    /**
     * Find large documents (for storage optimization)
     */
    @Query("SELECT ud FROM UserDocument ud WHERE " +
           "ud.fileSize > :sizeThreshold ORDER BY ud.fileSize DESC")
    List<UserDocument> findLargeDocuments(@Param("sizeThreshold") Long sizeThreshold);
    
    /**
     * Get total storage used by user
     */
    @Query("SELECT COALESCE(SUM(ud.fileSize), 0) FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId")
    long getTotalStorageUsedByUser(@Param("userProfileId") UUID userProfileId);
    
    /**
     * Find documents with specific MIME types
     */
    @Query("SELECT ud FROM UserDocument ud WHERE " +
           "ud.mimeType IN :mimeTypes AND " +
           "ud.userProfile.id = :userProfileId")
    List<UserDocument> findByUserProfileAndMimeTypes(
        @Param("userProfileId") UUID userProfileId,
        @Param("mimeTypes") List<String> mimeTypes
    );
    
    /**
     * Count documents uploaded today for a user (rate limiting)
     */
    @Query("SELECT COUNT(ud) FROM UserDocument ud WHERE " +
           "ud.userProfile.id = :userProfileId AND " +
           "ud.uploadedAt >= :startOfDay")
    long countDocumentsUploadedToday(
        @Param("userProfileId") UUID userProfileId,
        @Param("startOfDay") Instant startOfDay
    );
}