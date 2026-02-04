package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Queue for tracking social media share requests.
 * Supports retry logic for failed shares.
 */
@Entity
@Table(name = "social_share_queue", indexes = {
        @Index(name = "idx_share_status", columnList = "status"),
        @Index(name = "idx_share_entity", columnList = "entityType, entityId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialShareQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialPlatformConfig.Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShareStatus status = ShareStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    // External post ID if available (e.g., Telegram message_id)
    private String externalPostId;

    private LocalDateTime scheduledAt;

    private LocalDateTime sharedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum EntityType {
        NEWS,
        OPINION
    }

    public enum ShareStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markSuccess(String externalId) {
        this.status = ShareStatus.SUCCESS;
        this.externalPostId = externalId;
        this.sharedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = ShareStatus.FAILED;
        this.lastError = error;
    }
}
