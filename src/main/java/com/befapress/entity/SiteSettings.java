package com.befapress.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "site_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Branding
    @Column(nullable = false)
    private String siteName;

    @Column
    private String siteNameAmharic;

    @Column(length = 500)
    private String tagline;

    @Column(length = 500)
    private String taglineAmharic;

    @Column
    private String logoUrl;

    @Column
    private String faviconUrl;

    // Localization
    @Column(nullable = false)
    @Builder.Default
    private String defaultLanguage = "en";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> availableLanguages = List.of("en", "am");

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Africa/Addis_Ababa";

    @Column
    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";

    @Column
    @Builder.Default
    private String timeFormat = "HH:mm";

    // Contact & Social
    @Column
    private String contactEmail;

    @Column
    private String contactPhone;

    @Column(length = 500)
    private String contactAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> socialLinks;

    // SEO
    @Column(length = 500)
    private String metaDescription;

    @Column(length = 500)
    private String metaDescriptionAmharic;

    @Column
    private String defaultOgImage;

    // Subscriptions
    @Column
    @Builder.Default
    private Integer subscriptionDays = 30;

    @Column
    @Builder.Default
    private Integer expiryReminderDays = 3;

    // System
    @Column
    @Builder.Default
    private Boolean maintenanceMode = false;

    @Column
    @Builder.Default
    private Boolean analyticsEnabled = true;

    @Column
    @Builder.Default
    private Boolean commentsEnabled = true;

    @Column
    @Builder.Default
    private Boolean registrationEnabled = true;

    // Audit
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
