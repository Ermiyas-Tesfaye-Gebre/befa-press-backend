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
public class OpinionResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String coverImage;
    private AuthorResponse author;
    private String status;
    private int viewCount;
    private boolean isFeatured;
    private String metaTitle;
    private String metaDescription;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectionReason;
}
