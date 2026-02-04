package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportCommentRequest {
    @NotBlank(message = "Reason is required")
    private String reason; // INSULT, MOCKERY, THREAT, AD_HOMINEM, OFF_TOPIC, HATE_SPEECH, OTHER

    private String description; // Optional details
}
