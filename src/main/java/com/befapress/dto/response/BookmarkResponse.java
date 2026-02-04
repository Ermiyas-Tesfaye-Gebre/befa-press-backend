package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkResponse {

    private Long id;
    private String type; // NEWS or OPINION
    private Long itemId;
    private String title;
    private String excerpt;
    private String slug;
    private String imageUrl;
    private String authorName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
