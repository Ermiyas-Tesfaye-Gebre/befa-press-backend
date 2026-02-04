package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatsDTO {
    private long newsPublishedToday;
    private long newsPublishedThisWeek;
    private long newsPublishedThisMonth;
    private long opinionsPublishedToday;
    private long opinionsPublishedThisWeek;
    private long opinionsPublishedThisMonth;
    private Map<String, Long> newsByCategory;
    private Map<String, Long> opinionsByCategory;
    private Map<String, Long> contentByLanguage;
}
