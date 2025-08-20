package com.trademaster.auth.security;

import com.trademaster.auth.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Set test configuration values using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", 
            "mySecretKeyThatIsAtLeast256BitsLongForHMACSHA256SecurityRequirement");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 900000L); // 15 minutes
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationMs", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtIssuer", "trademaster-auth-service");
        
        // Initialize the provider
        jwtTokenProvider.init();

        testUser = User.builder()
            .id(1L)
            .email("test@trademaster.com")
            .passwordHash("hashedPassword")
            .kycStatus(User.KycStatus.APPROVED)
            .subscriptionTier(User.SubscriptionTier.PREMIUM)
            .accountStatus(User.AccountStatus.ACTIVE)
            .emailVerified(true)
            .build();

        authentication = new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
    }

    @Test
    void generateToken_ShouldCreateValidJWT() {
        // Arrange
        String deviceFingerprint = "testDeviceFingerprint";
        String ipAddress = "127.0.0.1";

        // Act
        String token = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertFalse(jwtTokenProvider.isRefreshToken(token));
    }

    @Test
    void generateRefreshToken_ShouldCreateValidRefreshToken() {
        // Arrange
        String deviceFingerprint = "testDeviceFingerprint";

        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser, deviceFingerprint);

        // Assert
        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 0);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectUserId() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(testUser.getId(), userId);
    }

    @Test
    void getEmailFromToken_ShouldReturnCorrectEmail() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    void getDeviceFingerprintFromToken_ShouldReturnCorrectFingerprint() {
        // Arrange
        String deviceFingerprint = "testDeviceFingerprint";
        String token = jwtTokenProvider.generateToken(authentication, deviceFingerprint, "127.0.0.1");

        // Act
        String extractedFingerprint = jwtTokenProvider.getDeviceFingerprintFromToken(token);

        // Assert
        assertEquals(deviceFingerprint, extractedFingerprint);
    }

    @Test
    void getClaimsFromToken_ShouldReturnAllClaims() {
        // Arrange
        String deviceFingerprint = "testDeviceFingerprint";
        String ipAddress = "127.0.0.1";
        String token = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);

        // Act
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        // Assert
        assertNotNull(claims);
        assertEquals(testUser.getId().toString(), claims.getSubject());
        assertEquals(testUser.getEmail(), claims.get("email"));
        assertEquals(testUser.getSubscriptionTier().getValue(), claims.get("subscription_tier"));
        assertEquals(testUser.getKycStatus().getValue(), claims.get("kyc_status"));
        assertEquals(deviceFingerprint, claims.get("device_fingerprint"));
        assertEquals(ipAddress, claims.get("ip_address"));
        assertEquals("access", claims.get("token_type"));
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.here");

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForNullToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_ShouldReturnFalseForFreshToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    void validateDeviceFingerprint_ShouldReturnTrueForMatchingFingerprint() {
        // Arrange
        String deviceFingerprint = "testDeviceFingerprint";
        String token = jwtTokenProvider.generateToken(authentication, deviceFingerprint, "127.0.0.1");

        // Act
        boolean isValid = jwtTokenProvider.validateDeviceFingerprint(token, deviceFingerprint);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateDeviceFingerprint_ShouldReturnFalseForDifferentFingerprint() {
        // Arrange
        String originalFingerprint = "originalFingerprint";
        String differentFingerprint = "differentFingerprint";
        String token = jwtTokenProvider.generateToken(authentication, originalFingerprint, "127.0.0.1");

        // Act
        boolean isValid = jwtTokenProvider.validateDeviceFingerprint(token, differentFingerprint);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getTokenFromHeader_ShouldExtractTokenFromBearerHeader() {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String authHeader = "Bearer " + token;

        // Act
        String extractedToken = jwtTokenProvider.getTokenFromHeader(authHeader);

        // Assert
        assertEquals(token, extractedToken);
    }

    @Test
    void getTokenFromHeader_ShouldReturnNullForInvalidHeader() {
        // Act
        String extractedToken = jwtTokenProvider.getTokenFromHeader("Invalid header");

        // Assert
        assertNull(extractedToken);
    }

    @Test
    void getTokenFromHeader_ShouldReturnNullForNullHeader() {
        // Act
        String extractedToken = jwtTokenProvider.getTokenFromHeader(null);

        // Assert
        assertNull(extractedToken);
    }

    @Test
    void generateSystemToken_ShouldCreateValidSystemToken() {
        // Arrange
        String purpose = "automated-cleanup";
        long expirationMs = 3600000L; // 1 hour

        // Act
        String systemToken = jwtTokenProvider.generateSystemToken(purpose, expirationMs);

        // Assert
        assertNotNull(systemToken);
        assertTrue(jwtTokenProvider.validateToken(systemToken));
        
        Claims claims = jwtTokenProvider.getClaimsFromToken(systemToken);
        assertEquals("system", claims.getSubject());
        assertEquals(purpose, claims.get("purpose"));
        assertEquals("system", claims.get("token_type"));
    }

    @Test
    void getRemainingExpirationTime_ShouldReturnPositiveValueForFreshToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        long remainingTime = jwtTokenProvider.getRemainingExpirationTime(token);

        // Assert
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= 900000L); // Should be less than or equal to 15 minutes
    }

    @Test
    void getTokenTypeFromToken_ShouldReturnCorrectType() {
        // Arrange
        String accessToken = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser, "deviceFingerprint");

        // Act
        String accessTokenType = jwtTokenProvider.getTokenTypeFromToken(accessToken);
        String refreshTokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);

        // Assert
        assertEquals("access", accessTokenType);
        assertEquals("refresh", refreshTokenType);
    }

    @Test
    void getExpirationDateFromToken_ShouldReturnValidDate() {
        // Arrange
        String token = jwtTokenProvider.generateToken(authentication, "deviceFingerprint", "127.0.0.1");

        // Act
        java.util.Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Assert
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new java.util.Date()));
    }

    @Test
    void tokenClaims_ShouldContainUserContextInformation() {
        // Arrange
        String deviceFingerprint = "device123";
        String ipAddress = "192.168.1.100";
        String token = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);

        // Act
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        // Assert
        assertEquals("trademaster-auth-service", claims.getIssuer());
        assertEquals(testUser.getId().toString(), claims.getSubject());
        assertEquals(testUser.getEmail(), claims.get("email"));
        assertEquals(testUser.getSubscriptionTier().getValue(), claims.get("subscription_tier"));
        assertEquals(testUser.getKycStatus().getValue(), claims.get("kyc_status"));
        assertEquals(deviceFingerprint, claims.get("device_fingerprint"));
        assertEquals(ipAddress, claims.get("ip_address"));
        assertEquals("access", claims.get("token_type"));
    }
}