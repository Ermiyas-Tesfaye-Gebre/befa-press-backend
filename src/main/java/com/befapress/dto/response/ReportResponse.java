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
public class ReportResponse {
    private Long id;
    private Long commentId;
    private String commentContent;
    private String commentAuthor;

    // Context - which article
    private Long opinionId;
    private String opinionTitle;
    private Long newsId;
    private String newsTitle;

    // Report details
    private String reporterEmail;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
}
