package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Cloudinary Configuration Entity
 * Single-row pattern — stores cloud storage settings in the database
 * so they can be edited from the admin UI without redeploying.
 */
@Entity
@Table(name = "cloudinary_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudinaryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloud_name", nullable = false)
    private String cloudName;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "api_secret", nullable = false)
    private String apiSecret;

    @Column(name = "default_folder")
    @Builder.Default
    private String defaultFolder = "befa-press";

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "max_image_size_mb")
    @Builder.Default
    private Integer maxImageSizeMb = 10;

    @Column(name = "max_video_size_mb")
    @Builder.Default
    private Integer maxVideoSizeMb = 50;

    @Column(name = "upload_preset")
    private String uploadPreset;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
