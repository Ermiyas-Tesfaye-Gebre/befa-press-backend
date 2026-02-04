package com.befapress.service;

import com.befapress.dto.request.UpdateSettingsRequest;
import com.befapress.dto.response.SiteSettingsResponse;
import com.befapress.entity.SiteSettings;
import com.befapress.repository.SiteSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SiteSettingsRepository settingsRepository;

    /**
     * Get current site settings, creating defaults if none exist
     */
    public SiteSettingsResponse getSettings() {
        SiteSettings settings = settingsRepository.getSettings()
                .orElseGet(this::createDefaultSettings);
        return mapToResponse(settings);
    }

    /**
     * Get public-facing settings only (no admin-only fields)
     */
    public Map<String, Object> getPublicSettings() {
        SiteSettings settings = settingsRepository.getSettings()
                .orElseGet(this::createDefaultSettings);

        Map<String, Object> publicSettings = new HashMap<>();
        publicSettings.put("siteName", settings.getSiteName());
        publicSettings.put("siteNameAmharic", settings.getSiteNameAmharic());
        publicSettings.put("tagline", settings.getTagline());
        publicSettings.put("taglineAmharic", settings.getTaglineAmharic());
        publicSettings.put("logoUrl", settings.getLogoUrl());
        publicSettings.put("faviconUrl", settings.getFaviconUrl());
        publicSettings.put("defaultLanguage", settings.getDefaultLanguage());
        publicSettings.put("availableLanguages", settings.getAvailableLanguages());
        publicSettings.put("timezone", settings.getTimezone());
        publicSettings.put("socialLinks", settings.getSocialLinks());
        publicSettings.put("maintenanceMode", settings.getMaintenanceMode());
        publicSettings.put("commentsEnabled", settings.getCommentsEnabled());
        publicSettings.put("registrationEnabled", settings.getRegistrationEnabled());
        publicSettings.put("contactEmail", settings.getContactEmail());
        publicSettings.put("contactPhone", settings.getContactPhone());
        publicSettings.put("contactAddress", settings.getContactAddress());

        return publicSettings;
    }

    /**
     * Update site settings
     */
    @Transactional
    public SiteSettingsResponse updateSettings(UpdateSettingsRequest request, String updatedBy) {
        SiteSettings settings = settingsRepository.getSettings()
                .orElseGet(this::createDefaultSettings);

        // Update branding
        settings.setSiteName(request.getSiteName());
        settings.setSiteNameAmharic(request.getSiteNameAmharic());
        settings.setTagline(request.getTagline());
        settings.setTaglineAmharic(request.getTaglineAmharic());
        if (request.getLogoUrl() != null) {
            settings.setLogoUrl(request.getLogoUrl());
        }
        if (request.getFaviconUrl() != null) {
            settings.setFaviconUrl(request.getFaviconUrl());
        }

        // Update localization
        settings.setDefaultLanguage(request.getDefaultLanguage());
        if (request.getAvailableLanguages() != null) {
            settings.setAvailableLanguages(request.getAvailableLanguages());
        }
        if (request.getTimezone() != null) {
            settings.setTimezone(request.getTimezone());
        }
        if (request.getDateFormat() != null) {
            settings.setDateFormat(request.getDateFormat());
        }
        if (request.getTimeFormat() != null) {
            settings.setTimeFormat(request.getTimeFormat());
        }

        // Update contact & social
        settings.setContactEmail(request.getContactEmail());
        settings.setContactPhone(request.getContactPhone());
        settings.setContactAddress(request.getContactAddress());
        if (request.getSocialLinks() != null) {
            settings.setSocialLinks(request.getSocialLinks());
        }

        // Update SEO
        settings.setMetaDescription(request.getMetaDescription());
        settings.setMetaDescriptionAmharic(request.getMetaDescriptionAmharic());
        if (request.getDefaultOgImage() != null) {
            settings.setDefaultOgImage(request.getDefaultOgImage());
        }

        // Update subscriptions
        if (request.getSubscriptionDays() != null) {
            settings.setSubscriptionDays(request.getSubscriptionDays());
        }
        if (request.getExpiryReminderDays() != null) {
            settings.setExpiryReminderDays(request.getExpiryReminderDays());
        }

        // Update system flags
        if (request.getMaintenanceMode() != null) {
            settings.setMaintenanceMode(request.getMaintenanceMode());
        }
        if (request.getAnalyticsEnabled() != null) {
            settings.setAnalyticsEnabled(request.getAnalyticsEnabled());
        }
        if (request.getCommentsEnabled() != null) {
            settings.setCommentsEnabled(request.getCommentsEnabled());
        }
        if (request.getRegistrationEnabled() != null) {
            settings.setRegistrationEnabled(request.getRegistrationEnabled());
        }

        // Audit
        settings.setUpdatedBy(updatedBy);
        settings.setUpdatedAt(LocalDateTime.now());

        settings = settingsRepository.save(settings);
        log.info("Settings updated by {}", updatedBy);

        return mapToResponse(settings);
    }

    /**
     * Get list of available timezones
     */
    public List<String> getAvailableTimezones() {
        List<String> timezones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(timezones);
        return timezones;
    }

    /**
     * Get list of supported languages
     */
    public List<Map<String, String>> getSupportedLanguages() {
        return List.of(
                Map.of("code", "en", "name", "English", "nativeName", "English"),
                Map.of("code", "am", "name", "Amharic", "nativeName", "አማርኛ"),
                Map.of("code", "ti", "name", "Tigrinya", "nativeName", "ትግርኛ"),
                Map.of("code", "or", "name", "Oromo", "nativeName", "Afaan Oromoo"),
                Map.of("code", "so", "name", "Somali", "nativeName", "Soomaali"));
    }

    /**
     * Create default settings if none exist
     */
    private SiteSettings createDefaultSettings() {
        SiteSettings defaults = SiteSettings.builder()
                .siteName("BEFA Press")
                .siteNameAmharic("ቤፋ ፕሬስ")
                .tagline("Breaking Ethiopian Facts & Articles")
                .taglineAmharic("ሰበር የኢትዮጵያ ዜናዎች እና ጽሁፎች")
                .defaultLanguage("en")
                .availableLanguages(List.of("en", "am"))
                .timezone("Africa/Addis_Ababa")
                .dateFormat("DD/MM/YYYY")
                .timeFormat("HH:mm")
                .subscriptionDays(30)
                .expiryReminderDays(3)
                .maintenanceMode(false)
                .analyticsEnabled(true)
                .commentsEnabled(true)
                .registrationEnabled(true)
                .contactEmail("contact@befapress.com")
                .contactPhone("+251 911 123 456")
                .contactAddress("Addis Ababa, Ethiopia")
                .socialLinks(Map.of(
                        "facebook", "",
                        "twitter", "",
                        "telegram", "",
                        "youtube", ""))
                .build();

        return settingsRepository.save(defaults);
    }

    private SiteSettingsResponse mapToResponse(SiteSettings settings) {
        return SiteSettingsResponse.builder()
                .siteName(settings.getSiteName())
                .siteNameAmharic(settings.getSiteNameAmharic())
                .tagline(settings.getTagline())
                .taglineAmharic(settings.getTaglineAmharic())
                .logoUrl(settings.getLogoUrl())
                .faviconUrl(settings.getFaviconUrl())
                .defaultLanguage(settings.getDefaultLanguage())
                .availableLanguages(settings.getAvailableLanguages())
                .timezone(settings.getTimezone())
                .dateFormat(settings.getDateFormat())
                .timeFormat(settings.getTimeFormat())
                .contactEmail(settings.getContactEmail())
                .contactPhone(settings.getContactPhone())
                .contactAddress(settings.getContactAddress())
                .socialLinks(settings.getSocialLinks())
                .metaDescription(settings.getMetaDescription())
                .metaDescriptionAmharic(settings.getMetaDescriptionAmharic())
                .defaultOgImage(settings.getDefaultOgImage())
                .subscriptionDays(settings.getSubscriptionDays())
                .expiryReminderDays(settings.getExpiryReminderDays())
                .maintenanceMode(settings.getMaintenanceMode())
                .analyticsEnabled(settings.getAnalyticsEnabled())
                .commentsEnabled(settings.getCommentsEnabled())
                .registrationEnabled(settings.getRegistrationEnabled())
                .updatedAt(settings.getUpdatedAt())
                .updatedBy(settings.getUpdatedBy())
                .build();
    }
}
