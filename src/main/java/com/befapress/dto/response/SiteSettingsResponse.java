package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteSettingsResponse {

    // Branding
    private String siteName;
    private String siteNameAmharic;
    private String tagline;
    private String taglineAmharic;
    private String logoUrl;
    private String faviconUrl;

    // Localization
    private String defaultLanguage;
    private List<String> availableLanguages;
    private String timezone;
    private String dateFormat;
    private String timeFormat;

    // Contact & Social
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private Map<String, String> socialLinks;

    // SEO
    private String metaDescription;
    private String metaDescriptionAmharic;
    private String defaultOgImage;

    // Subscriptions
    private Integer subscriptionDays;
    private Integer expiryReminderDays;

    // System
    private Boolean maintenanceMode;
    private Boolean analyticsEnabled;
    private Boolean commentsEnabled;
    private Boolean registrationEnabled;

    // Audit
    private LocalDateTime updatedAt;
    private String updatedBy;
}
