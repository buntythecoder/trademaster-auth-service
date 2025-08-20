package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * User Device entity for device fingerprinting and trusted device management
 * 
 * Tracks user devices for security purposes:
 * - Device fingerprinting for fraud detection
 * - Trusted device management
 * - Location tracking for suspicious activity detection
 * - Device type classification
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_devices",
       indexes = {
           @Index(name = "idx_user_devices_user_id", columnList = "userId"),
           @Index(name = "idx_user_devices_fingerprint", columnList = "deviceFingerprint"),
           @Index(name = "idx_user_devices_trusted", columnList = "isTrusted"),
           @Index(name = "idx_user_devices_last_seen", columnList = "lastSeenAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "device_fingerprint"})
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_fingerprint", nullable = false, length = 512)
    private String deviceFingerprint;

    @Column(name = "device_name", length = 200)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 50)
    private DeviceType deviceType;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Type(JsonType.class)
    @Column(name = "location", columnDefinition = "jsonb")
    private Map<String, Object> location;

    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    // Business logic methods
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public void trust() {
        this.isTrusted = true;
    }

    public void untrust() {
        this.isTrusted = false;
    }

    public boolean isRecentlyActive() {
        return lastSeenAt != null && 
               lastSeenAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    public boolean isStaleDevice() {
        return lastSeenAt != null && 
               lastSeenAt.isBefore(LocalDateTime.now().minusDays(90));
    }

    public long getDaysSinceLastSeen() {
        if (lastSeenAt == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(lastSeenAt, LocalDateTime.now()).toDays();
    }

    public String getLocationString() {
        if (location == null || location.isEmpty()) {
            return "Unknown";
        }
        
        String city = (String) location.get("city");
        String country = (String) location.get("country");
        
        if (city != null && country != null) {
            return city + ", " + country;
        } else if (country != null) {
            return country;
        } else if (city != null) {
            return city;
        }
        
        return "Unknown";
    }

    public void updateLocation(String city, String country, String region, Double latitude, Double longitude) {
        if (location == null) {
            location = new java.util.HashMap<>();
        }
        
        if (city != null) location.put("city", city);
        if (country != null) location.put("country", country);
        if (region != null) location.put("region", region);
        if (latitude != null) location.put("latitude", latitude);
        if (longitude != null) location.put("longitude", longitude);
        location.put("updated_at", LocalDateTime.now().toString());
    }

    // Device Type enum
    public enum DeviceType {
        MOBILE("mobile"),
        DESKTOP("desktop"),
        TABLET("tablet"),
        API("api");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DeviceType fromUserAgent(String userAgent) {
            if (userAgent == null) {
                return API;
            }
            
            String ua = userAgent.toLowerCase();
            
            if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
                return MOBILE;
            } else if (ua.contains("tablet") || ua.contains("ipad")) {
                return TABLET;
            } else if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("firefox")) {
                return DESKTOP;
            } else {
                return API;
            }
        }
    }

    // Device classification methods
    public boolean isMobileDevice() {
        return DeviceType.MOBILE.equals(deviceType);
    }

    public boolean isDesktopDevice() {
        return DeviceType.DESKTOP.equals(deviceType);
    }

    public boolean isTabletDevice() {
        return DeviceType.TABLET.equals(deviceType);
    }

    public boolean isApiDevice() {
        return DeviceType.API.equals(deviceType);
    }

    // Security risk assessment
    public int calculateRiskScore() {
        int riskScore = 0;
        
        // Trust factor
        if (!isTrusted) {
            riskScore += 20;
        }
        
        // Activity factor
        long daysSinceLastSeen = getDaysSinceLastSeen();
        if (daysSinceLastSeen > 90) {
            riskScore += 30; // Very stale device
        } else if (daysSinceLastSeen > 30) {
            riskScore += 15; // Somewhat stale device
        }
        
        // Device type factor
        if (isApiDevice()) {
            riskScore += 10; // API access slightly more risky
        }
        
        // Location factor
        if (location == null || location.isEmpty()) {
            riskScore += 5; // Unknown location slightly risky
        }
        
        return Math.min(100, riskScore);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserDevice{id=%d, userId=%d, deviceType=%s, isTrusted=%s, location='%s', riskScore=%d}", 
                           id, userId, deviceType, isTrusted, getLocationString(), calculateRiskScore());
    }
}