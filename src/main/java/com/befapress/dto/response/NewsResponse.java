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
public class NewsResponse {
    private Long id;
    private String title;
    private String titleAmharic;
    private String slug;
    private String excerpt;
    private String content;
    private String coverImage;
    private AuthorResponse author;
    private CategoryResponse category;
    private String status;
    private int viewCount;
    private boolean isFeatured;
    private boolean isBreaking;
    private boolean isTrending;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long commentCount;
}
