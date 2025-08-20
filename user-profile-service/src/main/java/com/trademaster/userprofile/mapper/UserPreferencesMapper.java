package com.trademaster.userprofile.mapper;

import com.trademaster.userprofile.dto.UserPreferencesDto;
import com.trademaster.userprofile.entity.UserPreferences;
import org.mapstruct.*;

/**
 * Mapper for UserPreferences entity and DTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserPreferencesMapper {

    /**
     * Convert entity to DTO
     */
    @Mapping(source = "userProfile.id", target = "userProfileId")
    UserPreferencesDto toDto(UserPreferences preferences);

    /**
     * Convert DTO to entity (excluding userProfile)
     */
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserPreferences toEntity(UserPreferencesDto dto);

    /**
     * Update existing entity from DTO
     */
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(UserPreferencesDto dto, @MappingTarget UserPreferences preferences);

    /**
     * Create a minimal DTO with just theme preferences
     */
    @Named("toThemeDto")
    default UserPreferencesDto toThemeDto(UserPreferences preferences) {
        if (preferences == null) return null;
        
        return UserPreferencesDto.builder()
            .id(preferences.getId())
            .userProfileId(preferences.getUserProfile() != null ? preferences.getUserProfile().getId() : null)
            .theme(preferences.getTheme())
            .language(preferences.getLanguage())
            .timezone(preferences.getTimezone())
            .build();
    }

    /**
     * Create a minimal DTO with just notification preferences
     */
    @Named("toNotificationDto")
    default UserPreferencesDto toNotificationDto(UserPreferences preferences) {
        if (preferences == null) return null;
        
        return UserPreferencesDto.builder()
            .id(preferences.getId())
            .userProfileId(preferences.getUserProfile() != null ? preferences.getUserProfile().getId() : null)
            .emailNotifications(preferences.getEmailNotifications())
            .smsNotifications(preferences.getSmsNotifications())
            .pushNotifications(preferences.getPushNotifications())
            .tradingAlerts(preferences.getTradingAlerts())
            .marketNews(preferences.getMarketNews())
            .priceAlerts(preferences.getPriceAlerts())
            .build();
    }

    /**
     * Create a minimal DTO with just trading preferences
     */
    @Named("toTradingDto")
    default UserPreferencesDto toTradingDto(UserPreferences preferences) {
        if (preferences == null) return null;
        
        return UserPreferencesDto.builder()
            .id(preferences.getId())
            .userProfileId(preferences.getUserProfile() != null ? preferences.getUserProfile().getId() : null)
            .defaultOrderType(preferences.getDefaultOrderType())
            .confirmationDialogs(preferences.getConfirmationDialogs())
            .riskWarnings(preferences.getRiskWarnings())
            .chartType(preferences.getChartType())
            .defaultTimeFrame(preferences.getDefaultTimeFrame())
            .build();
    }
}