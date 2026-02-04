package com.befapress.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // INSULT, HATE_SPEECH, etc.
    @Column(nullable = false)
    private String category;

    // AMHARIC, ENGLISH
    @Column(nullable = false)
    private String language;

    // The sensitive word or regex pattern
    @Column(nullable = false)
    private String pattern;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
