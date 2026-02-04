package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for recording page hits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageHitRequest {
    private String entityType; // "NEWS", "OPINION", "HOME"
    private Long entityId; // nullable for HOME
    private String sessionId;
    private String referrer;
    private String language; // "am" or "en"
}
