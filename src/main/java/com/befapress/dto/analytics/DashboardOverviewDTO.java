package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Dashboard overview containing all key metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {

    // Key Metrics
    private MetricDTO totalPageViews;
    private MetricDTO avgSessionDuration;
    private MetricDTO newSubscribers;
    private MetricDTO bounceRate;

    // Quick Stats
    private Long activeUsersNow;
    private Long totalArticles;
    private Long totalUsers;

    // Distributions
    private Map<String, Long> deviceBreakdown;
    private Map<String, Long> topCountries;
}
