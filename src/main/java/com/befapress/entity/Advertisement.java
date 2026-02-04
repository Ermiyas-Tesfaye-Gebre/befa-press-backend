package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Advertisement Entity
 * Handles all ad types: Display, Native, Video, Script/Programmatic
 */
@Entity
@Table(name = "advertisements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long id;

    @Column(nullable = false)
    private String title; // Internal reference name

    /**
     * Ad Type:
     * - DISPLAY_IMAGE (Banner, Sidebar Image)
     * - DISPLAY_SCRIPT (AdSense, Programmatic)
     * - NATIVE (Native In-Feed)
     * - VIDEO (Video Ad)
     * - INTERSTITIAL (Popup/Sticky)
     */
    @Column(name = "ad_type", nullable = false)
    private String adType;

    /**
     * Placement Zone:
     * - HOME_BANNER (Top of home)
     * - SIDEBAR (Right sidebar)
     * - IN_ARTICLE (Middle of articles)
     * - IN_FEED (Between news items)
     * - POPUP (Overlay)
     */
    @Column(name = "placement_zone", nullable = false)
    private String placementZone;

    // === CONTENT FIELDS ===

    @Column(name = "image_url")
    private String imageUrl; // For DISPLAY_IMAGE, NATIVE, INTERSTITIAL

    @Column(name = "video_url")
    private String videoUrl; // For VIDEO

    @Column(name = "target_url")
    private String targetUrl; // Destination link for clicks

    // For Programmatic/Script ads (e.g. AdSense)
    @Column(name = "script_content", columnDefinition = "TEXT")
    private String scriptContent;

    // For Native Ads
    @Column(name = "heading")
    private String heading;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cta_text")
    private String ctaText; // "Learn More", "Sign Up"

    // === SCHEDULING ===

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // ACTIVE, INACTIVE, EXPIRED, SCHEDULED
    @Column(name = "ad_status", nullable = false)
    @Builder.Default
    private String adStatus = "ACTIVE";

    // === ANALYTICS ===

    @Column(name = "position")
    private Integer position = 0;

    @Column(name = "views")
    @Builder.Default
    private Long views = 0L;

    @Column(name = "clicks")
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (adStatus == null)
            adStatus = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
