package com.befapress.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {

    // Branding
    @NotBlank(message = "Site name is required")
    private String siteName;
    private String siteNameAmharic;
    private String tagline;
    private String taglineAmharic;
    private String logoUrl;
    private String faviconUrl;

    // Localization
    @NotBlank(message = "Default language is required")
    private String defaultLanguage;
    private List<String> availableLanguages;
    private String timezone;
    private String dateFormat;
    private String timeFormat;

    // Contact & Social
    @Email(message = "Invalid email format")
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private Map<String, String> socialLinks;

    // SEO
    private String metaDescription;
    private String metaDescriptionAmharic;
    private String defaultOgImage;

    // Subscriptions
    @Min(value = 1, message = "Subscription days must be at least 1")
    private Integer subscriptionDays;
    @Min(value = 1, message = "Expiry reminder days must be at least 1")
    private Integer expiryReminderDays;

    // System
    private Boolean maintenanceMode;
    private Boolean analyticsEnabled;
    private Boolean commentsEnabled;
    private Boolean registrationEnabled;
}
