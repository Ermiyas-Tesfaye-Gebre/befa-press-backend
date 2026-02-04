package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for live analytics updates via WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAnalyticsDTO {
    private Long activeUsersNow;
    private Long pageViewsLastMinute;
    private String topTrendingArticle;
    private Long topTrendingViews;
    private Map<String, Long> realtimeDevices;
    private LocalDateTime timestamp;
}
