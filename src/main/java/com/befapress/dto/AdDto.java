package com.befapress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdDto {
    private Long id;
    private String title;
    private String adType;
    private String placementZone;
    private String imageUrl;
    private String videoUrl;
    private String targetUrl;
    private String scriptContent;
    private String heading;
    private String description;
    private String ctaText;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Integer position;
    private Long views;
    private Long clicks;
}
