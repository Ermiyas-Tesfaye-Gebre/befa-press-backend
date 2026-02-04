package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category performance statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceDTO {
    private Long categoryId;
    private String name;
    private Long totalViews;
    private Long totalArticles;
    private Double avgEngagement;
    private String changePercent;
    private String trend;
}
