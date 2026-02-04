package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditorialStatsDTO {
    // News status breakdown
    private long newsDraft;
    private long newsPublished;
    private long newsPending;
    private long newsArchived;

    // Opinion status breakdown
    private long opinionsDraft;
    private long opinionsPublished;
    private long opinionsPending;
    private long opinionsRejected;

    // Totals
    private long totalNews;
    private long totalOpinions;
    private long totalContent;
}
