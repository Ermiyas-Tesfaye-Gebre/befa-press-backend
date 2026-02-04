package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Configuration for social media platform integrations.
 * Stores credentials and settings for each platform.
 */
@Entity
@Table(name = "social_platform_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPlatformConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Platform platform;

    @Column(nullable = false)
    private Boolean enabled = false;

    // Encrypted JSON containing platform-specific credentials
    // For Telegram: {"botToken": "...", "channelId": "..."}
    // For Facebook: {"pageId": "...", "accessToken": "..."}
    @Column(columnDefinition = "TEXT")
    private String credentials;

    // Public URL of the channel/page for display purposes
    @Column(length = 255)
    private String channelUrl;

    // Optional: Custom message template
    @Column(columnDefinition = "TEXT")
    private String messageTemplate;

    // Share news articles to this platform
    @Column(nullable = false)
    private Boolean shareNews = true;

    // Share opinion articles to this platform
    @Column(nullable = false)
    private Boolean shareOpinions = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Platform {
        TELEGRAM,
        FACEBOOK,
        TWITTER,
        LINKEDIN,
        WHATSAPP
    }
}
