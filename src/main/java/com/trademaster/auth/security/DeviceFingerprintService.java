package com.trademaster.auth.security;

import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    
    // Functional pattern maps for device classification
    private static final Map<String, Predicate<String>> DEVICE_TYPE_PATTERNS = Map.of(
        "mobile", ua -> ua.contains("mobile") || ua.contains("iphone") || ua.contains("android"),
        "tablet", ua -> ua.contains("tablet") || ua.contains("ipad"),
        "desktop", ua -> ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("firefox")
    );
    
    private static final Map<String, Predicate<String>> OS_PATTERNS = Map.of(
        "Windows", ua -> ua.contains("windows"),
        "macOS", ua -> ua.contains("mac os") || ua.contains("macos"),
        "Linux", ua -> ua.contains("linux"),
        "Android", ua -> ua.contains("android"),
        "iOS", ua -> ua.contains("iphone") || ua.contains("ipad")
    );
    
    private static final Map<String, Predicate<String>> BROWSER_PATTERNS = Map.of(
        "Chrome", ua -> ua.contains("chrome") && !ua.contains("edg"),
        "Firefox", ua -> ua.contains("firefox"),
        "Safari", ua -> ua.contains("safari") && !ua.contains("chrome"),
        "Edge", ua -> ua.contains("edg"),
        "Opera", ua -> ua.contains("opera")
    );

    /**
     * Generate device fingerprint from HTTP request
     */
    public String generateFingerprint(HttpServletRequest request) {
        return SafeOperations.safelyToResult(() -> buildFingerprintData(request))
            .flatMap(this::generateHash)
            .fold(
                hash -> hash,
                error -> {
                    log.error("Error generating device fingerprint: {}", error);
                    return generateBasicFingerprint(request);
                }
            );
    }

    /**
     * Normalize User Agent string for consistent fingerprinting
     */
    private String normalizeUserAgent(String userAgent) {
        return Optional.ofNullable(userAgent)
            .map(ua -> Stream.of(
                new String[]{"Chrome/[\\d.]+", "Chrome/*"},
                new String[]{"Firefox/[\\d.]+", "Firefox/*"},
                new String[]{"Safari/[\\d.]+", "Safari/*"},
                new String[]{"Edge/[\\d.]+", "Edge/*"},
                new String[]{"Version/[\\d.]+", "Version/*"}
            )
                .reduce(ua, (current, replacement) -> current.replaceAll(replacement[0], replacement[1]), (a, b) -> b))
            .map(normalized -> normalized.length() > 500 ? normalized.substring(0, 500) : normalized)
            .orElse("unknown");
    }

    /**
     * Generate basic fingerprint as fallback
     */
    private String generateBasicFingerprint(HttpServletRequest request) {
        String basicData = String.join("|",
                "ua:" + Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown"),
                "accept:" + Optional.ofNullable(request.getHeader("Accept")).orElse("unknown"),
                "lang:" + Optional.ofNullable(request.getHeader("Accept-Language")).orElse("unknown"),
                "salt:" + FINGERPRINT_SALT
        );
        
        return generateHash(basicData).orElse(String.valueOf(basicData.hashCode()));
    }

    /**
     * Generate SHA-256 hash of input string
     */
    private Result<String, String> generateHash(String input) {
        return SafeOperations.safelyToResult(() -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

                String hash = IntStream.range(0, hashBytes.length)
                    .mapToObj(i -> String.format("%02x", hashBytes[i] & 0xff))
                    .collect(Collectors.joining());

                return hash;
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate hash: " + e.getMessage(), e);
            }
        }).fold(
            error -> Result.failure("Hash generation failed: " + error),
            hash -> Result.success(hash)
        );
    }

    /**
     * Extract device type from User Agent
     */
    public String getDeviceType(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent"))
            .map(String::toLowerCase)
            .map(ua -> DEVICE_TYPE_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().test(ua))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("api"))
            .orElse("unknown");
    }

    /**
     * Extract operating system from User Agent
     */
    public String getOperatingSystem(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent"))
            .map(String::toLowerCase)
            .map(ua -> OS_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().test(ua))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("unknown"))
            .orElse("unknown");
    }

    /**
     * Extract browser name from User Agent
     */
    public String getBrowserName(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent"))
            .map(String::toLowerCase)
            .map(ua -> BROWSER_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().test(ua))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("unknown"))
            .orElse("unknown");
    }

    /**
     * Validate fingerprint strength
     */
    public boolean isStrongFingerprint(String fingerprint) {
        return Optional.ofNullable(fingerprint)
            .filter(fp -> !fp.isEmpty() && !fp.equals("unknown"))
            .map(fp -> fp.length() >= 32)
            .orElse(false);
    }

    /**
     * Calculate fingerprint entropy (simple measure)
     */
    public double calculateFingerprintEntropy(String fingerprint) {
        return Optional.ofNullable(fingerprint)
            .filter(fp -> !fp.isEmpty())
            .map(fp -> {
                int[] charFreq = new int[256];
                fp.chars().forEach(c -> charFreq[c]++);
                
                return Arrays.stream(charFreq)
                    .filter(freq -> freq > 0)
                    .mapToDouble(freq -> {
                        double probability = (double) freq / fp.length();
                        return probability * (Math.log(probability) / Math.log(2));
                    })
                    .map(p -> -p)
                    .sum();
            })
            .orElse(0.0);
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
        return Optional.ofNullable(fp1)
            .flatMap(f1 -> Optional.ofNullable(fp2).map(f2 -> new String[]{f1, f2}))
            .filter(fps -> fps[0].equals(fps[1]) || Math.abs(fps[0].length() - fps[1].length()) <= 5)
            .map(fps -> fps[0].equals(fps[1]) ? 1.0 : calculateSimilarityScore(fps[0], fps[1]))
            .map(similarity -> similarity >= threshold)
            .orElse(false);
    }
    
    private double calculateSimilarityScore(String fp1, String fp2) {
        int minLength = Math.min(fp1.length(), fp2.length());
        long differences = IntStream.range(0, minLength)
            .filter(i -> fp1.charAt(i) != fp2.charAt(i))
            .count();
        return 1.0 - ((double) differences / minLength);
    }

    // Functional helper methods
    
    private String buildFingerprintData(HttpServletRequest request) {
        Map<String, String> headers = Map.of(
            "ua", normalizeUserAgent(request.getHeader("User-Agent")),
            "accept", Optional.ofNullable(request.getHeader("Accept")).orElse(""),
            "lang", Optional.ofNullable(request.getHeader("Accept-Language")).orElse(""),
            "enc", Optional.ofNullable(request.getHeader("Accept-Encoding")).orElse(""),
            "conn", Optional.ofNullable(request.getHeader("Connection")).orElse("")
        );
        
        Map<String, String> customHeaders = Map.of(
            "xff", extractFirstXForwardedFor(request),
            "secua", Optional.ofNullable(request.getHeader("Sec-CH-UA")).orElse(""),
            "platform", Optional.ofNullable(request.getHeader("Sec-CH-UA-Platform")).orElse(""),
            "screen", Optional.ofNullable(request.getHeader("X-Screen-Resolution")).orElse(""),
            "tz", Optional.ofNullable(request.getHeader("X-Timezone-Offset")).orElse(""),
            "color", Optional.ofNullable(request.getHeader("X-Color-Depth")).orElse("")
        );
        
        String headerData = headers.entrySet().stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining("|"));
            
        String customData = customHeaders.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining("|"));
            
        return String.join("|", headerData, customData, "salt:" + FINGERPRINT_SALT);
    }
    
    private String extractFirstXForwardedFor(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .map(xff -> xff.split(",")[0].trim())
            .orElse("");
    }
}