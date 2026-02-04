package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PageHit entity - tracks individual page views and user engagement metrics
 */
@Entity
@Table(name = "page_hits", indexes = {
        @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_created", columnList = "created_at"),
        @Index(name = "idx_country", columnList = "country"),
        @Index(name = "idx_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hit_id")
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType; // "NEWS", "OPINION", "CATEGORY", "HOME"

    @Column(name = "entity_id")
    private Long entityId; // nullable for pages like HOME

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // nullable for anonymous users

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", length = 500)
    private String referrer;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "device", length = 20)
    private String device; // "MOBILE", "DESKTOP", "TABLET"

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "language", length = 10)
    private String language; // "am", "en"

    @Column(name = "scroll_depth")
    @Builder.Default
    private Integer scrollDepth = 0; // 0-100%

    @Column(name = "time_on_page")
    @Builder.Default
    private Integer timeOnPage = 0; // seconds

    @Column(name = "is_bounce")
    @Builder.Default
    private Boolean isBounce = true; // true if user left without interaction

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
