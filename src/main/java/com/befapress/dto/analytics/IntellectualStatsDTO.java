package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntellectualStatsDTO {
    private Long authorId;
    private String authorName;
    private String profilePic;
    private long totalOpinions;
    private long totalViews;
    private long totalShares;
    private String topCategory;
}
