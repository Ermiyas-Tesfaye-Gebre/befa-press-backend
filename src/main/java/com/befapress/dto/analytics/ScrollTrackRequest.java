package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for updating scroll depth
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrollTrackRequest {
    private String entityType;
    private Long entityId;
    private String sessionId;
    private Integer scrollDepth; // 0-100
    private Integer timeOnPage; // seconds
}
