package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewsRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private String titleAmharic;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private String coverImage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private boolean isFeatured = false;

    private boolean isBreaking = false;

    private String metaTitle;

    private String metaDescription;

    private String metaKeywords;

    private LocalDateTime scheduledAt;

    // Status: DRAFT, PENDING, PUBLISHED, ARCHIVED
    private String status = "DRAFT";
}
