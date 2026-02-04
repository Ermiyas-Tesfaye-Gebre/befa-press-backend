package com.befapress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CommentReport entity - tracks user reports on comments with specific reasons
 */
@Entity
@Table(name = "comment_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    // Who reported (null for guests)
    @Column(name = "reporter_email")
    private String reporterEmail;

    // For tracking guest reporters
    @Column(name = "reporter_ip")
    private String reporterIp;

    // INSULT, MOCKERY, THREAT, AD_HOMINEM, OFF_TOPIC, HATE_SPEECH, OTHER
    @Column(nullable = false, length = 50)
    private String reason;

    // Optional additional details from reporter
    @Column(columnDefinition = "TEXT")
    private String description;

    // PENDING, RESOLVED_APPROVED, RESOLVED_REJECTED
    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
