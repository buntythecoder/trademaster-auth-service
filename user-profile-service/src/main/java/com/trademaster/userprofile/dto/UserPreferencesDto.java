package com.trademaster.userprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for User Preferences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesDto {

    private UUID id;

    private UUID userProfileId;

    // Theme and Display Preferences
    @NotBlank(message = "Theme is required")
    @Pattern(regexp = "^(light|dark|auto)$", message = "Theme must be one of: light, dark, auto")
    @Builder.Default
    private String theme = "auto";

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 5, message = "Language code must be between 2 and 5 characters")
    @Builder.Default
    private String language = "en";

    @NotBlank(message = "Timezone is required")
    @Builder.Default
    private String timezone = "UTC";

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be uppercase letters")
    @Builder.Default
    private String currency = "USD";

    // Notification Preferences
    @NotNull(message = "Email notifications preference is required")
    @Builder.Default
    private Boolean emailNotifications = true;

    @NotNull(message = "SMS notifications preference is required")
    @Builder.Default
    private Boolean smsNotifications = false;

    @NotNull(message = "Push notifications preference is required")
    @Builder.Default
    private Boolean pushNotifications = true;

    @NotNull(message = "Trading alerts preference is required")
    @Builder.Default
    private Boolean tradingAlerts = true;

    @NotNull(message = "Market news preference is required")
    @Builder.Default
    private Boolean marketNews = true;

    @NotNull(message = "Price alerts preference is required")
    @Builder.Default
    private Boolean priceAlerts = true;

    // Dashboard and UI Preferences
    @NotBlank(message = "Dashboard layout is required")
    @Builder.Default
    private String dashboardLayout = "default";

    @NotBlank(message = "Chart type is required")
    @Pattern(regexp = "^(candlestick|line|bar|area)$", message = "Chart type must be one of: candlestick, line, bar, area")
    @Builder.Default
    private String chartType = "candlestick";

    @NotBlank(message = "Default time frame is required")
    @Pattern(regexp = "^(1m|5m|15m|30m|1h|4h|1d|1w|1M)$", message = "Invalid time frame")
    @Builder.Default
    private String defaultTimeFrame = "1d";

    @NotNull(message = "Show portfolio performance preference is required")
    @Builder.Default
    private Boolean showPortfolioPerformance = true;

    @NotNull(message = "Show market overview preference is required")
    @Builder.Default
    private Boolean showMarketOverview = true;

    @NotNull(message = "Show watchlist preference is required")
    @Builder.Default
    private Boolean showWatchlist = true;

    // Trading Preferences
    @NotBlank(message = "Default order type is required")
    @Pattern(regexp = "^(market|limit|stop|stop_limit)$", message = "Order type must be one of: market, limit, stop, stop_limit")
    @Builder.Default
    private String defaultOrderType = "limit";

    @NotNull(message = "Confirmation dialogs preference is required")
    @Builder.Default
    private Boolean confirmationDialogs = true;

    @NotNull(message = "Risk warnings preference is required")
    @Builder.Default
    private Boolean riskWarnings = true;

    // Privacy Preferences
    @NotBlank(message = "Profile visibility is required")
    @Pattern(regexp = "^(public|private|friends)$", message = "Profile visibility must be one of: public, private, friends")
    @Builder.Default
    private String profileVisibility = "private";

    @NotNull(message = "Show trading history preference is required")
    @Builder.Default
    private Boolean showTradingHistory = false;

    @NotNull(message = "Allow analytics preference is required")
    @Builder.Default
    private Boolean allowAnalytics = true;

    // Advanced Preferences
    private Map<String, String> advancedPreferences;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    // Convenience methods
    public boolean isDarkTheme() {
        return "dark".equalsIgnoreCase(this.theme);
    }

    public boolean isLightTheme() {
        return "light".equalsIgnoreCase(this.theme);
    }

    public boolean isAutoTheme() {
        return "auto".equalsIgnoreCase(this.theme);
    }

    public boolean hasAnyNotificationsEnabled() {
        return Boolean.TRUE.equals(emailNotifications) || 
               Boolean.TRUE.equals(smsNotifications) || 
               Boolean.TRUE.equals(pushNotifications);
    }

    public boolean hasTradingNotificationsEnabled() {
        return Boolean.TRUE.equals(tradingAlerts) || Boolean.TRUE.equals(priceAlerts);
    }
}

/**
 * DTO for updating specific preference categories
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ThemePreferencesDto {
    @Pattern(regexp = "^(light|dark|auto)$", message = "Theme must be one of: light, dark, auto")
    private String theme;
    
    @Size(min = 2, max = 5, message = "Language code must be between 2 and 5 characters")
    private String language;
    
    private String timezone;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class NotificationPreferencesDto {
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    private Boolean tradingAlerts;
    private Boolean marketNews;
    private Boolean priceAlerts;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class TradingPreferencesDto {
    @Pattern(regexp = "^(market|limit|stop|stop_limit)$", message = "Order type must be one of: market, limit, stop, stop_limit")
    private String defaultOrderType;
    
    private Boolean confirmationDialogs;
    private Boolean riskWarnings;
    
    @Pattern(regexp = "^(candlestick|line|bar|area)$", message = "Chart type must be one of: candlestick, line, bar, area")
    private String chartType;
    
    @Pattern(regexp = "^(1m|5m|15m|30m|1h|4h|1d|1w|1M)$", message = "Invalid time frame")
    private String defaultTimeFrame;
}