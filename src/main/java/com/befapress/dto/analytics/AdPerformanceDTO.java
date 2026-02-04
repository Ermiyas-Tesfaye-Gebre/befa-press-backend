package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Ad performance statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdPerformanceDTO {
    private Long adId;
    private String title;
    private String position;
    private Long impressions;
    private Long clicks;
    private Double ctr; // Click-through rate percentage
    private String changePercent;
    private String trend;
}
