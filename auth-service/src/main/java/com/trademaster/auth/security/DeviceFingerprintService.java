package com.trademaster.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Device Fingerprinting Service for generating unique device identifiers
 * 
 * Creates device fingerprints based on:
 * - User Agent string
 * - Accept headers
 * - Accept-Language
 * - Accept-Encoding
 * - Screen resolution (if available)
 * - Timezone offset (if available)
 * 
 * Used for fraud detection and trusted device management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class DeviceFingerprintService {

    private static final String FINGERPRINT_SALT = "trademaster-device-salt-2024";

    /**
     * Generate device fingerprint from HTTP request
     */
    public String generateFingerprint(HttpServletRequest request) {
        try {
            StringBuilder fingerprintData = new StringBuilder();
            
            // Core browser information
            String userAgent = normalizeUserAgent(request.getHeader("User-Agent"));
            fingerprintData.append("ua:").append(userAgent).append("|");
            
            // Accept headers
            String accept = request.getHeader("Accept");
            fingerprintData.append("accept:").append(accept != null ? accept : "").append("|");
            
            String acceptLanguage = request.getHeader("Accept-Language");
            fingerprintData.append("lang:").append(acceptLanguage != null ? acceptLanguage : "").append("|");
            
            String acceptEncoding = request.getHeader("Accept-Encoding");
            fingerprintData.append("enc:").append(acceptEncoding != null ? acceptEncoding : "").append("|");
            
            // Connection information
            String connection = request.getHeader("Connection");
            fingerprintData.append("conn:").append(connection != null ? connection : "").append("|");
            
            // Custom headers that might indicate device characteristics
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null) {
                fingerprintData.append("xff:").append(xForwardedFor.split(",")[0].trim()).append("|");
            }
            
            // Browser-specific headers
            String secChUa = request.getHeader("Sec-CH-UA");
            if (secChUa != null) {
                fingerprintData.append("secua:").append(secChUa).append("|");
            }
            
            String secChUaPlatform = request.getHeader("Sec-CH-UA-Platform");
            if (secChUaPlatform != null) {
                fingerprintData.append("platform:").append(secChUaPlatform).append("|");
            }
            
            // Additional fingerprinting from custom headers (set by frontend)
            String screenResolution = request.getHeader("X-Screen-Resolution");
            if (screenResolution != null) {
                fingerprintData.append("screen:").append(screenResolution).append("|");
            }
            
            String timezone = request.getHeader("X-Timezone-Offset");
            if (timezone != null) {
                fingerprintData.append("tz:").append(timezone).append("|");
            }
            
            String colorDepth = request.getHeader("X-Color-Depth");
            if (colorDepth != null) {
                fingerprintData.append("color:").append(colorDepth).append("|");
            }
            
            // Add salt for security
            fingerprintData.append("salt:").append(FINGERPRINT_SALT);
            
            // Generate SHA-256 hash
            return generateHash(fingerprintData.toString());
            
        } catch (Exception e) {
            log.error("Error generating device fingerprint: {}", e.getMessage());
            // Fallback to basic fingerprint
            return generateBasicFingerprint(request);
        }
    }

    /**
     * Normalize User Agent string for consistent fingerprinting
     */
    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        
        // Remove version numbers that change frequently
        String normalized = userAgent
            .replaceAll("Chrome/[\\d.]+", "Chrome/xxx")
            .replaceAll("Firefox/[\\d.]+", "Firefox/xxx")
            .replaceAll("Safari/[\\d.]+", "Safari/xxx")
            .replaceAll("Edge/[\\d.]+", "Edge/xxx")
            .replaceAll("Version/[\\d.]+", "Version/xxx");
        
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    /**
     * Generate basic fingerprint as fallback
     */
    private String generateBasicFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        
        String basicData = String.format("ua:%s|accept:%s|lang:%s|salt:%s",
            userAgent != null ? userAgent : "unknown",
            accept != null ? accept : "unknown",
            acceptLanguage != null ? acceptLanguage : "unknown",
            FINGERPRINT_SALT);
        
        return generateHash(basicData);
    }

    /**
     * Generate SHA-256 hash of input string
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Extract device type from User Agent
     */
    public String getDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return "mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        } else if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("firefox")) {
            return "desktop";
        } else {
            return "api";
        }
    }

    /**
     * Extract operating system from User Agent
     */
    public String getOperatingSystem(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os") || ua.contains("macos")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        } else {
            return "unknown";
        }
    }

    /**
     * Extract browser name from User Agent
     */
    public String getBrowserName(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("chrome") && !ua.contains("edg")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("edg")) {
            return "Edge";
        } else if (ua.contains("opera")) {
            return "Opera";
        } else {
            return "unknown";
        }
    }

    /**
     * Validate fingerprint strength
     */
    public boolean isStrongFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            return false;
        }
        
        // Check if fingerprint has minimum entropy
        return fingerprint.length() >= 32 && !fingerprint.equals("unknown");
    }

    /**
     * Calculate fingerprint entropy (simple measure)
     */
    public double calculateFingerprintEntropy(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            return 0.0;
        }
        
        // Simple entropy calculation based on character distribution
        int[] charFreq = new int[256];
        for (char c : fingerprint.toCharArray()) {
            charFreq[c]++;
        }
        
        double entropy = 0.0;
        int length = fingerprint.length();
        
        for (int freq : charFreq) {
            if (freq > 0) {
                double probability = (double) freq / length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        
        return entropy;
    }

    /**
     * Generate device name from fingerprint components
     */
    public String generateDeviceName(HttpServletRequest request) {
        String os = getOperatingSystem(request);
        String browser = getBrowserName(request);
        String deviceType = getDeviceType(request);
        
        return String.format("%s %s on %s", browser, deviceType, os);
    }

    /**
     * Check if two fingerprints are similar (for browser updates tolerance)
     */
    public boolean areFingerprintsSimilar(String fp1, String fp2, double threshold) {
        if (fp1 == null || fp2 == null) {
            return false;
        }
        
        if (fp1.equals(fp2)) {
            return true;
        }
        
        // Calculate Hamming distance for similar-length strings
        if (Math.abs(fp1.length() - fp2.length()) > 5) {
            return false;
        }
        
        int differences = 0;
        int minLength = Math.min(fp1.length(), fp2.length());
        
        for (int i = 0; i < minLength; i++) {
            if (fp1.charAt(i) != fp2.charAt(i)) {
                differences++;
            }
        }
        
        double similarity = 1.0 - ((double) differences / minLength);
        return similarity >= threshold;
    }
}