package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPerformanceDTO {
    private Long authorId;
    private String fullName;
    private String avatar;
    private Long totalViews;
    private Long totalArticles;
    private Double avgEngagement; // average scroll depth or time
    private String topCategory;
}
