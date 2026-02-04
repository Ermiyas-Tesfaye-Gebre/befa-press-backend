package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsListResponse {
    private Long id;
    private String title;
    private String titleAmharic;
    private String slug;
    private String excerpt;
    private String coverImage;
    private String authorName;
    private String categoryName;
    private String categorySlug;
    private Long categoryId;
    private String status;
    private int viewCount;
    private boolean isFeatured;
    private boolean isBreaking;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
