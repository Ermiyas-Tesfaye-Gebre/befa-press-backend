package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top article with engagement metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopArticleDTO {
    private Long id;
    private String title;
    private String category;
    private String entityType; // "NEWS" or "OPINION"
    private Long views;
    private String avgTime; // formatted as "4m 32s"
    private Integer avgScrollDepth;
    private Long comments;
    private Long shares;
    private String coverImage;
    private String slug;
    private java.time.LocalDateTime publishedAt;
}
