package com.befapress.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * UserPreferences entity - stores user settings for notifications, display,
 * etc.
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class UserPreferences {

    @Id
    private Long id; // Same as user_id (one-to-one)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // ==================== NOTIFICATION PREFERENCES ====================

    @Column(name = "push_enabled")
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(name = "notify_breaking_news")
    @Builder.Default
    private boolean notifyBreakingNews = true;

    @Column(name = "notify_new_opinions")
    @Builder.Default
    private boolean notifyNewOpinions = false;

    @Column(name = "notify_comments")
    @Builder.Default
    private boolean notifyComments = true; // When someone replies to their comment

    @Column(name = "notify_likes")
    @Builder.Default
    private boolean notifyLikes = false;

    @Column(name = "email_notifications")
    @Builder.Default
    private boolean emailNotifications = true;

    // ==================== DISPLAY PREFERENCES ====================

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en"; // en, am

    @Column(name = "font_size", length = 20)
    @Builder.Default
    private String fontSize = "medium"; // small, medium, large

    @Column(name = "dark_mode")
    @Builder.Default
    private boolean darkMode = false;

    // ==================== CONTENT PREFERENCES ====================

    @Column(name = "preferred_categories", length = 500)
    private String preferredCategories; // Comma-separated category IDs

    // ==================== TIMESTAMPS ====================

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
