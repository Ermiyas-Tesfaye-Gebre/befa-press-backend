package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comment entity - represents comments on news articles
 */
@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = true)
    private News news;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinion_id")
    private Opinion opinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable for guest comments

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // For nested replies

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "is_reported")
    @Builder.Default
    private boolean isReported = false;

    @Column(name = "report_count")
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * Comma-separated list of detected moderation flags
     * e.g., "INSULT,HATE_SPEECH" for opinion comments pending review
     */
    @Column(name = "moderation_flags", length = 500)
    private String moderationFlags;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

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

    // Check if comment is by a guest
    public boolean isGuestComment() {
        return user == null;
    }

    // Get author name (user or guest)
    public String getAuthorName() {
        return user != null ? user.getFullName() : guestName;
    }
}
