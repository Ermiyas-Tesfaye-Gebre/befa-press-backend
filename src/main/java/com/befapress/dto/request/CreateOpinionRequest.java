package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpinionRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private String coverImage;

    private boolean isFeatured = false;

    private String metaTitle;

    private String metaDescription;

    private LocalDateTime scheduledAt;

    // Status: DRAFT, PENDING (for approval), PUBLISHED, REJECTED
    private String status = "DRAFT";
}
