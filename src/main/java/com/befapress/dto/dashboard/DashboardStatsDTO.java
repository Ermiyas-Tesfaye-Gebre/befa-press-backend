package com.befapress.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStatsDTO {
    // Key Stats
    private long totalNews;
    private long totalUsers;
    private long activeAds;
    private long totalPageViews;

    // Percentage changes (calculated or placeholder)
    private String newsChange;
    private String usersChange;
    private String adsChange;
    private String viewsChange;

    // Pending items
    private long pendingNews;
    private long pendingOpinions;
    private long flaggedComments;

    // Traffic Chart (Last 24h)
    private List<Integer> trafficData;
}
