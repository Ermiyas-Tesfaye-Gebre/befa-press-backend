package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for tracking social shares
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareTrackRequest {
    private String entityType; // "NEWS", "OPINION"
    private Long entityId;
    private String platform; // "FACEBOOK", "TWITTER", "LINKEDIN", "WHATSAPP", "TELEGRAM", "COPY_LINK"
    private String sessionId;
}
