package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SocialShare entity - tracks social media shares of articles
 */
@Entity
@Table(name = "social_shares", indexes = {
        @Index(name = "idx_share_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_share_platform", columnList = "platform"),
        @Index(name = "idx_share_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_id")
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType; // "NEWS", "OPINION"

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform; // "FACEBOOK", "TWITTER", "LINKEDIN", "WHATSAPP", "TELEGRAM", "COPY_LINK"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // nullable for anonymous shares

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
