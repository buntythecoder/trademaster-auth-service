package com.trademaster.userprofile.integration;

import com.trademaster.userprofile.UserProfileServiceApplication;
import com.trademaster.userprofile.dto.UserProfileResponse;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.repository.UserProfileRepository;
import com.trademaster.userprofile.repository.UserDocumentRepository;
import com.trademaster.userprofile.repository.ProfileAuditLogRepository;
import com.trademaster.userprofile.service.UserProfileService;
import com.trademaster.userprofile.service.ProfileValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TradeMaster User Profile Service
 * 
 * Tests complete KYC and profile management workflows:
 * - PostgreSQL for profile persistence and audit trails
 * - MinIO for document storage and encryption
 * - KYC document validation and compliance workflows
 * - Risk profile assessment and business rules
 * - Profile audit and compliance tracking
 * 
 * MANDATORY: TestContainers for enterprise-grade testing
 * MANDATORY: 80%+ coverage with realistic KYC workflows
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(classes = UserProfileServiceApplication.class)
@Testcontainers
@ActiveProfiles("integration-test")
@Transactional
public class UserProfileServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("trademaster_userprofile_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
            .withExposedPorts(9000, 9001)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin123")
            .withCommand("server", "/data", "--console-address", ":9001")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        
        // MinIO configuration for document storage
        registry.add("document.storage.endpoint", () -> 
            "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("document.storage.access-key", () -> "minioadmin");
        registry.add("document.storage.secret-key", () -> "minioadmin123");
        registry.add("document.storage.bucket-name", () -> "trademaster-documents-test");
        
        // JPA configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        
        // Virtual Threads configuration
        registry.add("spring.threads.virtual.enabled", () -> "true");
        
        // Disable external service calls for integration tests
        registry.add("trademaster.kyc.mock-mode", () -> "true");
        registry.add("trademaster.notifications.mock-mode", () -> "true");
    }

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ProfileValidationService profileValidationService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private ProfileAuditLogRepository auditLogRepository;

    private Long testUserId;
    private String testEmail;
    private String testPanNumber;

    @BeforeEach
    void setUp() {
        testUserId = 12345L;
        testEmail = "test@trademaster.com";
        testPanNumber = "ABCDE1234F";
        
        // Clear any existing test data
        auditLogRepository.deleteAll();
        userDocumentRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void createUserProfile_WithValidData_ShouldPersistSuccessfully() {
        // Arrange
        UserProfile profileRequest = UserProfile.builder()
                .userId(testUserId)
                .firstName("John")
                .lastName("Doe")
                .email(testEmail)
                .phoneNumber("+91-9876543210")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .panNumber(testPanNumber)
                .aadharNumber("123456789012")
                .address("123 Test Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pinCode("400001")
                .country("India")
                .kycStatus(KycStatus.PENDING)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        // Act
        UserProfile createdProfile = userProfileService.createProfile(profileRequest);

        // Assert
        assertNotNull(createdProfile);
        assertNotNull(createdProfile.getId());
        assertEquals(testUserId, createdProfile.getUserId());
        assertEquals("John", createdProfile.getFirstName());
        assertEquals("Doe", createdProfile.getLastName());
        assertEquals(testEmail, createdProfile.getEmail());
        assertEquals(testPanNumber, createdProfile.getPanNumber());
        assertEquals(KycStatus.PENDING, createdProfile.getKycStatus());
        assertEquals(AccountStatus.ACTIVE, createdProfile.getAccountStatus());
        
        // Verify persistence in database
        Optional<UserProfile> persistedProfile = userProfileRepository.findByUserId(testUserId);
        assertTrue(persistedProfile.isPresent());
        assertEquals(createdProfile.getId(), persistedProfile.get().getId());
        
        // Verify audit log entry was created
        List<ProfileAuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(testUserId);
        assertEquals(1, auditLogs.size());
        assertEquals("PROFILE_CREATED", auditLogs.get(0).getAction());
    }

    @Test
    void completeKycWorkflow_WithDocumentUpload_ShouldUpdateStatusCorrectly() {
        // Step 1: Create initial profile
        UserProfile profile = createTestProfile(testUserId, KycStatus.PENDING);
        
        // Step 2: Upload KYC documents
        UserDocument panCard = UserDocument.builder()
                .userId(testUserId)
                .documentType(DocumentType.PAN_CARD)
                .fileName("pan_card.pdf")
                .filePath("/documents/kyc/pan_card.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .uploadedAt(LocalDateTime.now())
                .verified(false)
                .encrypted(true)
                .build();
        
        UserDocument aadharCard = UserDocument.builder()
                .userId(testUserId)
                .documentType(DocumentType.AADHAR_CARD)
                .fileName("aadhar_card.pdf")
                .filePath("/documents/kyc/aadhar_card.pdf")
                .fileSize(2048L)
                .mimeType("application/pdf")
                .uploadedAt(LocalDateTime.now())
                .verified(false)
                .encrypted(true)
                .build();

        userDocumentRepository.save(panCard);
        userDocumentRepository.save(aadharCard);

        // Step 3: Validate documents and update KYC status
        boolean validationResult = profileValidationService.validateKycDocuments(testUserId);
        assertTrue(validationResult);

        // Step 4: Approve KYC
        UserProfile approvedProfile = userProfileService.updateKycStatus(testUserId, KycStatus.APPROVED);
        
        // Assert
        assertEquals(KycStatus.APPROVED, approvedProfile.getKycStatus());
        assertNotNull(approvedProfile.getKycApprovedAt());
        
        // Verify documents marked as verified
        List<UserDocument> documents = userDocumentRepository.findByUserId(testUserId);
        assertEquals(2, documents.size());
        documents.forEach(doc -> assertTrue(doc.isVerified()));
        
        // Verify audit trail
        List<ProfileAuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(testUserId);
        assertTrue(auditLogs.size() >= 2); // PROFILE_CREATED + KYC_APPROVED
        
        ProfileAuditLog kycApprovalLog = auditLogs.stream()
                .filter(log -> "KYC_APPROVED".equals(log.getAction()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("KYC approval audit log not found"));
        assertEquals("KYC_APPROVED", kycApprovalLog.getAction());
        assertEquals(testUserId, kycApprovalLog.getUserId());
    }

    @Test
    void riskProfileAssessment_WithCompleteData_ShouldCalculateCorrectly() {
        // Arrange
        UserProfile profile = createTestProfile(testUserId, KycStatus.APPROVED);
        
        // Create risk profile data
        RiskProfile riskProfile = RiskProfile.builder()
                .userId(testUserId)
                .investmentExperience(3) // 3 years
                .riskTolerance("MODERATE")
                .investmentHorizon(5) // 5 years
                .incomeRange("5_10_LAKH")
                .investmentObjective("WEALTH_GROWTH")
                .riskLevel(RiskLevel.MODERATE)
                .riskScore(65)
                .build();

        // Act
        RiskProfile savedRiskProfile = userProfileService.updateRiskProfile(testUserId, riskProfile);
        
        // Assert
        assertNotNull(savedRiskProfile);
        assertEquals(testUserId, savedRiskProfile.getUserId());
        assertEquals("MODERATE", savedRiskProfile.getRiskTolerance());
        assertEquals(RiskLevel.MODERATE, savedRiskProfile.getRiskLevel());
        assertEquals(65, savedRiskProfile.getRiskScore());
        
        // Verify profile updated with risk assessment
        UserProfile updatedProfile = userProfileRepository.findByUserId(testUserId)
                .orElseThrow(() -> new AssertionError("Profile not found"));
        assertNotNull(updatedProfile.getRiskProfile());
        assertEquals(RiskLevel.MODERATE, updatedProfile.getRiskProfile().getRiskLevel());
        
        // Verify audit log
        List<ProfileAuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(testUserId);
        boolean hasRiskProfileUpdate = auditLogs.stream()
                .anyMatch(log -> "RISK_PROFILE_UPDATED".equals(log.getAction()));
        assertTrue(hasRiskProfileUpdate, "Risk profile update should be audited");
    }

    @Test
    void profileValidation_WithInvalidData_ShouldReturnErrors() {
        // Test 1: Invalid email format
        UserProfile invalidEmail = createBaseProfileBuilder()
                .email("invalid-email")
                .build();
        
        assertThrows(Exception.class, () -> userProfileService.createProfile(invalidEmail));
        
        // Test 2: Invalid PAN number format
        UserProfile invalidPan = createBaseProfileBuilder()
                .panNumber("INVALID123")
                .build();
        
        assertThrows(Exception.class, () -> userProfileService.createProfile(invalidPan));
        
        // Test 3: Future date of birth
        UserProfile futureDob = createBaseProfileBuilder()
                .dateOfBirth(LocalDate.now().plusYears(1))
                .build();
        
        assertThrows(Exception.class, () -> userProfileService.createProfile(futureDob));
        
        // Test 4: Invalid phone number
        UserProfile invalidPhone = createBaseProfileBuilder()
                .phoneNumber("123")
                .build();
        
        assertThrows(Exception.class, () -> userProfileService.createProfile(invalidPhone));
        
        // Verify no invalid profiles were persisted
        List<UserProfile> profiles = userProfileRepository.findAll();
        assertEquals(0, profiles.size(), "No invalid profiles should be persisted");
    }

    @Test
    void concurrentProfileCreation_With50Users_ShouldHandleAllSuccessfully() throws InterruptedException {
        // Arrange
        int numberOfUsers = 50;
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        // Act - Create profiles concurrently using Virtual Threads
        List<CompletableFuture<UserProfile>> futures = IntStream.range(0, numberOfUsers)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        UserProfile profile = UserProfile.builder()
                                .userId(testUserId + i)
                                .firstName("User" + i)
                                .lastName("Test" + i)
                                .email("user" + i + "@test.com")
                                .phoneNumber("+91-987654" + String.format("%04d", i))
                                .dateOfBirth(LocalDate.of(1990 + (i % 30), 1 + (i % 12), 1 + (i % 28)))
                                .panNumber("ABCD" + String.format("%05d", i) + "F")
                                .aadharNumber(String.format("%012d", 123456789000L + i))
                                .address("Address " + i)
                                .city("City" + i)
                                .state("State" + i)
                                .pinCode(String.format("%06d", 400000 + i))
                                .country("India")
                                .kycStatus(KycStatus.PENDING)
                                .accountStatus(AccountStatus.ACTIVE)
                                .build();
                        
                        return userProfileService.createProfile(profile);
                    } finally {
                        latch.countDown();
                    }
                }))
                .toList();

        // Wait for all profiles to be created
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All profiles should be created within 30 seconds");

        // Assert - Verify all profiles created successfully
        List<UserProfile> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        assertEquals(numberOfUsers, results.size(), "All profiles should be created");
        
        // Verify all profiles are unique and valid
        results.forEach(profile -> {
            assertNotNull(profile.getId());
            assertTrue(profile.getUserId() >= testUserId);
            assertTrue(profile.getUserId() < testUserId + numberOfUsers);
        });

        // Verify persistence in database
        List<UserProfile> persistedProfiles = userProfileRepository.findAll();
        assertEquals(numberOfUsers, persistedProfiles.size(), "All profiles should be persisted");
        
        // Verify audit logs created for all profiles
        List<ProfileAuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(numberOfUsers, auditLogs.size(), "Audit logs should be created for all profiles");
    }

    @Test
    void profileUpdate_WithValidChanges_ShouldMaintainAuditTrail() {
        // Step 1: Create initial profile
        UserProfile originalProfile = createTestProfile(testUserId, KycStatus.PENDING);
        
        // Step 2: Update profile information
        originalProfile.setPhoneNumber("+91-9999888877");
        originalProfile.setAddress("New Updated Address");
        originalProfile.setCity("New Delhi");
        originalProfile.setState("Delhi");
        
        UserProfile updatedProfile = userProfileService.updateProfile(originalProfile);
        
        // Assert profile updated
        assertEquals("+91-9999888877", updatedProfile.getPhoneNumber());
        assertEquals("New Updated Address", updatedProfile.getAddress());
        assertEquals("New Delhi", updatedProfile.getCity());
        assertEquals("Delhi", updatedProfile.getState());
        
        // Step 3: Update KYC status
        userProfileService.updateKycStatus(testUserId, KycStatus.APPROVED);
        
        // Step 4: Update account status
        userProfileService.updateAccountStatus(testUserId, AccountStatus.SUSPENDED);
        
        // Verify complete audit trail
        List<ProfileAuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(testUserId);
        assertTrue(auditLogs.size() >= 4); // CREATE + UPDATE + KYC + ACCOUNT_STATUS
        
        // Verify specific audit log entries
        String[] expectedActions = {"ACCOUNT_STATUS_UPDATED", "KYC_STATUS_UPDATED", "PROFILE_UPDATED", "PROFILE_CREATED"};
        for (int i = 0; i < expectedActions.length; i++) {
            assertEquals(expectedActions[i], auditLogs.get(i).getAction());
            assertEquals(testUserId, auditLogs.get(i).getUserId());
            assertNotNull(auditLogs.get(i).getTimestamp());
        }
    }

    @Test
    void profileRetrieval_WithCompleteData_ShouldIncludeAllRelatedEntities() {
        // Arrange - Create complete profile with all related data
        UserProfile profile = createTestProfile(testUserId, KycStatus.APPROVED);
        
        // Add documents
        UserDocument document = UserDocument.builder()
                .userId(testUserId)
                .documentType(DocumentType.PAN_CARD)
                .fileName("pan.pdf")
                .filePath("/docs/pan.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .uploadedAt(LocalDateTime.now())
                .verified(true)
                .encrypted(true)
                .build();
        userDocumentRepository.save(document);
        
        // Add risk profile
        RiskProfile riskProfile = RiskProfile.builder()
                .userId(testUserId)
                .riskTolerance("MODERATE")
                .riskLevel(RiskLevel.MODERATE)
                .riskScore(70)
                .build();
        userProfileService.updateRiskProfile(testUserId, riskProfile);
        
        // Act
        UserProfileResponse profileResponse = userProfileService.getCompleteProfile(testUserId);
        
        // Assert
        assertNotNull(profileResponse);
        assertEquals(testUserId, profileResponse.getUserId());
        assertEquals("John", profileResponse.getFirstName());
        assertEquals("Doe", profileResponse.getLastName());
        assertEquals(KycStatus.APPROVED, profileResponse.getKycStatus());
        
        // Verify documents included
        assertNotNull(profileResponse.getDocuments());
        assertEquals(1, profileResponse.getDocuments().size());
        assertEquals(DocumentType.PAN_CARD, profileResponse.getDocuments().get(0).getDocumentType());
        
        // Verify risk profile included
        assertNotNull(profileResponse.getRiskProfile());
        assertEquals(RiskLevel.MODERATE, profileResponse.getRiskProfile().getRiskLevel());
        assertEquals(70, profileResponse.getRiskProfile().getRiskScore());
    }

    @Test
    void documentEncryption_ShouldProtectSensitiveData() {
        // Arrange
        UserProfile profile = createTestProfile(testUserId, KycStatus.PENDING);
        
        UserDocument sensitiveDoc = UserDocument.builder()
                .userId(testUserId)
                .documentType(DocumentType.BANK_STATEMENT)
                .fileName("bank_statement.pdf")
                .filePath("/encrypted/bank_statement.enc")
                .fileSize(4096L)
                .mimeType("application/pdf")
                .uploadedAt(LocalDateTime.now())
                .verified(false)
                .encrypted(true)
                .encryptionKey("test-encryption-key-12345")
                .build();
        
        // Act
        UserDocument savedDoc = userDocumentRepository.save(sensitiveDoc);
        
        // Assert
        assertTrue(savedDoc.isEncrypted());
        assertNotNull(savedDoc.getEncryptionKey());
        assertEquals("test-encryption-key-12345", savedDoc.getEncryptionKey());
        
        // Verify retrieval maintains encryption info
        UserDocument retrievedDoc = userDocumentRepository.findById(savedDoc.getId())
                .orElseThrow(() -> new AssertionError("Document not found"));
        assertTrue(retrievedDoc.isEncrypted());
        assertEquals("test-encryption-key-12345", retrievedDoc.getEncryptionKey());
    }

    /**
     * Helper methods for test data creation
     */
    private UserProfile createTestProfile(Long userId, KycStatus kycStatus) {
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phoneNumber("+91-9876543210")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .panNumber("ABCDE1234F")
                .aadharNumber("123456789012")
                .address("123 Test Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pinCode("400001")
                .country("India")
                .kycStatus(kycStatus)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        
        return userProfileService.createProfile(profile);
    }
    
    private UserProfile.UserProfileBuilder createBaseProfileBuilder() {
        return UserProfile.builder()
                .userId(testUserId + 1000) // Unique ID for validation tests
                .firstName("Test")
                .lastName("User")
                .email("valid@test.com")
                .phoneNumber("+91-9876543210")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .panNumber("ABCDE1234F")
                .aadharNumber("123456789012")
                .address("Test Address")
                .city("Test City")
                .state("Test State")
                .pinCode("123456")
                .country("India")
                .kycStatus(KycStatus.PENDING)
                .accountStatus(AccountStatus.ACTIVE);
    }
}