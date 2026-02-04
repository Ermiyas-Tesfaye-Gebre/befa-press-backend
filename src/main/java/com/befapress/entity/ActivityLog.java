package com.befapress.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // "NEWS", "USER", "COMMENT", "AD", "vSYSTEM"

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String actor; // "System", "Admin", "Moderator", or username

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "related_id")
    private Long relatedId; // ID of the related entity (news id, user id, etc)

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
