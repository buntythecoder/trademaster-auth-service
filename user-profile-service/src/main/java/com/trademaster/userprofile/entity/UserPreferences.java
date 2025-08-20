package com.trademaster.userprofile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User Preferences Entity
 * Stores user customization settings and preferences
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the user profile
     */
    @OneToOne
    @JoinColumn(name = "user_profile_id", referencedColumnName = "id", unique = true)
    private UserProfile userProfile;

    /**
     * Theme preference: light, dark, auto
     */
    @Column(name = "theme", nullable = false)
    @Builder.Default
    private String theme = "auto";

    /**
     * Language preference (ISO 639-1 code)
     */
    @Column(name = "language", nullable = false, length = 5)
    @Builder.Default
    private String language = "en";

    /**
     * Timezone preference
     */
    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    /**
     * Currency preference (ISO 4217 code)
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Notification preferences
     */
    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    @Builder.Default
    private Boolean smsNotifications = false;

    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private Boolean pushNotifications = true;

    @Column(name = "trading_alerts", nullable = false)
    @Builder.Default
    private Boolean tradingAlerts = true;

    @Column(name = "market_news", nullable = false)
    @Builder.Default
    private Boolean marketNews = true;

    @Column(name = "price_alerts", nullable = false)
    @Builder.Default
    private Boolean priceAlerts = true;

    /**
     * Dashboard and UI preferences
     */
    @Column(name = "dashboard_layout", nullable = false)
    @Builder.Default
    private String dashboardLayout = "default";

    @Column(name = "chart_type", nullable = false)
    @Builder.Default
    private String chartType = "candlestick";

    @Column(name = "default_time_frame", nullable = false)
    @Builder.Default
    private String defaultTimeFrame = "1d";

    @Column(name = "show_portfolio_performance", nullable = false)
    @Builder.Default
    private Boolean showPortfolioPerformance = true;

    @Column(name = "show_market_overview", nullable = false)
    @Builder.Default
    private Boolean showMarketOverview = true;

    @Column(name = "show_watchlist", nullable = false)
    @Builder.Default
    private Boolean showWatchlist = true;

    /**
     * Trading preferences
     */
    @Column(name = "default_order_type", nullable = false)
    @Builder.Default
    private String defaultOrderType = "limit";

    @Column(name = "confirmation_dialogs", nullable = false)
    @Builder.Default
    private Boolean confirmationDialogs = true;

    @Column(name = "risk_warnings", nullable = false)
    @Builder.Default
    private Boolean riskWarnings = true;

    /**
     * Privacy preferences
     */
    @Column(name = "profile_visibility", nullable = false)
    @Builder.Default
    private String profileVisibility = "private";

    @Column(name = "show_trading_history", nullable = false)
    @Builder.Default
    private Boolean showTradingHistory = false;

    @Column(name = "allow_analytics", nullable = false)
    @Builder.Default
    private Boolean allowAnalytics = true;

    /**
     * Advanced preferences stored as JSON
     * For complex preferences that don't warrant individual columns
     */
    @ElementCollection
    @CollectionTable(
        name = "user_preference_data",
        joinColumns = @JoinColumn(name = "user_preferences_id")
    )
    @MapKeyColumn(name = "preference_key")
    @Column(name = "preference_value", length = 1000)
    private Map<String, String> advancedPreferences;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking
     */
    @Version
    private Integer version;

    // Convenience methods for theme management
    public boolean isDarkTheme() {
        return "dark".equalsIgnoreCase(this.theme);
    }

    public boolean isLightTheme() {
        return "light".equalsIgnoreCase(this.theme);
    }

    public boolean isAutoTheme() {
        return "auto".equalsIgnoreCase(this.theme);
    }

    // Convenience methods for notifications
    public boolean hasAnyNotificationsEnabled() {
        return emailNotifications || smsNotifications || pushNotifications;
    }

    public boolean hasTradingNotificationsEnabled() {
        return tradingAlerts || priceAlerts;
    }

    // Method to get advanced preference with default
    public String getAdvancedPreference(String key, String defaultValue) {
        if (advancedPreferences != null && advancedPreferences.containsKey(key)) {
            return advancedPreferences.get(key);
        }
        return defaultValue;
    }

    // Method to set advanced preference
    public void setAdvancedPreference(String key, String value) {
        if (advancedPreferences == null) {
            advancedPreferences = new java.util.HashMap<>();
        }
        advancedPreferences.put(key, value);
    }
}